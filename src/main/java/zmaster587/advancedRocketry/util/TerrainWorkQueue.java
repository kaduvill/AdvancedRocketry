package zmaster587.advancedRocketry.util;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

/** Initial chunk passes stay compact; individual retries retain their place in the same FIFO. */
final class TerrainWorkQueue {
    private final Deque<Work> work = new ArrayDeque<>();
    private long columns;

    void addChunk(int x, int z) {
        addChunk(x, z, null);
    }

    void addChunk(int x, int z, boolean[][] generated) {
        ChunkWork chunk = new ChunkWork(x, z, generated);
        if (chunk.remaining() == 0) return;
        work.addLast(chunk);
        columns += chunk.remaining();
    }

    void addColumn(BlockPos pos) {
        work.addLast(new ColumnWork(pos.getX(), pos.getY(), pos.getZ()));
        columns++;
    }

    boolean isEmpty() {
        return work.isEmpty();
    }

    boolean hasWorkInChunk(int x, int z) {
        for (Work entry : work) {
            if (entry.isInChunk(x, z)) return true;
        }
        return false;
    }

    BlockPos remove(boolean random) {
        if (work.isEmpty()) return null;
        if (!random) {
            Work first = work.getFirst();
            int remaining = first.remaining();
            BlockPos result = first.take(0);
            if (remaining == 1) work.removeFirst();
            columns--;
            return result;
        }

        // Preserve uniform selection over logical columns, rather than over chunk jobs.
        return removeAt(ThreadLocalRandom.current().nextLong(columns));
    }

    BlockPos removeRandomFromFirst(int limit) {
        if (work.isEmpty()) return null;
        return removeAt(ThreadLocalRandom.current().nextInt((int) Math.min(columns, limit)));
    }

    private BlockPos removeAt(long index) {
        Iterator<Work> iterator = work.iterator();
        while (iterator.hasNext()) {
            Work entry = iterator.next();
            int remaining = entry.remaining();
            if (index < remaining) {
                BlockPos result = entry.take((int) index);
                if (remaining == 1) iterator.remove();
                columns--;
                return result;
            }
            index -= remaining;
        }
        throw new IllegalStateException("Terrain queue column count is inconsistent");
    }

    private interface Work {
        int remaining();
        BlockPos take(int index);
        boolean isInChunk(int x, int z);
    }

    private static final class ColumnWork implements Work {
        private final int x, y, z;

        private ColumnWork(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int remaining() { return 1; }
        public BlockPos take(int index) { return new BlockPos(x, y, z); }
        public boolean isInChunk(int x, int z) { return this.x >> 4 == x && this.z >> 4 == z; }
    }

    private static final class ChunkWork implements Work {
        private final int x, z;
        private int nextColumn;
        private int remaining = 256;
        // Filtered chunks and out-of-order pulls need a mask. Full FIFO passes use the cursor.
        private BitSet randomColumns;

        private ChunkWork(int x, int z, boolean[][] generated) {
            this.x = x;
            this.z = z;
            if (generated == null) return;
            for (int column = 0; column < 256; column++) {
                if (generated[column >> 4][column & 15]) {
                    pendingColumns().clear(column);
                    remaining--;
                }
            }
        }

        private BitSet pendingColumns() {
            if (randomColumns == null) {
                randomColumns = new BitSet(256);
                randomColumns.set(nextColumn, 256);
            }
            return randomColumns;
        }

        public int remaining() { return remaining; }
        public boolean isInChunk(int x, int z) { return this.x == x && this.z == z; }

        public BlockPos take(int index) {
            int column;
            if (randomColumns == null && index == 0) {
                column = nextColumn++;
            } else {
                column = pendingColumns().nextSetBit(0);
                for (int i = 0; i < index; i++) column = randomColumns.nextSetBit(column + 1);
                randomColumns.clear(column);
            }
            remaining--;
            return new BlockPos((x << 4) + (column >> 4), 0, (z << 4) + (column & 15));
        }
    }
}
