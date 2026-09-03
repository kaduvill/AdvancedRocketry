package zmaster587.advancedRocketry.integration.jei.orbitalLaserDrill;

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

import java.util.List;

public final class OrbitalLaserDrillCategory implements IRecipeCategory<OrbitalLaserDrillWrapper> {

    public static final int COLUMNS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int MAX_ROWS = 9;
    public static final int MAX_OUTPUTS_PER_PAGE = COLUMNS * MAX_ROWS;

    private static final int WIDTH = 166;
    private static final int GRID_X = 2;
    private static final int GRID_Y = 24;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;
    private final IDrawable slotFrame;
    private IDrawable background;
    private int pageSize;
    private boolean layoutInitialized;

    public OrbitalLaserDrillCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(AdvancedRocketryBlocks.blockSpaceLaser));
        this.slotFrame = guiHelper.getSlotDrawable();
        this.pageSize = MAX_OUTPUTS_PER_PAGE;
        this.background = guiHelper.createBlankDrawable(WIDTH, GRID_Y + MAX_ROWS * SLOT_SIZE);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void initializeLayout(List<OrbitalLaserDrillWrapper> recipes) {
        if (layoutInitialized || recipes == null || recipes.isEmpty()) return;

        int maxOutputs = 0;

        for (OrbitalLaserDrillWrapper recipe : recipes) {
            maxOutputs = Math.max(maxOutputs, recipe.getOutputCount());
        }

        int rows = Math.max(1, Math.min(MAX_ROWS, (maxOutputs + COLUMNS - 1) / COLUMNS));
        this.pageSize = rows * COLUMNS;
        this.background = guiHelper.createBlankDrawable(WIDTH, GRID_Y + rows * SLOT_SIZE);
        this.layoutInitialized = true;
    }

    public void resetLayout() {
        this.pageSize = MAX_OUTPUTS_PER_PAGE;
        this.background = guiHelper.createBlankDrawable(WIDTH, GRID_Y + MAX_ROWS * SLOT_SIZE);
        this.layoutInitialized = false;
    }

    @Override
    public String getUid() {
        return ARPlugin.orbitalLaserDrillUUID;
    }

    @Override
    public String getTitle() {
        return new ItemStack(AdvancedRocketryBlocks.blockSpaceLaser).getDisplayName();
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
    public void setRecipe(IRecipeLayout layout, OrbitalLaserDrillWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);
        int slotCount = Math.min(outputs.size(), pageSize);

        for (int index = 0; index < slotCount; index++) {
            items.init(
                    index,
                    false,
                    GRID_X + index % COLUMNS * SLOT_SIZE,
                    GRID_Y + index / COLUMNS * SLOT_SIZE
            );
            items.setBackground(index, slotFrame);
        }

        items.set(ingredients);
    }
}