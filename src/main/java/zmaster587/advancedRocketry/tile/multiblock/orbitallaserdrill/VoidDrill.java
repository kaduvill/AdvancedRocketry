package zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.util.LaserDrillOreTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This drill is used if the laserDrillPlanet config option is disabled. It simply conjures ores from nowhere
 */
class VoidDrill extends AbstractDrill {

    private final Random random = new Random();
    private final List<ItemStack> ores = new ArrayList<>();
    private boolean voidCobble; // performance optimization: if true, cobble is not even generated
    private int opCounter = 0; // counts operations when voidCobble is true
    private static final ItemStack[] EMPTY = new ItemStack[0];
    private int sourceDimId = Integer.MIN_VALUE;
    private int cachedDimId = Integer.MIN_VALUE;
    private boolean oreCacheValid = false;


    VoidDrill() {
    }

    void setVoidCobble(boolean voidCobble) {
        this.voidCobble = voidCobble;
    }
    void setSourceDimId(int dimId) {
        if (this.sourceDimId != dimId) {
            this.sourceDimId = dimId;
            if (oreCacheValid && dimId != Integer.MIN_VALUE) {
                rebuildOreList(dimId);
            } else {
                oreCacheValid = false;
            }
        }
    }
    private void rebuildOreList(int dimId) {
        DimensionProperties properties = dimId == Integer.MIN_VALUE
                ? null
                : DimensionManager.getInstance().getDimensionProperties(dimId);

        ores.clear();
        ores.addAll(LaserDrillOreTable.getEffectiveOres(properties));
        cachedDimId = dimId;
        oreCacheValid = true;
    }

    @Override
    ItemStack[] performOperation() {

        // --- VOID-COBBLE MODE: only ores, every 10th operation ---
        if (voidCobble) {
            if (ores.isEmpty()) {
                // No configured ores -> nothing to give
                return EMPTY;
            }

            opCounter++;
            // 9 out of 10 operations: no items at all
            if (opCounter % 10 != 0) {
                return EMPTY;
            }

            // 10th operation: roll one ore stack
            ItemStack[] result = new ItemStack[1];
            ItemStack template = ores.get(random.nextInt(ores.size()));
            result[0] = template.copy();
            return result;
        }

        // --- NORMAL MODE: 10% ore, 90% cobble (old behavior) ---

        // 10% ore
        boolean produceOre = !ores.isEmpty() && random.nextInt(10) == 0;

        if (produceOre) {
            ItemStack[] result = new ItemStack[1];
            ItemStack template = ores.get(random.nextInt(ores.size()));
            result[0] = template.copy();
            return result;
        }

        // Cobble case
        ItemStack[] result = new ItemStack[1];
        result[0] = new ItemStack(Blocks.COBBLESTONE, 1);
        return result;
    }

    void clearOreCache() {
        ores.clear();
        sourceDimId = Integer.MIN_VALUE;
        cachedDimId = Integer.MIN_VALUE;
        oreCacheValid = false;
        opCounter = 0;
    }

    @Override
    boolean activate(World world, int x, int z) {
        opCounter = 0;

        int dimId = Integer.MIN_VALUE;
        if (world != null) {
            dimId = world.provider.getDimension();
        } else if (sourceDimId != Integer.MIN_VALUE) {
            dimId = sourceDimId;
        }

        if (!oreCacheValid || cachedDimId != dimId) {
            rebuildOreList(dimId);
        }

        return true;
    }


    @Override
    void deactivate() {
        // No state required
    }

    @Override
    boolean isFinished() {
        return false;
    }

    @Override
    boolean needsRestart() {
        return false;
    }
}
