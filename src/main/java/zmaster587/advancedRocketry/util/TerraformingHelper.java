package zmaster587.advancedRocketry.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeManager;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.advancedRocketry.world.ChunkProviderPlanet;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

enum TerraformingType{
    ALLOWED, BORDER, PROTECTED
}

public class TerraformingHelper {
    public BiomeProvider chunkMgrTerraformed;
    public int dimId;
    public List<BiomeManager.BiomeEntry> biomeList;
    // Only the lazily created terrain generator owns a World. Targets and progress
    // survive dimension unload; these two cache fields must not.
    public World world;
    public ChunkProviderPlanet generator;
    private DimensionProperties props;

    private Map<ChunkPos, chunkdata> chunkDataMap = new HashMap<>();
    public final List<ChunkPos> registeredChunks = new ArrayList<>();


    // A block is placed in queue if (OR)
    // - Initial block placements - both only set blocks if the terrain at the BlockPos is not fully generated in the "real" world
    //      - The TerraformingHelper is created ( causes all loaded chunks to place their blocks here, from DimProps )
    //      - A new chunk is loaded ( called from PlanetEventHandler )
    // - After the BiomeHandler has made a change at a BlockPos
    //      - For type ALLOWED: if the terrain heigt does not match the target, re-add the block to the queue
    //      - For type BORDER: if BiomeHandler has made a change of the height at BlockPos re-add to queue
    //      - For type BORDER: if a type.ALLOWED has changed its height it will add blocks next to it (if type==BORDER) to the queue
    //      - For type PROTECTED: the block is never re-added to queue because it will change its biome only once
    // - When a protecting Block has been removed and chunk status is re-calculated
    //   add every chunk that was PROTECTED and no longer is PROTECTED to the queue for terraforming.
    //   doesn't matter if it is type ALLOWED or type BORDER
    private TerrainWorkQueue terraformingqueue;
    // Snapshot mutable inputs on enqueue so queued positions can be returned directly.
    private List<BlockPos> decorationqueue;
    private TerrainWorkQueue biomechangingqueue;

    int safe_zone_radius = 3; // radius for protected zone
    int border_zone = 3; // border zone size

    public boolean has_blocks_in_tf_queue(){
        return !terraformingqueue.isEmpty();
    }
    public boolean has_blocks_in_dec_queue(){
        return !decorationqueue.isEmpty();
    }

    public TerraformingHelper(int dimension, List<BiomeManager.BiomeEntry> biomes, HashSet<ChunkPos> generated_chunks, HashSet<ChunkPos> biomechanged_chunks){
        this(dimension, biomes, generated_chunks, biomechanged_chunks,
                net.minecraftforge.common.DimensionManager.getWorld(dimension),
                DimensionManager.getInstance().getDimensionProperties(dimension).hasRivers());
    }

    public TerraformingHelper(int dimension, List<BiomeManager.BiomeEntry> biomes,
            Set<ChunkPos> generated_chunks, Set<ChunkPos> biomechanged_chunks, World world, boolean rivers) {
        this.dimId = dimension;
        this.props = DimensionManager.getInstance().getDimensionProperties(dimension);
        this.biomeList = biomes;
        this.chunkMgrTerraformed = new ChunkManagerPlanet(world, world.getWorldInfo().getGeneratorOptions(), biomeList, rivers);
        this.terraformingqueue = new TerrainWorkQueue();
        this.biomechangingqueue = new TerrainWorkQueue();
        this.decorationqueue = new ArrayList<>();
        chunkDataMap = new HashMap<>();

        for (ChunkPos i:generated_chunks){
            chunkdata data = new chunkdata(i.x,i.z,null, this);
            data.chunk_fully_generated = true;
            chunkDataMap.put(new ChunkPos(data.x,data.z), data);
        }
        for (ChunkPos i:biomechanged_chunks){
            generate_new_chunkdata(new ChunkPos(i.x, i.z));
            chunkdata data = getChunkFromList(i.x,i.z);
            data.chunk_fully_biomechanged = true;
        }
        recalculate_chunk_status();
    }

