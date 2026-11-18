package zmaster587.advancedRocketry.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.client.render.multiblocks.MultiblockRenderHelper;
import zmaster587.advancedRocketry.tile.TileRocketPrinter;
import zmaster587.libVulpes.render.RenderHelper;

public class RendererRocketPrinter extends TileEntitySpecialRenderer<TileRocketPrinter> {
    private static final ResourceLocation GRID = new ResourceLocation("advancedrocketry:textures/models/grid.png");
    private static final ResourceLocation GIRDER = new ResourceLocation("advancedrocketry:textures/models/girder.png");
    private static final ResourceLocation ROUND_H = new ResourceLocation("advancedrocketry:textures/models/round_h.png");

    @Override
    public void render(TileRocketPrinter tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (!tile.isRenderingFrame()) return;
        AxisAlignedBB pad = tile.getPad();
        double xOffset = pad.minX - tile.getPos().getX(), zOffset = pad.minZ - tile.getPos().getZ();
        double xSize = pad.maxX - pad.minX + 1, zSize = pad.maxZ - pad.minZ + 1;
        double baseY = pad.minY - tile.getPos().getY(), frameY = tile.getFrameHeight(partialTicks);
        double xMin = xOffset, xMax = xOffset + .5, zMin = zOffset, zMax = zOffset + zSize;
        double yMin = frameY, yMax = frameY + .25;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.alphaFunc(GL11.GL_GEQUAL, .01f);
            GlStateManager.color(.25f, .9f, 1f, .05f);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0xf0, 0xf0);
            bindTexture(GRID);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            for (int i = 0; i < 20; i++) {
                double offset = i / 80d;
                RenderHelper.renderBottomFaceWithUV(buffer, frameY + offset, xOffset, zOffset, xOffset + xSize, zOffset + zSize, 0, xSize, 0, zSize);
                RenderHelper.renderTopFaceWithUV(buffer, frameY + offset, xOffset, zOffset, xOffset + xSize, zOffset + zSize, 0, xSize, 0, zSize);
            }
            MultiblockRenderHelper.draw();

            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableFog();
            GlStateManager.color(1f, 1f, 1f, 1f);
            bindTexture(ROUND_H);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderBottomFaceWithUV(buffer, yMin, xMin, zMin, xMax, zMax, 0, 1, 0, 1);
            RenderHelper.renderWestFaceWithUV(buffer, xMin, yMin, zMin, yMax, zMax, 0, 1, 0, 1);
            RenderHelper.renderSouthFaceWithUV(buffer, zMax, xMin, yMin, xMax, yMax, 0, 1, 0, 1);
            RenderHelper.renderNorthFaceWithUV(buffer, zMin, xMin, yMin, xMax, yMax, 0, 1, 0, 1);
            RenderHelper.renderTopFaceWithUV(buffer, yMax, xMin, zMin, xMax, zMax, 0, 1, 0, 1);
            MultiblockRenderHelper.draw();

            GlStateManager.disableTexture2D();
            GlStateManager.color(.25f, .9f, 1f, 1f);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_NORMAL);
            RenderHelper.renderEastFace(buffer, xMax, yMin, zMin, yMax, zMax);
            xMin = xOffset + xSize - .5;
            xMax = xOffset + xSize;
            RenderHelper.renderWestFace(buffer, xMin, yMin, zMin, yMax, zMax);
            MultiblockRenderHelper.draw();

            GlStateManager.enableTexture2D();
            GlStateManager.color(1f, 1f, 1f, 1f);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderBottomFaceWithUV(buffer, yMin, xMin, zMin, xMax, zMax, 0, 1, 0, 1);
            RenderHelper.renderEastFaceWithUV(buffer, xMax, yMin, zMin, yMax, zMax, 0, 1, 0, 1);
            RenderHelper.renderSouthFaceWithUV(buffer, zMax, xMin, yMin, xMax, yMax, 0, 1, 0, 1);
            RenderHelper.renderNorthFaceWithUV(buffer, zMin, xMin, yMin, xMax, yMax, 0, 1, 0, 1);
            RenderHelper.renderTopFaceWithUV(buffer, yMax, xMin, zMin, xMax, zMax, 0, 1, 0, 1);
            MultiblockRenderHelper.draw();

            GlStateManager.color(.78f, .5f, .34f, 1f);
            bindTexture(GIRDER);
            double size = .25, girderV = (frameY - baseY) / size;
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderCubeWithUV(buffer, xOffset, baseY, zOffset, xOffset + size, frameY, zOffset + size, 0, 1, 0, girderV);
            RenderHelper.renderCubeWithUV(buffer, xOffset + xSize - size, baseY, zOffset, xOffset + xSize, frameY, zOffset + size, 0, 1, 0, girderV);
            RenderHelper.renderCubeWithUV(buffer, xOffset + xSize - size, baseY, zOffset + zSize - size, xOffset + xSize, frameY, zOffset + zSize, 0, 1, 0, girderV);
            RenderHelper.renderCubeWithUV(buffer, xOffset, baseY, zOffset + zSize - size, xOffset + size, frameY, zOffset + zSize, 0, 1, 0, girderV);
            MultiblockRenderHelper.draw();
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.alphaFunc(GL11.GL_GREATER, .1f);
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableFog();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.popMatrix();
        }
    }
}