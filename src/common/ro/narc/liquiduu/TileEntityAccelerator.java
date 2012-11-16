package ro.narc.liquiduu;

import cpw.mods.fml.common.Side;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICrafting;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet54PlayNoteBlock;
import net.minecraft.src.TileEntity;

import ic2.api.IWrenchable;
import ic2.common.Ic2Items;
import ic2.common.TileEntityMachine;
import ic2.common.TileEntityElectricMachine;
import ic2.common.TileEntityRecycler;

import buildcraft.api.core.Orientations;
import buildcraft.api.liquids.ILiquidTank;
import buildcraft.api.liquids.ITankContainer;
import buildcraft.api.liquids.LiquidManager;
import buildcraft.api.liquids.LiquidStack;
import buildcraft.api.inventory.ISpecialInventory;

public class TileEntityAccelerator extends TileEntityMachine implements IWrenchable, ISpecialInventory, ITankContainer {
    public short facing;
    public short prevFacing;

    public boolean initialized = false;

    public LiquidUUTank tank;

    public TileEntityAccelerator() {
        super(3); // inventory slots
        this.blockType = LiquidUU.liquidUUBlock;
        this.tank = new LiquidUUTank(2000);
    }

    public void updateEntity() {
        if(isInvalid()) {
            return;
        }

        if(!initialized) {
            initialize();
        }

        if((inventory[2] != null) && isItemStackUUM(inventory[2])) {
            if(tank.getLiquidAmount() <= 1000) {
                fill(0, LiquidUU.liquidUUStack.copy(), true);
                reduceInventorySlot(2, 1);
            }
        }
    }

    public void initialize() {
        initialized = true;
    }

    public Packet getDescriptionPacket() {
        return new Packet54PlayNoteBlock(xCoord, yCoord, zCoord, LiquidUU.liquidUUBlock.blockID, BlockGeneric.EID_FACING, facing);
    }

    /* Facings: YNeg=0; YPos=1; ZNeg=2; ZPos=3; XNeg=4; XPos=5 */
    public TileEntity getConnectedMachine() {
        int x = xCoord;
        int y = yCoord;
        int z = zCoord;

        switch(facing) {
            case 0:
                y--; break;
            case 1:
                y++; break;
            case 2:
                z--; break;
            case 3:
                z++; break;
            case 4:
                x--; break;
            case 5:
                x++; break;
        }

        return worldObj.getBlockTileEntity(x, y, z);
    }

    public ItemStack getConnectedMachineItem() {
        TileEntity te = getConnectedMachine();
        if(te == null) {
            return null;
        }

        int machineId = worldObj.getBlockId(te.xCoord, te.yCoord, te.zCoord);
        Block machineBlock = Block.blocksList[machineId];

        return machineBlock.getPickBlock(null, worldObj, te.xCoord, te.yCoord, te.zCoord);
    }

    public String getInvName() {
        return "liquiduu.accelerator";
    }

    public boolean wrenchCanSetFacing(EntityPlayer player, int side) {
        if(this.facing != side) {
            return true;
        }
        return false;
    }

    public short getFacing() {
        return this.facing;
    }

    public void setFacing(short facing) {
        this.facing = facing;

        if(LiquidUU.getSide() == Side.CLIENT) {
            return; // Client is a poopyface.
        }

        if(facing != this.prevFacing) {
            if(LiquidUU.DEBUG_NETWORK) {
                System.out.println("SetFacing: " + worldObj + ".addBlockEvent(" + xCoord + ", " + yCoord + ", " + zCoord + ", " + LiquidUU.liquidUUBlock.blockID + ", " + BlockGeneric.EID_FACING + ", " + facing + ")");
            }
            worldObj.addBlockEvent(xCoord, yCoord, zCoord, LiquidUU.liquidUUBlock.blockID, BlockGeneric.EID_FACING, facing);
        }
        this.prevFacing = facing;
    }

    public boolean wrenchCanRemove(EntityPlayer player) {
        return true;
    }

    public float getWrenchDropRate() {
        return 1.0F;
    }

    public boolean isItemStackUUM(ItemStack stack) {
        return stack.isItemEqual(Ic2Items.matter) || stack.isItemEqual(LiquidUU.cannedUU);
    }

    public int getOperationCost() {
        InstantRecipe recipe = getActiveRecipe();

        if(recipe == null) {
            return 0;
        }

        return recipe.uuCost;
    }

