package zmaster587.advancedRocketry.inventory.modules;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.client.util.ProgressBarImage;
import zmaster587.libVulpes.inventory.modules.IProgressBar;
import zmaster587.libVulpes.inventory.modules.ModuleBase;

import java.util.ArrayList;
import java.util.List;

public class ModuleRocketFuelProgress extends ModuleBase {

    // Manual liquid-column hitbox.
    private static final int HOVER_X = 0;
    private static final int HOVER_Y = 0;
    private static final int HOVER_W = 5;
    private static final int HOVER_H = 71;

    private final int id;
    private final ProgressBarImage progressBar;
    private final IProgressBar progress;
    private final EntityRocket rocket;
    private final FuelType fixedType;
    private final List<String> tooltip = new ArrayList<String>(2);

    private int prevProgress;
    private int prevTotalProgress;

    /**
     * @param fixedType null = use rocket's main fuel type,
     *                  otherwise use exact type, e.g. LIQUID_OXIDIZER
     */
    public ModuleRocketFuelProgress(int offsetX, int offsetY, int id,
                                    ProgressBarImage progressBar,
                                    EntityRocket rocket,
                                    FuelType fixedType) {
        super(offsetX, offsetY);
        this.id = id;
        this.progressBar = progressBar;
        this.progress = rocket;
        this.rocket = rocket;
        this.fixedType = fixedType;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderBackground(GuiContainer gui, int x, int y, int mouseX, int mouseY, FontRenderer font) {
        progressBar.renderProgressBar(x + offsetX, y + offsetY, getProgress(), gui);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderForeground(int guiOffsetX, int guiOffsetY, int mouseX, int mouseY,
                                 float zLevel, GuiContainer gui, FontRenderer font) {
        if (isMouseOverLiquidColumn(mouseX, mouseY)) {
            List<String> tooltip = getToolTip();

            if (tooltip != null && !tooltip.isEmpty()) {
                drawTooltip(gui, tooltip, mouseX, MathHelper.clamp(mouseY, 16, Integer.MAX_VALUE), zLevel, font);
            }
        }
    }

    private boolean isMouseOverLiquidColumn(int mouseX, int mouseY) {
        int x = offsetX + HOVER_X;
        int y = offsetY + HOVER_Y;

        return mouseX >= x
                && mouseX < x + HOVER_W
                && mouseY >= y
                && mouseY < y + HOVER_H;
    }

    private float getProgress() {
        return progress.getNormallizedProgress(id);
    }

    private List<String> getToolTip() {
        FuelType type = fixedType != null ? fixedType : rocket.getRocketFuelType();

        tooltip.clear();

        if (type == null) {
            tooltip.add(tr("msg.entity.rocket.fuel.none"));
            return tooltip;
        }

        int amount = Math.max(0, rocket.getFuelAmount(type));
        int capacity = Math.max(0, rocket.getFuelCapacity(type));

        tooltip.add(getFuelName(type));
        tooltip.add(amount + " / " + capacity + " mB");

        return tooltip;
    }

    private static String getFuelName(FuelType type) {
        switch (type) {
            case LIQUID_MONOPROPELLANT:
                return tr("msg.entity.rocket.fuel.monopropellant");
            case LIQUID_BIPROPELLANT:
                return tr("msg.entity.rocket.fuel.bipropellant");
            case LIQUID_OXIDIZER:
                return tr("msg.entity.rocket.fuel.oxidizer");
            case NUCLEAR_WORKING_FLUID:
                return tr("msg.entity.rocket.fuel.nuclearWorkingFluid");
            default:
                return tr("msg.entity.rocket.fuel.main");
        }
    }

    private static String tr(String key) {
        String value = LibVulpes.proxy.getLocalizedString(key);
        return value == null || value.equals(key) ? key : value;
    }

    @Override
    public boolean needsUpdate(int localId) {
        switch (localId) {
            case 0:
                return prevProgress != progress.getProgress(id);
            case 1:
                return prevTotalProgress != progress.getTotalProgress(id);
            default:
                return false;
        }
    }

    @Override
    protected void updatePreviousState(int localId) {
        switch (localId) {
            case 0:
                prevProgress = progress.getProgress(id);
                break;
            case 1:
                prevTotalProgress = progress.getTotalProgress(id);
                break;
        }
    }

    @Override
    public void sendChanges(Container container, IContainerListener crafter, int variableId, int localId) {
        switch (localId) {
            case 0:
                crafter.sendWindowProperty(container, variableId, progress.getProgress(id));
                break;
            case 1:
                crafter.sendWindowProperty(container, variableId, progress.getTotalProgress(id));
                break;
        }
    }

    @Override
    public void onChangeRecieved(int slot, int value) {
        switch (slot) {
            case 0:
                progress.setProgress(id, value);
                break;
            case 1:
                progress.setTotalProgress(id, value);
                break;
        }
    }

    @Override
    public int numberOfChangesToSend() {
        return 2;
    }
}