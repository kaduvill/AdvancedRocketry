package zmaster587.advancedRocketry.util;

import io.netty.channel.ChannelFutureListener;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.network.PacketBiomeIDChange;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

/** A single producer update's changes; never retained between ticks or worlds. */
public final class BiomeUpdateBatch implements AutoCloseable {
    private final World world;
    private Long2ObjectOpenHashMap<BlockPos> changes;

    public BiomeUpdateBatch(World world) {
        this.world = world;
    }

    public void add(BlockPos pos) {
        if (changes == null) changes = new Long2ObjectOpenHashMap<>(2);
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        if (!changes.containsKey(key)) changes.put(key, pos.toImmutable());
    }

    @Override
    public void close() {
        if (changes == null) return;
        Long2ObjectOpenHashMap<BlockPos> pending = changes;
        changes = null;
        for (BlockPos pos : pending.values()) send(world, pos);
    }

    /** Lookup only: neither create tracking entries nor load chunks to send updates. */
    public static void send(World world, BlockPos pos) {
        if (!(world instanceof WorldServer) || world.isRemote
                || net.minecraftforge.common.DimensionManager.getWorld(world.provider.getDimension()) != world) return;

        PlayerChunkMapEntry entry = ((WorldServer) world).getPlayerChunkMap()
                .getEntry(pos.getX() >> 4, pos.getZ() >> 4);
        if (entry == null || entry.getWatchingPlayers().isEmpty()) return;
        Chunk chunk = entry.getChunk();
        if (chunk == null) return;

        // Keep the packet's serialization intact for REID. Forge encodes once,
        // then distributes the payload to players who have received this chunk.
        FMLEmbeddedChannel channel = PacketHandler.channels.get(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET)
                .set(FMLOutboundHandler.OutboundTarget.TRACKING_POINT);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
                .set(new NetworkRegistry.TargetPoint(world.provider.getDimension(),
                        pos.getX(), pos.getY(), pos.getZ(), 0));
        channel.writeAndFlush(new PacketBiomeIDChange(chunk, world, new HashedBlockPosition(pos)))
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
