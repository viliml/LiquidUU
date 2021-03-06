package ro.narc.liquiduu;

import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;


public class LiquidUUTank extends LiquidTank {
    public LiquidUUTank(int capacity) {
        super(capacity);
    }

    public int getLiquidAmount() {
        LiquidStack liquid = this.getLiquid();

        if(liquid == null) {
            return 0;
        }

        return liquid.amount;
    }
}
