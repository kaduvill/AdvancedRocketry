package zmaster587.advancedRocketry.integration.jei.asteroids;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.item.ItemStack;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.integration.jei.ARPlugin;
import zmaster587.libVulpes.LibVulpes;

import java.util.List;

public class AsteroidCategory implements IRecipeCategory<AsteroidWrapper> {

    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 6;
    private static final int TITLE_HEIGHT = 22;
    private static final int GRID_X = PADDING;
    private static final int GRID_Y = TITLE_HEIGHT + PADDING;
    private static final int BACKGROUND_WIDTH = PADDING + AsteroidWrapper.COLUMNS * SLOT_SIZE + PADDING;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;
    private final IDrawable slotFrame;
    private IDrawable background;
    private int rows = 1;
    private boolean layoutInitialized;

    public AsteroidCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.background = guiHelper.createBlankDrawable(
                BACKGROUND_WIDTH,
                TITLE_HEIGHT + PADDING + SLOT_SIZE + PADDING
        );
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(AdvancedRocketryBlocks.blockObservatory));
        this.slotFrame = guiHelper.getSlotDrawable();
    }

    public void initializeLayout(List<AsteroidWrapper> recipes) {
        if (layoutInitialized)
            return;

        int largestPage = 0;
        if (recipes != null) {
            for (AsteroidWrapper recipe : recipes)
                largestPage = Math.max(largestPage, recipe.getOutputCount());
        }

        rows = Math.max(1, Math.min(
                AsteroidWrapper.MAX_ROWS,
                (largestPage + AsteroidWrapper.COLUMNS - 1) / AsteroidWrapper.COLUMNS
        ));
        background = guiHelper.createBlankDrawable(
                BACKGROUND_WIDTH,
                TITLE_HEIGHT + PADDING + rows * SLOT_SIZE + PADDING
        );
        layoutInitialized = true;
    }

    public boolean isLayoutInitialized() {
        return layoutInitialized;
    }

    public void resetLayout() {
        rows = 1;
        layoutInitialized = false;
        background = guiHelper.createBlankDrawable(
                BACKGROUND_WIDTH,
                TITLE_HEIGHT + PADDING + SLOT_SIZE + PADDING
        );
    }

    @Override
    public String getUid() {
        return ARPlugin.asteroidsUUID;
    }

    @Override
    public String getTitle() {
        return LibVulpes.proxy.getLocalizedString("jei.ar.asteroids");
    }

    @Override
    public String getModName() {
        return "Advanced Rocketry";
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout layout, AsteroidWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup stacks = layout.getItemStacks();
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);
        int outputCount = Math.min(outputs.size(), AsteroidWrapper.MAX_OUTPUTS_PER_PAGE);

        for (int i = 0; i < outputCount; i++) {
            stacks.init(
                    i,
                    false,
                    GRID_X + i % AsteroidWrapper.COLUMNS * SLOT_SIZE,
                    GRID_Y + i / AsteroidWrapper.COLUMNS * SLOT_SIZE
            );
            stacks.setBackground(i, slotFrame);
            stacks.set(i, outputs.get(i));
        }
    }
}