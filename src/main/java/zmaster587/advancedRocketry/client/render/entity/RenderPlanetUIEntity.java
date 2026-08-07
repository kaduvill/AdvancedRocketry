package zmaster587.advancedRocketry.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.backwardCompat.ModelFormatException;
import zmaster587.advancedRocketry.backwardCompat.WavefrontObject;
import zmaster587.advancedRocketry.client.render.multiblocks.RendererWarpCore;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityUIPlanet;
import zmaster587.libVulpes.render.RenderHelper;

public class RenderPlanetUIEntity extends Render<EntityUIPlanet> implements IRenderFactory<EntityUIPlanet> {

    public static ResourceLocation planetUIBG = new ResourceLocation("advancedrocketry:textures/gui/planetUIOverlay.png");
    public static ResourceLocation planetUIFG = new ResourceLocation("advancedrocketry:textures/gui/planetUIOverlayFG.png");
    private static WavefrontObject sphere;

    static {
        try {
            sphere = new WavefrontObject(new ResourceLocation("advancedrocketry:models/atmosphere.obj"));
        } catch (ModelFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public RenderPlanetUIEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public Render<? super EntityUIPlanet> createRenderFor(
            RenderManager manager) {
        return new RenderPlanetUIEntity(manager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUIPlanet entity) {
        return DimensionProperties.PlanetIcons.EARTHLIKE.getResource();
    }

    @Override
    public void doRender(EntityUIPlanet entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {

        DimensionProperties properties = entity.getProperties();
        if (properties == null)
            return;

        float sizeScale = Math.max(properties.gravitationalMultiplier * properties.gravitationalMultiplier * entity.getScale(), .5f);

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y + sizeScale * 0.03f, (float) z);

        GL11.glScalef(0.1F * sizeScale, 0.1F * sizeScale, 0.1F * sizeScale);
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        Minecraft.getMinecraft().renderEngine.bindTexture(properties.getPlanetIconLEO());

        //Render the actual planet as solid geometry.
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPushMatrix();
        GL11.glRotatef((entity.world.getTotalWorldTime() + partialTicks) % 360.0F, 0.0F, 1.0F, 0.0F);
        sphere.renderAll();
        GL11.glPopMatrix();

        //Everything after the planet core may use transparency.
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        //Render shadow
        GL11.glPushMatrix();
        GL11.glScalef(1.1F, 1.1F, 1.1F);
        GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
        GL11.glRotated(-(properties.orbitTheta * 180.0D / Math.PI), 1.0D, 0.0D, 0.0D);
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Minecraft.getMinecraft().renderEngine.bindTexture(DimensionProperties.shadow3);

        GlStateManager.color(0.1F, 0.1F, 0.1F, 0.75F);
        sphere.renderAll();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        //Restore before rendering rings.
        GlStateManager.depthMask(true);
        if (properties.hasRings) {
            //Rotate for rings
            GL11.glRotatef(90, 1, 0, 0);
            GL11.glRotatef(-90, 0, 0, 1);

            //Draw ring
            GlStateManager.color(properties.ringColor[0], properties.ringColor[1], properties.ringColor[2], 0.5f);
            Minecraft.getMinecraft().renderEngine.bindTexture(DimensionProperties.planetRings);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderTopFaceWithUV(buffer, 0, -1, -1, 1, 1, 0, 1, 0, 1);
            RenderHelper.renderBottomFaceWithUV(buffer, 0, -1, -1, 1, 1, 0, 1, 0, 1);
            Tessellator.getInstance().draw();

            //Draw ring shadow
            Minecraft.getMinecraft().renderEngine.bindTexture(DimensionProperties.planetRingShadow);
            GlStateManager.color(1, 1, 1, 0.5f);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderTopFaceWithUV(buffer, 0, -1, -1, 1, 1, 0, 1, 0, 1);
            RenderHelper.renderBottomFaceWithUV(buffer, 0, -1, -1, 1, 1, 0, 1, 0, 1);
            Tessellator.getInstance().draw();
        }
        GL11.glPopMatrix();

        //Decorative effects: depth-tested, but do not modify the depth buffer.
        GlStateManager.depthMask(false);

        // Atmosphere
        if (properties.hasAtmosphere()) {
            GL11.glPushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(properties.skyColor[0], properties.skyColor[1], properties.skyColor[2], 0.04F);
            GL11.glScalef(1.012F, 1.012F, 1.012F);
            sphere.renderAll();
            GlStateManager.enableTexture2D();
            GL11.glPopMatrix();
        }

        // Selection indicator
        if (entity.isSelected()) {
            GL11.glPushMatrix();
            GlStateManager.disableTexture2D();
            double speedRotate = 0.025D;
            double rotation = speedRotate * System.currentTimeMillis() % 360.0D;
            GlStateManager.color(0.4F, 0.4F, 1.0F, 0.6F);
            GL11.glTranslated(0.0D, -1.25D, 0.0D);
            GL11.glPushMatrix();
            GL11.glRotated(rotation, 0.0D, 1.0D, 0.0D);
            RendererWarpCore.model.renderOnly("Rotate1");
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glRotated(180.0D + rotation, 0.0D, 1.0D, 0.0D);
            RendererWarpCore.model.renderOnly("Rotate1");
            GL11.glPopMatrix();
            GlStateManager.enableTexture2D();
            GL11.glPopMatrix();
        }
        GlStateManager.depthMask(true);
        GL11.glPopMatrix();

        RayTraceResult hitObj = Minecraft.getMinecraft().objectMouseOver;
        if (hitObj != null && hitObj.entityHit == entity) {

            GL11.glPushMatrix();
            GlStateManager.color(1, 1, 1);
            GL11.glTranslated(x, y + sizeScale * 0.03f, z);
            sizeScale = .1f * sizeScale;
            GL11.glScaled(sizeScale, sizeScale, sizeScale);

            //Render atmosphere UI/planet info

            RenderHelper.setupPlayerFacingMatrix(Minecraft.getMinecraft().player.getDistanceSq(hitObj.hitVec.z, hitObj.hitVec.y, hitObj.hitVec.x), 0, 0, 0);
            buffer = Tessellator.getInstance().getBuffer();

            //Draw Mass indicator
            Minecraft.getMinecraft().renderEngine.bindTexture(planetUIFG);
            GlStateManager.color(1, 1, 1, 0.8f);
            renderMassIndicator(buffer, Math.min(properties.gravitationalMultiplier / 2f, 1f));

            //Draw background
            GlStateManager.color(1, 1, 1, 1);
            Minecraft.getMinecraft().renderEngine.bindTexture(planetUIBG);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderNorthFaceWithUV(buffer, 1, -40, -25, 40, 55, 1, 0, 1, 0);
            Tessellator.getInstance().draw();


            //Render ATM
            Minecraft.getMinecraft().renderEngine.bindTexture(planetUIFG);
            renderATMIndicator(buffer, Math.min(properties.getAtmosphereDensity() / 200f, 1f));
            //Render Temp
            renderTemperatureIndicator(buffer, Math.min(properties.getAverageTemp() / 400f, 1f));

            //Render planet name
            RenderHelper.cleanupPlayerFacingMatrix();
            RenderHelper.renderTag(Minecraft.getMinecraft().player.getDistanceSq(hitObj.hitVec.z, hitObj.hitVec.y, hitObj.hitVec.x), properties.getName(), 0, .9, 0, 5);
            RenderHelper.renderTag(Minecraft.getMinecraft().player.getDistanceSq(hitObj.hitVec.z, hitObj.hitVec.y, hitObj.hitVec.x), "NumMoons: " + properties.getChildPlanets().size(), 0, .6, 0, 5);

            GL11.glPopMatrix();
        }
        restoreRenderState();
    }

    protected void renderMassIndicator(BufferBuilder buffer, float percent) {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float maxUV = (1 - percent) * 0.5f;

        RenderHelper.renderNorthFaceWithUV(buffer, 0, -20, -5 + 41 * (1 - percent), 20, 36, .5f, 0f, .5, maxUV);
        Tessellator.getInstance().draw();
    }

    protected void renderATMIndicator(BufferBuilder buffer, float percent) {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float maxUV = (1 - percent) * 0.406f + .578f;
        //Offset by 15 for Y
        RenderHelper.renderNorthFaceWithUV(buffer, 0, 6, 20 + (1 - percent) * 33, 39, 53, .5624f, .984f, .984f, maxUV);
        Tessellator.getInstance().draw();
    }

    protected void renderTemperatureIndicator(BufferBuilder buffer, float percent) {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float maxUV = (1 - percent) * 0.406f + .578f;
        //Offset by 15 for Y
        RenderHelper.renderNorthFaceWithUV(buffer, 0, -38, 21.4f + (1 - percent) * 33, -4, 53, .016f, .4376f, .984f, maxUV);
        Tessellator.getInstance().draw();
    }

    private static void restoreRenderState() {
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }
}
