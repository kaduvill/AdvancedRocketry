package zmaster587.advancedRocketry.integration.theoneprobe;

import java.util.Stack;

import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.NumberFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import zmaster587.advancedRocketry.integration.dataloaders.AbstractDataContext;

public class TOPDataContext extends AbstractDataContext {

    Stack<IProbeInfo> probeStack;
    private static final int FUEL_BORDER_COLOR = 0xFF555555;
    private static final int FUEL_BACKGROUND_COLOR = 0xFF000000;
    private static final int FUEL_FILLED_COLOR = 0xFF284892;
    private static final int FUEL_ALT_FILLED_COLOR = 0xFF162F69;

    public TOPDataContext(IProbeInfo probeInfo) {
        this.probeStack = new Stack<>();
        this.probeStack.add(probeInfo);
    }

    @Override
    public void addMessage(String message, TextFormatting formatting) {
        current().text(formatting + message + TextFormatting.RESET);
    }

    @Override
    public void addFluidInformation(String message, int amount, int capacity) {
        IProbeInfo probeInfo = current();

        probeInfo.text(message);
        probeInfo.progress(
            amount,
            capacity,
            probeInfo.defaultProgressStyle()
                    .borderColor(FUEL_BORDER_COLOR)
                    .backgroundColor(FUEL_BACKGROUND_COLOR)
                    .filledColor(FUEL_FILLED_COLOR)
                    .alternateFilledColor(FUEL_ALT_FILLED_COLOR)
                    .height(12)
                    .width(100)
                    .showText(true)
                    .suffix(" mB")
                    .numberFormat(NumberFormat.COMMAS)
        );
    }

    private IProbeInfo current() {
        return this.probeStack.peek();
    }

    @Override
    public void pushStack(ItemStack stack) {
        IProbeInfo row = current().horizontal();
        row.item(stack, current().defaultItemStyle().width(16).height(16));
        this.probeStack.push(row);
    }

    @Override
    public void popStack() {
        this.probeStack.pop();
    }

    @Override
    public String translate(String key) {
        // this runs on the server side
        return IProbeInfo.STARTLOC + key + IProbeInfo.ENDLOC;
    }
    
}
