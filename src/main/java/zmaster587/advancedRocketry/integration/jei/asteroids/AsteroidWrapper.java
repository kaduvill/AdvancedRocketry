package zmaster587.advancedRocketry.integration.jei.asteroids;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import zmaster587.advancedRocketry.util.Asteroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class AsteroidWrapper implements IRecipeWrapper {

    public static final int COLUMNS = 9;
    public static final int MAX_ROWS = 9;
    public static final int MAX_OUTPUTS_PER_PAGE = COLUMNS * MAX_ROWS;

    private final String asteroidKey;
    private final Asteroid asteroid;
    private final int pageIndex;
    private final int pageCount;
    private final List<ItemStack> outputsPage;

    public AsteroidWrapper(String asteroidKey, Asteroid asteroid, int pageIndex, int pageCount, List<ItemStack> outputsPage) {
        this.asteroidKey = asteroidKey;
        this.asteroid = asteroid;
        this.pageIndex = Math.max(0, pageIndex);
        this.pageCount = Math.max(1, pageCount);
        this.outputsPage = outputsPage == null ? Collections.emptyList() : new ArrayList<>(outputsPage);
    }

    public boolean isValid() {
        return asteroid != null;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getOutputCount() {
        return outputsPage.size();
    }

    public String getDisplayName() {
        if (asteroid != null) {
            String name = asteroid.getName();
            if (name != null && !name.isEmpty())
                return name;
        }
        return asteroidKey != null && !asteroidKey.isEmpty() ? asteroidKey : "Asteroid";
    }

    public String getHeaderText() {
        String name = getDisplayName();
        if (pageCount > 1)
            name += " (" + (pageIndex + 1) + "/" + pageCount + ")";
        return name;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setOutputs(VanillaTypes.ITEM, outputsPage);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.fontRenderer == null) return;

        String header = getHeaderText();

        if (header != null && !header.isEmpty()) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            minecraft.fontRenderer.drawString(
                    minecraft.fontRenderer.trimStringToWidth(header, recipeWidth - 12),
                    6,
                    2,
                    0x404040
            );
        }

        if (asteroid != null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75f, 0.75f, 1.0f);
            minecraft.fontRenderer.drawString(
                    I18n.format("jei.advancedrocketry.asteroids.distance", asteroid.getDistance()),
                    Math.round(7 / 0.75f),
                    Math.round(12 / 0.75f),
                    0x7A7A7A
            );
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static List<ItemStack> collectOutputsFromConfig(Asteroid asteroid) {
        if (asteroid == null || asteroid.itemStacks == null)
            return Collections.emptyList();

        LinkedHashMap<String, ItemStack> outputs = new LinkedHashMap<>();
        for (ItemStack stack : asteroid.itemStacks) {
            if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null)
                continue;

            ItemStack output = stack.copy();
            output.setCount(1);
            String key = output.getItem().getRegistryName() + "@" + output.getMetadata();
            if (!outputs.containsKey(key))
                outputs.put(key, output);
        }
        return new ArrayList<>(outputs.values());
    }
}