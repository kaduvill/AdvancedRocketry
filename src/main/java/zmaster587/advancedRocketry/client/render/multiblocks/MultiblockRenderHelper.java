package zmaster587.advancedRocketry.client.render.multiblocks;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.backwardCompat.WavefrontObject;

/**
 * Isolates multiblock mesh submissions from colour arrays left enabled by other renderers.
 * Constant colours/tints are preserved, and the caller's client array state is restored.
 */
@SideOnly(Side.CLIENT)
public final class MultiblockRenderHelper {
    private MultiblockRenderHelper() {
    }

    public static void renderAll(WavefrontObject model) {
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            model.renderAll();
        } finally {
            GL11.glPopClientAttrib();
        }
    }

    public static void renderOnly(WavefrontObject model, String... groupNames) {
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            model.renderOnly(groupNames);
        } finally {
            GL11.glPopClientAttrib();
        }
    }

    public static void renderPart(WavefrontObject model, String partName) {
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            model.renderPart(partName);
        } finally {
            GL11.glPopClientAttrib();
        }
    }

    public static void draw() {
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        try {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            Tessellator.getInstance().draw();
        } finally {
            GL11.glPopClientAttrib();
        }
    }
}
