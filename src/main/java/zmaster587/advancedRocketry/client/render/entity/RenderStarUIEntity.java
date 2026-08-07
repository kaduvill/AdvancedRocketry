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
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.client.render.multiblocks.RendererWarpCore;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityUIStar;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.libVulpes.render.RenderHelper;

public class RenderStarUIEntity extends Render<EntityUIStar> implements IRenderFactory<EntityUIStar> {

    public RenderStarUIEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public Render<? super EntityUIStar> createRenderFor(
            RenderManager manager) {
        return new RenderStarUIEntity(manager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityUIStar entity) {
        return DimensionProperties.PlanetIcons.EARTHLIKE.getResource();
    }

    @Override
    public void doRender(EntityUIStar entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {

        StellarBody body = entity.getStarProperties();
        if (body == null)
            return;
        float sizeScale = entity.getScale();
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glScalef(sizeScale, sizeScale, sizeScale);

        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        //Render the translucent star at its actual depth.
        RenderHelper.setupPlayerFacingMatrix(Minecraft.getMinecraft().player.getDistanceSq(entity), 0.0D, -0.45D, 0.0D);
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureResources.locationSunNew);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        int starColor = body.getColorRGB8();
        GlStateManager.color((starColor & 0xFF) / 255.0F, ((starColor >>> 8) & 0xFF) / 255.0F, ((starColor >>> 16) & 0xFF) / 255.0F, 1.0F);

        //Depth-only star core.
        GlStateManager.disableBlend();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.25F);
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(false, false, false, false);
        renderStarQuad(buffer);
        GlStateManager.colorMask(true, true, true, true);

        //Visible star and non-blocking glow.
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.12F);
        GlStateManager.depthMask(false);
        renderStarQuad(buffer);

        /*
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.depthMask(true);
        renderStarQuad(buffer);
*/
        RenderHelper.cleanupPlayerFacingMatrix();
        //GlStateManager.depthMask(false);
        //Selection effect remains translucent and shares the star's depthMask(false) state.
        if (entity.isSelected()) {
            GL11.glPushMatrix();
            GL11.glScaled(0.1D, 0.1D, 0.1D);

            GlStateManager.disableTexture2D();

            double speedRotate = 0.025D;
            double rotation = speedRotate * System.currentTimeMillis() % 360.0D;

            GlStateManager.color(0.4F, 0.4F, 1.0F, 0.6F);
            GL11.glTranslated(0.0D, -0.75D, 0.0D);
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


        //Restore ordinary depth writes before leaving the main star
        //matrix and before rendering the hover-information panel.
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
            RenderHelper.setupPlayerFacingMatrix(Minecraft.getMinecraft().player.getDistanceSq(hitObj.hitVec.x, hitObj.hitVec.y, hitObj.hitVec.z), 0, 0, 0);
            buffer = Tessellator.getInstance().getBuffer();

            //Draw Mass indicator
            Minecraft.getMinecraft().renderEngine.bindTexture(RenderPlanetUIEntity.planetUIFG);
            GlStateManager.color(1, 1, 1, 0.8f);
            renderMassIndicator(buffer, body.getTemperature() / 200f);

            //Draw background
            GlStateManager.color(1, 1, 1, 1);
            Minecraft.getMinecraft().renderEngine.bindTexture(RenderPlanetUIEntity.planetUIBG);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            RenderHelper.renderNorthFaceWithUV(buffer, 1, -40, -25, 40, 55, 1, 0, 1, 0);
            Tessellator.getInstance().draw();
            RenderHelper.cleanupPlayerFacingMatrix();
            RenderHelper.renderTag(Minecraft.getMinecraft().player.getDistanceSq(hitObj.hitVec.x, hitObj.hitVec.y, hitObj.hitVec.z), body.getName(), 0, .9, 0, 5);
            RenderHelper.renderTag(Minecraft.getMinecraft().player.getDistanceSq(hitObj.hitVec.x, hitObj.hitVec.y, hitObj.hitVec.z), "Num Planets: " + body.getNumPlanets(), 0, .6, 0, 5);

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

    private static void renderStarQuad(BufferBuilder buffer) {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        RenderHelper.renderNorthFaceWithUV(buffer, 0, -5, -5, 5, 5, 0, 1, 0, 1);
        Tessellator.getInstance().draw();
    }


    private static void restoreRenderState() {
        GlStateManager.colorMask(true, true, true, true);
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