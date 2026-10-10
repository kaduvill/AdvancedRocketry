package zmaster587.advancedRocketry.integration.jei.artifacts;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArtifactRecipeMaker {

    private ArtifactRecipeMaker() {}

    public static List<ArtifactWrapper> getRecipes() {
        List<ArtifactWrapper> recipes = new ArrayList<>();
        if (!ARConfiguration.getCurrentConfig().registerJeiArtifactPage) return recipes;

        DimensionManager manager = DimensionManager.getInstance();
        if (manager == null) return recipes;

        Integer[] dimensionIds = manager.getRegisteredDimensions();
        if (dimensionIds == null) return recipes;

        List<DimensionProperties> planets = new ArrayList<>();
        for (Integer dimensionId : dimensionIds) {
            if (dimensionId == null) continue;

            DimensionProperties properties = manager.getDimensionProperties(dimensionId);
            if (properties == null || properties.isStation() || properties.getRequiredArtifacts().isEmpty()) continue;

            planets.add(properties);
        }

        planets.sort(Comparator.comparing(ArtifactRecipeMaker::getPlanetName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(DimensionProperties::getId));

        Set<ArtifactWrapper> seen = new HashSet<>();
        for (DimensionProperties properties : planets) {
            StellarBody star = properties.getStar();
            String starName = star == null ? "" : star.getName();
            String planetName = getPlanetName(properties);
            ResourceLocation planetIcon = star == null
                    ? DimensionProperties.PlanetIcons.UNKNOWN.getResource()
                    : properties.getPlanetIcon();

            for (ItemStack artifact : properties.getRequiredArtifacts()) {
                if (artifact == null || artifact.isEmpty() || artifact.getItem().getRegistryName() == null) continue;

                ArtifactWrapper recipe = new ArtifactWrapper(properties.getId(), planetName, starName,
                        planetIcon, artifact);
                if (seen.add(recipe)) recipes.add(recipe);
            }
        }

        return recipes;
    }

    private static String getPlanetName(DimensionProperties properties) {
        String name = properties.getName();
        return name == null || name.isEmpty() ? "DIM " + properties.getId() : name;
    }
}
