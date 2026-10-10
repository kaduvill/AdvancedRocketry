package zmaster587.advancedRocketry.integration.jei.artifacts;

import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;

public final class ArtifactRecipeHandler implements IRecipeHandler<ArtifactWrapper> {

    @Override
    public Class<ArtifactWrapper> getRecipeClass() {
        return ArtifactWrapper.class;
    }

    @Override
    public String getRecipeCategoryUid(ArtifactWrapper recipe) {
        return ArtifactCategory.UID;
    }

    @Override
    public IRecipeWrapper getRecipeWrapper(ArtifactWrapper recipe) {
        return recipe;
    }

    @Override
    public boolean isRecipeValid(ArtifactWrapper recipe) {
        return recipe != null && recipe.isValid();
    }
}
