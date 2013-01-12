package ro.narc.liquiduu;

import buildcraft.api.recipes.RefineryRecipe;
import buildcraft.BuildCraftEnergy;
import buildcraft.BuildCraftFactory;
import buildcraft.BuildCraftTransport;
import buildcraft.api.fuels.*;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

import ic2.core.Ic2Items;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import net.minecraftforge.liquids.LiquidContainerData;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;

public class CommonProxy {
    public static int machineBlockID      =  1300;
    public static int liquidUUItemID      = 21001;
    public static int cannedUUItemID      = 21002;
    public static int electricWaterItemID = 21003;

    public static int baseConversionUU        =  3;
    public static int convenienceConversionUU =  5;
    public static int coolantConversionUU     =  2;
    public static int miscConversionUU        =  1;

    public static int baseConversionMJ = 5;

    public static int baseConversion        =  1;
    public static int convenienceConversion =  1;
    public static int coolantConversion     =  2;
    public static int miscConversion        =  5;
    
    public static float baseEffectiveness =
        IronEngineFuel.getFuelForLiquid(new LiquidStack(BuildCraftEnergy.oilStill)).powerPerCycle;
    public static float convenienceEffectiveness =
        IronEngineFuel.getFuelForLiquid(new LiquidStack(BuildCraftEnergy.fuel)).powerPerCycle;
    public static float coolantEffectiveness =
        IronEngineFuel.getFuelForLiquid(new LiquidStack(BuildCraftEnergy.oilStill)).coolingPerUnit;
    
    // Canonical item stacks, liquid stacks, and blocks
    public static ItemStack liquidUUItemStack;
    public static ItemStack electricWaterItemStack;
    public static ItemStack cannedUUItemStack;

    public static LiquidStack liquidUULiquidStack;
    public static LiquidStack electricWaterLiquidStack;

    public static Block machineBlock;
    public static ItemStack accelerator;
    public static ItemStack electrolyzer;

    public void init(Configuration config) {
        loadConfig(config);

        initLiquids();
        initBlocks();
        initLanguage();
        initRecipes();
        initRefineryRecipes();

        NetworkRegistry.instance().registerGuiHandler(LiquidUU.instance, new LiquidUUGUIHandler());
    }

    public void loadConfig(Configuration config) {
        // Machine block ID needs to be transitioned from the previous key at block.accelerator.
        Property machineBlockID;
        if(!config.hasKey("block", "machine") && config.hasKey("block", "accelerator")) {
            machineBlockID = config.getBlock("accelerator", this.machineBlockID);
            machineBlockID.comment = "This configuration key (block.accelerator) is deprecated. It may safely be removed.";
            config.get("block", "machine", machineBlockID.getInt(this.machineBlockID));
        }
        else {
            machineBlockID = config.getBlock("machine", this.machineBlockID);
            if(config.hasKey("block", "accelerator")) {
                Property deprecated = config.get("block", "accelerator", this.machineBlockID);
                deprecated.comment = "This configuration key (block.accelerator) is deprecated. It may safely be removed.";
            }
        }

        Property liquidUUItemID      = config.getItem("liquid.uu", this.liquidUUItemID);
        Property cannedUUItemID      = config.getItem("canned.uu", this.cannedUUItemID);
        Property electricWaterItemID = config.getItem("electricWater", this.electricWaterItemID);

        config.addCustomCategoryComment("conversion", "Conversion ratios for UU used in a Buildcraft refinery. Input will " +
                "always be 1 millibucket of the liquid, output will always be 1 + conversionRatio.");
        config.addCustomCategoryComment("conversion.base", "This category is for liquids that can be further processed (e.g. oil can be processed to fuel).");
        config.addCustomCategoryComment("conversion.convenience", "This category is for liquids that are burned directly (e.g. fuel, biofuel).");
        config.addCustomCategoryComment("conversion.coolant", "This category is for liquids that are used as coolants.");
        config.addCustomCategoryComment("conversion.misc", "This category is for liquids that are not burned at all.");
        Property baseConversionMJ     = config.get("conversion", "mj.per.uu", this.baseConversionMJ);
        baseConversionMJ.comment = "The Refinery will consume this many MJ per unit of UU being converted in each step.";
        Property baseConversionUU = config.get("conversion", "uu.base", this.baseConversionUU);
        baseConversionUU.comment = "All conversions in the \"base\" category will consume this much UU";
        Property convenienceConversionUU = config.get("conversion", "uu.convenience", this.convenienceConversionUU);
        convenienceConversionUU.comment = "All conversions in the \"convenience\" category will consume this much UU";
        Property coolantConversionUU = config.get("conversion", "uu.coolant", this.coolantConversionUU);
        nonfuelConversionUU.comment = "All conversions in the \"coolant\" category will consume this much UU";
        Property miscConversionUU = config.get("conversion", "uu.misc", this.miscConversionUU);
        miscConversionUU.comment = "All conversions in the \"misc\" category will consume this much UU";

        Property baseConversion        = config.get("conversion.base", "base", this.baseConversion);
        Property convenienceConversion = config.get("conversion.convenience", "convenience", this.convenienceConversion);
        Property coolantConversion     = config.get("conversion.coolant", "coolant", this.coolantConversion);
        Property miscConversion        = config.get("conversion.misc", "misc", this.miscConversion)

        Property debugOverride = config.get("general", "debug.override", LiquidUU.DEBUG);
        debugOverride.comment  = "This flag allows you to force LiquidUU debugging on, which may help figure out why stuff broke.";
        LiquidUU.DEBUG = debugOverride.getBoolean(LiquidUU.DEBUG);

        // Looks like it came from the Department of Redundancy Department, but gets the job done...
        this.machineBlockID       = machineBlockID.getInt(this.machineBlockID);
        this.liquidUUItemID       = liquidUUItemID.getInt(this.liquidUUItemID);
        this.cannedUUItemID       = cannedUUItemID.getInt(this.cannedUUItemID);
        this.electricWaterItemID  = electricWaterItemID.getInt(this.electricWaterItemID);

        this.baseConversionMJ = baseConversionMJ.getInt(this.baseConversionMJ);

        this.baseConversionUU        = baseConversionUU.getInt(this.baseConversionUU);
        this.convenienceConversionUU = convenienceConversionUU.getInt(this.convenienceConversionUU);
        this.coolantConversionUU     = coolantConversionUU.getInt(this.nonfuelConversionUU);
        this.miscConversionUU     = miscConversionUU.getInt(this.miscConversionUU);

        this.baseConversion        = baseConversion.getInt(this.baseConversion);
        this.convenienceConversion = convenienceConversion.getInt(this.convenienceConversion);
        this.coolantConversion     = coolantConversion.getInt(this.coolantConversion);
    }

