package zmaster587.advancedRocketry.integration.jei.orbitalLaserDrill;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.integration.jei.PlanetRecipeHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrbitalLaserDrillWrapper implements IRecipeWrapper {

    private final String contextName;
    private final String starName;
    private final ResourceLocation planetIcon;
    private final int pageIndex;
    private final int pageCount;
    private final List<ItemStack> outputsPage;

    public OrbitalLaserDrillWrapper(String contextName, String starName, ResourceLocation planetIcon,
                                    int pageIndex, int pageCount, List<ItemStack> outputsPage) {
        this.contextName = contextName == null ? "" : contextName;
        this.starName = starName == null ? "" : starName;
        this.planetIcon = planetIcon;
        this.pageIndex = Math.max(0, pageIndex);
        this.pageCount = Math.max(1, pageCount);
        this.outputsPage = outputsPage == null
                ? Collections.<ItemStack>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(outputsPage));
    }

    public int getOutputCount() {
        return outputsPage.size();
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setOutputs(VanillaTypes.ITEM, outputsPage);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String pageText = pageCount > 1
                ? I18n.format("jei.advancedrocketry.orbitallaser.page", pageIndex + 1, pageCount)
                : "";
        String orbitingText = starName.isEmpty() ? ""
                : I18n.format("jei.advancedrocketry.orbitallaser.orbiting", starName);
        PlanetRecipeHeader.draw(minecraft, recipeWidth, planetIcon, contextName, orbitingText, pageText);
    }
}