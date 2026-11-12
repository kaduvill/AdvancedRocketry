package zmaster587.advancedRocketry.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.util.Constants.NBT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Server-owned targets, independent of World and of the planet's generation biomes. */
public final class TerraformingBiomeState {
    private static final String NBT_KEY = "terraformingBiomes";

    private final List<BiomeEntry> biomes;
    private final boolean rivers;
    private final boolean invalidSavedEntries;
    private int ordinaryCount;
    // Revalidate after loading: registries, configuration and XML may have changed.
    private int climate = -1;
    private int limit;
    private boolean allowOtherMods;

    private TerraformingBiomeState(List<BiomeEntry> biomes, int ordinaryCount, boolean rivers, boolean invalid) {
        this.biomes = Collections.unmodifiableList(biomes);
        this.ordinaryCount = ordinaryCount;
        this.rivers = rivers;
        this.invalidSavedEntries = invalid;
    }

    public List<BiomeEntry> getBiomes() {
        return biomes;
    }

    public boolean hasRivers() {
        return rivers;
    }

    public boolean matchesClimate(int climate, int limit, boolean allowOtherMods) {
        return this.climate == climate && this.limit == limit && this.allowOtherMods == allowOtherMods;
    }

    /** Retain eligible entries and their weights; only choose replacements for available slots. */
    public static TerraformingBiomeState reconcile(@Nullable TerraformingBiomeState previous,
            List<Biome> eligible, List<Biome> highPressure, int limit, boolean rivers,
            int climate, boolean allowOtherMods) {
        int count = Math.min(Math.max(0, limit), eligible.size());
        List<BiomeEntry> next = new ArrayList<>(count + highPressure.size());
        Set<Biome> remaining = new HashSet<>(eligible);
        int retainedCount = 0;
        if (previous != null) {
            for (int i = 0; i < previous.ordinaryCount; i++) {
                BiomeEntry entry = previous.biomes.get(i);
                boolean keep = retainedCount < count && remaining.remove(entry.biome);
                if (keep) retainedCount++;
                if (count >= previous.ordinaryCount) {
                    // Replacing a hole in place avoids moving the surviving weighted slots.
                    next.add(keep ? entry : null);
                } else if (keep) {
                    next.add(entry);
                }
            }
        }

        List<Biome> available = new ArrayList<>();
        for (Biome biome : eligible) {
            if (remaining.contains(biome)) available.add(biome);
        }
        Random random = null;
        int availableIndex = 0;
        for (int i = 0; i < count; i++) {
            if (i == next.size()) next.add(null);
            if (next.get(i) != null) continue;
            Biome biome;
            if (eligible.size() > count) {
                if (random == null) random = new Random();
                int index = random.nextInt(available.size());
                biome = available.get(index);
                available.set(index, available.get(available.size() - 1));
                available.remove(available.size() - 1);
            } else {
                biome = available.get(availableIndex++);
            }
            next.set(i, entryFor(biome));
        }

        // These are mandatory additions outside the ordinary biome limit, as before.
        for (Biome biome : highPressure) {
            BiomeEntry entry = null;
            if (previous != null) {
                for (int i = previous.ordinaryCount; i < previous.biomes.size(); i++) {
                    if (previous.biomes.get(i).biome == biome) {
                        entry = previous.biomes.get(i);
                        break;
                    }
                }
            }
            next.add(entry == null ? entryFor(biome) : entry);
        }

        TerraformingBiomeState result;
        if (previous != null && !previous.invalidSavedEntries && previous.rivers == rivers
                && sameEntries(previous.biomes, next)) {
            result = previous;
            result.ordinaryCount = count;
        } else {
            result = new TerraformingBiomeState(next, count, rivers, false);
        }
        result.climate = climate;
        result.limit = limit;
        result.allowOtherMods = allowOtherMods;
        return result;
    }

    private static BiomeEntry entryFor(Biome biome) {
        for (BiomeManager.BiomeType type : BiomeManager.BiomeType.values()) {
            for (BiomeEntry entry : BiomeManager.getBiomes(type)) {
                if (entry.biome == biome) return new BiomeEntry(biome, entry.itemWeight);
            }
        }
        return new BiomeEntry(biome, 30);
    }

    private static boolean sameEntries(List<BiomeEntry> first, List<BiomeEntry> second) {
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).biome != second.get(i).biome
                    || first.get(i).itemWeight != second.get(i).itemWeight) return false;
        }
        return true;
    }

    public void writeToNBT(NBTTagCompound root) {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();
        for (BiomeEntry biome : biomes) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("biome", biome.biome.getRegistryName().toString());
            entry.setInteger("weight", biome.itemWeight);
            entries.appendTag(entry);
        }
        tag.setTag("biomes", entries);
        tag.setInteger("ordinaryCount", ordinaryCount);
        tag.setBoolean("rivers", rivers);
        if (invalidSavedEntries) tag.setBoolean("invalidTargets", true);
        root.setTag(NBT_KEY, tag);
    }

    @Nullable
    public static TerraformingBiomeState readFromNBT(NBTTagCompound root) {
        if (!root.hasKey(NBT_KEY, NBT.TAG_COMPOUND)) return null;
        NBTTagCompound tag = root.getCompoundTag(NBT_KEY);
        NBTTagList entries = tag.getTagList("biomes", NBT.TAG_COMPOUND);
        List<BiomeEntry> biomes = new ArrayList<>(entries.tagCount());
        int savedCount = tag.getInteger("ordinaryCount");
        boolean invalid = tag.getBoolean("invalidTargets") || !tag.hasKey("biomes", NBT.TAG_LIST)
                || !tag.hasKey("ordinaryCount", NBT.TAG_INT) || !tag.hasKey("rivers")
                || savedCount < 0 || savedCount > entries.tagCount();
        int ordinaryCount = 0;
        long totalWeight = 0;
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            try {
                ResourceLocation name = new ResourceLocation(entry.getString("biome"));
                int weight = entry.getInteger("weight");
                if (!Biome.REGISTRY.containsKey(name) || !entry.hasKey("weight", NBT.TAG_INT)
                        || weight < 0 || totalWeight + weight > Integer.MAX_VALUE) {
                    invalid = true;
                    continue;
                }
                biomes.add(new BiomeEntry(Biome.REGISTRY.getObject(name), weight));
                totalWeight += weight;
                if (i < savedCount) ordinaryCount++;
            } catch (IllegalArgumentException e) {
                invalid = true;
            }
        }
        if (!biomes.isEmpty() && totalWeight == 0) {
            biomes.clear();
            ordinaryCount = 0;
            invalid = true;
        }
        return new TerraformingBiomeState(biomes, ordinaryCount, tag.getBoolean("rivers"), invalid);
    }
}