    //0 = no
    //1 = yes
    // -1 = never (if it includes a type.PROTECTED chunk
    public int can_populate(int x, int z){
        return can_populate(net.minecraftforge.common.DimensionManager.getWorld(dimId), x, z);
    }

    boolean isCurrentWorld(World world) {
        return world != null && !world.isRemote
                && net.minecraftforge.common.DimensionManager.getWorld(dimId) == world;
    }

    /** Release only resources belonging to this World, without resetting any work. */
    public void releaseWorldResources(World unloadedWorld) {
        if (world == unloadedWorld) {
            generator = null;
            world = null;
        }
    }

    public int can_populate(World world, int x, int z){
        if (!isCurrentWorld(world)) return 0;

        chunkdata currentchunk = getChunkFromList(x,z);
        chunkdata currentchunkx1 = getChunkFromList(x+1,z);
        chunkdata currentchunkz1 = getChunkFromList(x,z+1);
        chunkdata currentchunkx1z1 = getChunkFromList(x+1,z+1);

        if (currentchunk != null && currentchunkz1 != null && currentchunkx1 != null && currentchunkx1z1 != null){

            if (currentchunk.type == TerraformingType.PROTECTED || currentchunkz1.type == TerraformingType.PROTECTED ||currentchunkx1.type == TerraformingType.PROTECTED ||currentchunkx1z1.type == TerraformingType.PROTECTED)
                return -1; // chunks contain a protected chunk

            if (currentchunkz1.terrain_fully_generated && currentchunkx1.terrain_fully_generated && currentchunkx1z1.terrain_fully_generated && currentchunk.terrain_fully_generated) {
                currentchunk.populate_chunk_if_not_already_done(world);
                return 1;
            }
        }
        return 0;
    }

    /*
    used to add BORDER type block positions to queue for updating again
     */
    public void register_height_change_actual(BlockPos pos){
        ChunkPos cpos = getChunkPosFromBlockPos(pos);
        chunkdata data= getChunkFromList(cpos.x,cpos.z);
        if (data !=null  && data.type == TerraformingType.BORDER) {
            add_position_to_queue(pos);
            //System.out.println("add position to queue again");
        }
    }
    public void register_height_change(BlockPos pos){
        register_height_change_actual(pos.add(1,0,0));
        register_height_change_actual(pos.add(-1,0,0));
        register_height_change_actual(pos.add(0,0,1));
        register_height_change_actual(pos.add(0,0,-1));
    }


