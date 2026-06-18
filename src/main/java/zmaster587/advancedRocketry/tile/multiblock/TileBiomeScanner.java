package zmaster587.advancedRocketry.tile.multiblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.multiblock.TileMultiPowerConsumer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

public class TileBiomeScanner extends TileMultiPowerConsumer
        implements IButtonInventory {

    private static final int SCAN_ENERGY_COST = 10_000;

    private static final int INVALID_SCAN_DIMENSION = Integer.MIN_VALUE;

    private static final byte RESULT_NONE   = 0;
    private static final byte RESULT_BIOMES = 1;
    private static final byte RESULT_GAS    = 2;
    private static final byte RESULT_STAR   = 3;

    private static final byte STATUS_NONE             = 0;
    private static final byte STATUS_SUCCESS          = 1;
    private static final byte STATUS_NO_POWER         = 2;
    private static final byte STATUS_INVALID_LOCATION = 3;

    private static final int GUI_BUTTON_SCAN = 0;

    private static final byte NET_BUTTON_SCAN   = 10;
    private static final byte NET_REQUEST_REOPEN = 11;

    private int cachedDimensionId = INVALID_SCAN_DIMENSION;
    private byte cachedResultType = RESULT_NONE;
    private byte scanStatus = STATUS_NONE;

    private final List<Integer> cachedBiomeIds = new ArrayList<>();

    private int scanNonce = 0;

    // Client only
    private boolean pendingReopenAfterScan = false;

    private static final Object[][][] structure = new Object[][][]{
            {{null, null, null, null, null},
                    {null, null, null, null, null},
                    {null, null, 'P', null, null},
                    {null, null, null, null, null},
                    {null, null, null, null, null}},

            {{null, null, null, null, null},
                    {null, null, null, null, null},
                    {null, null, 'c', null, null},
                    {null, null, null, null, null},
                    {null, null, null, null, null}},

            {{null, null, null, null, null},
                    {null, null, null, null, null},
                    {null, null, LibVulpesBlocks.motors, null, null},
                    {null, null, null, null, null},
                    {null, null, null, null, null}},

            {{null, "blockAluminum", "blockAluminum", "blockAluminum", null},
                    {"blockAluminum", "blockAluminum", AdvancedRocketryBlocks.blockStructureTower, "blockAluminum", "blockAluminum"},
                    {"blockAluminum", AdvancedRocketryBlocks.blockStructureTower, LibVulpesBlocks.blockStructureBlock, AdvancedRocketryBlocks.blockStructureTower, "blockAluminum"},
                    {"blockAluminum", "blockAluminum", AdvancedRocketryBlocks.blockStructureTower, "blockAluminum", "blockAluminum"},
                    {null, "blockAluminum", "blockAluminum", "blockAluminum", null}},

            {{Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR},
                    {Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR},
                    {Blocks.AIR, Blocks.AIR, Blocks.REDSTONE_BLOCK, Blocks.AIR, Blocks.AIR},
                    {Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR},
                    {Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR}}};


    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    private boolean hasCachedScan() {
        return cachedDimensionId != INVALID_SCAN_DIMENSION
                && cachedResultType != RESULT_NONE;
    }

    private void clearScanCacheLocal() {
        cachedDimensionId = INVALID_SCAN_DIMENSION;
        cachedResultType = RESULT_NONE;
        scanStatus = STATUS_NONE;
        cachedBiomeIds.clear();
    }

    private void syncScanState() {
        if (world == null || world.isRemote) {return;}
        scanNonce++;
        markDirty();
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
    }

    private boolean hasClearViewBelow() {
        if (world == null) {return false;}
        for (int y = pos.getY() - 4; y > 0; y--) {
            if (!world.isAirBlock(new BlockPos(pos.getX(), y, pos.getZ()))) {return false;}
        }
        return true;
    }

    private int getCurrentEffectiveDimensionId() {
        if (world == null) {
            return INVALID_SCAN_DIMENSION;
        }

        ISpaceObject station = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(pos);

        if (station == null
                || station.getOrbitingPlanetId() == SpaceObjectManager.WARPDIMID) {
            return INVALID_SCAN_DIMENSION;
        }

        DimensionProperties effectiveProperties = DimensionManager.getEffectiveDimId(world, pos);
        if (effectiveProperties == null
                || effectiveProperties.getId() == SpaceObjectManager.WARPDIMID) {
            return INVALID_SCAN_DIMENSION;
        }
        return effectiveProperties.getId();
    }

    private void performScan() {
        if (world == null || world.isRemote) {
            return;
        }

        int effectiveDimensionId = getCurrentEffectiveDimensionId();

        if (!isComplete() || !hasClearViewBelow() || effectiveDimensionId == INVALID_SCAN_DIMENSION) {

            clearScanCacheLocal();
            scanStatus = STATUS_INVALID_LOCATION;
            syncScanState();
            return;
        }

        DimensionProperties properties =
                DimensionManager.getInstance()
                        .getDimensionProperties(effectiveDimensionId);

        if (properties == null) {
            clearScanCacheLocal();
            scanStatus = STATUS_INVALID_LOCATION;
            syncScanState();
            return;
        }

        /*
         * The current cached scan is already valid for this planet.
         * Do not charge repeatedly for identical immutable information.
         */
        if (hasCachedScan()
                && cachedDimensionId == effectiveDimensionId) {

            scanStatus = STATUS_SUCCESS;
            syncScanState();
            return;
        }

        if (getBatteries().extractEnergy(SCAN_ENERGY_COST, true)
                < SCAN_ENERGY_COST) {

            clearScanCacheLocal();
            scanStatus = STATUS_NO_POWER;
            syncScanState();
            return;
        }

        byte newResultType;
        List<Integer> newBiomeIds = new ArrayList<>();

        if (properties.isGasGiant()) {
            newResultType = RESULT_GAS;
        } else if (properties.isStar()) {
            newResultType = RESULT_STAR;
        } else {
            newResultType = RESULT_BIOMES;

            if (properties.getId() == 0) {
                for (Biome biome : Biome.REGISTRY) {
                    if (biome != null) {
                        int biomeId = Biome.getIdForBiome(biome);
                        if (biomeId >= 0) {
                            newBiomeIds.add(biomeId);
                        }
                    }
                }
            } else {
                for (BiomeEntry entry : properties.getBiomes()) {
                    if (entry != null && entry.biome != null) {
                        int biomeId = Biome.getIdForBiome(entry.biome);
                        if (biomeId >= 0) {
                            newBiomeIds.add(biomeId);
                        }
                    }
                }
            }
        }

        int extracted = getBatteries().extractEnergy(SCAN_ENERGY_COST, false);
        if (extracted < SCAN_ENERGY_COST) {
            clearScanCacheLocal();
            scanStatus = STATUS_NO_POWER;
            syncScanState();
            return;
        }

        cachedBiomeIds.clear();
        cachedBiomeIds.addAll(newBiomeIds);

        cachedDimensionId = effectiveDimensionId;
        cachedResultType = newResultType;
        scanStatus = STATUS_SUCCESS;

        syncScanState();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void update() {
        super.update();

        if (world == null
                || world.isRemote
                || !hasCachedScan()
                || world.getTotalWorldTime() % 20L != 0L) {
            return;
        }

        int currentDimensionId = getCurrentEffectiveDimensionId();

        if (!isComplete()
                || currentDimensionId != cachedDimensionId) {

            clearScanCacheLocal();
            syncScanState();
        }
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> list = new LinkedList<>();

        list.add(new ModulePower(155, 25, getBatteries()));
        list.add(new ModuleButton(100, 120, GUI_BUTTON_SCAN,
                LibVulpes.proxy.getLocalizedString(
                        "msg.biomescanner.scan.button"), this,
                zmaster587.libVulpes.inventory.TextureResources.buttonBuild, String.format(
                        java.util.Locale.ROOT,
                        LibVulpes.proxy.getLocalizedString("msg.biomescanner.scan.tooltip"), SCAN_ENERGY_COST),
                64,
                18));

        if (world != null && world.isRemote) {
            list.add(new ModuleImage(24, 14, zmaster587.advancedRocketry.inventory.TextureResources
                            .earthCandyIcon));

            List<ModuleBase> scanLines = new LinkedList<>();

            if (cachedResultType == RESULT_GAS) {
                scanLines.add(new ModuleText(10, 16,
                        LibVulpes.proxy.getLocalizedString("msg.biomescanner.gas"), 0x202020));
            } else if (cachedResultType == RESULT_STAR) {
                scanLines.add(new ModuleText(10, 16,
                        LibVulpes.proxy.getLocalizedString("msg.biomescanner.star"), 0x202020
                ));
            } else if (cachedResultType == RESULT_BIOMES) {
                int index = 0;

                for (int biomeId : cachedBiomeIds) {
                    Biome biome = Biome.getBiome(biomeId);

                    if (biome != null) {
                        scanLines.add(new ModuleText(10, 16 + 12 * index++,
                                AdvancedRocketry.proxy.getNameFromBiome(biome), 0x202020
                        ));
                    }
                }
            } else {
                String messageKey;

                if (scanStatus == STATUS_NO_POWER) {
                    messageKey = "msg.biomescanner.scan.nopower";
                } else if (scanStatus == STATUS_INVALID_LOCATION) {
                    messageKey = "msg.biomescanner.scan.invalid";
                } else {
                    messageKey = "msg.biomescanner.scan.required";
                }
                scanLines.add(new ModuleText(10, 16,
                        LibVulpes.proxy.getLocalizedString(messageKey), 0x202020
                ));
            }

            list.add(new ModuleContainerPan(4, 16,
                    scanLines, new LinkedList<>(), null,
                    140, 103, 0, -64,
                    0, 1000
            ));
        }

        return list;
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        if (buttonId != GUI_BUTTON_SCAN || world == null) {
            return;
        }

        if (world.isRemote) {
            AdvancedRocketry.proxy.clearScrollCache();

            pendingReopenAfterScan = true;

            PacketHandler.sendToServer(
                    new PacketMachine(this, NET_BUTTON_SCAN)
            );
        } else {
            performScan();
        }
    }

    @Override
    public void useNetworkData(
            EntityPlayer player,
            Side side,
            byte id,
            NBTTagCompound nbt) {

        super.useNetworkData(player, side, id, nbt);

        if (world == null || world.isRemote) {
            return;
        }

        if (id == NET_BUTTON_SCAN) {
            performScan();
        } else if (id == NET_REQUEST_REOPEN) {
            player.openGui(
                    LibVulpes.instance,
                    GuiHandler.guiId.MODULARNOINV.ordinal(),
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        }
    }

    @Override
    protected void writeNetworkData(NBTTagCompound nbt) {
        super.writeNetworkData(nbt);

        nbt.setInteger("biomeScanNonce", scanNonce);
        nbt.setInteger("biomeScanDimension", cachedDimensionId);
        nbt.setByte("biomeScanResultType", cachedResultType);
        nbt.setByte("biomeScanStatus", scanStatus);

        int[] biomeIds = new int[cachedBiomeIds.size()];

        for (int i = 0; i < cachedBiomeIds.size(); i++) {
            biomeIds[i] = cachedBiomeIds.get(i);
        }

        nbt.setIntArray("biomeScanBiomeIds", biomeIds);
    }

    @Override
    protected void readNetworkData(NBTTagCompound nbt) {
        int previousNonce = scanNonce;

        super.readNetworkData(nbt);

        scanNonce = nbt.getInteger("biomeScanNonce");

        cachedDimensionId = nbt.hasKey("biomeScanDimension")
                ? nbt.getInteger("biomeScanDimension")
                : INVALID_SCAN_DIMENSION;

        cachedResultType = nbt.getByte("biomeScanResultType");
        scanStatus = nbt.getByte("biomeScanStatus");

        cachedBiomeIds.clear();

        for (int biomeId : nbt.getIntArray("biomeScanBiomeIds")) {
            cachedBiomeIds.add(biomeId);
        }

        if (world != null
                && world.isRemote
                && pendingReopenAfterScan
                && previousNonce != scanNonce
                && net.minecraft.client.Minecraft
                .getMinecraft().currentScreen
                instanceof zmaster587.libVulpes.inventory.GuiModular) {

            pendingReopenAfterScan = false;

            PacketHandler.sendToServer(
                    new PacketMachine(this, NET_REQUEST_REOPEN)
            );
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();

        clearScanCacheLocal();
        pendingReopenAfterScan = false;

        if (world != null && world.isRemote) {
            AdvancedRocketry.proxy.clearScrollCache();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();

        clearScanCacheLocal();
        pendingReopenAfterScan = false;

        if (world != null && world.isRemote) {
            AdvancedRocketry.proxy.clearScrollCache();
        }
    }

    @Override
    @Nonnull
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-5, -3, -5), pos.add(5, 3, 5));
    }

    @Override
    public String getMachineName() {
        return AdvancedRocketryBlocks.blockBiomeScanner.getLocalizedName();
    }
}