    public void initLiquids() {
        // The liquid items and item stacks
        Item liquidUUItem = new ItemGeneric("liquidUU", 0, liquidUUItemID);
        liquidUUItemStack = new ItemStack(liquidUUItem, 1);

        Item electricWaterItem = new ItemGeneric("electricWater", 2, electricWaterItemID);
        electricWaterItemStack = new ItemStack(electricWaterItem, 1);

        // Our special liquid container
        Item cannedUUItem = new ItemGeneric("cannedUU", 1, cannedUUItemID);
        cannedUUItemStack = new ItemStack(cannedUUItem, 1);

        // Liquid stacks
        liquidUULiquidStack = new LiquidStack(liquidUUItem, 1000);
        liquidUULiquidStack = LiquidDictionary.getOrCreateLiquid("liquidUU", liquidUULiquidStack);

        electricWaterLiquidStack = new LiquidStack(electricWaterItem, 1000);
        electricWaterLiquidStack = LiquidDictionary.getOrCreateLiquid("electrolyzedWater", electricWaterLiquidStack);

        // And the liquid containers that go with the liquid stacks
        LiquidContainerData liquidUUData = new LiquidContainerData(liquidUULiquidStack,
                Ic2Items.matter, Ic2Items.cell);
        LiquidContainerRegistry.registerLiquid(liquidUUData);

        LiquidContainerData electricWaterData = new LiquidContainerData(electricWaterLiquidStack,
                Ic2Items.electrolyzedWaterCell, Ic2Items.cell);
        LiquidContainerRegistry.registerLiquid(electricWaterData);
    }

    public void initBlocks() {
        machineBlock = new BlockGeneric(machineBlockID);
        machineBlock.setCreativeTab(ic2.core.IC2.tabIC2);
        GameRegistry.registerBlock(machineBlock, ItemGenericBlock.class, machineBlock.getBlockName());

        GameRegistry.registerTileEntity(TileEntityAccelerator.class, "Accelerator");
        GameRegistry.registerTileEntity(TileEntityElectrolyzer.class, "liquiduu.Electrolyzer");

        accelerator  = new ItemStack(machineBlock, 1, BlockGeneric.DATA_ACCELERATOR);
        electrolyzer = new ItemStack(machineBlock, 1, BlockGeneric.DATA_ELECTROLYZER);
    }

