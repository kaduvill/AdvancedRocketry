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
        work.addLast(new ChunkWork(x, z));
        columns += 256;
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
        long index = ThreadLocalRandom.current().nextLong(columns);
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
        // Only random pulls need a mask. The normal FIFO consumer uses just the cursor.
        private BitSet randomColumns;

        private ChunkWork(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public int remaining() { return remaining; }
        public boolean isInChunk(int x, int z) { return this.x == x && this.z == z; }

        public BlockPos take(int index) {
            int column;
            if (randomColumns == null && index == 0) {
                column = nextColumn++;
            } else {
                if (randomColumns == null) {
                    randomColumns = new BitSet(256);
                    randomColumns.set(nextColumn, 256);
                }
                column = randomColumns.nextSetBit(0);
                for (int i = 0; i < index; i++) column = randomColumns.nextSetBit(column + 1);
                randomColumns.clear(column);
            }
            remaining--;
            return new BlockPos((x << 4) + (column >> 4), 0, (z << 4) + (column & 15));
        }
    }
}
