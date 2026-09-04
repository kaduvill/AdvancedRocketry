package zmaster587.advancedRocketry.integration.jei.asteroids;

import mezz.jei.api.IJeiHelpers;
import net.minecraft.item.ItemStack;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.util.Asteroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AsteroidRecipeMaker {

    public static List<AsteroidWrapper> getRecipes(IJeiHelpers helpers) {
        Map<String, Asteroid> asteroidTypes = ARConfiguration.getCurrentConfig().asteroidTypes;
        if (asteroidTypes == null || asteroidTypes.isEmpty())
            return Collections.emptyList();

        List<AsteroidWrapper> recipes = new ArrayList<>();
        for (Map.Entry<String, Asteroid> entry : asteroidTypes.entrySet()) {
            Asteroid asteroid = entry.getValue();
            if (asteroid == null)
                continue;

            List<ItemStack> outputs = AsteroidWrapper.collectOutputsFromConfig(asteroid);
            int pageCount = Math.max(1, (outputs.size() + AsteroidWrapper.MAX_OUTPUTS_PER_PAGE - 1) /
                    AsteroidWrapper.MAX_OUTPUTS_PER_PAGE);

            for (int page = 0; page < pageCount; page++) {
                int from = page * AsteroidWrapper.MAX_OUTPUTS_PER_PAGE;
                int to = Math.min(outputs.size(), from + AsteroidWrapper.MAX_OUTPUTS_PER_PAGE);
                List<ItemStack> pageOutputs = from < to ? outputs.subList(from, to) : Collections.emptyList();
                recipes.add(new AsteroidWrapper(entry.getKey(), asteroid, page, pageCount, pageOutputs));
            }
        }

        recipes.sort(Comparator
                .comparing(AsteroidWrapper::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(AsteroidWrapper::getPageIndex));
        return recipes;
    }

    public static List<AsteroidWrapper> getMachineRecipes(IJeiHelpers helpers, Class<?> ignored) {
        return getRecipes(helpers);
    }
}