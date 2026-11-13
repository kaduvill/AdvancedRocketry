package zmaster587.advancedRocketry.tile.satellite;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.block.BlockTileTerraformer;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.inventory.modules.ModuleSatellite;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.item.ItemBiomeChanger;
import zmaster587.advancedRocketry.item.ItemSatelliteIdentificationChip;
import zmaster587.advancedRocketry.satellite.SatelliteBiomeChanger;
import zmaster587.advancedRocketry.util.BiomeHandler;
import zmaster587.advancedRocketry.util.BiomeUpdateBatch;
import zmaster587.advancedRocketry.util.TerraformingHelper;
import zmaster587.advancedRocketry.util.TerraformingBiomeState;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IUniversalEnergy;
import zmaster587.libVulpes.gui.CommonResources;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.GuiModular;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumer;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.IconResource;
import zmaster587.libVulpes.cap.TeslaHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public class TileTerraformingTerminal extends TileInventoriedRFConsumer implements INetworkMachine, IModularInventory, IButtonInventory, IGuiCallback, IDataSync {

    private static final byte TAB_SWITCH = 23, CLIMATE_INFO = 24, REQUEST_CLIMATE_INFO = 25;

    private ModuleText moduleText;
    private ModuleText climateInfoText;
    private Container climateContainer;
    private int requestedTab;
    private int requestedWindowId;
    private NBTTagCompound climateInfo;
    private int climateRevision = -1;
    private int requestedClimateRevision = -1;
    private long lastClimateCheck = Long.MIN_VALUE;
    private TerraformingBiomeState climateTargets;
    public boolean was_enabled_last_tick;
    private int sat_power_per_tick;
    private float randomblocks_per_tick;

    public TileTerraformingTerminal() {
        super(1, 1);
        sat_power_per_tick = 0;
        randomblocks_per_tick = 0;
        was_enabled_last_tick = false;
    }

    @Override
    @Nonnull
    public int[] getSlotsForFace(@Nullable EnumFacing side) {
        return new int[0];
    }

    @Override
    public String getModularInventoryName() {
        return AdvancedRocketryBlocks.blockTerraformingTerminal.getLocalizedName();
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        return slot == 0
                && !stack.isEmpty()
                && stack.getItem() instanceof ItemBiomeChanger;
    }

    @Override
    public boolean canPerformFunction() {
        return true;
    }

    @Override
    public int getPowerPerOperation() {
        return 0;
    }

    @Override
    public void performFunction() {
        if (world.isRemote)
            updateInventoryInfo();
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte packetId) {
        if (packetId == (byte) 22) {
            out.writeInt(sat_power_per_tick);
            out.writeFloat(randomblocks_per_tick);
        } else if (packetId == TAB_SWITCH) {
            out.writeByte(requestedTab);
            out.writeInt(requestedWindowId);
        } else if (packetId == CLIMATE_INFO) {
            out.writeInt(climateRevision);
            out.writeInt(climateInfo.getInteger("pressure"));
            out.writeInt(climateInfo.getInteger("temperature"));
            out.writeByte(climateInfo.getByte("climate"));
            new PacketBuffer(out).writeVarIntArray(climateInfo.getIntArray("biomes"));
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte packetId,
                                    NBTTagCompound nbt) {
        if (packetId == (byte) 22) {
            nbt.setInteger("powergen", in.readInt());
            nbt.setFloat("blockpertick", in.readFloat());
        } else if (packetId == TAB_SWITCH) {
            nbt.setByte("tab", in.readByte());
            nbt.setInteger("window", in.readInt());
        } else if (packetId == CLIMATE_INFO) {
            nbt.setInteger("revision", in.readInt());
            nbt.setInteger("pressure", in.readInt());
            nbt.setInteger("temperature", in.readInt());
            nbt.setByte("climate", in.readByte());
            nbt.setIntArray("biomes", new PacketBuffer(in).readVarIntArray());
        }
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
        if (id == (byte) 22) {
            this.sat_power_per_tick = nbt.getInteger("powergen");
            this.randomblocks_per_tick = nbt.getFloat("blockpertick");
            this.updateInventoryInfo();
        } else if (side == Side.SERVER && player.world == world && isUsableByPlayer(player)) {
            if (id == TAB_SWITCH) {
                int tab = nbt.getByte("tab");
                if ((tab == 0 || tab == 1) && player.openContainer.windowId == nbt.getInteger("window"))
                    player.openGui(LibVulpes.instance, (tab == 1 ? GuiHandler.guiId.MODULARNOINV
                            : GuiHandler.guiId.MODULAR).ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
            } else if (id == REQUEST_CLIMATE_INFO) {
                refreshClimateInfo();
                PacketHandler.sendToPlayer(new PacketMachine(this, CLIMATE_INFO), player);
            }
        } else if (side == Side.CLIENT && id == CLIMATE_INFO) {
            receiveClimateInfo(nbt);
        }
    }

    private void refreshClimateInfo() {
        long now = world.getTotalWorldTime();
        if (climateInfo != null && now >= lastClimateCheck && now - lastClimateCheck < 20) return;
        lastClimateCheck = now;

        DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension());
        int temperature = properties.getAverageTemp();
        DimensionProperties.BiomeClimate climate = properties.getBiomeClimate(temperature);
        TerraformingBiomeState targets = properties.getOrCreateTerraformingBiomeTargets(temperature);
        int[] biomes;
        if (targets != climateTargets) {
            biomes = targets.getBiomes().stream().mapToInt(entry -> Biome.getIdForBiome(entry.biome))
                    .filter(id -> id >= 0).distinct().sorted().toArray();
            climateTargets = targets;
        } else {
            biomes = climateInfo.getIntArray("biomes");
        }
        if (climateInfo != null && climateInfo.getInteger("pressure") == properties.getAtmosphereDensity()
                && climateInfo.getInteger("temperature") == temperature
                && climateInfo.getByte("climate") == climate.ordinal()
                && Arrays.equals(climateInfo.getIntArray("biomes"), biomes)) return;

        climateInfo = new NBTTagCompound();
        climateInfo.setInteger("pressure", properties.getAtmosphereDensity());
        climateInfo.setInteger("temperature", temperature);
        climateInfo.setByte("climate", (byte) climate.ordinal());
        climateInfo.setIntArray("biomes", biomes);
        climateRevision = (climateRevision + 1) & 0x7fff; // Container properties are signed shorts.
    }

    @Override
    public int getData(int id) {
        if (!world.isRemote) refreshClimateInfo();
        return climateRevision;
    }

    @Override
    public void setData(int id, int value) {
        if (world.isRemote) requestClimateInfo(value);
    }

    @SideOnly(Side.CLIENT)
    private void requestClimateInfo(int revision) {
        climateContainer = Minecraft.getMinecraft().player.openContainer;
        if ((climateInfo == null || climateRevision != revision) && requestedClimateRevision != revision) {
            requestedClimateRevision = revision;
            PacketHandler.sendToServer(new PacketMachine(this, REQUEST_CLIMATE_INFO));
        }
    }

    @SideOnly(Side.CLIENT)
    private void receiveClimateInfo(NBTTagCompound info) {
        boolean listChanged = climateInfo == null
                || !Arrays.equals(climateInfo.getIntArray("biomes"), info.getIntArray("biomes"));
        climateInfo = info;
        climateRevision = info.getInteger("revision");
        Minecraft mc = Minecraft.getMinecraft();
        if (climateInfoText == null || !(mc.currentScreen instanceof GuiModular)
                || mc.player.openContainer != climateContainer) return;
        if (listChanged) {
            // Rebuild the list only when its contents change, retaining the standard scroll cache.
            sendTabRequest(1);
        } else {
            climateInfoText.setText(getClimateText());
        }
    }

    @Override
    public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        updateInventoryInfo();
    }

    @Override
    public void update() {
        if (world == null) {
            return;}

        // Fast path: truly idle — no chip and never enabled
        if (getStackInSlot(0).isEmpty() && !was_enabled_last_tick) {
            return;
        }

        int powerrequired = 80; //120;

        if (!world.isRemote) {
            boolean has_redstone = world.isBlockIndirectlyGettingPowered(getPos()) != 0;
            boolean has_valid = hasValidBiomeChanger();

            // Only sync when there’s actually a biome changer present
            if (has_valid && (world.getTotalWorldTime() + 6) % 21 == 0) {
                PacketHandler.sendToNearby(new PacketMachine(this, (byte) 22),
                        world.provider.getDimension(), pos, 16);
            }

            if (has_valid && has_redstone) {
                was_enabled_last_tick = true;
                if (!world.getBlockState(pos).getValue(BlockTileTerraformer.STATE)) {
                    world.setBlockState(pos,
                            world.getBlockState(pos).withProperty(BlockTileTerraformer.STATE, true), 3);
                }

                Item biomeChanger = getStackInSlot(0).getItem();
                if (biomeChanger instanceof ItemBiomeChanger) {
                    SatelliteBiomeChanger sat = (SatelliteBiomeChanger)
                            ItemSatelliteIdentificationChip.getSatellite(getStackInSlot(0));
                    sat_power_per_tick = sat.getPowerPerTick();
                    randomblocks_per_tick = (float) sat_power_per_tick / powerrequired;
                }
            } else {
                was_enabled_last_tick = false;
                if (world.getBlockState(pos).getValue(BlockTileTerraformer.STATE)) {
                    world.setBlockState(pos,
                            world.getBlockState(pos).withProperty(BlockTileTerraformer.STATE, false), 3);
                }
            }
        }

        if (!world.isRemote && was_enabled_last_tick) {
            if (ARConfiguration.getCurrentConfig().enableTerraforming) {
                Item biomeChanger = getStackInSlot(0).getItem();
                if (biomeChanger instanceof ItemBiomeChanger) {
                    SatelliteBiomeChanger sat = (SatelliteBiomeChanger) ItemSatelliteIdentificationChip.getSatellite(getStackInSlot(0));
                    IUniversalEnergy battery = sat.getBattery();

                    try (BiomeUpdateBatch updates = new BiomeUpdateBatch(world)) {
                        for (int i = 0; i < 1000; i++) {
                            //TODO: Better imp

                            if (battery.getUniversalEnergyStored() > powerrequired) {
                                try {


                                    TerraformingHelper t = DimensionProperties.proxylists.gethelper(world.provider.getDimension());

                                    if (t == null) {
                                        DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).load_terraforming_helper(false);
                                        t = DimensionProperties.proxylists.gethelper(world.provider.getDimension());
                                    }

                                    BiomeProvider chunkmgr = t.chunkMgrTerraformed;
                                    //BlockPos next_block_pos = t.get_next_position(false);
                                    //BlockPos next_block_pos = t.get_next_position_biomechanging(false);
                                    BlockPos next_block_pos = t.get_next_position_biomechanging(true);

                                    if (next_block_pos != null) { // it is null when there is everything terraformed
                                        battery.extractEnergy(powerrequired, false);
                                        //BiomeHandler.terraform(world, ((ChunkManagerPlanet) chunkmgr).getBiomeGenAt(next_block_pos.getX(), next_block_pos.getZ()), next_block_pos, false, world.provider.getDimension());
                                        BiomeHandler.terraform_biomes(world, ((ChunkManagerPlanet) chunkmgr).getBiomeGenAt(next_block_pos.getX(), next_block_pos.getZ()), next_block_pos, world.provider.getDimension(), updates);
                                    } else {
                                        //System.out.println("nothing to terraform");
                                        break; // nothing to do, everything is terraformed
                                    }

                                    //} catch (NullPointerException e) {
                                    //    e.printStackTrace();
                                } catch (NoClassDefFoundError e) {
                                    e.printStackTrace(); //WTF
                                }

                            } else
                                break;
                        }
                    }
                }
            }
        }
    }

    public void updateInventoryInfo() {
        if (moduleText != null) {

            if (hasValidBiomeChanger() && world.isBlockIndirectlyGettingPowered(getPos()) != 0) {
                BigDecimal bd = new BigDecimal(randomblocks_per_tick);
                bd = bd.setScale(2, RoundingMode.HALF_UP);

                String header = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.terraforming");
                String powerLabel = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.powergen");
                String blocksLabel = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.blockspertick");

                moduleText.setText(
                        header + "\n" +
                        "\n" + powerLabel + " " + sat_power_per_tick +
                        "\n" + blocksLabel + " " + bd
                );

            } else if (hasValidBiomeChanger()) {

                String redstoneLine1 = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.needredstone.line1");
                String redstoneLine2 = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.needredstone.line2");

                moduleText.setText(
                        redstoneLine1 + "\n" +
                        redstoneLine2
                );

            } else {

                String insertLine1 = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.insertchip.line1");
                String insertLine2 = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.insertchip.line2");
                String insertLine3 = LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.insertchip.line3");

                moduleText.setText(
                        "\n" + insertLine1 + "\n" +
                        insertLine2 + "\n" +
                        insertLine3
                );
            }
        }
    }

    public boolean hasValidBiomeChanger() {
        ItemStack biomeChanger = getStackInSlot(0);
        SatelliteBase satellite;

        return !biomeChanger.isEmpty() &&
                (biomeChanger.getItem() instanceof ItemBiomeChanger) &&
                SatelliteRegistry.getSatellite(biomeChanger) != null &&
                (satellite = ((ItemSatelliteIdentificationChip) AdvancedRocketryItems.itemBiomeChanger).getSatellite(biomeChanger)).getDimensionId() == world.provider.getDimension() &&
                satellite instanceof SatelliteBiomeChanger;
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        boolean climateView = ID == GuiHandler.guiId.MODULARNOINV.ordinal();
        List<ModuleBase> modules = new LinkedList<>();
        if (!climateView) modules.add(new ModuleSatellite(152, 10, this, 0));

        ModuleTab tabs = new ModuleTab(4, 0, 0, this, 2, new String[]{
                LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.tab.operation"),
                LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.tab.climate")
        }, new ResourceLocation[][]{TextureResources.tabData, TextureResources.tabPlanet});
        tabs.setTab(climateView ? 1 : 0);
        modules.add(tabs);

        moduleText = null;
        climateInfoText = null;
        if (climateView) {
            modules.add(new ModuleSync(0, this));
            if (world.isRemote) addClimateModules(modules);
        } else {
            moduleText = new ModuleText(5, 20, LibVulpes.proxy.getLocalizedString("msg.satctrlcenter.nolink"), 0x404040);
            modules.add(moduleText);
            updateInventoryInfo();
        }
        return modules;
    }

    @SideOnly(Side.CLIENT)
    private void addClimateModules(List<ModuleBase> modules) {
        // Use the Orbital Registry's extended background and AR window frames.
        modules.add(new ModuleImage(173, 0, new IconResource(98, 0, 78, 86, CommonResources.genericBackground)));
        modules.add(new ModuleImage(173, 86, new IconResource(98, 3, 78, 82, CommonResources.genericBackground)));
        modules.add(new ModuleImage(173, 168, new IconResource(98, 168, 78, 3, CommonResources.genericBackground)));
        final int top = 44, height = 96, listX = 106, listWidth = 138;
        for (int pane = 0; pane < 2; pane++) {
            int x = pane == 0 ? 6 : listX;
            int width = pane == 0 ? 94 : listWidth;
            modules.add(new ModuleScaledImage(x - 3, top - 3, 3, height + 6, TextureResources.verticalBar));
            modules.add(new ModuleScaledImage(x + width, top - 3, -3, height + 6, TextureResources.verticalBar));
            modules.add(new ModuleScaledImage(x, top - 3, width, 3, TextureResources.horizontalBar));
            modules.add(new ModuleScaledImage(x, top + height, width, -3, TextureResources.horizontalBar));
        }
        modules.add(new ModuleText(9, 31, LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.current"), 0x404040));
        climateInfoText = new ModuleText(9, top + 3, getClimateText(), 0x404040);
        modules.add(climateInfoText);

        List<Biome> biomes = new ArrayList<>();
        if (climateInfo != null) {
            for (int id : climateInfo.getIntArray("biomes")) {
                Biome biome = Biome.getBiome(id);
                if (biome != null) biomes.add(biome);
            }
        }
        biomes.sort(Comparator.comparing(AdvancedRocketry.proxy::getNameFromBiome, String.CASE_INSENSITIVE_ORDER));
        String heading = climateInfo == null ? LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.tab.climate")
                : I18n.format("msg.terraformingterminal.targets", biomes.size());
        modules.add(new ModuleText(listX + 3, 31, heading, 0x404040));

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        List<ModuleBase> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (climateInfo == null) names.add(LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.loading"));
        else {
            if (biomes.isEmpty()) names.add(LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.empty"));
            for (Biome biome : biomes) names.add(AdvancedRocketry.proxy.getNameFromBiome(biome));
        }
        int y = 3;
        for (String name : names) {
            List<String> lines = font.listFormattedStringToWidth(name, listWidth - 8);
            rows.add(new ModuleText(3, y, String.join("\n", lines), 0x404040));
            y += lines.size() * font.FONT_HEIGHT + 3;
        }
        modules.add(AdvancedRocketry.proxy.createScrollListPan(listX, top, rows, listWidth, height, y));
    }

    @SideOnly(Side.CLIENT)
    private String getClimateText() {
        if (climateInfo == null) return LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.loading");
        int temperature = climateInfo.getInteger("temperature");
        DimensionProperties.BiomeClimate climate = DimensionProperties.BiomeClimate.values()[climateInfo.getByte("climate")];
        String condition = "";
        if (climate == DimensionProperties.BiomeClimate.MOON)
            condition = I18n.format("msg.terraformingterminal.lowpressure", String.format(Locale.ROOT, "%.2f",
                    DimensionProperties.AtmosphereTypes.LOW.getAtmosphereValue() / 100.0));
        else if (climate == DimensionProperties.BiomeClimate.TOOHOT)
            condition = I18n.format("msg.terraformingterminal.atleast", climate.minimumTemperature);
        else if (climate == DimensionProperties.BiomeClimate.FROZEN)
            condition = I18n.format("msg.terraformingterminal.below", climate.maximumTemperature + 1);
        else if (climate != DimensionProperties.BiomeClimate.EMPTY)
            condition = climate.minimumTemperature + " - " + climate.maximumTemperature + " K";
        return LibVulpes.proxy.getLocalizedString("msg.terraformer.pressure") + "\n"
                + String.format(Locale.ROOT, "%.2f", climateInfo.getInteger("pressure") / 100.0) + "\n\n"
                + LibVulpes.proxy.getLocalizedString("data.temperature.name") + "\n" + temperature + " K\n("
                + (temperature - 273) + " \u00b0C)\n\n"
                + LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.tab.climate") + "\n"
                + LibVulpes.proxy.getLocalizedString("msg.terraformingterminal.climate." + climate.name().toLowerCase(Locale.ROOT))
                + "\n" + condition;
    }

    @Override
    public void onModuleUpdated(ModuleBase module) {
        if (world.isRemote && module instanceof ModuleTab)
            sendTabRequest(((ModuleTab) module).getTab());
    }

    @SideOnly(Side.CLIENT)
    private void sendTabRequest(int tab) {
        requestedTab = tab;
        requestedWindowId = Minecraft.getMinecraft().player.openContainer.windowId;
        PacketHandler.sendToServer(new PacketMachine(this, TAB_SWITCH));
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        PacketHandler.sendToServer(new PacketMachine(this, (byte) (buttonId)));
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        //nbt.setBoolean("was_enabled_last_tick", was_enabled_last_tick);
        //nbt.setInteger("sat_power_per_tick", sat_power_per_tick);
        //nbt.setFloat("randomblocks_per_tick", randomblocks_per_tick);
        return nbt;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing side) {
        return false;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public int getEnergyStored(EnumFacing side) {
        return 0;
    }

    @Override
    public int getMaxEnergyStored(EnumFacing side) {
        return 0;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        // Hide Forge Energy capability
        if (capability == CapabilityEnergy.ENERGY) return false;
        // Hide any Tesla capability the base class would expose
        if (TeslaHandler.hasTeslaCapability(this, capability)) return false;
        return super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        // Don’t provide energy handlers to probes/pipes
        if (capability == CapabilityEnergy.ENERGY) return null;
        if (TeslaHandler.hasTeslaCapability(this, capability)) return null;
        return super.getCapability(capability, facing);
    }


    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        //was_enabled_last_tick = nbt.getBoolean("was_enabled_last_tick");
        //sat_power_per_tick = nbt.getInteger("sat_power_per_tick");
        //randomblocks_per_tick = nbt.getFloat("randomblocks_per_tick");
    }

    //has been replaced with custom network package
    /*
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound nbt = pkt.getNbtCompound();
        readFromNBT(nbt);
    }
    */


    @Override
    public boolean canInteractWithContainer(EntityPlayer entity) {
        return true;
    }
}
