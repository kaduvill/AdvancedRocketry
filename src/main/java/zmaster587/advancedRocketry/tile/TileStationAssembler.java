package zmaster587.advancedRocketry.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.item.ItemPackedStructure;
import zmaster587.advancedRocketry.item.ItemStationChip;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.util.StorageChunk;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.util.EmbeddedInventory;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class TileStationAssembler extends TileRocketAssemblingMachine implements IInventory {

    EmbeddedInventory inventory;
    Long storedId;

    public TileStationAssembler() {
        super();
        inventory = new EmbeddedInventory(4);
        status = ErrorCodes.EMPTY;
    }

    @Override
    protected boolean handlesRocketLifecycleEvents() {return false;}

    @Override
    public boolean canScan() {
        if (!super.canScan())
            return false;
        ItemStack stack = new ItemStack(AdvancedRocketryBlocks.blockLoader, 1, 1);

        if (inventory.getStackInSlot(0).isEmpty() || !stack.isItemEqual(inventory.getStackInSlot(0))) {
            status = ErrorCodes.NOSATELLITEHATCH;
            return false;
        }

        if (inventory.getStackInSlot(1).isEmpty() || AdvancedRocketryItems.itemSpaceStationChip != inventory.getStackInSlot(1).getItem()) {
            status = ErrorCodes.NOSATELLITECHIP;
            return false;
        }
        if (!inventory.getStackInSlot(2).isEmpty() || !inventory.getStackInSlot(3).isEmpty()) {
            status = ErrorCodes.OUTPUTBLOCKED;
            return false;
        }

        return true;
    }

    @Override
    public AxisAlignedBB scanRocket(World world, BlockPos pos2, AxisAlignedBB bb) {

        int actualMinX = (int) bb.maxX,
            actualMinY = (int) bb.maxY,
            actualMinZ = (int) bb.maxZ,
            actualMaxX = (int) bb.minX,
            actualMaxY = (int) bb.minY,
            actualMaxZ = (int) bb.minZ;

        boolean foundNonAir = false;

        for (int xCurr = (int) bb.minX; xCurr <= bb.maxX; xCurr++) {
            for (int zCurr = (int) bb.minZ; zCurr <= bb.maxZ; zCurr++) {
                for (int yCurr = (int) bb.minY; yCurr <= bb.maxY; yCurr++) {

                    BlockPos posCurr = new BlockPos(xCurr, yCurr, zCurr);

                    if (!world.isAirBlock(posCurr)) {
                        foundNonAir = true;

                        if (xCurr < actualMinX) actualMinX = xCurr;
                        if (yCurr < actualMinY) actualMinY = yCurr;
                        if (zCurr < actualMinZ) actualMinZ = zCurr;
                        if (xCurr > actualMaxX) actualMaxX = xCurr;
                        if (yCurr > actualMaxY) actualMaxY = yCurr;
                        if (zCurr > actualMaxZ) actualMaxZ = zCurr;
                    }
                }
            }
        }

        // Tell the player whats up
        if (!foundNonAir) {
            status = ErrorCodes.EMPTY;              // nothing to pack inside bb
            return bb;                              // sanity check 
        } else {
            status = ErrorCodes.SUCCESS_STATION;    // ok to proceed with packing
        }

        return new AxisAlignedBB(actualMinX, actualMinY, actualMinZ, actualMaxX, actualMaxY, actualMaxZ);
    }

    @Override
    public void assembleRocket() {
        if (world.isRemote || bbCache == null) return;
        //Need to scan again b/c something may have changed
        scanRocket(world, pos, bbCache);

        if (status != ErrorCodes.SUCCESS_STATION) {
            syncStatsToClient();
            return;
        }
        StorageChunk storageChunk;
        try {
            storageChunk = StorageChunk.cutWorldBB(world, bbCache);
        } catch (NegativeArraySizeException e) {
            status = ErrorCodes.FAIL_CUT;
            syncStatsToClient();
            return;
        }

        ItemStack outputStack = new ItemStack(AdvancedRocketryItems.itemSpaceStation, 1);
        ItemStack outputChip;
        if (storedId == null) {
            SpaceStationObject spaceStationObject = new SpaceStationObject();
            SpaceObjectManager.getSpaceManager().registerSpaceObject(spaceStationObject, Constants.INVALID_PLANET);
            int stationId = spaceStationObject.getId();
            ItemStationChip.setUUID(outputStack, stationId);
            outputChip = new ItemStack(AdvancedRocketryItems.itemSpaceStationChip, 1);
            ItemStationChip.setUUID(outputChip, stationId);
        } else {
            int stationId = storedId.intValue();
            ItemStationChip.setUUID(outputStack, stationId);
            outputChip = inventory.getStackInSlot(1).copy();
            outputChip.setCount(1);
            ItemStationChip.setUUID(outputChip, stationId);
        }
        ((ItemPackedStructure) outputStack.getItem()).setStructure(outputStack, storageChunk);
        inventory.setInventorySlotContents(2, outputStack);
        inventory.setInventorySlotContents(3, outputChip);

        this.status = ErrorCodes.FINISHED;
        storedId = null;
        inventory.decrStackSize(0, 1);
        inventory.decrStackSize(1, 1);
        syncStatsToClient();
    }

    @Override
    protected void updateText() {
        if (world != null && !world.isRemote) {
            if (getRocketPadBounds(world, pos) == null) {
                setStatus(ErrorCodes.INCOMPLETESTRCUTURE.ordinal());
            } else if (ErrorCodes.INCOMPLETESTRCUTURE.equals(getStatus())) {
                setStatus(ErrorCodes.UNSCANNED_STATION.ordinal());
            }
        }

        if (errorText != null) {
            errorText.setText(status.getErrorCode());
        }
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> modules = new LinkedList<>();

        // GUI-open reset errorcode if pad is valid and we're idle
        if (!world.isRemote) {
            ErrorCodes oldStatus = status;
            AxisAlignedBB bounds = getRocketPadBounds(world, pos);
            if (bounds == null) {
                setStatus(ErrorCodes.INCOMPLETESTRCUTURE.ordinal());
            } else if (!isScanning()) {
                ErrorCodes s = getStatus();
                if (s == ErrorCodes.SUCCESS_STATION || s == ErrorCodes.SUCCESS ||
                        s == ErrorCodes.FINISHED || s == ErrorCodes.EMPTY ||
                        s == ErrorCodes.UNSCANNED) {
                    setStatus(ErrorCodes.UNSCANNED_STATION.ordinal());
                }
            }
            if (status != oldStatus) {
                syncStatsToClient();
            }
        }
        modules.add(new ModulePower(160, 30, this));
        modules.add(new ModuleProgress(149, 30, 2, verticalProgressBar, this));
        modules.add(new ModuleButton(5, 34, 0, LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.scan"), this, zmaster587.libVulpes.inventory.TextureResources.buttonScan));
        ModuleButton buttonBuild;
        modules.add(buttonBuild = new ModuleButton(5, 60, 1, LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.build"), this, zmaster587.libVulpes.inventory.TextureResources.buttonBuild));
        buttonBuild.setColor(0xFFFF2222);
        modules.add(errorText = new ModuleText(5, 22, "", 0xFFFFFF22));
        updateText();

        modules.add(new ModuleLimitedSlotArray(90, 40, this, 0, 1));
        modules.add(new ModuleTexturedLimitedSlotArray(108, 40, this, 1, 2, TextureResources.idChip));
        modules.add(new ModuleOutputSlotArray(90, 60, this, 2, 4));

        return modules;
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
        if (world != null && !world.isRemote && id == 1 && !isScanning()) {
            ItemStack chipStack = inventory.getStackInSlot(1);
            if (!chipStack.isEmpty() && chipStack.getItem() == AdvancedRocketryItems.itemSpaceStationChip) {
                int stationId = ItemStationChip.getUUID(chipStack);
                storedId = stationId == 0 ? null : (long) stationId;
            } else {storedId = null;}
            markDirty();
        }

        super.useNetworkData(player, side, id, nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        inventory.writeToNBT(nbt);
        if (storedId != null) {
            nbt.setLong("storedID", storedId);
        }
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inventory.readFromNBT(nbt);
        storedId = nbt.hasKey("storedID") ? nbt.getLong("storedID") : null;
    }

    @Override
    public int getSizeInventory() {
        return inventory.getSizeInventory();
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    @Nonnull
    public ItemStack decrStackSize(int slot, int amt) {
        return inventory.decrStackSize(slot, amt);
    }

    @Override
    public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
        inventory.setInventorySlotContents(slot, stack);
    }

    @Override
    public String getName() {
        return "tile.stationBuilder.name";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return inventory.getInventoryStackLimit();
    }

    @Override
    public boolean isUsableByPlayer(@Nonnull EntityPlayer player) {
        return inventory.isUsableByPlayer(player);
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public void openInventory(EntityPlayer pos) {    }

    @Override
    public void closeInventory(EntityPlayer pos) {    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == 0) {
            ItemStack satelliteHatch =
                    new ItemStack(AdvancedRocketryBlocks.blockLoader, 1, 1);
            return satelliteHatch.isItemEqual(stack);}
        if (slot == 1) {
            return stack.getItem() == AdvancedRocketryItems.itemSpaceStationChip;}
        return false;
    }

    @Override
    @Nonnull
    public ItemStack removeStackFromSlot(int index) {
        return inventory.removeStackFromSlot(index);
    }

    @Override
    public int getField(int id) {
        return inventory.getField(id);
    }

    @Override
    public void setField(int id, int value) {
        inventory.setField(id, value);
    }

    @Override
    public int getFieldCount() {
        return inventory.getFieldCount();
    }

    @Override
    public void clear() {
        inventory.clear();
    }
}