    // We have a negative-indexed slot, so we need to override these IInventory bits:
    public synchronized ItemStack getStackInSlot(int slotnum)
    {
        if(slotnum < -1) {
            return null;
        }
        else if(slotnum == -1) {
            return getConnectedMachineItem();
        }
        else if (slotnum == 1) {
            ItemStack output = null;

            if(inventory[1] != null) {
                output = inventory[1].copy();
            }

            InstantRecipe recipe = getActiveRecipe();
            int batches = batchesPossible(recipe);

            if (batches <= 0) {
                return output;
            }

            if(output == null) {
                output = recipe.output.copy();
                output.stackSize *= batches;
            }
            else {
                output.stackSize += recipe.output.stackSize * batches;
            }

            return output;
        }
        return super.getStackInSlot(slotnum);
    }

    public synchronized ItemStack decrStackSize(int slotnum, int amount)
    {
        if (slotnum < 0) {
            return null;
        }
        else if (slotnum == 1) {
            return getOutput(amount, true);
        }
        return super.decrStackSize(slotnum, amount);
    }

    public synchronized void setInventorySlotContents(int slotnum, ItemStack itemstack) {
        if(slotnum < 0 || slotnum == 1) {
            return;
        }
        super.setInventorySlotContents(slotnum, itemstack);
    }

     public InstantRecipe getActiveRecipe() {
        TileEntity te = getConnectedMachine();

        if(te == null) {
            return null;
        }

        InstantRecipe recipe = null;

        if(inventory[0] == null) {
            return null;
        }

        if (te instanceof IAcceleratorFriend) {
            IAcceleratorFriend pal = (IAcceleratorFriend) te;

            if (!pal.instantReady(inventory[0])) {
                // Fine, fine, we won't force you to.
                return null;
            }

            recipe = pal.getInstantRecipe(inventory[0]);

            if(recipe == null) { // Why didn't you say so before? Eh, whatever.
                return null;
            }
        }
        else if (te instanceof TileEntityElectricMachine) {
            if (te instanceof TileEntityRecycler) {
                // We don't do garbage duty.
                return null;
            }

            TileEntityElectricMachine machine = (TileEntityElectricMachine) te;

            ItemStack input = inventory[0].copy();
            ItemStack result = machine.getResultFor(input, true);

            // Get the true input stack size.
            input.stackSize = inventory[0].stackSize - input.stackSize;

            int cost = (machine.defaultEnergyConsume * machine.defaultOperationLength
                        * machine.defaultOperationLength) / 10000;

            if (cost == 0) {
                cost = 1;
            }

            recipe = new InstantRecipe(input, result, cost);
        }

        if (recipe.validate()) {
            return recipe;
        }

        return null;
    }

    public void consumeUU(int amount) {
        tank.drain(amount, true);
    }

    public int batchesPossible() {
        return batchesPossible(getActiveRecipe());
    }

    // Protected because it's assumed that activeRecipe == this.getActiveRecipe().
    protected int batchesPossible(InstantRecipe activeRecipe) {
        if (activeRecipe == null) {
            // Do what, now?
            return 0;
        }

        if (inventory[1] != null) {
            if (!activeRecipe.output.isItemEqual(inventory[1])) {
                return 0;
            }
        }

        int count = inventory[0].stackSize / activeRecipe.getInputSize();

        count = Math.min(count, tank.getLiquidAmount() / activeRecipe.uuCost);

        if (activeRecipe.machine != null) {
            count = Math.min(count, activeRecipe.machine.instantCapacity(activeRecipe, count));
        }

        int max_additional;
        if (inventory[1] != null) {
            max_additional = inventory[1].getMaxStackSize() - inventory[1].stackSize;
        }
        else {
            max_additional = activeRecipe.output.getMaxStackSize();
        }

        count = Math.min(count, max_additional/activeRecipe.getOutputSize());

        return Math.max(0, count);
    }

    // Protected because it's assumed that activeRecipe == this.getActiveRecipe() and that
    // batches <= batchesPossible(activeRecipe)
    protected ItemStack process(InstantRecipe activeRecipe, int batches, boolean wetRun) {
        if((activeRecipe == null) || (batches <= 0)) {
            return null;
        }

        if(wetRun) {
            if (activeRecipe.machine != null) {
                activeRecipe.machine.instantProcess(activeRecipe, batches);
            }

            consumeUU(activeRecipe.uuCost * batches);
            reduceInventorySlot(0, activeRecipe.getInputSize() * batches);
        }
        ItemStack out = activeRecipe.output.copy();
        out.stackSize *= batches;

        return out;
    }

