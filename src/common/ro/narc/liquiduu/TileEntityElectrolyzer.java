package ro.narc.liquiduu;

import cpw.mods.fml.relauncher.Side;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.*;
import ic2.api.energy.tile.*;

import ic2.api.Direction;
import ic2.api.IWrenchable;

public class TileEntityElectrolyzer extends TileEntity implements IWrenchable, IHasMachineFaces, ITankContainer, IEnergySink, IEnergySource {
    public short facing;
    public short prevFacing;
    public MachineFace[] faces = new MachineFace[6];

    public boolean initialized = false;
    public boolean convertsWater = true;
    
    public LiquidTank waterTank = new LiquidTank(16 *
    		LiquidContainerRegistry.BUCKET_VOLUME);
    public LiquidTank electroTank = new LiquidTank(16 *
    		LiquidContainerRegistry.BUCKET_VOLUME);
    
    public TileEntityElectrolyzer() {
        super();
        this.blockType = LiquidUU.liquidUUBlock;
    }

    public void rotateFaces(short prevFront, short newFront) {
        // TODO: Actually rotate the faces rather than just setting all of them to None except the front.
        for(int i = 0; i < faces.length; i++) {
            faces[i] = MachineFace.None;
        }
        faces[newFront] = MachineFace.ElectrolyzerFront;

        // TODO: resend description packet, hopefully causing the client to redraw us
    }

//public interface IWrenchable {
    public boolean wrenchCanSetFacing(EntityPlayer player, int side) {
        if(side != facing) {
            return true;
        }

        return false;
    }

    public short getFacing() {
        return facing;
    }

    public void setFacing(short side) {
        if(side < 2) { // DOWN or UP -- just ignore them. We cannot face that way, ever.
            return;
        }

        facing = side;

        if(facing != prevFacing) {
            rotateFaces(prevFacing, facing);
            prevFacing = facing;
        }
    }

    public boolean wrenchCanRemove(EntityPlayer player) {
        return true;
    }

    public float getWrenchDropRate() {
        return 1.0F;
    }

    public ItemStack getWrenchDrop(EntityPlayer player) {
        return LiquidUU.electrolyzer;
    }
//}

//public interface IHasMachineFaces {
    public MachineFace getMachineFace(int side) {
        return faces[side];
    }
//}

	@Override
	public int fill(ForgeDirection from, LiquidStack resource, boolean doFill) {
		if (from == ForgeDirection.DOWN ||
			from == ForgeDirection.UP ||
			from == ForgeDirection.NORTH ||
			from == ForgeDirection.SOUTH)
				return 0;
		if (resource.isLiquidEqual(new ItemStack(Block.waterStill)) &&
			convertsWater &&
			from == ForgeDirection.EAST)
		{
			return waterTank.fill(resource, doFill);
		}
		if (resource.isLiquidEqual(LiquidUU.electrolyzedWaterStack) &&
			!convertsWater &&
			from == ForgeDirection.WEST)
		{
			return electroTank.fill(resource, doFill);
		}
		return 0;
	}

	@Override
	public int fill(int tankIndex, LiquidStack resource, boolean doFill) {
		if (tankIndex<0 || tankIndex>1)
					return 0;
			if (resource.isLiquidEqual(new ItemStack(Block.waterStill)) &&
				convertsWater &&
				tankIndex == 0)
			{
				return waterTank.fill(resource, doFill);
			}
			if (resource.isLiquidEqual(LiquidUU.electrolyzedWaterStack) &&
				!convertsWater &&
				tankIndex == 1)
			{
				return electroTank.fill(resource, doFill);
			}
			return 0;
	}

	@Override
	public LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		if (from == ForgeDirection.DOWN ||
			from == ForgeDirection.UP ||
			from == ForgeDirection.NORTH ||
			from == ForgeDirection.SOUTH)
					return null;
			if (convertsWater &&
				from == ForgeDirection.EAST)
			{
				return waterTank.drain(maxDrain, doDrain);
			}
			if (convertsWater &&
				from == ForgeDirection.WEST)
			{
				return electroTank.drain(maxDrain, doDrain);
			}
			return null;
	}

	@Override
	public LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain) {
		if (tankIndex<0 || tankIndex>1)
			return null;
		if (!convertsWater &&
			tankIndex == 0)
		{
			return waterTank.drain(maxDrain, doDrain);
		}
		if (convertsWater &&
			tankIndex == 1)
		{
			return electroTank.drain(maxDrain, doDrain);
		}
		return null;
	}

	@Override
	public ILiquidTank[] getTanks(ForgeDirection direction) {
		if (direction == ForgeDirection.DOWN ||
			direction == ForgeDirection.UP ||
			direction == ForgeDirection.NORTH ||
			direction == ForgeDirection.SOUTH)
				return null;
		if (direction == ForgeDirection.EAST)
			return new ILiquidTank[] {waterTank};
		if (direction == ForgeDirection.WEST)
			return new ILiquidTank[] {electroTank};
		return null;
	}

	@Override
	public ILiquidTank getTank(ForgeDirection direction, LiquidStack type) {
		if (direction == ForgeDirection.DOWN ||
			direction == ForgeDirection.UP ||
			direction == ForgeDirection.NORTH ||
			direction == ForgeDirection.SOUTH)
				return null;
		if (direction == ForgeDirection.EAST &&
			type.isLiquidEqual(waterTank.getLiquid()))
				return waterTank;
		if (direction == ForgeDirection.WEST &&
			type.isLiquidEqual(electroTank.getLiquid()))
			return electroTank;
		return null;
	}

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction) {
		return direction.toForgeDirection() == ForgeDirection.NORTH &&
			convertsWater;
	}

	@Override
	public boolean isAddedToEnergyNet() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, Direction direction) {
		return direction.toForgeDirection() == ForgeDirection.SOUTH &&
			!convertsWater;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 32;
	}

	@Override
	public int demandsEnergy() {
		if (!convertsWater) return 0;
		return 32;
	}

	@Override
	public int injectEnergy(Direction directionFrom, int amount) {
		if (directionFrom.toForgeDirection() != ForgeDirection.NORTH)
			return amount;
		if (!convertsWater) return amount;
		if (waterTank.drain((int)((float)amount / 15000F *
				(float)LiquidContainerRegistry.BUCKET_VOLUME),
				false) != null &&
			electroTank.fill(
				new LiquidStack(LiquidUU.electrolyzedWater.getItem(),
					(int)((float)amount / 15000F *
						(float)LiquidContainerRegistry.BUCKET_VOLUME)),
				false) != 0 &&
			amount > 0)
		{
			amount -= electroTank.fill(
			new LiquidStack(LiquidUU.electrolyzedWater.getItem(),
				waterTank.drain((int)((float)amount / 15000F *
						(float)LiquidContainerRegistry.BUCKET_VOLUME), true).amount),
				true)*15000;
			return amount;
		}
		return 0;
	}

	@Override
	public int getMaxSafeInput() {
		return 32;
	}
}
