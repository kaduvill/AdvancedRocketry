package zmaster587.advancedRocketry.integration.jei.orbitalLaserDrill;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrbitalLaserDrillWrapper implements IRecipeWrapper {

    private final String contextName;
    private final ResourceLocation planetIcon;
    private final int pageIndex;
    private final int pageCount;
    private final List<ItemStack> outputsPage;

    public OrbitalLaserDrillWrapper(String contextName, ResourceLocation planetIcon,
                                    int pageIndex, int pageCount, List<ItemStack> outputsPage) {
        this.contextName = contextName == null ? "" : contextName;
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
        FontRenderer font = minecraft.fontRenderer;

        if (planetIcon != null) {
            GlStateManager.pushMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableBlend();
            minecraft.getTextureManager().bindTexture(planetIcon);
            Gui.drawModalRectWithCustomSizedTexture(4, 4, 0, 0, 16, 16, 16, 16);
            GlStateManager.popMatrix();
        }

        String pageText = pageCount > 1
                ? I18n.format("jei.advancedrocketry.orbitallaser.page", pageIndex + 1, pageCount)
                : "";
        int nameX = planetIcon == null ? 4 : 24;
        int nameWidth = recipeWidth - nameX - 4;

        if (!pageText.isEmpty()) {
            nameWidth -= font.getStringWidth(pageText) + 6;
            font.drawString(pageText, recipeWidth - font.getStringWidth(pageText) - 4, 7, 0x606060);
        }

        font.drawString(font.trimStringToWidth(contextName, Math.max(0, nameWidth)), nameX, 7, 0x404040);
    }
}