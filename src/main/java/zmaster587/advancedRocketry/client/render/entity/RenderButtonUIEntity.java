package zmaster587.advancedRocketry.client.render.entity;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityUIButton;
import zmaster587.libVulpes.render.RenderHelper;

public class RenderButtonUIEntity extends Render<EntityUIButton> implements IRenderFactory<EntityUIButton> {

    public RenderButtonUIEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public Render<? super EntityUIButton> createRenderFor(
            RenderManager manager) {
        return new RenderButtonUIEntity(manager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUIButton entity) {
        return DimensionProperties.PlanetIcons.EARTHLIKE.getResource();
    }

    @Override
    public void doRender(EntityUIButton entity, double x, double y, double z, float entityYaw, float partialTicks) {
        renderDepthTag(Minecraft.getMinecraft().player.getDistanceSq(entity), I18n.format("msg.planetholo..uplevel"), x, y - 0.25D, z, 8);
    }

    private static void renderDepthTag(double distanceSq, String text, double x, double y, double z, int range) {
        if (distanceSq > range * range)
            return;

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        int halfWidth = font.getStringWidth(text) / 2;

        RenderHelper.setupPlayerFacingMatrix(distanceSq, x, y, z);

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(1.0F, 10.0F);
        GlStateManager.color(0, 0, 0, 0.25F);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(-halfWidth - 1, -1, 0).endVertex();
        buffer.pos(-halfWidth - 1, 8, 0).endVertex();
        buffer.pos(halfWidth + 1, 8, 0).endVertex();
        buffer.pos(halfWidth + 1, -1, 0).endVertex();
        tessellator.draw();

        GlStateManager.doPolygonOffset(0, 0);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableTexture2D();

        GlStateManager.color(1, 1, 1, 1);
        font.drawString(text, -halfWidth, 0, -1);

        RenderHelper.cleanupPlayerFacingMatrix();
        restoreRenderState();
    }

    private static void restoreRenderState() {
        GlStateManager.doPolygonOffset(0, 0);
        GlStateManager.disablePolygonOffset();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }
}