    public void initLanguage() {
        LanguageRegistry.addName(liquidUUItemStack, "Liquid UU-Matter");
        LanguageRegistry.addName(electricWaterItemStack, "Electrolyzed Water");

        LanguageRegistry.addName(cannedUUItemStack, "UU-Matter Can");

        LanguageRegistry.addName(machineBlock, "LiquidUU Machine");
        LanguageRegistry.addName(accelerator, "Accelerator");
        LanguageRegistry.addName(electrolyzer, "Liquid Electrolyzer");
    }

    public void initRecipes() {
        GameRegistry.addRecipe(accelerator, "TH", "SA", " w",
                Character.valueOf('T'), BuildCraftFactory.tankBlock,
                Character.valueOf('H'), BuildCraftFactory.hopperBlock,
                Character.valueOf('S'), BuildCraftTransport.pipeLiquidsStone,
                Character.valueOf('A'), Ic2Items.advancedCircuit,
                Character.valueOf('w'), BuildCraftTransport.pipeItemsWood
        );
        GameRegistry.addRecipe(accelerator, " ST", "wAH",
                Character.valueOf('T'), BuildCraftFactory.tankBlock,
                Character.valueOf('H'), BuildCraftFactory.hopperBlock,
                Character.valueOf('S'), BuildCraftTransport.pipeLiquidsStone,
                Character.valueOf('A'), Ic2Items.advancedCircuit,
                Character.valueOf('w'), BuildCraftTransport.pipeItemsWood
        );
        GameRegistry.addRecipe(accelerator, "wAH", " ST",
                Character.valueOf('T'), BuildCraftFactory.tankBlock,
                Character.valueOf('H'), BuildCraftFactory.hopperBlock,
                Character.valueOf('S'), BuildCraftTransport.pipeLiquidsStone,
                Character.valueOf('A'), Ic2Items.advancedCircuit,
                Character.valueOf('w'), BuildCraftTransport.pipeItemsWood
        );
    }

    public static void addConversionRecipe(int uuCost, int outputItemID, int outputItemMeta, int outputAmount) {
        Item liquidUUItem = liquidUUItemStack.getItem();

        RefineryRecipe.registerRefineryRecipe(new RefineryRecipe(
            new LiquidStack(liquidUUItem, uuCost),
            new LiquidStack(outputItemID, 1,                outputItemMeta),
            new LiquidStack(outputItemID, 1 + outputAmount, outputItemMeta),
            uuCost * baseConversionMJ, 1
        ));
    }

    public void initRefineryRecipes() {
        addConversionRecipe(convenienceConversionUU, Block.lavaStill.blockID, 0, lavaConversion);

        addConversionRecipe(baseConversionUU, BuildCraftEnergy.oilStill.blockID, 0, baseConversion);
        addConversionRecipe(convenienceConversionUU, BuildCraftEnergy.fuel.shiftedIndex, 0, convenienceConversion);

        addConversionRecipe(nonfuelConversionUU, Ic2Items.coolant.itemID, Ic2Items.coolant.getItemDamage(), coolantConversion);
        
        for (LiquidStack liquid: LiquidDictionary.getLiquids())
        {
            if (!liquid.isLiquidEqual(new LiquidStack(Block.lavaStill, 1)) ||
                !liquid.isLiquidEqual(new LiquidStack(Block.waterStill, 1)) ||
                !liquid.isLiquidEqual(new LiquidStack(BuildCraftEnergy.oilStill, 1)) ||
                !liquid.isLiquidEqual(new LiquidStack(BuildCraftEnergy.fuel, 1)))
                    continue;
            if ((IronEngineFuel fuel = IronEngineFuel.fuels.getFuelForLiquid(liquid)) != null)
            {
                if (RefineryRecipe.findRefineryRecipe(liquid, null) != null) 
                    addConversionRecipe(baseConversionUU, liquid.itemID, liquid.itemMeta, baseConversion*(baseEffectiveness/fuel.powerPerCycle));
                else addConversionRecipe(convenienceConversionUU, liquid.itemID, liquid.itemMeta, convenienceConversion*(convenienceEffectiveness/fuel.powerPerCycle));
            }
            else if ((IronEngineCoolant coolant = IronEngineCoolant.coolants.getCoolantForLiquid(liquid)) != null)
                addConversionRecipe(coolantConversionUU, liquid.itemID, liquid.itemMeta, coolantConversion*(coolantEffectiveness/coolant.coolingPerUnit));
            else addConversionRecipe(miscConversionUU, liquid.itemID, liquid.itemMeta, miscConversion);
        }
    }
}
