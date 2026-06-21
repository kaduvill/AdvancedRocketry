package zmaster587.advancedRocketry.tile;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.api.RocketEvent.RocketLandedEvent;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry;
import zmaster587.advancedRocketry.block.*;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.advancedRocketry.item.ItemPackedStructure;
import zmaster587.advancedRocketry.network.PacketInvalidLocationNotify;
import zmaster587.advancedRocketry.tile.hatch.TileSatelliteHatch;
import zmaster587.advancedRocketry.util.StorageChunk;
import zmaster587.advancedRocketry.util.WeightEngine;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.client.util.ProgressBarImage;
import zmaster587.libVulpes.interfaces.ILinkableTile;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.items.ItemLinker;
import zmaster587.libVulpes.network.PacketEntity;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.IMultiblock;
import zmaster587.libVulpes.tile.TileEntityRFConsumer;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.IconResource;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * Purpose: validate the rocket structure as well as give feedback to the player as to what needs to be
 * changed to complete the rocket structure
 * Also will be used to "build" the rocket components from the placed frames, control fuel flow etc
 **/
public class TileRocketAssemblingMachine extends TileEntityRFConsumer implements ITickable, IButtonInventory, INetworkMachine, IDataSync, IModularInventory, IProgressBar, ILinkableTile {

    protected static final ResourceLocation backdrop = new ResourceLocation("advancedrocketry", "textures/gui/rocketBuilder.png");
    protected static final ProgressBarImage verticalProgressBar = new ProgressBarImage(76, 93, 8, 52, 176, 15, 2, 38, 3, 2, EnumFacing.UP, backdrop);
    private final static int MAXSCANDELAY = 10;
    private final static int ENERGYFOROP = 100;
    private final static int MAX_SIZE = 16;
    private final static int MAX_SIZE_Y = 128;
    private final static int MIN_SIZE = 3;
    private final static int MIN_SIZE_Y = 4;
    private static final Block[] viableBlocks = {AdvancedRocketryBlocks.blockLaunchpad, AdvancedRocketryBlocks.blockLandingPad};
    protected ModuleText errorText;
    protected StatsRocket stats;
    protected AxisAlignedBB bbCache;
    protected ErrorCodes status;
    private ModuleText thrustText, weightText, fuelText, accelerationText;
    private ModuleText twrText, gravityText, liftText, fuelStatusText;
    private int totalProgress;
    private int progress; // How long until scan is finished from 0 -> num blocks
    private int prevProgress; // Used for client/server sync
    private boolean building; //True is rocket is being built, false if only scanning or otherwise
    private int lastRocketID;
    private List<HashedBlockPosition> blockPos;
    private int relinkRetries = 0;           // how many relinking tries left
    private long nextRelinkAttempt = 0L;     // world time for next try

    public TileRocketAssemblingMachine() {
        super(100000);

        blockPos = new LinkedList<>();

        status = ErrorCodes.UNSCANNED;
        stats = new StatsRocket();
        building = false;
        prevProgress = 0;
    }

    private boolean registeredBus = false;

