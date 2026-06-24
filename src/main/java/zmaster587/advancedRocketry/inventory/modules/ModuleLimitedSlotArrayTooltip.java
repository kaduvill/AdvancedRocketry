package zmaster587.advancedRocketry.inventory.modules;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.libVulpes.inventory.modules.ModuleLimitedSlotArray;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ModuleLimitedSlotArrayTooltip extends ModuleLimitedSlotArray {

    private final IInventory inventory;
    private final String tooltipText;

    public ModuleLimitedSlotArrayTooltip(int offsetX,
                                         int offsetY,
                                         IInventory inventory,
                                         int startSlot,
                                         int endSlot,
                                         String tooltipText) {
        super(offsetX, offsetY, inventory, startSlot, endSlot);
        this.inventory = inventory;
        this.tooltipText = tooltipText;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderToolTip(int guiOffsetX,
                              int guiOffsetY,
                              int mouseX,
                              int mouseY,
                              float zLevel,
                              GuiContainer gui,
                              FontRenderer font) {
        super.renderToolTip(
                guiOffsetX,
                guiOffsetY,
                mouseX,
                mouseY,
                zLevel,
                gui,
                font);

        if (tooltipText == null || tooltipText.isEmpty())
            return;

        for (Object object : slotList) {
            Slot slot = (Slot) object;

            if (!inventory.getStackInSlot(slot.getSlotIndex()).isEmpty())
                continue;

            boolean hovering =
                    mouseX >= slot.xPos
                            && mouseX < slot.xPos + 16
                            && mouseY >= slot.yPos
                            && mouseY < slot.yPos + 16;

            if (hovering) {
                List<String> tooltip = new LinkedList<>(
                        Arrays.asList(tooltipText.split("\n")));

                drawTooltip(gui, tooltip, mouseX, mouseY, zLevel, font);
                return;
            }
        }
    }
}