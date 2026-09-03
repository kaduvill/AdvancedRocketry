package zmaster587.advancedRocketry.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.dimension.DimensionProperties;

import java.util.ArrayList;
import java.util.List;

public final class LaserDrillOreTable {

    private LaserDrillOreTable() {
    }

    public static List<ItemStack> getEffectiveOres(DimensionProperties properties) {
        List<ItemStack> globalOres = properties != null && properties.laserDrillOresReplace
                ? null
                : buildGlobalOres();
        return getEffectiveOres(properties, globalOres);
    }

    public static List<ItemStack> getEffectiveOres(DimensionProperties properties, List<ItemStack> globalOres) {
        List<ItemStack> ores = new ArrayList<>();

        if ((properties == null || !properties.laserDrillOresReplace) && globalOres != null) {
            ores.addAll(globalOres);
        }

        if (properties != null && properties.laserDrillOres != null) {
            for (ItemStack stack : properties.laserDrillOres) {
                if (stack != null && !stack.isEmpty() && !containsOreEntry(ores, stack)) {
                    ores.add(stack.copy());
                }
            }
        }

        return ores;
    }

    public static boolean sameOreTable(List<ItemStack> first, List<ItemStack> second) {
        if (first == second) return true;
        if (first == null || second == null || first.size() != second.size()) return false;

        boolean[] matched = new boolean[second.size()];

        for (ItemStack stack : first) {
            boolean found = false;

            for (int i = 0; i < second.size(); i++) {
                ItemStack candidate = second.get(i);
                boolean bothEmpty = stack != null && candidate != null && stack.isEmpty() && candidate.isEmpty();

                if (!matched[i] && (bothEmpty || sameOreEntry(stack, candidate))) {
                    matched[i] = true;
                    found = true;
                    break;
                }
            }

            if (!found) return false;
        }

        return true;
    }

    private static List<ItemStack> buildGlobalOres() {
        List<ItemStack> ores = new ArrayList<>();
        List<String> configOres = ARConfiguration.getCurrentConfig().standardLaserDrillOres;

        if (configOres == null) return ores;

        for (String oreDictName : configOres) {
            if (oreDictName == null || oreDictName.isEmpty()) continue;

            String[] args = oreDictName.split(":");
            List<ItemStack> globalOres = OreDictionary.getOres(args[0]);

            if (globalOres != null && !globalOres.isEmpty()) {
                int amount = 1;

                if (args.length > 1) {
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }

                ItemStack base = globalOres.get(0);
                ores.add(new ItemStack(base.getItem(), amount, base.getItemDamage()));
                continue;
            }

            String name;

            try {
                name = args[0] + ":" + args[1];
            } catch (IndexOutOfBoundsException e) {
                AdvancedRocketry.logger.warn("Unexpected ore name: \"{}\" during laser drill harvesting", oreDictName);
                continue;
            }

            int metadata = 0;
            int size = 1;

            if (args.length > 2) {
                try {
                    metadata = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                }
            }

            if (args.length > 3) {
                try {
                    size = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {
                }
            }

            ItemStack stack = ItemStack.EMPTY;
            Block block = Block.getBlockFromName(name);

            if (block == null) {
                Item item = Item.getByNameOrId(name);

                if (item != null) {
                    stack = new ItemStack(item, size, metadata);
                }
            } else {
                stack = new ItemStack(block, size, metadata);
            }

            if (!stack.isEmpty()) {
                ores.add(stack);
            }
        }

        return ores;
    }

    private static boolean containsOreEntry(List<ItemStack> ores, ItemStack stack) {
        for (ItemStack existing : ores) {
            if (sameOreEntry(existing, stack)) return true;
        }

        return false;
    }

    private static boolean sameOreEntry(ItemStack first, ItemStack second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) return false;
        if (first.getItem() != second.getItem()) return false;
        if (first.getMetadata() != second.getMetadata()) return false;
        if (first.getCount() != second.getCount()) return false;

        if (first.getTagCompound() == null) {
            return second.getTagCompound() == null;
        }

        return first.getTagCompound().equals(second.getTagCompound());
    }
}