package ro.narc.liquiduu;

import net.minecraft.src.ItemStack;

public interface IAcceleratorFriend {
    // Is this machine ready to use this input?
    public boolean instantReady(ItemStack input);

    // What's the recipe for this input?
    public InstantRecipe getInstantRecipe(ItemStack input);

    // How many of these batches can the machine handle right now?
    // NOTE: Only called in advanced mode (recipe.simple == false)
    public int instantCapacity(InstantRecipe recipe, int batches);

    // We have made some batches of the recipe.
    // NOTE: Only called in advanced mode (recipe.simple == false)
    public void instantProcess(InstantRecipe recipe, int batches);
}