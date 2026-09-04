package zmaster587.advancedRocketry.integration.jei.orbitalLaserDrill;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.util.LaserDrillOreTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class OrbitalLaserDrillRecipeMaker {

    public static List<OrbitalLaserDrillWrapper> getRecipes(int pageSize) {
        List<OrbitalLaserDrillWrapper> recipes = new ArrayList<>();
        ARConfiguration config = ARConfiguration.getCurrentConfig();

        if (!config.enableLaserDrill || config.laserDrillPlanet || pageSize < 1) {
            return recipes;
        }

        DimensionManager manager = DimensionManager.getInstance();
        if (manager == null) return recipes;

        Integer[] dimensionIds = manager.getRegisteredDimensions();
        if (dimensionIds == null || dimensionIds.length == 0) return recipes;

        Arrays.sort(dimensionIds, new Comparator<Integer>() {
            @Override
            public int compare(Integer first, Integer second) {
                DimensionProperties firstProperties = manager.getDimensionProperties(first);
                DimensionProperties secondProperties = manager.getDimensionProperties(second);
                String firstName = firstProperties != null && firstProperties.getName() != null
                        ? firstProperties.getName()
                        : "";
                String secondName = secondProperties != null && secondProperties.getName() != null
                        ? secondProperties.getName()
                        : "";

                int byName = String.CASE_INSENSITIVE_ORDER.compare(firstName, secondName);
                return byName != 0 ? byName : Integer.compare(first, second);
            }
        });

        List<ItemStack> baseline = LaserDrillOreTable.getEffectiveOres(null);
        List<OrbitalLaserDrillWrapper> planetRecipes = new ArrayList<>();
        int baselinePlanetCount = 0;
        boolean hasDifferentPlanet = false;

        for (Integer dimensionId : dimensionIds) {
            if (dimensionId == null) continue;

            DimensionProperties properties = manager.getDimensionProperties(dimensionId);

            if (properties == null || !manager.canTravelTo(dimensionId)) continue;
            if (config.laserBlackListDims != null && config.laserBlackListDims.contains(dimensionId)) continue;

            List<ItemStack> effective = LaserDrillOreTable.getEffectiveOres(properties, baseline);

            if (LaserDrillOreTable.sameOreTable(baseline, effective)) {
                baselinePlanetCount++;
                continue;
            }

            hasDifferentPlanet = true;

            if (effective.isEmpty()) continue;

            String planetName = properties.getName() == null || properties.getName().isEmpty()
                    ? "DIM " + dimensionId
                    : properties.getName();

            StellarBody star = properties.getStar();
            String starName = star != null && star.getName() != null ? star.getName() : "";

            addPages(planetRecipes, planetName, starName, properties.getPlanetIcon(), effective, pageSize);
        }

        if (baselinePlanetCount > 0 && !baseline.isEmpty()) {
            String contextName = I18n.format(hasDifferentPlanet
                    ? "jei.advancedrocketry.orbitallaser.all_other_planets"
                    : "jei.advancedrocketry.orbitallaser.all_planets");

            addPages(recipes, contextName, "", DimensionProperties.PlanetIcons.EARTHLIKE.getResource(),
                    baseline, pageSize);
        }

        recipes.addAll(planetRecipes);
        return recipes;
    }

    private static void addPages(List<OrbitalLaserDrillWrapper> recipes, String contextName,
                                 String starName, ResourceLocation planetIcon,
                                 List<ItemStack> ores, int pageSize) {
        int pageCount = (ores.size() + pageSize - 1) / pageSize;

        for (int page = 0; page < pageCount; page++) {
            int from = page * pageSize;
            int to = Math.min(ores.size(), from + pageSize);

            recipes.add(new OrbitalLaserDrillWrapper(
                    contextName,
                    starName,
                    planetIcon,
                    page,
                    pageCount,
                    ores.subList(from, to)
            ));
        }
    }
}