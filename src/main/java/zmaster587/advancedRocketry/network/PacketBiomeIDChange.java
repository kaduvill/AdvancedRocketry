package zmaster587.advancedRocketry.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.libVulpes.network.BasePacket;
import zmaster587.libVulpes.util.HashedBlockPosition;

public class PacketBiomeIDChange extends BasePacket {

    Chunk chunk;
    int worldId, xPos, zPos;
    byte[] array;
    HashedBlockPosition pos;

    public PacketBiomeIDChange() {
        array = new byte[256];
        pos = new HashedBlockPosition(0, 0, 0);
    }

    public PacketBiomeIDChange(Chunk chunk, World world, HashedBlockPosition pos) {
        this.chunk = chunk;
        this.pos = pos;
        worldId = world.provider.getDimension();
    }

    @Override
    public void write(ByteBuf out) {
        out.writeInt(worldId);
        out.writeInt(chunk.x);
        out.writeInt(chunk.z);
        out.writeInt(pos.x);
        out.writeShort(pos.y);
        out.writeInt(pos.z);
        out.writeBytes(chunk.getBiomeArray());
    }

    @Override
    public void readClient(ByteBuf in) {
        worldId = in.readInt();
        xPos = in.readInt();
        zPos = in.readInt();

        pos.x = in.readInt();
        pos.y = in.readShort();
        pos.z = in.readInt();

        in.readBytes(array);
    }

    @Override
    public void read(ByteBuf in) {
        // One way transmission
    }

    @Override
    public void executeClient(EntityPlayer thePlayer) {
        // The player captured when scheduling this packet may precede a respawn.
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world == null || world.provider.getDimension() != worldId) return;
        chunk = world.getChunkProvider().getLoadedChunk(xPos, zPos);
        if (chunk == null || !chunk.isLoaded()) return;

        // Use biome lookups, not the byte array: REID stores extended IDs separately.
        BiomeProvider provider = world.getBiomeProvider();
        Biome[] previous = new Biome[256];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = xPos << 4, baseZ = zPos << 4;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                previous[z * 16 + x] = chunk.getBiome(cursor.setPos(baseX + x, 0, baseZ + z), provider);
            }
        }

        chunk.setBiomeArray(array); // Keep this call in executeClient for REID's client mixin.
        int minX = 16, minZ = 16, maxX = -1, maxZ = -1;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (previous[z * 16 + x] != chunk.getBiome(cursor.setPos(baseX + x, 0, baseZ + z), provider)) {
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        if (maxX >= 0) {
            // RenderGlobal adds the neighboring block margin used for biome tint blending.
            world.markBlockRangeForRenderUpdate(baseX + minX, 0, baseZ + minZ,
                    baseX + maxX, world.getHeight() - 1, baseZ + maxZ);
        }
        if (minecraft.player != null && minecraft.gameSettings.particleSetting < 2)
            AdvancedRocketry.proxy.spawnParticle("smallLazer", world, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    @Override
    public void executeServer(EntityPlayerMP player) {
        // How about no

    }

}
