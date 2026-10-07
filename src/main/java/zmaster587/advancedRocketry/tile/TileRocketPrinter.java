package zmaster587.advancedRocketry.tile;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.advancedRocketry.util.RocketBlueprint;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IUniversalEnergy;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.cap.ForgePowerCapability;
import zmaster587.libVulpes.cap.TeslaHandler;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.multiblock.hatch.TileInventoryHatch;
import zmaster587.libVulpes.util.EmbeddedInventory;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.UniversalBattery;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TileRocketPrinter extends TileInventoryHatch implements ITickable, IButtonInventory, INetworkMachine, IUniversalEnergy, IProgressBar {
    private static final int BLOCKS_PER_TICK = 4, CHECKS_PER_TICK = 128, MAX_SLOTS_PER_HANDLER = 256;
    private static final int SAVE_TICKS_PER_LAYER = 16, PRINT_TICKS_PER_LAYER = 24;
    private static final int ENERGY_CAPACITY = 100000, ENERGY_PER_TICK = 100;
    public static final byte READY = 0, NO_ASSEMBLER = 1, NO_ROCKET = 2, SAVED = 3, NO_BLUEPRINT = 4, INVALID = 5,
            PAD_BUSY = 6, BAD_PAD = 7, BLOCKED = 8, MISSING = 9, UNSUPPORTED = 10, PRINTING = 11, PAUSED = 12, COMPLETE = 13,
            DETECTED = 14, BLUEPRINT_READY = 15, CHUNKS_UNLOADED = 16, SAVING = 17, NO_POWER = 18;
    private byte status = READY;
    private boolean printing, saving;
    private RocketBlueprint pendingBlueprint;
    private ItemStack saveStack = ItemStack.EMPTY;
    private EntityRocket saveRocket;
    private TileRocketAssemblingMachine saveAssembler;
    private EnumFacing saveFacing;
    private RocketBlueprint job;
    private ItemStack jobStack = ItemStack.EMPTY;
    private NBTTagCompound jobTag;
    private BlockPos origin;
    private BlockPos attachedPos;
    private EnumFacing attachedFacing;
    private AxisAlignedBB pad;
    private int index, layer = -1, verificationY;
    private int frameFromY = -1, frameToY = -1, frameDuration;
    private long frameStartTick;
    private long framePausedAt = -1;
    private int clientFrameElapsed;
    private int operationTotal, guiProgress;
    private final UniversalBattery energy = new UniversalBattery(ENERGY_CAPACITY);
    private final ForgePowerCapability energyCapability = new ForgePowerCapability(this);
    private int[] sourceSides, sourceSlots;
    private ModuleText statusText;
    private ModuleButton actionButton;
    private ModuleBase blueprintPreview;

    public TileRocketPrinter() {
        super(1);
        inventory = new EmbeddedInventory(1, this) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileRocketPrinter.this.isItemValidForSlot(slot, stack);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
            }
        };
        inventory.setCanInsertSlot(0, true);
        inventory.setCanExtractSlot(0, true);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || TeslaHandler.hasTeslaCapability(this, capability)
                || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return (T) energyCapability;
        if (TeslaHandler.hasTeslaCapability(this, capability)) return (T) TeslaHandler.getHandler(this);
        return super.getCapability(capability, facing);
    }

    @Override
    public int acceptEnergy(int amount, boolean simulate) {
        int received = energy.acceptEnergy(Math.max(0, amount), simulate);
        if (!simulate && received > 0) markDirty();
        return received;
    }

    @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
    @Override public int getUniversalEnergyStored() { return energy.getUniversalEnergyStored(); }
    @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return false; }
    @Override public void setEnergyStored(int amount) {
        energy.setEnergyStored(Math.max(0, Math.min(amount, energy.getMaxEnergyStored())));
    }
    @Override public void setMaxEnergyStored(int maximum) {
        energy.setMaxEnergyStored(Math.max(0, maximum));
        setEnergyStored(energy.getUniversalEnergyStored());
    }

    private TileRocketAssemblingMachine getAssembler() {
        if (world == null || !world.isBlockLoaded(pos)) return null;
        TileRocketAssemblingMachine found = null;
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos adjacent = pos.offset(side);
            if (!world.isBlockLoaded(adjacent) || world.getBlockState(adjacent).getBlock() != AdvancedRocketryBlocks.blockRocketBuilder) continue;
            EnumFacing front = RotatableBlock.getFront(world.getBlockState(adjacent));
            if (front.getAxis() == EnumFacing.Axis.Y || side.getAxis() == front.getAxis()) continue;
            TileEntity tile = world.getTileEntity(adjacent);
            if (tile instanceof TileRocketAssemblingMachine) {
                if (found != null) return null; // two assemblers: no ambiguous choice
                found = (TileRocketAssemblingMachine) tile;
            }
        }
        return found;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && !stack.isEmpty() && stack.getItem() == AdvancedRocketryItems.itemRocketBlueprint;
    }
    @Override
    public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return isItemValidForSlot(slot, stack) && super.canInsertItem(slot, stack, side);
    }
    @Override
    public int getInventoryStackLimit() { return 1; }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        if (world != null && !world.isRemote) {
            if (printing || saving) stop(PAUSED);
            else refreshIdleStatus();
            markDirty();
        }
        updateActionButton();
    }

    @Override
    public void onInventoryUpdated(int slot) {
        if (world != null && !world.isRemote && (printing || saving)) stop(PAUSED);
        else refreshIdleStatus();
        if (world != null) { markDirty(); super.onInventoryUpdated(slot); }
        updateActionButton();
    }

    private void setStatus(byte next) {
        if (status == next) return;
        status = next;
        markDirty();
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
        if (statusText != null) {
            statusText.setText(label());
            statusText.setColor(statusColor());
        }
        updateActionButton();
    }

    private String label() {
        String[] keys = {"ready", "no_assembler", "no_rocket", "saved", "no_blueprint", "invalid", "pad_busy", "bad_pad",
                "blocked", "missing", "unsupported", "printing", "paused", "complete", "detected", "blueprint_ready",
                "chunks_unloaded", "saving", "no_power"};
        return LibVulpes.proxy.getLocalizedString("msg.rocketprinter." + keys[status]);
    }
    private int statusColor() {
        switch (status) {
            case INVALID:
            case BAD_PAD:
            case BLOCKED:
            case UNSUPPORTED:
                return 0xFFFF5555;
            case PAD_BUSY:
            case MISSING:
            case CHUNKS_UNLOADED:
            case NO_POWER:
            case DETECTED:
            case BLUEPRINT_READY:
            case SAVING:
            case SAVED:
            case PRINTING:
            case COMPLETE:
                return 0xFFFFFF22;
            default:
                return 0x404040;
        }
    }

    private boolean hasBlueprintData(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("RocketBlueprint");
    }

    private void refreshIdleStatus() {
        if (world == null || world.isRemote || printing || saving) return;
        TileRocketAssemblingMachine assembler = getAssembler();
        if (assembler == null) { setStatus(NO_ASSEMBLER); return; }
        if (!padLoaded(assembler)) { setStatus(CHUNKS_UNLOADED); return; }
        ItemStack stack = getStackInSlot(0);
        if (!isItemValidForSlot(0, stack)) { setStatus(NO_BLUEPRINT); return; }
        RocketBlueprint blueprint = RocketBlueprint.read(stack);
        if (blueprint == null && hasBlueprintData(stack)) { setStatus(INVALID); return; }
        AxisAlignedBB bounds = assembler.getRocketPadBounds(world, assembler.getPos());
        if (bounds == null) { setStatus(BAD_PAD); return; }
        if (assembler.isScanning()) { setStatus(PAD_BUSY); return; }
        if (blueprint == null) {
            if (assembler.getLinkedRocket() != null) setStatus(DETECTED);
            else setStatus(assembler.hasRocketOnPad(bounds) ? PAD_BUSY : NO_ROCKET);
            return;
        }
        if (assembler.hasRocketOnPad(bounds)) { setStatus(PAD_BUSY); return; }
        if (blueprint.sizeX > (int) bounds.maxX - (int) bounds.minX + 1
                || blueprint.sizeZ > (int) bounds.maxZ - (int) bounds.minZ + 1
                || blueprint.sizeY > (int) bounds.maxY - (int) bounds.minY + 1) { setStatus(BAD_PAD); return; }
        for (IBlockState state : blueprint.palette) if (RocketBlueprint.material(state).isEmpty()) {
            setStatus(UNSUPPORTED);
            return;
        }
        setStatus(BLUEPRINT_READY);
    }

    private void updateActionButton() {
        if (actionButton == null) return;
        String key = status == PRINTING || status == SAVING || status == NO_POWER ? "stop"
                : hasBlueprintData(getStackInSlot(0)) ? "print" : "save";
        actionButton.setText(LibVulpes.proxy.getLocalizedString("msg.rocketprinter." + key));
    }
    private void save() {
        TileRocketAssemblingMachine assembler = getAssembler();
        if (assembler == null) { setStatus(NO_ASSEMBLER); return; }
        if (!padLoaded(assembler)) { setStatus(CHUNKS_UNLOADED); return; }
        ItemStack stack = getStackInSlot(0);
        if (!isItemValidForSlot(0, stack)) { setStatus(NO_BLUEPRINT); return; }
        if (hasBlueprintData(stack)) { setStatus(INVALID); return; }
        EntityRocket rocket = assembler.getLinkedRocket();
        if (rocket == null) { setStatus(NO_ROCKET); return; }
        AxisAlignedBB bounds = assembler.getRocketPadBounds(world, assembler.getPos());
        if (bounds == null) { setStatus(BAD_PAD); return; }
        RocketBlueprint blueprint = RocketBlueprint.fromRocket(rocket.storage);
        if (blueprint == null) { setStatus(INVALID); return; }
        pendingBlueprint = blueprint;
        saveStack = stack.copy();
        saveRocket = rocket;
        saveAssembler = assembler;
        saveFacing = RotatableBlock.getFront(world.getBlockState(assembler.getPos()));
        pad = bounds;
        frameFromY = (int) (bounds.maxY - bounds.minY);
        frameToY = -1;
        frameDuration = SAVE_TICKS_PER_LAYER * (frameFromY + 1);
        frameStartTick = world.getTotalWorldTime();
        framePausedAt = -1;
        operationTotal = frameDuration;
        saving = true;
        setStatus(SAVING);
    }

    private void finishSave() {
        ItemStack stack = getStackInSlot(0);
        if (!isItemValidForSlot(0, stack) || !ItemStack.areItemStacksEqual(stack, saveStack)) { stop(PAUSED); return; }
        TileRocketAssemblingMachine assembler = getAssembler();
        if (assembler == null || assembler != saveAssembler
                || RotatableBlock.getFront(world.getBlockState(assembler.getPos())) != saveFacing) { stop(NO_ASSEMBLER); return; }
        if (!padLoaded(assembler)) { stop(CHUNKS_UNLOADED); return; }
        AxisAlignedBB bounds = assembler.getRocketPadBounds(world, assembler.getPos());
        if (bounds == null || bounds.minX != pad.minX || bounds.minY != pad.minY || bounds.minZ != pad.minZ
                || bounds.maxX != pad.maxX || bounds.maxY != pad.maxY || bounds.maxZ != pad.maxZ) { stop(BAD_PAD); return; }
        if (assembler.getLinkedRocket() != saveRocket) { stop(NO_ROCKET); return; }
        ItemStack saved = stack.copy();
        pendingBlueprint.write(saved);
        saving = false;
        pendingBlueprint = null;
        saveStack = ItemStack.EMPTY;
        saveRocket = null;
        saveAssembler = null;
        saveFacing = null;
        setInventorySlotContents(0, saved);
        setStatus(SAVED);
    }

    private boolean padLoaded(TileRocketAssemblingMachine assembler) {
        BlockPos p = assembler.getPos();
        return world.isAreaLoaded(p.add(-17, -1, -17), p.add(17, 0, 17));
    }

    private boolean checkLayer(int y, boolean complete) {
        TileRocketAssemblingMachine assembler = getAssembler();
        if (assembler == null) { stop(NO_ASSEMBLER); return false; }
        if (!padLoaded(assembler)) { stop(CHUNKS_UNLOADED); return false; }
        AxisAlignedBB current = assembler.getRocketPadBounds(world, assembler.getPos());
        if (current == null || current.minX != pad.minX || current.minY != pad.minY || current.minZ != pad.minZ
                || current.maxX != pad.maxX || current.maxY != pad.maxY || current.maxZ != pad.maxZ) { stop(BAD_PAD); return false; }
        if (assembler.isScanning() || assembler.hasRocketOnPad(current)) { stop(PAD_BUSY); return false; }
        int low = 0, high = job.cells.length / 2;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (job.cells[mid * 2] < (y << 8)) low = mid + 1; else high = mid;
        }
        int entry = low * 2;
        for (int x = 0; x < job.sizeX; x++) for (int z = 0; z < job.sizeZ; z++) {
            int packed = y << 8 | x << 4 | z;
            while (entry < job.cells.length && job.cells[entry] < packed && job.cells[entry] >> 8 == y) entry += 2;
            BlockPos target = origin.add(x, y, z);
            if (!world.isBlockLoaded(target)) { stop(CHUNKS_UNLOADED); return false; }
            IBlockState currentState = world.getBlockState(target);
            if (entry < job.cells.length && job.cells[entry] == packed) {
                IBlockState wanted = job.palette[job.cells[entry + 1]];
                if (!currentState.equals(wanted) && (complete || !replaceable(target, currentState))) { stop(BLOCKED); return false; }
                entry += 2;
            } else if (!world.isAirBlock(target)) { stop(BLOCKED); return false; }
        }
        return true;
    }

    private boolean replaceable(BlockPos target, IBlockState state) {
        return world.isAirBlock(target) || (!state.getMaterial().isLiquid() && state.getBlock().isReplaceable(world, target));
    }

    private void start(EntityPlayer player) {
        TileRocketAssemblingMachine assembler = getAssembler();
        if (assembler == null) { setStatus(NO_ASSEMBLER); return; }
        if (!padLoaded(assembler)) { setStatus(CHUNKS_UNLOADED); return; }
        ItemStack stack = getStackInSlot(0);
        if (!isItemValidForSlot(0, stack)) { setStatus(NO_BLUEPRINT); return; }
        RocketBlueprint blueprint = RocketBlueprint.read(stack);
        if (blueprint == null) { setStatus(INVALID); return; }
        AxisAlignedBB bounds = assembler.getRocketPadBounds(world, assembler.getPos());
        if (bounds == null) { setStatus(BAD_PAD); return; }
        if (assembler.isScanning() || assembler.hasRocketOnPad(bounds)) { setStatus(PAD_BUSY); return; }
        int width = (int) bounds.maxX - (int) bounds.minX + 1;
        int depth = (int) bounds.maxZ - (int) bounds.minZ + 1;
        if (blueprint.sizeX > width || blueprint.sizeZ > depth || blueprint.sizeY > (int) bounds.maxY - (int) bounds.minY + 1) { setStatus(BAD_PAD); return; }
        for (IBlockState state : blueprint.palette) if (RocketBlueprint.material(state).isEmpty()) {
            setStatus(UNSUPPORTED);
            player.sendMessage(new TextComponentTranslation("msg.rocketprinter.unsupported_block", state.getBlock().getRegistryName()));
            return;
        }
        job = blueprint;
        jobStack = stack;
        jobTag = stack.getTagCompound().getCompoundTag("RocketBlueprint").copy();
        sourceSides = new int[blueprint.palette.length];
        sourceSlots = new int[blueprint.palette.length];
        Arrays.fill(sourceSides, -1);
        attachedPos = assembler.getPos();
        attachedFacing = RotatableBlock.getFront(world.getBlockState(attachedPos));
        origin = new BlockPos((int) bounds.minX + (width - blueprint.sizeX) / 2, (int) bounds.minY,
                (int) bounds.minZ + (depth - blueprint.sizeZ) / 2);
        pad = bounds;
        if (!checkLayer(job.cells[0] >> 8, false)) return;
        index = 0;
        layer = -1;
        verificationY = 0;
        frameFromY = frameToY = -1;
        frameDuration = 0;
        frameStartTick = world.getTotalWorldTime();
        framePausedAt = -1;
        operationTotal = PRINT_TICKS_PER_LAYER * ((int) (pad.maxY - pad.minY) + 1);
        printing = true;
        setStatus(PRINTING);
    }

    private void stop(byte reason) {
        printing = false;
        saving = false;
        pendingBlueprint = null;
        saveStack = ItemStack.EMPTY;
        saveRocket = null;
        saveAssembler = null;
        saveFacing = null;
        job = null;
        jobStack = ItemStack.EMPTY;
        jobTag = null;
        sourceSides = sourceSlots = null;
        origin = null;
        attachedPos = null;
        attachedFacing = null;
        pad = null;
        index = 0;
        layer = -1;
        verificationY = 0;
        frameFromY = frameToY = -1;
        frameDuration = 0;
        frameStartTick = 0;
        framePausedAt = -1;
        operationTotal = guiProgress = 0;
        setStatus(reason);
    }

    @Override
    public void onChunkUnload() {
        if (world != null && !world.isRemote && (saving || printing)) stop(PAUSED);
        super.onChunkUnload();
    }

    private int getFrameElapsed() {
        if (world == null || frameDuration <= 0) return 0;
        long tick = framePausedAt >= 0 ? framePausedAt : world.getTotalWorldTime();
        return (int) Math.max(0L, Math.min((long) frameDuration, tick - frameStartTick));
    }

    private boolean consumeWorkPower(byte runningStatus) {
        if (energy.getUniversalEnergyStored() < ENERGY_PER_TICK) {
            if (framePausedAt < 0) {
                framePausedAt = Math.max(frameStartTick, world.getTotalWorldTime() - 1);
                setStatus(NO_POWER);
            }
            return false;
        }
        if (framePausedAt >= 0) {
            frameStartTick += world.getTotalWorldTime() - framePausedAt - 1;
            framePausedAt = -1;
            setStatus(runningStatus);
        }
        energy.extractEnergy(ENERGY_PER_TICK, false);
        markDirty();
        return true;
    }

    @Override
    public void update() {
        if (world == null) return;
        if (world.isRemote) {
            // World time is periodically reset by server packets; animation must use local ticks.
            if (isRenderingFrame() && framePausedAt < 0 && clientFrameElapsed < frameDuration) clientFrameElapsed++;
            return;
        }
        if (saving) {
            if (!consumeWorkPower(SAVING)) return;
            if (getFrameElapsed() >= frameDuration) finishSave();
            return;
        }
        if (!printing) return;
        if (getStackInSlot(0) != jobStack || jobTag == null || !jobStack.hasTagCompound()) { stop(PAUSED); return; }
        TileRocketAssemblingMachine assembler = getAssembler();
        if (assembler == null || !assembler.getPos().equals(attachedPos)
                || RotatableBlock.getFront(world.getBlockState(attachedPos)) != attachedFacing) { stop(NO_ASSEMBLER); return; }
        if (assembler.isScanning() || assembler.hasRocketOnPad(pad)) { stop(PAD_BUSY); return; }
        if (!consumeWorkPower(PRINTING)) return;
        if (index >= job.cells.length) {
            if (!jobTag.equals(jobStack.getTagCompound().getCompoundTag("RocketBlueprint"))) { stop(PAUSED); return; }
            if (verificationY < job.sizeY) {
                if (!checkLayer(verificationY, true)) return;
                verificationY++;
                if (verificationY < job.sizeY) return;
            }
            int top = (int) (pad.maxY - pad.minY);
            if (layer < top && frameToY != top) {
                frameFromY = layer;
                frameToY = top;
                frameDuration = PRINT_TICKS_PER_LAYER * (top - layer);
                frameStartTick = world.getTotalWorldTime();
                markDirty();
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 2);
            }
            if (getFrameElapsed() >= frameDuration) stop(COMPLETE);
            return;
        }
        int checks = 0, placed = 0;
        while (index < job.cells.length && checks++ < CHECKS_PER_TICK && placed < BLOCKS_PER_TICK) {
            int packed = job.cells[index];
            int y = packed >> 8;
            if (y != layer) {
                if (!jobTag.equals(jobStack.getTagCompound().getCompoundTag("RocketBlueprint")) || !checkLayer(y, false)) { if (printing) stop(PAUSED); return; }
                frameFromY = layer;
                frameToY = y;
                frameDuration = PRINT_TICKS_PER_LAYER * (y - layer);
                frameStartTick = world.getTotalWorldTime();
                layer = y;
                markDirty();
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 2);
            }
            if (getFrameElapsed() < frameDuration) return;
            BlockPos target = origin.add(packed >> 4 & 15, y, packed & 15);
            if (!world.isBlockLoaded(target)) { stop(CHUNKS_UNLOADED); return; }
            IBlockState wanted = job.palette[job.cells[index + 1]];
            IBlockState before = world.getBlockState(target);
            if (before.equals(wanted)) { index += 2; continue; }
            if (!replaceable(target, before)) { stop(BLOCKED); return; }
            ItemStack needed = RocketBlueprint.material(wanted);
            ItemStack paid = take(needed, job.cells[index + 1]);
            if (paid.isEmpty()) { stop(MISSING); return; }
            boolean refused = false;
            try { refused = !world.setBlockState(target, wanted, 3); } catch (RuntimeException exception) {
                if (world.getBlockState(target).equals(before)) returnMaterial(paid);
                stop(BLOCKED);
                return;
            }
            IBlockState after = world.getBlockState(target);
            if (!after.equals(wanted)) {
                if (refused && after.equals(before)) returnMaterial(paid);
                stop(BLOCKED);
                return;
            }
            placed++;
            index += 2;
        }
    }

    private ItemStack take(ItemStack wanted, int paletteIndex) {
        if (sourceSides[paletteIndex] >= 0) {
            EnumFacing side = EnumFacing.VALUES[sourceSides[paletteIndex]];
            BlockPos source = pos.offset(side);
            if (world.isBlockLoaded(source)) {
                TileEntity tile = world.getTileEntity(source);
                if (tile != null && tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) {
                    IItemHandler items = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
                    if (items != null && sourceSlots[paletteIndex] < items.getSlots()) {
                        ItemStack paid = extract(items, sourceSlots[paletteIndex], wanted);
                        if (!paid.isEmpty()) return paid;
                    }
                }
            }
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos source = pos.offset(side);
            if (!world.isBlockLoaded(source)) continue;
            TileEntity tile = world.getTileEntity(source);
            if (tile == null || tile == this || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) continue;
            IItemHandler items = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (items == null) continue;
            for (int slot = 0; slot < Math.min(MAX_SLOTS_PER_HANDLER, items.getSlots()); slot++) {
                ItemStack paid = extract(items, slot, wanted);
                if (paid.isEmpty()) continue;
                sourceSides[paletteIndex] = side.ordinal();
                sourceSlots[paletteIndex] = slot;
                return paid;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack extract(IItemHandler items, int slot, ItemStack wanted) {
        ItemStack available = items.getStackInSlot(slot);
        if (available.isEmpty() || available.hasTagCompound() || !ItemStack.areItemsEqual(wanted, available)) return ItemStack.EMPTY;
        ItemStack simulated = items.extractItem(slot, 1, true);
        if (simulated.isEmpty() || simulated.hasTagCompound() || !ItemStack.areItemsEqual(wanted, simulated) || simulated.getCount() != 1) return ItemStack.EMPTY;
        ItemStack paid = items.extractItem(slot, 1, false);
        if (paid.isEmpty()) return ItemStack.EMPTY;
        if (!paid.hasTagCompound() && ItemStack.areItemsEqual(wanted, paid) && paid.getCount() == 1) return paid;
        returnMaterial(paid);
        return ItemStack.EMPTY;
    }

    private void returnMaterial(ItemStack stack) {
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos source = pos.offset(side);
            if (!world.isBlockLoaded(source)) continue;
            TileEntity tile = world.getTileEntity(source);
            if (tile == null || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) continue;
            IItemHandler items = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (items == null) continue;
            for (int slot = 0; slot < Math.min(MAX_SLOTS_PER_HANDLER, items.getSlots()) && !stack.isEmpty(); slot++) stack = items.insertItem(slot, stack, false);
        }
        if (!stack.isEmpty()) world.spawnEntity(new EntityItem(world, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack));
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        // Results stay visible after an operation, then current conditions are checked on reopening.
        refreshIdleStatus();
        List<ModuleBase> modules = new LinkedList<>();
        modules.add(new ModulePower(160, 21, this));
        modules.add(new ModuleProgress(149, 21, 0, TileRocketAssemblingMachine.verticalProgressBar, this));
        modules.add(new ModuleLimitedSlotArray(8, 21, this, 0, 1));
        modules.add(actionButton = new ModuleButton(34, 22, 0, "", this, zmaster587.libVulpes.inventory.TextureResources.buttonBuild));
        modules.add(statusText = new ModuleText(8, 58, label(), statusColor()));
        if (world.isRemote) {
            String tooltip = "\u00a7f" + LibVulpes.proxy.getLocalizedString("tooltip.rocketprinter.preview")
                    + "\n\u00a77" + LibVulpes.proxy.getLocalizedString("tooltip.rocketprinter.rotate")
                    + "\n\u00a77" + LibVulpes.proxy.getLocalizedString("tooltip.rocketprinter.zoom");
            modules.add(new ModuleButton(126, 21, 3, "3D", this, zmaster587.libVulpes.inventory.TextureResources.buttonSquare, tooltip, 16, 16));
            blueprintPreview = AdvancedRocketry.proxy.createRocketBlueprintPreview(-116, 5, this);
            blueprintPreview.setVisible(false);
            modules.add(blueprintPreview);
        }
        updateActionButton();
        return modules;
    }

    @Override
    public float getNormallizedProgress(int id) {
        int total = getTotalProgress(id);
        return total > 0 ? Math.min(1f, getProgress(id) / (float) total) : 0f;
    }

    @Override
    public int getProgress(int id) {
        if (id != 0 || world == null) return 0;
        if (world.isRemote) return guiProgress;
        if (!saving && !printing) return 0;
        int elapsed = getFrameElapsed();
        return saving ? elapsed : Math.min(operationTotal, (frameFromY + 1) * PRINT_TICKS_PER_LAYER + elapsed);
    }

    @Override
    public void setProgress(int id, int value) {if (id == 0) guiProgress = Math.max(0, value);}

    @Override
    public int getTotalProgress(int id) {return id == 0 ? operationTotal : 0;}
    @Override
    public void setTotalProgress(int id, int value) {if (id == 0) operationTotal = Math.max(0, value);}

    @Override
    public String getModularInventoryName() { return LibVulpes.proxy.getLocalizedString("tile.rocketPrinter.name"); }

    @Override
    public void onInventoryButtonPressed(int id) {
        if (world == null || !world.isRemote) return;
        if (id == 3) {
            if (blueprintPreview != null) blueprintPreview.setVisible(!blueprintPreview.getVisible());
            return;
        }
        if (id == 0) {
            byte request = status == PRINTING || status == SAVING || status == NO_POWER ? (byte) 2
                    : hasBlueprintData(getStackInSlot(0)) ? (byte) 1 : (byte) 0;
            PacketHandler.sendToServer(new PacketMachine(this, request));
        }
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte id) { }
    @Override
    public void readDataFromNetwork(ByteBuf in, byte id, NBTTagCompound nbt) { }
    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
        if (side != Side.SERVER || world == null || world.isRemote || player == null || player.getDistanceSq(pos) > 64
                || player.world != world || !isUsableByPlayer(player)) return;
        if (id == 0 && !printing && !saving && !hasBlueprintData(getStackInSlot(0))) save();
        else if (id == 1 && !printing && !saving && hasBlueprintData(getStackInSlot(0))) start(player);
        else if (id == 2 && (printing || saving)) stop(PAUSED);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        energy.writeToNBT(nbt);
        nbt.setByte("printStatus", printing || saving ? PAUSED : status);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        energy.setEnergyStored(Math.max(0, Math.min(ENERGY_CAPACITY, nbt.getInteger("energy"))));
        operationTotal = guiProgress = 0;
        framePausedAt = -1;
        byte value = nbt.getByte("printStatus");
        status = value >= READY && value <= NO_POWER
                ? (value == SAVING || value == PRINTING || value == NO_POWER ? PAUSED : value) : INVALID;
        printing = saving = false;
        pendingBlueprint = null;
        saveStack = ItemStack.EMPTY;
        saveRocket = null;
        saveAssembler = null;
        saveFacing = null;
        pad = null;
        layer = frameFromY = frameToY = -1;
        frameStartTick = 0;
        frameDuration = 0;
        clientFrameElapsed = 0;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeVisualData(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (tag.hasKey("status")) readVisualData(tag);
    }

    private NBTTagCompound writeVisualData(NBTTagCompound tag) {
        tag.setByte("status", status);
        tag.setInteger("frameFrom", frameFromY);
        tag.setInteger("frameTo", frameToY);
        tag.setInteger("frameDuration", frameDuration);
        tag.setLong("frameStart", frameStartTick);
        tag.setLong("framePausedAt", framePausedAt);
        tag.setInteger("frameElapsed", getFrameElapsed());
        if (pad != null) {
            tag.setIntArray("pad", new int[]{(int) pad.minX, (int) pad.minY, (int) pad.minZ, (int) pad.maxX, (int) pad.maxY, (int) pad.maxZ});
        }
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, writeVisualData(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readVisualData(pkt.getNbtCompound());
    }

    private void readVisualData(NBTTagCompound tag) {
        boolean sameFrame = pad != null && frameFromY == tag.getInteger("frameFrom")
                && frameToY == tag.getInteger("frameTo") && frameDuration == tag.getInteger("frameDuration")
                && frameStartTick == tag.getLong("frameStart") && framePausedAt == tag.getLong("framePausedAt");
        byte value = tag.getByte("status");
        status = value >= READY && value <= NO_POWER ? value : INVALID;
        frameFromY = tag.getInteger("frameFrom");
        frameToY = tag.getInteger("frameTo");
        frameDuration = tag.getInteger("frameDuration");
        frameStartTick = tag.getLong("frameStart");
        framePausedAt = tag.getLong("framePausedAt");
        // Duplicate updates must not rewind a running animation between layer changes.
        if (!sameFrame || framePausedAt >= 0) clientFrameElapsed = Math.max(0, Math.min(frameDuration, tag.getInteger("frameElapsed")));
        int[] box = tag.getIntArray("pad");
        pad = box.length == 6 ? new AxisAlignedBB(box[0], box[1], box[2], box[3], box[4], box[5]) : null;
        if (statusText != null) {
            statusText.setText(label());
            statusText.setColor(statusColor());
        }
        updateActionButton();
    }

    public boolean isRenderingFrame() {
        return pad != null && frameDuration > 0 && (status == PRINTING || status == SAVING || status == NO_POWER);
    }

    public double getFrameHeight(float partialTicks) {
        double elapsed = world.isRemote ? clientFrameElapsed : getFrameElapsed();
        if (framePausedAt < 0) elapsed += partialTicks;
        double progress = Math.max(0, Math.min(1, elapsed / frameDuration));
        return pad.minY + frameFromY + 1 + (frameToY - frameFromY) * progress - pos.getY();
    }

    public AxisAlignedBB getPad() { return pad; }

    @Override
    public boolean shouldRenderInPass(int pass) { return pass == 1; }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return isRenderingFrame() ? pad.grow(1).union(new AxisAlignedBB(pos)) : super.getRenderBoundingBox();
    }
}
