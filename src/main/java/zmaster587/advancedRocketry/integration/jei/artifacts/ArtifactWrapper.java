package zmaster587.advancedRocketry.integration.jei.artifacts;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.integration.jei.PlanetRecipeHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ArtifactWrapper implements IRecipeWrapper {

    private final int dimId;
    private final String planetName;
    private final String starName;
    private final ResourceLocation planetIcon;
    private final ItemStack artifact;
    private final int hashCode;

    public ArtifactWrapper(int dimId, String planetName, String starName,
                           ResourceLocation planetIcon, ItemStack artifact) {
        this.dimId = dimId;
        this.planetName = planetName == null ? "" : planetName;
        this.starName = starName == null ? "" : starName;
        this.planetIcon = planetIcon;
        this.artifact = artifact == null ? ItemStack.EMPTY : artifact.copy();
        this.hashCode = Objects.hash(dimId, this.planetName, this.starName, planetIcon,
                this.artifact.getItem(), this.artifact.getItemDamage(), this.artifact.getCount(),
                this.artifact.getTagCompound());
    }

    public boolean isValid() {
        return !planetName.isEmpty() && !artifact.isEmpty() && artifact.getItem().getRegistryName() != null;
    }

    public ItemStack getArtifact() {
        return artifact.copy();
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(VanillaTypes.ITEM, Collections.singletonList(getArtifact()));
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        PlanetRecipeHeader.draw(minecraft, recipeWidth, planetIcon, planetName, getOrbitingText(), "");
        minecraft.fontRenderer.drawString(
                minecraft.fontRenderer.trimStringToWidth(artifact.getDisplayName(), Math.max(0, recipeWidth - 30)),
                26, ArtifactCategory.SLOT_Y + 5, 0x404040
        );
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        if (mouseX < 0 || mouseX >= ArtifactCategory.WIDTH || mouseY < 0 || mouseY >= 22) {
            return Collections.emptyList();
        }

        List<String> tooltip = new ArrayList<>();
        tooltip.add(planetName);
        if (!starName.isEmpty()) tooltip.add(getOrbitingText());
        return tooltip;
    }

    private String getOrbitingText() {
        return starName.isEmpty() ? "" : I18n.format("jei.advancedrocketry.artifacts.orbiting", starName);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ArtifactWrapper)) return false;

        ArtifactWrapper recipe = (ArtifactWrapper) other;
        return dimId == recipe.dimId
                && planetName.equals(recipe.planetName)
                && starName.equals(recipe.starName)
                && Objects.equals(planetIcon, recipe.planetIcon)
                && ItemStack.areItemStacksEqual(artifact, recipe.artifact);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
