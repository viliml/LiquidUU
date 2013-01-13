package ro.narc.liquiduu.integration;

import buildcraft.api.recipes.RefineryRecipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.liquids.LiquidStack;

import railcraft.common.api.core.items.ItemRegistry;

import ro.narc.liquiduu.LiquidUU;
import ro.narc.liquiduu.CommonProxy;

public class Railcraft {
    public static boolean init() {
        if(LiquidUU.DEBUG) {
            ItemRegistry.printItemTags();
        }

        

        return true;
    }
}