    @Override
    public void onLoad() {
        if (!world.isRemote && !registeredBus) {
            MinecraftForge.EVENT_BUS.register(this);
            registeredBus = true;
        }
        if (!world.isRemote) {
            relinkRetries = 15; // give it time
            nextRelinkAttempt = world.getTotalWorldTime() + 20;
            tryRelinkNow(); // best-effort first shot
        }
        if (world.isRemote) return;

        // Recompute pad bounds and relink infra to any rockets already on the pad
        bbCache = getRocketPadBounds(world, pos);
        if (bbCache != null) {
            final AxisAlignedBB box = bbCache.grow(1.0E-4, 1.0E-4, 1.0E-4);
            List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, box);
            if (!rockets.isEmpty()) {
                for (IInfrastructure infra : getConnectedInfrastructure()) {
                    for (EntityRocketBase r : rockets) {
                        if (infra instanceof zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) {
                            ((zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) infra)
                                    .markRocketFromAssembler(r);
                        }
                        r.linkInfrastructure(infra);
                    }
                }
            }
        }
    }  

    @Override
    public void invalidate() {
        super.invalidate();
        unregisterFromBus();
        relinkRetries = 0;
        nextRelinkAttempt = 0L;
        // Notify linked multiblocks BEFORE clearing (server only)
        if (world != null && !world.isRemote) {
            for (HashedBlockPosition p : blockPos) {
                TileEntity te = world.getTileEntity(p.getBlockPos());
                if (te instanceof IMultiblock) {
                    ((IMultiblock) te).setIncomplete();
                }
            }
        }

        // Clear caches
        bbCache = null;
        stats.reset();
        blockPos.clear();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        unregisterFromBus();
        relinkRetries = 0;
        nextRelinkAttempt = 0L;
        // Clear caches
        bbCache = null;
        stats.reset();
        blockPos.clear();
    }


    private void unregisterFromBus() {
        if (registeredBus) {
            MinecraftForge.EVENT_BUS.unregister(this);
            registeredBus = false;
        }
    }

    public ErrorCodes getStatus() {
        return status;
    }

    public void setStatus(int value) {
        status = ErrorCodes.values()[value];
    }

    public StatsRocket getRocketStats() {
        return stats;
    }

    public AxisAlignedBB getBBCache() {
        return bbCache;
    }

    public int getTotalProgress() {
        return totalProgress;
    }

    public void setTotalProgress(int scanTotalBlocks) {
        this.totalProgress = scanTotalBlocks;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int scanTime) {
        this.progress = scanTime;
    }

    public double getNormallizedProgress() {
        return progress / (double) (totalProgress * MAXSCANDELAY);
    }

    public float getAcceleration(float gravitationalMultiplier) {
        return stats.getAcceleration(gravitationalMultiplier);
    }

    public float getWeight() {
        return stats.getWeight();
    }

    public int getThrust() {
        return stats.getThrust();
    }

    public float getNeededThrust() {
        return stats.getNeededThrust(getGravityMultiplier());
    }

    private float getPreviewFullTankFuelWeight() {
        if (!ARConfiguration.getCurrentConfig().advancedWeightSystem) {
            return 0f;
        }

        float weight = 0f;
        weight += getPreviewFuelWeight(FuelType.LIQUID_MONOPROPELLANT);
        weight += getPreviewFuelWeight(FuelType.LIQUID_BIPROPELLANT);
        weight += getPreviewFuelWeight(FuelType.LIQUID_OXIDIZER);
        weight += getPreviewFuelWeight(FuelType.NUCLEAR_WORKING_FLUID);
        return weight;
    }

    private float getPreviewFuelWeight(@Nonnull FuelType type) {
        int amount = Math.max(stats.getFuelCapacity(type), stats.getFuelAmount(type));
        if (amount <= 0) {
            return 0f;
        }
        return WeightEngine.INSTANCE.getRocketPropellantWeight(type, amount);
    }

    private float getPreviewWetWeight() {
        return stats.getWeight_NoFuel() + getPreviewFullTankFuelWeight();
    }

    protected float getPreviewNeededThrust() {
        float weight = getPreviewWetWeight();
        return weight * (ARConfiguration.getCurrentConfig().gravityAffectsFuel ? getGravityMultiplier() : 1f);
    }

    private float getPreviewAcceleration() {
        float weight = getPreviewWetWeight();

        if (weight <= 0f) {
            return 0f;
        }

        float netThrust = getThrust() - getPreviewNeededThrust();
        return netThrust / weight / 20f;
    }

    protected boolean hasEnoughFuelCapacity(@Nonnull FuelType fuelType) {
        if (!ARConfiguration.getCurrentConfig().rocketRequireFuel) {
            return true;
        }

        int capacity = stats.getFuelCapacity(fuelType);
        if (capacity <= 0) {
            return false;
        }

        int needed = getEstimatedFuelNeeded(fuelType);
        return needed >= 0 && capacity >= needed;
    }

    protected int getAssemblerTargetOrbitHeight() {
        if (world.provider.getDimension() == ARConfiguration.getCurrentConfig().spaceDimId) {
            return ARConfiguration.getCurrentConfig().stationClearanceHeight;
        }

        return ARConfiguration.getCurrentConfig().orbit;
    }

    public float getGravityMultiplier() {
        return DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getGravitationalMultiplier();
    }

    public boolean isBuilding() {
        return building;
    }

    public void setBuilding(boolean building) {
        this.building = building;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Override
    public int getPowerPerOperation() {
        return ENERGYFOROP;
    }

    @Override
    public void performFunction() {
        if (!isScanning()) return;
        if (progress >= (totalProgress * MAXSCANDELAY)) {
            if (!world.isRemote) {
                if (building) {
                    assembleRocket();
                } else {
                    scanRocket(world, pos, bbCache);
                }
            }
            totalProgress = -1;
            progress = 0;
            prevProgress = 0;
            building = false; // Done building/scanning
            if (!world.isRemote) {
                syncStatsToClient();
            }
            if (thrustText != null) {
                updateText();
            }
        }

        progress++;

        if (!this.world.isRemote && this.energy.getUniversalEnergyStored() < getPowerPerOperation() && progress - prevProgress > 0) {
            prevProgress = progress;
            PacketHandler.sendToNearby(new PacketMachine(this, (byte) 2), this.world.provider.getDimension(), this.getPos(), 32);
        }

    }

    @Override
    public boolean canPerformFunction() {
        return isScanning();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (isScanning() && bbCache != null) {
            return bbCache;
        }
        return super.getRenderBoundingBox();
    }

    public boolean isScanning() {
        return totalProgress > 0;
    }

    public AxisAlignedBB scanRocket(World world, BlockPos pos2, AxisAlignedBB bb) {
        stats = new StatsRocket(); // reset stats

        //if a rocket already exists, output their stats
        if (getBBCache() == null) {
            bbCache = getRocketPadBounds(world, pos);
        }

        if (getBBCache() != null) {
            double buffer = 0.0001;
            AxisAlignedBB bufferedBB = bbCache.grow(buffer, buffer, buffer);
            List<EntityRocket> rockets = world.getEntitiesWithinAABB(EntityRocket.class, bufferedBB);
            if (rockets.size() == 1) {
                EntityRocket rocket = rockets.get(0);
                rocket.recalculateStats();

                // Pull current fuel amounts from the rocket data manager into rocket.stats.
                rocket.getFuelAmount(FuelType.LIQUID_MONOPROPELLANT);
                rocket.getFuelAmount(FuelType.LIQUID_BIPROPELLANT);
                rocket.getFuelAmount(FuelType.LIQUID_OXIDIZER);
                rocket.getFuelAmount(FuelType.NUCLEAR_WORKING_FLUID);

                this.stats = rocket.stats.copy();
                status = ErrorCodes.ALREADY_ASSEMBLED;
                syncStatsToClient();
                return null;
            }
        }

        int thrustMonopropellant = 0;
        int thrustBipropellant = 0;
        int thrustNuclearNozzleLimit = 0;
        int thrustNuclearReactorLimit = 0;
        int thrustNuclearTotalLimit = 0;
        int monopropellantfuelUse = 0;
        int bipropellantfuelUse = 0;
        int nuclearWorkingFluidUseMax = 0;
        int fuelCapacityMonopropellant = 0;
        int fuelCapacityBipropellant = 0;
        int fuelCapacityOxidizer = 0;
        int fuelCapacityNuclearWorkingFluid = 0;

        float drillPower = 0f;
        stats.reset();

        int actualMinX = (int) bb.maxX,
                actualMinY = (int) bb.maxY,
                actualMinZ = (int) bb.maxZ,
                actualMaxX = (int) bb.minX,
                actualMaxY = (int) bb.minY,
                actualMaxZ = (int) bb.minZ;


        for (int xCurr = (int) bb.minX; xCurr <= bb.maxX; xCurr++) {
            for (int zCurr = (int) bb.minZ; zCurr <= bb.maxZ; zCurr++) {
                for (int yCurr = (int) bb.minY; yCurr <= bb.maxY; yCurr++) {

                    BlockPos currBlockPos = new BlockPos(xCurr, yCurr, zCurr);
                    IBlockState state = world.getBlockState(currBlockPos);
                    Block block = state.getBlock();

                    if (!world.isAirBlock(currBlockPos)) {
                        if (xCurr < actualMinX)
                            actualMinX = xCurr;
                        if (yCurr < actualMinY)
                            actualMinY = yCurr;
                        if (zCurr < actualMinZ)
                            actualMinZ = zCurr;
                        if (xCurr > actualMaxX)
                            actualMaxX = xCurr;
                        if (yCurr > actualMaxY)
                            actualMaxY = yCurr;
                        if (zCurr > actualMaxZ)
                            actualMaxZ = zCurr;
                    }
                }
            }
        }

        boolean hasSatellite = false;
        boolean hasGuidance = false;
        boolean invalidBlock = false;
        float weight = 0;

        if (!verifyScan(bb, world)) {
            status = ErrorCodes.INCOMPLETESTRCUTURE;
            syncStatsToClient();
            return null;
        }
        for (int yCurr = (int) bb.minY; yCurr <= bb.maxY; yCurr++) {
            for (int xCurr = (int) bb.minX; xCurr <= bb.maxX; xCurr++) {
                for (int zCurr = (int) bb.minZ; zCurr <= bb.maxZ; zCurr++) {

                    BlockPos currBlockPos = new BlockPos(xCurr, yCurr, zCurr);
                    BlockPos abovePos = new BlockPos(xCurr, yCurr + 1, zCurr);
                    BlockPos belowPos = new BlockPos(xCurr, yCurr - 1, zCurr);

                    if (!world.isAirBlock(currBlockPos)) {
                        IBlockState state = world.getBlockState(currBlockPos);
                        Block block = state.getBlock();

                        if (ARConfiguration.getCurrentConfig().blackListRocketBlocks.contains(block)) {
                            if (!block.isReplaceable(world, currBlockPos)) {
                                invalidBlock = true;
                                if (!world.isRemote)
                                    PacketHandler.sendToNearby(new PacketInvalidLocationNotify(new HashedBlockPosition(xCurr, yCurr, zCurr)), world.provider.getDimension(), getPos(), 64);
                            }
                            continue;
                        }

                        if (ARConfiguration.getCurrentConfig().advancedWeightSystem) {
                            weight += WeightEngine.INSTANCE.getWeight(world, currBlockPos);
                        } else {
                            weight += 1;
                        }

                        //If rocketEngine increaseThrust
                        final float x = xCurr - actualMinX - ((actualMaxX - actualMinX) / 2f);
                        final float z = zCurr - actualMinZ - ((actualMaxZ - actualMinZ) / 2f);
                        if (block instanceof IRocketEngine && (world.getBlockState(belowPos).getBlock().isAir(world.getBlockState(belowPos), world, belowPos) || world.getBlockState(belowPos).getBlock() instanceof BlockLandingPad || world.getBlockState(belowPos).getBlock() == AdvancedRocketryBlocks.blockLaunchpad)) {
                            if (block instanceof BlockNuclearRocketMotor) {
                                nuclearWorkingFluidUseMax += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr, yCurr, zCurr);
                                thrustNuclearNozzleLimit += ((IRocketEngine) block).getThrust(world, currBlockPos);
                            } else if (block instanceof BlockBipropellantRocketMotor) {
                                bipropellantfuelUse += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr, yCurr, zCurr);
                                thrustBipropellant += ((IRocketEngine) block).getThrust(world, currBlockPos);
                            } else if (block instanceof BlockRocketMotor) {
                                monopropellantfuelUse += ((IRocketEngine) block).getFuelConsumptionRate(world, xCurr, yCurr, zCurr);
                                thrustMonopropellant += ((IRocketEngine) block).getThrust(world, currBlockPos);
                            }
                            stats.addEngineLocation(x + 0.5f, yCurr - actualMinY + 0.5f, z + 0.5f);
                        }

                        if (block instanceof IFuelTank) {
                            if (block instanceof BlockBipropellantFuelTank) {
                                fuelCapacityBipropellant += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            } else if (block instanceof BlockOxidizerFuelTank) {
                                fuelCapacityOxidizer += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            } else if (block instanceof BlockNuclearFuelTank) {
                                fuelCapacityNuclearWorkingFluid += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            } else if (block instanceof BlockFuelTank) {
                                fuelCapacityMonopropellant += (((IFuelTank) block).getMaxFill(world, currBlockPos, state) * ARConfiguration.getCurrentConfig().fuelCapacityMultiplier);
                            }
                        }

                        if (block instanceof IRocketNuclearCore && ((world.getBlockState(belowPos).getBlock() instanceof IRocketNuclearCore) || (world.getBlockState(belowPos).getBlock() instanceof IRocketEngine))) {
                            thrustNuclearReactorLimit += ((IRocketNuclearCore) block).getMaxThrust(world, currBlockPos);
                        }

                        if (block instanceof BlockSeat && world.getBlockState(abovePos).getBlock().isPassable(world, abovePos)) {
                            stats.addPassengerSeat((int) Math.floor(x), yCurr - actualMinY, (int) Math.floor(z));
                        }

                        if (block instanceof IMiningDrill) {
                            drillPower += ((IMiningDrill) block).getMiningSpeed(world, currBlockPos);
                        }

                        TileEntity tile = world.getTileEntity(currBlockPos);
                        if (tile instanceof TileSatelliteHatch) {
                            hasSatellite = true;
                            if (ARConfiguration.getCurrentConfig().advancedWeightSystem) {
                                TileSatelliteHatch hatch = (TileSatelliteHatch) tile;
                                if (hatch.getSatellite() != null) {
                                    weight += hatch.getSatellite().getProperties().getWeight();
                                } else if (hatch.getStackInSlot(0).getItem() instanceof ItemPackedStructure) {
                                    ItemPackedStructure struct = (ItemPackedStructure) hatch.getStackInSlot(0).getItem();
                                    weight += struct.getStructure(hatch.getStackInSlot(0)).getWeight();
                                }
                            }
                        } else if (tile instanceof TileGuidanceComputer) {
                            hasGuidance = true;
                        }
                    }
                }
            }
        }

        int nuclearWorkingFluidUse = 0;
        if (thrustNuclearNozzleLimit > 0) {
            //Only run the number of engines our cores can support - we can't throttle these effectively because they're small, so they shut off if they don't get full power
            thrustNuclearTotalLimit = Math.min(thrustNuclearNozzleLimit, thrustNuclearReactorLimit);
            nuclearWorkingFluidUse = (int) (nuclearWorkingFluidUseMax * (thrustNuclearTotalLimit / (float) thrustNuclearNozzleLimit));
            thrustNuclearTotalLimit = (nuclearWorkingFluidUse * thrustNuclearNozzleLimit) / nuclearWorkingFluidUseMax;
        }

        // Set fuel stats
        // Thrust depending on rocket type
        stats.setBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT, monopropellantfuelUse);
        stats.setBaseFuelRate(FuelType.LIQUID_BIPROPELLANT,   bipropellantfuelUse);
        stats.setBaseFuelRate(FuelType.LIQUID_OXIDIZER,       bipropellantfuelUse);
        stats.setBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID, nuclearWorkingFluidUse);

        stats.setFuelRate(FuelType.LIQUID_MONOPROPELLANT, monopropellantfuelUse);
        stats.setFuelRate(FuelType.LIQUID_BIPROPELLANT,   bipropellantfuelUse);
        stats.setFuelRate(FuelType.LIQUID_OXIDIZER,       bipropellantfuelUse);
        stats.setFuelRate(FuelType.NUCLEAR_WORKING_FLUID, nuclearWorkingFluidUse);

        // Fuel storage depending on rocket type
        stats.setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT,      fuelCapacityMonopropellant);
        stats.setFuelCapacity(FuelType.LIQUID_BIPROPELLANT,        fuelCapacityBipropellant);
        stats.setFuelCapacity(FuelType.LIQUID_OXIDIZER,            fuelCapacityOxidizer);
        stats.setFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID,      fuelCapacityNuclearWorkingFluid);

        //Non-fuel stats
        stats.setWeight(weight);
        stats.setThrust(Math.max(Math.max(thrustMonopropellant, thrustBipropellant), thrustNuclearTotalLimit));
        stats.setDrillingPower(drillPower);

        //Total stats, used to check if the user has tried to apply two or more types of thrust/fuel
        int totalFuel = fuelCapacityBipropellant + fuelCapacityNuclearWorkingFluid + fuelCapacityMonopropellant;
        int totalFuelUse = bipropellantfuelUse + nuclearWorkingFluidUse + monopropellantfuelUse;

        //Set status
        if (invalidBlock) {
            status = ErrorCodes.INVALIDBLOCK;

        } else if (((fuelCapacityBipropellant > 0 && totalFuel > fuelCapacityBipropellant)
                || (fuelCapacityMonopropellant > 0 && totalFuel > fuelCapacityMonopropellant)
                || (fuelCapacityNuclearWorkingFluid > 0 && totalFuel > fuelCapacityNuclearWorkingFluid))
                ||
                ((thrustBipropellant > 0 && totalFuelUse > bipropellantfuelUse)
                || (thrustMonopropellant > 0 && totalFuelUse > monopropellantfuelUse)
                || (thrustNuclearTotalLimit > 0 && totalFuelUse > nuclearWorkingFluidUse))) {
            status = ErrorCodes.COMBINEDTHRUST;

        } else if (!hasGuidance && !hasSatellite) {
            status = ErrorCodes.NOGUIDANCE;

        } else if (getThrust() <= getPreviewNeededThrust()) {
            status = ErrorCodes.NOENGINES;

        } else if ((thrustBipropellant > 0 && (!hasEnoughFuelCapacity(FuelType.LIQUID_BIPROPELLANT) || !hasEnoughFuelCapacity(FuelType.LIQUID_OXIDIZER)))
                || ((thrustMonopropellant > 0) && !hasEnoughFuelCapacity(FuelType.LIQUID_MONOPROPELLANT))
                || ((thrustNuclearTotalLimit > 0) && !hasEnoughFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID))) {
            status = ErrorCodes.NOFUEL;
        } else {
            status = ErrorCodes.SUCCESS;
        }

        
        // Normalize integer mins/maxes first
        int minXi = Math.min(actualMinX, actualMaxX);
        int minYi = Math.min(actualMinY, actualMaxY);
        int minZi = Math.min(actualMinZ, actualMaxZ);
        int maxXi = Math.max(actualMinX, actualMaxX);
        int maxYi = Math.max(actualMaxY, actualMinY);
        int maxZi = Math.max(actualMinZ, actualMaxZ);

        // use BlockPos ctor so the AABB is [min, max+1) in block space
        AxisAlignedBB result = new AxisAlignedBB(
                new BlockPos(minXi, minYi, minZi),
                new BlockPos(maxXi, maxYi, maxZi)
        );
        syncStatsToClient();
        return result;
    }

    protected void removeReplaceableBlocks(AxisAlignedBB bb) {
        for (int yCurr = (int) bb.minY; yCurr <= bb.maxY; yCurr++) {
            for (int xCurr = (int) bb.minX; xCurr <= bb.maxX; xCurr++) {
                for (int zCurr = (int) bb.minZ; zCurr <= bb.maxZ; zCurr++) {

                    BlockPos currBlockPos = new BlockPos(xCurr, yCurr, zCurr);

                    if (!world.isAirBlock(currBlockPos)) {
                        IBlockState state = world.getBlockState(currBlockPos);
                        Block block = state.getBlock();
                        if (ARConfiguration.getCurrentConfig().blackListRocketBlocks.contains(block) && block.isReplaceable(world, currBlockPos)) {
                            if (!world.isRemote)
                                world.setBlockToAir(currBlockPos);
                        }
                    }
                }
            }
        }
    }

    private static boolean isEmptyAABB(@Nullable AxisAlignedBB b) {
        return b == null || b.maxX < b.minX || b.maxY < b.minY || b.maxZ < b.minZ;
    }


    private static AxisAlignedBB normalize(AxisAlignedBB b) {
        double minX = Math.min(b.minX, b.maxX);
        double minY = Math.min(b.minY, b.maxY);
        double minZ = Math.min(b.minZ, b.maxZ);
        double maxX = Math.max(b.minX, b.maxX);
        double maxY = Math.max(b.minY, b.maxY);
        double maxZ = Math.max(b.minZ, b.maxZ);
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }


    public void assembleRocket() {
        // server only + need a pad cache
        if (world.isRemote || bbCache == null) return;

        // Re-scan to get a tight non-air AABB and fresh stats/status
        final AxisAlignedBB scanBB = scanRocket(world, pos, bbCache);
        if (status != ErrorCodes.SUCCESS || scanBB == null) return;

        // Normalize and defensively guard against degenerate boxes
        final AxisAlignedBB rocketBB = normalize(scanBB);
        if (isEmptyAABB(rocketBB)) {
            status = ErrorCodes.FAIL_CUT;
            return;
        }

        // Remove replaceable/blacklisted blocks *inside the tightened bounds*
        removeReplaceableBlocks(rocketBB);

        // Cut the world using the tightened box (avoid pad air)
        final StorageChunk storageChunk;
        try {
            storageChunk = StorageChunk.cutWorldBB(world, rocketBB);
        } catch (Throwable t) { // cover NegativeArraySizeException & other edge errors
            status = ErrorCodes.FAIL_CUT;
            return;
        }

        // Center spawn on tightened AABB
        final double cx = rocketBB.minX + (rocketBB.maxX - rocketBB.minX) / 2.0 + 0.5;
        final double cz = rocketBB.minZ + (rocketBB.maxZ - rocketBB.minZ) / 2.0 + 0.5;
        final double cy = this.getPos().getY();

        EntityRocket rocket = new EntityRocket(world, storageChunk, stats.copy(), cx, cy, cz);
        world.spawnEntity(rocket);

        NBTTagCompound nbtdata = new NBTTagCompound();
        rocket.writeToNBT(nbtdata);
        PacketHandler.sendToNearby(new PacketEntity(rocket, (byte) 0, nbtdata),
                rocket.world.provider.getDimension(), this.pos, 64);

        // Finish & link as before
        stats.reset();
        this.status = ErrorCodes.FINISHED;
        this.markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        for (IInfrastructure infrastructure : getConnectedInfrastructure()) {
            if (infrastructure instanceof zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) {
                ((zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) infrastructure)
                        .markRocketFromAssembler(rocket);
            }
            rocket.linkInfrastructure(infrastructure);
        }
        // Rescan so UI immediately reflects the post-build state
        scanRocket(world, pos, bbCache);
    }

    /**
     * Does not make sure the structure is complete, only gets max bounds!
     *
     * @param world the world
     * @param pos   coords to evaluate from
     * @return AxisAlignedBB bounds of structure if valid  otherwise null
     */
    public AxisAlignedBB getRocketPadBounds(World world, BlockPos pos) {
        EnumFacing direction = RotatableBlock.getFront(world.getBlockState(pos)).getOpposite();
        int xMin, zMin, xMax, zMax;
        int yCurrent = pos.getY() - 1;
        int xCurrent = pos.getX() + direction.getFrontOffsetX();
        int zCurrent = pos.getZ() + direction.getFrontOffsetZ();
        xMax = xMin = xCurrent;
        zMax = zMin = zCurrent;
        int xSize, zSize;

        BlockPos currPos = new BlockPos(xCurrent, yCurrent, zCurrent);

        if (world.isRemote)
            return null;

        //Get min and maximum Z/X bounds
        if (direction.getFrontOffsetX() != 0) {
            xSize = ZUtils.getContinuousBlockLength(world, direction, currPos, MAX_SIZE, viableBlocks);
            zMin = ZUtils.getContinuousBlockLength(world, EnumFacing.NORTH, currPos, MAX_SIZE, viableBlocks);
            zMax = ZUtils.getContinuousBlockLength(world, EnumFacing.SOUTH, currPos.add(0, 0, 1), MAX_SIZE - zMin, viableBlocks);
            zSize = zMin + zMax;

            zMin = zCurrent - zMin + 1;
            zMax = zCurrent + zMax;

            if (direction.getFrontOffsetX() > 0) {
                xMax = xCurrent + xSize - 1;
            }

            if (direction.getFrontOffsetX() < 0) {
                xMin = xCurrent - xSize + 1;
            }
        } else {
            zSize = ZUtils.getContinuousBlockLength(world, direction, currPos, MAX_SIZE, viableBlocks);
            xMin = ZUtils.getContinuousBlockLength(world, EnumFacing.WEST, currPos, MAX_SIZE, viableBlocks);
            xMax = ZUtils.getContinuousBlockLength(world, EnumFacing.EAST, currPos.add(1, 0, 0), MAX_SIZE - xMin, viableBlocks);
            xSize = xMin + xMax;

            xMin = xCurrent - xMin + 1;
            xMax = xCurrent + xMax;

            if (direction.getFrontOffsetZ() > 0) {
                zMax = zCurrent + zSize - 1;
            }

            if (direction.getFrontOffsetZ() < 0) {
                zMin = zCurrent - zSize + 1;
            }
        }


        int maxTowerSize = 0;
        //Check perimeter for structureBlocks and get the size
        for (int i = xMin; i <= xMax; i++) {
            if (world.getBlockState(new BlockPos(i, yCurrent, zMin - 1)).getBlock() == AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP, new BlockPos(i, yCurrent, zMin - 1), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }

            if (world.getBlockState(new BlockPos(i, yCurrent, zMax + 1)).getBlock() == AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP, new BlockPos(i, yCurrent, zMax + 1), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }
        }

        for (int i = zMin; i <= zMax; i++) {
            if (world.getBlockState(new BlockPos(xMin - 1, yCurrent, i)).getBlock() == AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP, new BlockPos(xMin - 1, yCurrent, i), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }

            if (world.getBlockState(new BlockPos(xMax + 1, yCurrent, i)).getBlock() == AdvancedRocketryBlocks.blockStructureTower) {
                maxTowerSize = Math.max(maxTowerSize, ZUtils.getContinuousBlockLength(world, EnumFacing.UP, new BlockPos(xMax + 1, yCurrent, i), MAX_SIZE_Y, AdvancedRocketryBlocks.blockStructureTower));
            }
        }

        //if tower does not meet criteria then reutrn null
        if (maxTowerSize < MIN_SIZE_Y || xSize < MIN_SIZE || zSize < MIN_SIZE) {
            return null;
        }

        return new AxisAlignedBB(new BlockPos(xMin, yCurrent + 1, zMin), new BlockPos(xMax, yCurrent + maxTowerSize - 1, zMax));
    }

    protected boolean verifyScan(AxisAlignedBB bb, World world) {
        boolean whole = true;

        boundLoop:
        for (int xx = (int) bb.minX; xx <= (int) bb.maxX; xx++) {
            for (int zz = (int) bb.minZ; zz <= (int) bb.maxZ; zz++) {
                Block blockAtSpot = world.getBlockState(new BlockPos(xx, (int) bb.minY - 1, zz)).getBlock();
                boolean contained = false;
                for (Block b : viableBlocks) {
                    if (blockAtSpot == b) {
                        contained = true;
                        break;
                    }
                }

                if (!contained) {
                    whole = false;
                    break boundLoop;
                }
            }
        }

        return whole;
    }

    public int getVolume(World world, AxisAlignedBB bb) {
        return (int) ((bb.maxX - bb.minX) * (bb.maxY - bb.minY) * (bb.maxZ - bb.minZ));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        stats.writeToNBT(nbt);
        nbt.setInteger("scanTime", progress);
        nbt.setInteger("scanTotalBlocks", totalProgress);
        nbt.setBoolean("building", building);
        nbt.setInteger("status", status.ordinal());

        if (bbCache != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setDouble("minX", bbCache.minX);
            tag.setDouble("minY", bbCache.minY);
            tag.setDouble("minZ", bbCache.minZ);
            tag.setDouble("maxX", bbCache.maxX);
            tag.setDouble("maxY", bbCache.maxY);
            tag.setDouble("maxZ", bbCache.maxZ);

            nbt.setTag("bb", tag);
        }

        if (!blockPos.isEmpty()) {
            int[] array = new int[blockPos.size() * 3];
            int counter = 0;
            for (HashedBlockPosition pos : blockPos) {
                array[counter] = pos.x;
                array[counter + 1] = pos.y;
                array[counter + 2] = pos.z;
                counter += 3;
            }
            nbt.setIntArray("infrastructureLocations", array);
        }
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        stats.readFromNBT(nbt);

        prevProgress = progress = nbt.getInteger("scanTime");
        totalProgress = nbt.getInteger("scanTotalBlocks");
        status = ErrorCodes.values()[nbt.getInteger("status")];

        building = nbt.getBoolean("building");
        if (nbt.hasKey("bb")) {

            NBTTagCompound tag = nbt.getCompoundTag("bb");
            bbCache = new AxisAlignedBB(tag.getDouble("minX"),
                    tag.getDouble("minY"), tag.getDouble("minZ"),
                    tag.getDouble("maxX"), tag.getDouble("maxY"), tag.getDouble("maxZ"));

        }

        blockPos.clear();
        if (nbt.hasKey("infrastructureLocations")) {
            int[] array = nbt.getIntArray("infrastructureLocations");

            for (int counter = 0; counter < array.length; counter += 3) {
                blockPos.add(new HashedBlockPosition(array[counter], array[counter + 1], array[counter + 2]));
            }
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        super.getUpdatePacket();
        NBTTagCompound nbt = new NBTTagCompound();

        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
        updateText();
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte id) {
        //Used to sync clinet/server
        if (id == 2) {
            out.writeInt(energy.getUniversalEnergyStored());
            out.writeInt(this.progress);
        } else if (id == 3) {
            out.writeInt(lastRocketID);
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte id,
                                    NBTTagCompound nbt) {
        if (id == 2) {
            nbt.setInteger("pwr", in.readInt());
            nbt.setInteger("tik", in.readInt());
        } else if (id == 3) {
            nbt.setInteger("id", in.readInt());
        }
    }

    protected void syncStatsToClient() {
        if (world == null || world.isRemote) {return;}
        markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    public boolean canScan() {
        return bbCache != null;}

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id,
                               NBTTagCompound nbt) {
        if (id == 0) {

            bbCache = getRocketPadBounds(world, pos);
            if (!canScan())
                return;

            totalProgress = (int) (ARConfiguration.getCurrentConfig().buildSpeedMultiplier * this.getVolume(world, bbCache) / 10);
            this.markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        } else if (id == 1) {

            if (isScanning())
                return;

            building = true;

            bbCache = getRocketPadBounds(world, pos);
            if (!canScan())
                return;

            totalProgress = (int) (ARConfiguration.getCurrentConfig().buildSpeedMultiplier * this.getVolume(world, bbCache) / 10);
            this.markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

        } else if (id == 2) {
            energy.setEnergyStored(nbt.getInteger("pwr"));
            this.progress = nbt.getInteger("tik");
        } else if (id == 3) {
            EntityRocket rocket = (EntityRocket) world.getEntityByID(nbt.getInteger("id"));
            for (IInfrastructure infrastructure : getConnectedInfrastructure()) {
                rocket.linkInfrastructure(infrastructure);
            }
        }
    }

    private FuelType getDisplayFuelType() {
        if (stats.getFuelCapacity(FuelType.LIQUID_MONOPROPELLANT) > 0
                || stats.getBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT) > 0) {
            return FuelType.LIQUID_MONOPROPELLANT;
        }

        if (stats.getFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID) > 0
                || stats.getBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID) > 0) {
            return FuelType.NUCLEAR_WORKING_FLUID;
        }

        // Biprop uses both fuel and oxidizer.
        // Display the limiting side as the single generic "Fuel" value.
        return getFuelNeedRatio(FuelType.LIQUID_OXIDIZER) > getFuelNeedRatio(FuelType.LIQUID_BIPROPELLANT)
                ? FuelType.LIQUID_OXIDIZER
                : FuelType.LIQUID_BIPROPELLANT;
    }

    private boolean isBipropellantRocket() {
        return stats.getFuelCapacity(FuelType.LIQUID_BIPROPELLANT) > 0
                || stats.getFuelCapacity(FuelType.LIQUID_OXIDIZER) > 0
                || stats.getBaseFuelRate(FuelType.LIQUID_BIPROPELLANT) > 0
                || stats.getBaseFuelRate(FuelType.LIQUID_OXIDIZER) > 0;
    }

    private float getFuelNeedRatio(@Nonnull FuelType fuelType) {
        int needed = getEstimatedFuelNeeded(fuelType);
        int capacity = stats.getFuelCapacity(fuelType);

        if (needed < 0) {
            return Float.POSITIVE_INFINITY;
        }

        if (capacity <= 0) {
            return needed > 0 ? Float.POSITIVE_INFINITY : 0f;
        }

        return needed / (float) capacity;
    }

    private boolean hasEnoughDisplayFuelCapacity() {
        if (!ARConfiguration.getCurrentConfig().rocketRequireFuel) {
            return true;
        }

        if (isBipropellantRocket()) {
            return hasEnoughFuelCapacity(FuelType.LIQUID_BIPROPELLANT)
                    && hasEnoughFuelCapacity(FuelType.LIQUID_OXIDIZER);
        }

        return hasEnoughFuelCapacity(getDisplayFuelType());
    }

    private long getPreviewMissionFuelTicks() {
        float a = getPreviewAcceleration();

        if (a <= 0f) {
            return -1L;
        }

        double h = Math.max(0.0d, getAssemblerTargetOrbitHeight() - this.getPos().getY());

        long nTicks = (long)Math.ceil(Math.sqrt(2.0d * h / a));
        nTicks += 2L; // match EntityRocket.hasMissionFuelFor()

        return Math.max(1L, nTicks);
    }

    private int getEstimatedFuelNeeded(@Nonnull FuelType fuelType) {
        if (!ARConfiguration.getCurrentConfig().rocketRequireFuel) {
            return 0;
        }

        long fuelTicks = getPreviewMissionFuelTicks();
        if (fuelTicks < 0L) {
            return -1;
        }

        int rate = Math.max(1, getPredictedFuelRate(fuelType));

        long needed = fuelTicks * (long)rate;
        return needed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)needed;
    }

    private String formatMbCompact(int amount) {
        if (amount < 0) {
            return "--";
        }

        if (amount >= 10000) {
            return Math.round(amount / 1000f) + "k";
        }

        return Integer.toString(amount);
    }

    private String formatFuelNeed(String label, @Nonnull FuelType fuelType) {
        if (!ARConfiguration.getCurrentConfig().rocketRequireFuel) {
            return label + ": OFF";
        }

        int needed = getEstimatedFuelNeeded(fuelType);
        int capacity = stats.getFuelCapacity(fuelType);

        if (needed < 0 || capacity <= 0) {
            return label + ": --/--";
        }

        return String.format("%s: %s/%s",
                label,
                formatMbCompact(needed),
                formatMbCompact(capacity));
    }

    protected void updateText() {
        if (thrustText == null || weightText == null || fuelText == null || accelerationText == null
                || twrText == null || gravityText == null || liftText == null || fuelStatusText == null
                || errorText == null) {
            return;
        }

        if (isScanning()) {
            thrustText.setText(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.thrust") + ": ???");
            weightText.setText(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.weight") + ": ???");
            accelerationText.setText(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.acc") + ": ???");
            fuelText.setText(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.fuel") + ": ???");

            twrText.setText("TWR: ???");
            gravityText.setText(String.format("g: %.2fx", getGravityMultiplier()));
            liftText.setText("???");
            fuelStatusText.setText("???");

            thrustText.setColor(0xFF22FF22);
            weightText.setColor(0xFF22FF22);
            accelerationText.setColor(0xFF22FF22);
            fuelText.setColor(0xFF22FF22);
            twrText.setColor(0xFF22FF22);
            gravityText.setColor(0xFF22FF22);
            liftText.setColor(0xFF22FF22);
            fuelStatusText.setColor(0xFF22FF22);
        } else {
            float previewWeight = getPreviewWetWeight();
            float neededThrust = getPreviewNeededThrust();
            float previewAcceleration = getPreviewAcceleration();

            float twr = neededThrust > 0f ? getThrust() / neededThrust : 0f;
            boolean liftOk = neededThrust > 0f && getThrust() > neededThrust;

            FuelType displayFuelType = getDisplayFuelType();
            boolean fuelOk = hasEnoughDisplayFuelCapacity();

            thrustText.setText(String.format("%s: %dkN",
                    LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.thrust"),
                    getThrust()));

            weightText.setText(String.format("%s: %.2fkN",
                    LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.weight"),
                    previewWeight * getGravityMultiplier()));

            accelerationText.setText(String.format("%s: %.2fm/s\u00b2",
                    LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.acc"),
                    previewAcceleration * 20f));

            twrText.setText(neededThrust > 0f ? String.format("TWR: %.2fx", twr) : "TWR: --");
            gravityText.setText(String.format("g: %.2fx", getGravityMultiplier()));
            liftText.setText(liftOk ? "OK" : "LOW");

            thrustText.setColor(0xFF22FF22);
            weightText.setColor(0xFF22FF22);
            accelerationText.setColor(liftOk ? 0xFF22FF22 : 0xFFFF5555);
            twrText.setColor(liftOk ? 0xFF22FF22 : 0xFFFF5555);
            gravityText.setColor(0xFF22FF22);
            liftText.setColor(liftOk ? 0xFF22FF22 : 0xFFFF5555);

            fuelText.setText(formatFuelNeed("Fuel", displayFuelType));
            fuelStatusText.setText(fuelOk ? "OK" : "LOW");

            fuelText.setColor(fuelOk ? 0xFF22FF22 : 0xFFFF5555);
            fuelStatusText.setColor(fuelOk ? 0xFF22FF22 : 0xFFFF5555);
        }

        if (!world.isRemote) {
            if (getRocketPadBounds(world, pos) == null)
                setStatus(ErrorCodes.INCOMPLETESTRCUTURE.ordinal());
            else if (ErrorCodes.INCOMPLETESTRCUTURE.equals(getStatus()))
                setStatus(ErrorCodes.UNSCANNED.ordinal());
        }

        errorText.setText(getStatus().getErrorCode());
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {

        // Automatically set status to unscanned if no rocket is present when opening GUI
        if (!world.isRemote && status == ErrorCodes.ALREADY_ASSEMBLED) {
            AxisAlignedBB box = (bbCache != null) ? bbCache : getRocketPadBounds(world, pos);
            if (box == null || world.getEntitiesWithinAABB(EntityRocket.class, box).isEmpty()) {
                status = ErrorCodes.UNSCANNED;
                markDirty();
            }
        }

        List<ModuleBase> modules = new LinkedList<>();
        modules.add(new ModulePower(160, 90, this));

        if (world.isRemote)
            modules.add(new ModuleImage(4, 9, new IconResource(4, 9, 168, 74, backdrop)));

        modules.add(new ModuleProgress(149, 90, 2, verticalProgressBar, this));
        modules.add(new ModuleButton(5, 94, 0, LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.scan"), this, zmaster587.libVulpes.inventory.TextureResources.buttonScan));

        ModuleButton buttonBuild;
        modules.add(buttonBuild = new ModuleButton(5, 120, 1, LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.build"), this, zmaster587.libVulpes.inventory.TextureResources.buttonBuild));
        buttonBuild.setColor(0xFFFF2222);

        modules.add(thrustText = new ModuleText(8, 15, "", 0xFF22FF22));
        modules.add(twrText = new ModuleText(108, 15, "", 0xFF22FF22));
        modules.add(weightText = new ModuleText(8, 34, "", 0xFF22FF22));
        modules.add(gravityText = new ModuleText(108, 34, "", 0xFF22FF22));
        modules.add(accelerationText = new ModuleText(8, 52, "", 0xFF22FF22));
        modules.add(liftText = new ModuleText(108, 52, "", 0xFF22FF22));
        modules.add(fuelText = new ModuleText(8, 71, "", 0xFF22FF22));
        modules.add(fuelStatusText = new ModuleText(108, 71, "", 0xFF22FF22));
        modules.add(errorText = new ModuleText(5, 84, "", 0xFFFFFF22));

        updateText();
        return modules;
    }

    @Override
    public String getModularInventoryName() {
        return "";
    }

    @Override
    public float getNormallizedProgress(int id) {
        if (id != 2) {return 0f;}
        return (float) this.getNormallizedProgress();
    }

    @Override
    public void setProgress(int id, int progress) {
        if (id == 2)
            setProgress(progress);
    }

    @Override
    public int getProgress(int id) {
        if (id == 2)
            return getProgress();
        return 0;
    }

    @Override
    public int getTotalProgress(int id) {
        if (id == 2)
            return getTotalProgress();
        return 0;
    }

    @Override
    public void setTotalProgress(int id, int progress) {
        if (id == 2) {
            setTotalProgress(progress);
            updateText();
        }
    }

    @Override
    public void setData(int id, int value) {
        switch (id) {
            case 0:
                getRocketStats().setWeight(value/1000f);
                break;
            case 1:
                getRocketStats().setThrust(value);
                break;
            case 2:
                setStatus(value);
                break;
            case 3:
                getRocketStats().setBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT, value);
                break;
            case 4:
                getRocketStats().setFuelAmount(FuelType.LIQUID_MONOPROPELLANT, value);
                break;
            case 5:
                getRocketStats().setFuelCapacity(FuelType.LIQUID_MONOPROPELLANT, value);
                break;
            case 6:
                getRocketStats().setFuelRate(FuelType.LIQUID_MONOPROPELLANT, value);
                break;
            case 7:
                getRocketStats().setBaseFuelRate(FuelType.LIQUID_BIPROPELLANT, value);
                break;
            case 8:
                getRocketStats().setFuelAmount(FuelType.LIQUID_BIPROPELLANT, value);
                break;
            case 9:
                getRocketStats().setFuelCapacity(FuelType.LIQUID_BIPROPELLANT, value);
                break;
            case 10:
                getRocketStats().setFuelRate(FuelType.LIQUID_BIPROPELLANT, value);
                break;
            case 11:
                getRocketStats().setBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID, value);
                break;
            case 12:
                getRocketStats().setFuelAmount(FuelType.NUCLEAR_WORKING_FLUID, value);
                break;
            case 13:
                getRocketStats().setFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID, value);
                break;
            case 14:
                getRocketStats().setFuelRate(FuelType.NUCLEAR_WORKING_FLUID, value);
                break;
            case 15:
                getRocketStats().setBaseFuelRate(FuelType.LIQUID_OXIDIZER, value);
                break;
            case 16:
                getRocketStats().setFuelAmount(FuelType.LIQUID_OXIDIZER, value);
                break;
            case 17:
                getRocketStats().setFuelCapacity(FuelType.LIQUID_OXIDIZER, value);
                break;
            case 18:
                getRocketStats().setFuelRate(FuelType.LIQUID_OXIDIZER, value);
                break;
        }
        updateText();
    }

    @Override
    public int getData(int id) {
        switch (id) {
            case 0:
                return Math.round(getRocketStats().getWeight_NoFuel() * 1000f);
            case 1:
                return getRocketStats().getThrust();
            case 2:
                return getStatus().ordinal();
            case 3:
                return getRocketStats().getBaseFuelRate(FuelType.LIQUID_MONOPROPELLANT);
            case 4:
                return getRocketStats().getFuelAmount(FuelType.LIQUID_MONOPROPELLANT);
            case 5:
                return getRocketStats().getFuelCapacity(FuelType.LIQUID_MONOPROPELLANT);
            case 6:
                return getRocketStats().getFuelRate(FuelType.LIQUID_MONOPROPELLANT);
            case 7:
                return getRocketStats().getBaseFuelRate(FuelType.LIQUID_BIPROPELLANT);
            case 8:
                return getRocketStats().getFuelAmount(FuelType.LIQUID_BIPROPELLANT);
            case 9:
                return getRocketStats().getFuelCapacity(FuelType.LIQUID_BIPROPELLANT);
            case 10:
                return getRocketStats().getFuelRate(FuelType.LIQUID_BIPROPELLANT);
            case 11:
                return getRocketStats().getBaseFuelRate(FuelType.NUCLEAR_WORKING_FLUID);
            case 12:
                return getRocketStats().getFuelAmount(FuelType.NUCLEAR_WORKING_FLUID);
            case 13:
                return getRocketStats().getFuelCapacity(FuelType.NUCLEAR_WORKING_FLUID);
            case 14:
                return getRocketStats().getFuelRate(FuelType.NUCLEAR_WORKING_FLUID);
            case 15:
                return getRocketStats().getBaseFuelRate(FuelType.LIQUID_OXIDIZER);
            case 16:
                return getRocketStats().getFuelAmount(FuelType.LIQUID_OXIDIZER);
            case 17:
                return getRocketStats().getFuelCapacity(FuelType.LIQUID_OXIDIZER);
            case 18:
                return getRocketStats().getFuelRate(FuelType.LIQUID_OXIDIZER);
        }
        return 0;
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        PacketHandler.sendToServer(new PacketMachine(this, (byte) (buttonId)));
    }

    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing arg0) {
        return true;
    }

    @Override
    public boolean onLinkStart(@Nonnull ItemStack item, TileEntity entity,
                               EntityPlayer player, World world) {
        return true;
    }

    @Override
    public boolean onLinkComplete(@Nonnull ItemStack item, TileEntity entity,
                                  EntityPlayer player, World world) {
        TileEntity tile = world.getTileEntity(ItemLinker.getMasterCoords(item));
        float maxlinkDistance = 15;

        if (tile instanceof IInfrastructure) {
            HashedBlockPosition pos = new HashedBlockPosition(tile.getPos());

            if (pos.getDistance(new HashedBlockPosition(this.pos)) > maxlinkDistance) {
                if (!world.isRemote)
                    player.sendMessage(new TextComponentTranslation("the machine is too far away to be linked"));
                return false;
            }

            if (!blockPos.contains(pos))
                blockPos.add(pos);

            if (getBBCache() == null) {
                bbCache = getRocketPadBounds(world, getPos());
            }

            if (getBBCache() != null) {

                List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, bbCache);
                for (EntityRocketBase rocket : rockets) {
                    rocket.linkInfrastructure((IInfrastructure) tile);
                }
            }

            if (!world.isRemote) {
                player.sendMessage(new TextComponentTranslation("msg.linker.success"));

                if (tile instanceof IMultiblock)
                    ((IMultiblock) tile).setMasterBlock(getPos());
            }

            ItemLinker.resetPosition(item);
            return true;
        }
        return false;
    }

    public void removeConnectedInfrastructure(TileEntity tile) {
        blockPos.remove(new HashedBlockPosition(tile.getPos()));

        if (getBBCache() == null) {
            bbCache = getRocketPadBounds(world, this.getPos());
        }

        if (getBBCache() != null) {
            List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, bbCache);

            for (EntityRocketBase rocket : rockets) {
                rocket.unlinkInfrastructure((IInfrastructure) tile);
            }
        }
    }

    public List<IInfrastructure> getConnectedInfrastructure() {
        List<IInfrastructure> list = new LinkedList<>();
        for (HashedBlockPosition position : blockPos) {
            TileEntity te = world.getTileEntity(position.getBlockPos());
            if (te instanceof IInfrastructure) {
                list.add((IInfrastructure) te);
            }
        }
        return list;
    }

    @SubscribeEvent
    public void onRocketLand(RocketLandedEvent e) {
        // Server/world guard
        if (e.world.isRemote || e.world != this.world) return;

        // Ensure we have pad bounds
        bbCache = getRocketPadBounds(world, pos);
        if (bbCache == null) return;

        // Make sure the event entity is a rocket
        final net.minecraft.entity.Entity ent = e.getEntity();
        if (!(ent instanceof EntityRocketBase)) return;
        final EntityRocketBase landed = (EntityRocketBase) ent;

        // Quick membership test with tiny epsilon
        final AxisAlignedBB box = bbCache.grow(1.0E-4, 1.0E-4, 1.0E-4);
        if (!landed.getEntityBoundingBox().intersects(box)) return;

        // Track rocket id and (re)link infra
        lastRocketID = landed.getEntityId();
        for (IInfrastructure infra : getConnectedInfrastructure()) {
            if (infra instanceof zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) {
                ((zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) infra)
                        .markRocketFromAssembler(landed);
            }
            landed.linkInfrastructure(infra);
        }


        // only fast-path when exactly one rocket in the pad
        List<EntityRocket> rockets = world.getEntitiesWithinAABB(EntityRocket.class, box);
        if (rockets.size() == 1) {
            EntityRocket r = rockets.get(0);
            r.recalculateStats();
            this.stats = r.stats.copy();
            this.status = ErrorCodes.ALREADY_ASSEMBLED;
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        } else {
            // Fallback: rescan if something odd happens
            scanRocket(world, pos, bbCache);
        }
        PacketHandler.sendToPlayersTrackingEntity(new PacketMachine(this, (byte)3), landed);
    }

    protected enum ErrorCodes {
        SUCCESS(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.success")),
        NOFUEL(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nofuel")),
        NOSEAT(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.noseat")),
        NOENGINES(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.noengines")),
        NOGUIDANCE(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.noguidance")),
        UNSCANNED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.unscanned")),
        SUCCESS_STATION(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.success_station")),
        EMPTY(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.empty")),
        FINISHED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.finished")),
        INCOMPLETESTRCUTURE(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.incompletestructure")),
        NOSATELLITEHATCH(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nosatellitehatch")),
        NOSATELLITECHIP(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nosatellitechip")),
        OUTPUTBLOCKED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.outputblocked")),
        INVALIDBLOCK(LibVulpes.proxy.getLocalizedString("msg.rocketbuild.invalidblock")),
        COMBINEDTHRUST(LibVulpes.proxy.getLocalizedString("msg.rocketbuild.combinedthrust")),
        ALREADY_ASSEMBLED(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.alreadyassembled")),
        UNSCANNED_STATION(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.unscanned_station")),
        FAIL_CUT(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.fail_cut")),
        NOINTAKE(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.nointake")),
        NOTANK(LibVulpes.proxy.getLocalizedString("msg.rocketbuilder.notank"));

        String code;

        ErrorCodes(String code) {
            this.code = code;
        }
        public String getErrorCode() {
            return code;
        }
    }

    @Override
    public void update() {
        super.update(); 
        if (world.isRemote) return;

        if (relinkRetries > 0 && world.getTotalWorldTime() >= nextRelinkAttempt) {
            if (tryRelinkNow()) {
                relinkRetries = 0;
            } else {
                relinkRetries--;
                nextRelinkAttempt = world.getTotalWorldTime() + 20; // 1s
            }
        }
    }

    private boolean tryRelinkNow() {
        if (bbCache == null) bbCache = getRocketPadBounds(world, pos);
        if (bbCache == null) return false;

        AxisAlignedBB box = bbCache.grow(1.0e-4,1.0e-4,1.0e-4);
        java.util.List<EntityRocketBase> rockets = world.getEntitiesWithinAABB(EntityRocketBase.class, box);
        if (rockets.isEmpty()) return false;

        java.util.List<IInfrastructure> infraNow = getConnectedInfrastructure();
        if (infraNow.isEmpty()) return false;

        for (EntityRocketBase r : rockets) {
            for (IInfrastructure i : infraNow) {
                if (i instanceof zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) {
                    ((zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation) i)
                            .markRocketFromAssembler(r);
                }
                r.linkInfrastructure(i);
            }
        }
        return true;
    }

    private int getPredictedFuelRate(@Nullable FuelType type) {
        if (type == null) {
            return 0;
        }

        int actualRate = stats.getFuelRate(type);
        int baseRate = stats.getBaseFuelRate(type);

        if (baseRate <= 0) {
            return actualRate;
        }
        // If fuel is already known or inserted, use the actual synced rate.
        // This handles packs with multiple valid fluids, e.g. water;5 and rocketfuel;10.
        if (actualRate > 0 && (stats.getFuelAmount(type) > 0 || hasKnownFuelFluid(type) || actualRate != baseRate)) {
            return actualRate;
        }
        // Empty/unselected fuel: use conservative expected burn rate.
        // This avoids under-reporting before the player inserts fuel.
        return Math.round(baseRate * getMaxRegisteredFuelMultiplier(type));
    }

    private boolean hasKnownFuelFluid(@Nullable FuelType type) {
        String fluidName = getKnownFuelFluidName(type);
        return fluidName != null && !"null".equals(fluidName) && FluidRegistry.isFluidRegistered(fluidName);
    }

    @Nullable
    private String getKnownFuelFluidName(@Nullable FuelType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case LIQUID_MONOPROPELLANT:
            case LIQUID_BIPROPELLANT:
                return stats.getFuelFluid();

            case LIQUID_OXIDIZER:
                return stats.getOxidizerFluid();

            case NUCLEAR_WORKING_FLUID:
                return stats.getWorkingFluid();

            default:
                return null;
        }
    }

    private float getMaxRegisteredFuelMultiplier(@Nullable FuelType type) {
        if (type == null) {
            return 1f;
        }

        float max = 1f;
        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
            if (FuelRegistry.instance.isFuel(type, fluid)) {
                max = Math.max(max, FuelRegistry.instance.getMultiplier(type, fluid));
            }
        }
        return max;
    }
}