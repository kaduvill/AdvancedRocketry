package zmaster587.advancedRocketry.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public final class PlanetRecipeHeader {

    private PlanetRecipeHeader() {}

    public static void draw(Minecraft minecraft, int recipeWidth, ResourceLocation planetIcon,
                            String planetName, String orbitingText, String pageText) {
        FontRenderer font = minecraft.fontRenderer;

        if (planetIcon != null) {
            GlStateManager.pushMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableBlend();
            minecraft.getTextureManager().bindTexture(planetIcon);
            Gui.drawModalRectWithCustomSizedTexture(4, 4, 0, 0, 16, 16, 16, 16);
            GlStateManager.popMatrix();
        }

        int nameX = planetIcon == null ? 4 : 24;
        int nameWidth = recipeWidth - nameX - 4;

        if (!pageText.isEmpty()) {
            nameWidth -= font.getStringWidth(pageText) + 6;
            font.drawString(pageText, recipeWidth - font.getStringWidth(pageText) - 4, 7, 0x606060);
        }

        font.drawString(font.trimStringToWidth(planetName, Math.max(0, nameWidth)), nameX, 7, 0x404040);

        if (!orbitingText.isEmpty()) {
            int orbitingWidth = Math.max(0, (int) ((recipeWidth - nameX - 5) / 0.75f));

            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75f, 0.75f, 1.0f);
            font.drawString(
                    font.trimStringToWidth(orbitingText, orbitingWidth),
                    Math.round((nameX + 1) / 0.75f),
                    Math.round(16 / 0.75f),
                    0x7A7A7A
            );
            GlStateManager.popMatrix();
        }
    }
}
