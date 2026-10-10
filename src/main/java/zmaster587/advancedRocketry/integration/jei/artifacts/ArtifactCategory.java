package zmaster587.advancedRocketry.integration.jei.artifacts;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;

public final class ArtifactCategory implements IRecipeCategory<ArtifactWrapper> {

    public static final String UID = "zmaster587.AR.artifacts";
    public static final int WIDTH = 166;
    public static final int SLOT_Y = 26;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotFrame;

    public ArtifactCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, 48);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(AdvancedRocketryItems.itemPlanetIdChip));
        this.slotFrame = guiHelper.getSlotDrawable();
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("jei.advancedrocketry.artifacts.title");
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
    public void setRecipe(IRecipeLayout layout, ArtifactWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 4, SLOT_Y);
        items.setBackground(0, slotFrame);
        items.set(ingredients);
    }
}