    /*
     When a chunk is fully terrain-generated it will call this method to update border chunks next to it.
     A Border chunk is considered fully generated when every type.ALLOWED chunks next to it are fully generated.
     This is because a fully terrain generated chunk will no longer change its heightmap so it will not modify the heightmap of the border chunk next to it
     */
    public synchronized void check_next_border_chunk_fully_generated(int px, int pz) {

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                chunkdata data = getChunkFromList(px + x, pz + z);
                if (data == null)continue;
                if (data != null && !data.terrain_fully_generated && data.type == TerraformingType.BORDER) {
                    if (terraformingqueue.hasWorkInChunk(data.x, data.z)) return;


                    for (int x2 = -1; x2 <= 1; x2++) {
                        for (int z2 = -1; z2 <= 1; z2++) {
                            chunkdata data2 = getChunkFromList(px + x + x2, pz + z + z2);
                            if (data2 == null) return;
                            if (data2.type == TerraformingType.ALLOWED) {
                                if (!data2.terrain_fully_generated) {
                                    return;
                                }
                            }
                        }
                    }
                    data.terrain_fully_generated = true;
                    data.blockStates = null; // no longer needed, gc should collect them now - actually, these are never needed but who cares...
                    check_next_border_chunk_fully_generated(data.x, data.z); // update border chunks next to this one to check if they can decorate
                    check_can_decorate(data.x, data.z);

                }
            }
        }
    }
    public void check_can_decorate(int px, int pz){
        // for every chunk next to this one, check if it can decorate (except for if it is already decorated)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (getChunkFromList(px+x,pz+z) != null && !getChunkFromList(px+x,pz+z).chunk_fully_generated){
                    if (can_populate(px+x,pz+z) != 0){
                        //re-add all position to queue for decoration
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                add_position_to_decoration_queue(new BlockPos((px+x)*16+bx, 0, (pz+z)*16+bz));
                            }
                        }
                    }
                }
            }
        }

    }
    public ChunkPos getChunkPosFromBlockPos(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }



    public TerraformingType get_chunk_type(int x, int z) {

        TerraformingType type = TerraformingType.ALLOWED;
        for (BlockPos i : DimensionProperties.proxylists.getProtectingBlocksForDimension(props.getId())) {
            //System.out.println("found protecting block at "+i.getX()+":"+i.getY()+":"+i.getZ());
            ChunkPos cpos = getChunkPosFromBlockPos(i);
            int dx = cpos.x - x;
            int dz = cpos.z - z;
            if (Math.abs(dx) <= safe_zone_radius && Math.abs(dz) <= safe_zone_radius)
                return TerraformingType.PROTECTED;
            else if (Math.abs(dx) <= safe_zone_radius + border_zone && Math.abs(dz) <= safe_zone_radius + border_zone) {
                type = TerraformingType.BORDER;
            }
        }
        return type;
    }

    public void recalculate_chunk_status() {
        Iterator<Map.Entry<ChunkPos, chunkdata>> iterator = chunkDataMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, chunkdata> entry = iterator.next();
            chunkdata data = entry.getValue();
            TerraformingType prevtype = data.type;
            data.type = get_chunk_type(data.x, data.z);
            if (prevtype != TerraformingType.ALLOWED)
                if (data.type == TerraformingType.ALLOWED || data.type == TerraformingType.BORDER)
                    if (data.terrain_fully_generated)
                        iterator.remove();

        }
    }

    public chunkdata getChunkFromList(int x, int z){
        ChunkPos key = new ChunkPos(x,z);
        return chunkDataMap.get(key);
    }

    public synchronized void add_position_to_queue(BlockPos p){
        //System.out.println("add position: "+p.getX()+":"+p.getZ());
        if (p == null){
            System.out.print("ERROR POSITION IS NULL");
            return;
        }

        //if on protected chunk, instantly add to decoration queue as it will not work protected chunks
        ChunkPos cp = getChunkPosFromBlockPos(p);
        if(get_chunk_type(cp.x,cp.z) == TerraformingType.PROTECTED){
            int inchunkx = ((p.getX() % 16) + 16) % 16;
            int inchunkz = ((p.getZ() % 16) + 16) % 16;
            getChunkFromList(cp.x, cp.z).set_position_fully_generated(inchunkx, inchunkz);
        }else {
            terraformingqueue.addColumn(p);
        }
    }

    /** Queue an initial pass without allocating its 256 column positions. */
    public synchronized void add_chunk_to_queue(ChunkPos pos) {
        if (get_chunk_type(pos.x, pos.z) == TerraformingType.PROTECTED) {
            chunkdata data = getChunkFromList(pos.x, pos.z);
            // Preserve the protected-area completion and neighbour notifications.
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) data.set_position_fully_generated(x, z);
            }
        } else {
            terraformingqueue.addChunk(pos.x, pos.z);
        }
    }

    public synchronized void add_position_to_biomechanging_queue(BlockPos p){
        //System.out.println("add position: "+p.getX()+":"+p.getZ());
        if (p == null){
            System.out.print("ERROR POSITION IS NULL");
            return;
        }
        biomechangingqueue.addColumn(p);
    }

    public synchronized void add_chunk_to_biomechanging_queue(int x, int z, boolean[][] generated) {
        biomechangingqueue.addChunk(x, z, generated);
    }

    public synchronized void add_position_to_decoration_queue(BlockPos p){
        //System.out.println("add position: "+p.getX()+":"+p.getZ());
        if (p == null){
            System.out.print("ERROR POSITION IS NULL");
            return;
        }
        int insertionIndex = 0;
        if (decorationqueue.size() > 0) {
            insertionIndex = ThreadLocalRandom.current().nextInt(Math.min(decorationqueue.size(), 1000));
        }
        decorationqueue.add(insertionIndex, p.toImmutable());
    }


    public synchronized BlockPos get_next_position(boolean random){
        return terraformingqueue.remove(random);
    }
    public synchronized BlockPos get_next_position_biomechanging(boolean random) {
        return random ? biomechangingqueue.removeRandomFromFirst(8192) : biomechangingqueue.remove(false);
    }

    public synchronized BlockPos get_next_position_decoration(boolean random){
        if (decorationqueue.isEmpty())
            return null;
        int index = 0;
        if (random)
            index = ThreadLocalRandom.current().nextInt(decorationqueue.size());

        return decorationqueue.remove(index);
    }

    public void set_chunk_biomechanged(ChunkPos pos){ // mark a chunk ready for terraforming
        if (DimensionProperties.proxylists.getChunksFullyBiomeChanged(props.getId()).add(pos)) {
            props.add_chunk_to_terraforming_list_but_this_time_real_terraforming_and_not_biomechanging(pos);
        }

    }
    public void generate_new_chunkdata(ChunkPos cpos){
        chunkdata data;
        data = new chunkdata(cpos.x,cpos.z, null, this);
        data.type = get_chunk_type(data.x,data.z);
        chunkDataMap.put(new ChunkPos(data.x,data.z),data);
    }

    public IBlockState[] getBlocksAt(int x, int z){
        return getBlocksAt(net.minecraftforge.common.DimensionManager.getWorld(dimId), x, z);
    }

    public IBlockState[] getBlocksAt(World currentWorld, int x, int z){
        if (!isCurrentWorld(currentWorld)) return null;
        if (world != currentWorld) {
            generator = null;
            world = null;
        }
        ChunkPos cpos = getChunkPosFromBlockPos(new BlockPos(x,0,z));
        chunkdata data = getChunkFromList(cpos.x,cpos.z);
        if (data == null){
            generate_new_chunkdata(new ChunkPos(cpos.x, cpos.z));
            data = getChunkFromList(cpos.x,cpos.z);
        }
        if (data.blockStates == null){
            if (generator == null) {
                generator = new ChunkProviderPlanet(currentWorld, currentWorld.getSeed(),
                        currentWorld.getWorldInfo().isMapFeaturesEnabled(), currentWorld.getWorldInfo().getGeneratorOptions());
                world = currentWorld;
            }
            ChunkPrimer primer = generator.getChunkPrimer(cpos.x, cpos.z, chunkMgrTerraformed);
            IBlockState[][][] blockStates = new IBlockState[16][16][256];
            for (int px = 0; px < 16; px++) {
                for (int pz = 0; pz < 16; pz++) {
                    for (int py = 0; py < 256; py++) {
                        blockStates[px][pz][py] = primer.getBlockState(px,py,pz);
                    }
                }
            }
            data.blockStates = blockStates;
        }
        int chunkx = ((x % 16) + 16) % 16;
        int chunkz = ((z % 16) + 16) % 16;

        if (data.blockStates == null)
            return null;

        return data.blockStates[chunkx][chunkz];
    }

    public void setChunkFullyGenerated(int x, int z) {
        getChunkFromList(x,z).chunk_fully_generated = true;
        DimensionProperties.proxylists.getChunksFullyTerraformed(props.getId()).add(new ChunkPos(x,z));
    }
}