   public synchronized ItemStack getOutput(int amount, boolean wetRun) {
        if(amount <= 0) {
            return null;
        }

        InstantRecipe recipe = getActiveRecipe();
        int maxBatches = batchesPossible(recipe);

        // Try to request fewer (even zero) batches if we already have some in storage.
        int amount_ready = 0;
        if(inventory[1] != null) {
            amount_ready = inventory[1].stackSize;
        }

        int batches = 0;
        if(recipe != null) {
            float batches_requested = ((float) amount - amount_ready) / recipe.getOutputSize();
            batches = Math.min(maxBatches, MathHelper.ceiling_float_int(batches_requested));
        }

        // Go ahead and make the batches (simulate, if appropriate)
        ItemStack output = process(recipe, batches, wetRun);
        if(output == null) {
            if(inventory[1] == null) {
                return null;
            }

            output = inventory[1];
        }
        else {
            output.stackSize += amount_ready;
        }

        // Did we end up with more than we needed? Store the excess.
        if(wetRun) {
            inventory[1] = output.copy();
            reduceInventorySlot(1, amount);
        }
        output.stackSize = Math.min(amount, output.stackSize);

        return output;
    }

    // Convenience function.
    public synchronized void reduceInventorySlot(int slot, int amount) {
        if(inventory[slot] == null) {
            return;
        }

        inventory[slot].stackSize -= amount;

        if(inventory[slot].stackSize <= 0) {
            inventory[slot] = null;
        }
    }

    public synchronized int addToInventorySlot(int slot, ItemStack stack, boolean wetRun) {
        if((inventory[slot] != null) && !stack.isItemEqual(inventory[slot])) {
            return 0;
        }

        ItemStack workStack = inventory[slot];
        if(workStack == null) {
            workStack = stack.copy();
            workStack.stackSize = 0;
        }

        int transferAmount = Math.min(stack.stackSize,
                                      stack.getMaxStackSize() - workStack.stackSize);

        if(wetRun) {
            workStack.stackSize += transferAmount;
            inventory[slot] = workStack;
        }

        return transferAmount;
    }

    public synchronized int addItem(ItemStack stack, boolean wetRun, Orientations side) {
        if(isItemStackUUM(stack)) {
            return addToInventorySlot(2, stack, wetRun);
        }

        return addToInventorySlot(0, stack, wetRun);
    }

    public ItemStack[] extractItem(boolean wetRun, Orientations side, int amount) {
        ItemStack out = getOutput(amount, wetRun);

        return new ItemStack[]{out};
    }

    public int fill(Orientations side, LiquidStack resource, boolean wetRun) {
        return fill(0, resource, wetRun);
    }

    public int fill(int tankIndex, LiquidStack resource, boolean wetRun) {
        if((tankIndex != 0) || !LiquidUU.liquidUUStack.isLiquidEqual(resource)) {
            return 0; // What's this crap you're trying to feed me?
        }

        return tank.fill(resource, wetRun);
    }

    public LiquidStack drain(Orientations side, int amount, boolean wetRun) {
        return null; // No, you may not have my UUM!
    }

    public LiquidStack drain(int tankIndex, int amount, boolean wetRun) {
        return null;
    }

    public ILiquidTank[] getTanks() {
        return new ILiquidTank[]{tank};
    }

    public void getGUINetworkData(int key, int value) {
        switch(key) {
            case 0:
                LiquidStack uu = tank.getLiquid();
                if(uu == null) {
                    uu = LiquidUU.liquidUUStack.copy();
                }

                uu.amount = value;
                tank.setLiquid(uu);
                break;
            default:
                System.err.println("LiquidUU: Accelerator got unknown GUI network data for key " + key + ": " + value);
                break;
        }
    }

    public void sendGUINetworkData(ContainerAccelerator container, ICrafting iCrafting) {
        iCrafting.updateCraftingInventoryInfo(container, 0, tank.getLiquidAmount());
    }

    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.prevFacing = this.facing = tag.getShort("face");
        tank.setLiquid(LiquidStack.loadLiquidStackFromNBT(tag.getCompoundTag("uumTank")));
    }

    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setShort("face", this.facing);
        if(tank.getLiquid() != null) {
            NBTTagCompound newTag = new NBTTagCompound();
            tank.getLiquid().writeToNBT(newTag);
            tag.setCompoundTag("uumTank", newTag);
        }
    }
}