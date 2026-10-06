package zmaster587.advancedRocketry.event;

import net.minecraft.block.BlockTorch;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.SleepResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.advancements.ARAdvancements;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.IPlanetaryProvider;
import zmaster587.advancedRocketry.api.RocketEvent;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.client.render.planet.RenderPlanetarySky;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.dimension.watersourcelocked;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.advancedRocketry.item.ItemRocketFinder;
import zmaster587.advancedRocketry.network.PacketConfigSync;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStellarInfo;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.tile.TileRocketAssemblingMachine;
import zmaster587.advancedRocketry.tile.infrastructure.TileRocketMonitoringStation;
import zmaster587.advancedRocketry.tile.station.TileLandingPad;
import zmaster587.advancedRocketry.util.WeakIdentityRegistry;
import zmaster587.advancedRocketry.util.SpawnListEntryNBT;
import zmaster587.advancedRocketry.util.TransitionEntity;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;
import zmaster587.advancedRocketry.world.util.BasicTeleporter;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IModularArmor;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class PlanetEventHandler {

    // Forge owns only this shared handler. Neither registry owns a tile or its world.
    private static final WeakIdentityRegistry<TileEntity> serverRocketListeners = new WeakIdentityRegistry<>();
    private static final WeakIdentityRegistry<TileEntity> clientRocketListeners = new WeakIdentityRegistry<>();

    public static void registerRocketListener(TileEntity tile) {
        if (tile.getWorld() == null) return;
        (tile.getWorld().isRemote ? clientRocketListeners : serverRocketListeners).add(tile);
    }

    public static void unregisterRocketListener(TileEntity tile) {
        serverRocketListeners.remove(tile);
        clientRocketListeners.remove(tile);
    }

    private static boolean isLoadedRocketListener(TileEntity tile) {
        World world = tile.getWorld();
        if (tile.isInvalid() || world == null) return false;
        // Never load a chunk or create a tile while delivering an event.
        Chunk chunk = world.getChunkProvider().getLoadedChunk(tile.getPos().getX() >> 4, tile.getPos().getZ() >> 4);
        return chunk != null && chunk.isLoaded()
                && chunk.getTileEntity(tile.getPos(), Chunk.EnumCreateEntityType.CHECK) == tile;
    }

    private static void dispatchRocketEvent(RocketEvent event, Consumer<TileEntity> callback) {
        if (event.world == null) return;
        WeakIdentityRegistry<TileEntity> listeners = event.world.isRemote ? clientRocketListeners : serverRocketListeners;
        for (WeakReference<TileEntity> entry : listeners.snapshot()) {
            // Match Forge's default receiveCanceled=false for every individual callback.
            if (event.isCancelable() && event.isCanceled()) break;
            TileEntity tile = entry.get();
            if (tile != null && isLoadedRocketListener(tile)) callback.accept(tile);
        }
    }

    @SubscribeEvent
    public void onRocketLanded(RocketEvent.RocketLandedEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileRocketAssemblingMachine) ((TileRocketAssemblingMachine) tile).onRocketLand(event);
            else if (tile instanceof TileLandingPad) ((TileLandingPad) tile).onRocketLand(event);
            else if (tile instanceof TileRocketMonitoringStation) ((TileRocketMonitoringStation) tile).onLanded(event);
        });
    }

    @SubscribeEvent
    public void onRocketPreLaunch(RocketEvent.RocketPreLaunchEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileLandingPad) ((TileLandingPad) tile).onRocketLaunch(event);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRocketPreLaunchMonitor(RocketEvent.RocketPreLaunchEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileRocketMonitoringStation) ((TileRocketMonitoringStation) tile).onPreLaunch(event);
        });
    }

    @SubscribeEvent
    public void onRocketDismantle(RocketEvent.RocketDismantleEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileLandingPad) ((TileLandingPad) tile).onRocketDismantle(event);
        });
    }

    @SubscribeEvent
    public void onRocketLaunch(RocketEvent.RocketLaunchEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileRocketMonitoringStation) ((TileRocketMonitoringStation) tile).onLaunch(event);
        });
    }

    @SubscribeEvent
    public void onRocketOrbit(RocketEvent.RocketReachesOrbitEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileRocketMonitoringStation) ((TileRocketMonitoringStation) tile).onOrbit(event);
        });
    }

    @SubscribeEvent
    public void onRocketDeorbit(RocketEvent.RocketDeOrbitingEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileRocketMonitoringStation) ((TileRocketMonitoringStation) tile).onDeorbit(event);
        });
    }

    @SubscribeEvent
    public void onRocketAbort(RocketEvent.RocketAbortEvent event) {
        dispatchRocketEvent(event, tile -> {
            if (tile instanceof TileRocketMonitoringStation) ((TileRocketMonitoringStation) tile).onAbort(event);
        });
    }

    private static final ItemStack component = new ItemStack(AdvancedRocketryItems.itemUpgrade, 1, 4);
    public static long time = 0;
    private static long endTime, duration;
    private static List<TransitionEntity> transitionMap = new LinkedList<>();

    public static void addDelayedTransition(TransitionEntity entity) {
        transitionMap.add(entity);
    }

    /**
     * Starts a burst, used for move to warp effect
     *
     * @param endTime
     * @param duration
     */
    @SideOnly(Side.CLIENT)
    public static void runBurst(long endTime, long duration) {
        PlanetEventHandler.endTime = endTime;
        PlanetEventHandler.duration = duration;
    }

    @SubscribeEvent
    public void onCrafting(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent event) {
        if (!event.crafting.isEmpty()) {
            Item item = event.crafting.getItem();//TODO Advancments for crafting.
            //			if(item == LibVulpesItems.itemHoloProjector)
            //				event.player.addStat(ARAchivements.holographic);
            //			else if(item == Item.getItemFromBlock(AdvancedRocketryBlocks.blockRollingMachine))
            //				event.player.addStat(ARAchivements.rollin);
            //			else if(item == Item.getItemFromBlock(AdvancedRocketryBlocks.blockCrystallizer))
            //				event.player.addStat(ARAchivements.crystalline);
            //			else if(item == Item.getItemFromBlock(AdvancedRocketryBlocks.blockLathe))
            //				event.player.addStat(ARAchivements.spinDoctor);
            //			else if(item ==Item.getItemFromBlock(AdvancedRocketryBlocks.blockElectrolyser))
            //				event.player.addStat(ARAchivements.electrifying);
            //			else if(item == Item.getItemFromBlock(AdvancedRocketryBlocks.blockArcFurnace))
            //				event.player.addStat(ARAchivements.feelTheHeat);
            //			else if(item == Item.getItemFromBlock(AdvancedRocketryBlocks.blockWarpCore))
            //				event.player.addStat(ARAchivements.warp);
            //			else if(item == Item.getItemFromBlock(AdvancedRocketryBlocks.blockPlatePress))
            //				event.player.addStat(ARAchivements.blockPresser);
        }
    }

    @SubscribeEvent
    public void CheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        World world = event.getWorld();
        DimensionManager manager = DimensionManager.getInstance();

        if (manager.isInitialized()) {
            DimensionProperties properties = manager.getDimensionProperties(world.provider.getDimension());
            if (properties != null) {
                if (!properties.getAtmosphere().isImmune(event.getEntityLiving().getClass()))
                    event.setResult(Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public void SpawnEntity(WorldEvent.PotentialSpawns event) {
        World world = event.getWorld();

        DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension());
        if (properties != null) {
            List<SpawnListEntryNBT> entries = properties.getSpawnListEntries();
            if (!entries.isEmpty() && event.getType() != EnumCreatureType.MONSTER)
                event.getList().addAll(entries);
        }
    }

    @SubscribeEvent
    public void onWorldGen(OreGenEvent.GenerateMinable event) {

        if (event.getWorld().provider instanceof WorldProviderPlanet &&
                DimensionManager.getInstance().getDimensionProperties(event.getWorld().provider.getDimension()).getOreGenProperties(event.getWorld()) != null) {

            switch (event.getType()) {
                case COAL:
                case DIAMOND:
                case EMERALD:
                case GOLD:
                case IRON:
                case LAPIS:
                case QUARTZ:
                case REDSTONE:
                case CUSTOM:
                    event.setResult(Result.DENY);
                    break;
                default:
                    event.setResult(Result.DEFAULT);
            }
        }
    }

    //Handle gravity
    @SubscribeEvent
    public void playerTick(LivingUpdateEvent event) {
        EntityLivingBase livingEntity = event.getEntityLiving();
        // Only use suit oxygen when the entity needs underwater breathing protection.
        if (!livingEntity.world.isRemote && livingEntity.isInWater() && livingEntity.isEntityAlive()
                && !(livingEntity instanceof EntityPlayer && ((EntityPlayer) livingEntity).capabilities.disableDamage)
                && !livingEntity.canBreatheUnderwater() && !livingEntity.isPotionActive(MobEffects.WATER_BREATHING)
                && livingEntity.isInsideOfMaterial(Material.WATER)
                && AtmosphereType.LOWOXYGEN.isImmune(livingEntity)) {
            livingEntity.setAir(300);
        }

        if (!event.getEntity().world.isRemote && event.getEntity().world.getTotalWorldTime() % 20 == 0 && event.getEntity() instanceof EntityPlayer) {
            if (DimensionManager.getInstance().getDimensionProperties(event.getEntity().world.provider.getDimension()).getName().equals("Luna") &&
                    event.getEntity().getPosition().distanceSq(2347, 80, 67) < 512) {
                ARAdvancements.WENT_TO_THE_MOON.trigger((EntityPlayerMP) event.getEntity());
            }
        }

        if (event.getEntity() instanceof EntityPlayer && event.getEntity().world.provider.getDimension() == ARConfiguration.getCurrentConfig().spaceDimId && SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(event.getEntity().getPosition()) == null && !(event.getEntity().getRidingEntity() instanceof EntityRocket)) {
            double distance = 0;
            HashedBlockPosition teleportPosition = null;
            for (ISpaceObject spaceObject : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
                if (spaceObject instanceof SpaceStationObject) {
                    SpaceStationObject station = ((SpaceStationObject) spaceObject);
                    double distanceTo = event.getEntity().getPosition().getDistance(station.getSpawnLocation().x, station.getSpawnLocation().y, station.getSpawnLocation().z);
                    if (distanceTo > distance) {
                        distance = distanceTo;
                        teleportPosition = station.getSpawnLocation();
                    }
                }
            }
            if (teleportPosition != null) {
                event.getEntity().sendMessage(new TextComponentString(LibVulpes.proxy.getLocalizedString("msg.chat.nostation1")));
                event.getEntity().sendMessage(new TextComponentString(LibVulpes.proxy.getLocalizedString("msg.chat.nostation2")));
                event.getEntity().setPositionAndUpdate(teleportPosition.x, teleportPosition.y, teleportPosition.z);
            } else {
                event.getEntity().sendMessage(new TextComponentString(LibVulpes.proxy.getLocalizedString("msg.chat.nostation3")));
                event.getEntity().changeDimension(0, new BasicTeleporter(event.getEntity().getPosition()));
            }

        }
    }

    @SubscribeEvent
    public void sleepEvent(@Nonnull PlayerSleepInBedEvent event) {

        if (event.getEntity().world.provider instanceof WorldProviderPlanet) {
            WorldProvider provider = event.getEntity().world.provider;
            AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(provider.getDimension());

            if (!ARConfiguration.getCurrentConfig().forcePlayerRespawnInSpace && AtmosphereHandler.hasAtmosphereHandler(provider.getDimension()) && atmhandler != null &&
                    !atmhandler.getAtmosphereType(event.getPos()).isBreathable()) {
                event.setResult(SleepResult.OTHER_PROBLEM);
            }
        }
    }

    @SubscribeEvent
    public void blockPlacedEvent(@Nonnull PlaceEvent event) {
        WorldProvider provider = event.getWorld().provider;
        AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(provider.getDimension());

        if (!event.getWorld().isRemote && AtmosphereHandler.getOxygenHandler(provider.getDimension()) != null && atmhandler != null &&
                !atmhandler.getAtmosphereType(event.getPos()).allowsCombustion()) {

            if (event.getPlacedBlock().getBlock() == Blocks.TORCH) {
                EnumFacing direction = event.getPlacedBlock().getValue(BlockTorch.FACING);
                event.getWorld().setBlockState(event.getPos(), AdvancedRocketryBlocks.blockUnlitTorch.getDefaultState().withProperty(BlockTorch.FACING, direction));
            } else if (zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig().torchBlocks.contains(event.getPlacedBlock().getBlock())) {
                event.setResult(Result.DENY);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void blockRightClicked(@Nonnull RightClickBlock event) {
        EnumFacing direction = event.getFace();
        WorldProvider provider = event.getWorld().provider;
        AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(provider.getDimension());

        if (!event.getWorld().isRemote && direction != null && event.getEntityPlayer() != null && AtmosphereHandler.getOxygenHandler(provider.getDimension()) != null && atmhandler != null &&
                !atmhandler.getAtmosphereType(event.getPos().offset(direction)).allowsCombustion()) {

            if (!event.getEntityPlayer().getHeldItem(event.getHand()).isEmpty()) {
                if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.FLINT_AND_STEEL || event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.FIRE_CHARGE || event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.BLAZE_POWDER || event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.BLAZE_ROD)
                    event.setCanceled(true);
            }
        }

        if (!event.getWorld().isRemote && !event.getItemStack().isEmpty() && event.getItemStack().getItem() == Item.getItemFromBlock(AdvancedRocketryBlocks.blockGenericSeat) && event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.TNT) {
            ARAdvancements.BEER.trigger((EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public void disconnected(ClientDisconnectionFromServerEvent event) {
        ARConfiguration.useClientDiskConfig();
        if (Loader.isModLoaded("jei")) {
            zmaster587.advancedRocketry.integration.jei.ARPlugin.resetDimensionRecipeRefresh();
        }
    }

    //Tick dimensions, needed for satellites, and GUIs
    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        //Tick satellites
        if (event.phase == TickEvent.Phase.END) {
            DimensionManager.getInstance().tickDimensions();
            time++;

            if (!transitionMap.isEmpty()) {
                Iterator<TransitionEntity> itr = transitionMap.iterator();

                while (itr.hasNext()) {
                    TransitionEntity ent = itr.next();
                    if (ent.entity.world.getTotalWorldTime() >= ent.time) {
                        if (ent.entity.world.provider.getDimension() == ent.dimId) {
                            if (!ent.entity.isDead && ent.entity2 != null && !ent.entity2.isDead &&
                                    ent.entity.world == ent.entity2.world) {
                                ent.entity.setPositionAndUpdate(
                                        ent.entity2.posX,
                                        ent.entity2.posY,
                                        ent.entity2.posZ
                                );
                                ent.entity.startRiding(ent.entity2, true);
                                ItemRocketFinder.recordRocketArrival(ent.entity, ent.entity2);
                            }
                            itr.remove();
                            continue;
                        }

                        ent.entity.setLocationAndAngles(
                                ent.location.getX(),
                                ent.location.getY(),
                                ent.location.getZ(),
                                ent.entity.rotationYaw,
                                ent.entity.rotationPitch
                        );
                        WorldServer newWorld = ent.entity.getServer().getWorld(ent.dimId);
                        Entity moved = ent.entity.changeDimension(
                                ent.dimId,
                                new BasicTeleporter(ent.entity.getPosition())
                        );

                        if (moved != null) {
                            moved.getEntityData().setLong(
                                    "arRocketTransferGrace",
                                    newWorld.getTotalWorldTime() + 100L
                            );
                        }

                        Entity rocket = newWorld.getEntityFromUuid(ent.entity2.getPersistentID());
                        if (rocket != null && moved != null) {
                            moved.startRiding(rocket, true);
                            ItemRocketFinder.recordRocketArrival(moved, rocket);
                        }
                        itr.remove();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void tickClient(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            DimensionManager.getInstance().tickDimensionsClient();
    }

    //Make sure the player receives data about the dimensions
    @SubscribeEvent
    public void playerLoggedInEvent(ServerConnectionFromClientEvent event) {

        //Send config first
        if (!event.isLocal())
            PacketHandler.sendToDispatcher(new PacketConfigSync(), event.getManager());

        //Make sure stars are sent next
        for (int i : DimensionManager.getInstance().getStarIds()) {
            PacketHandler.sendToDispatcher(new PacketStellarInfo(i, DimensionManager.getInstance().getStar(i)), event.getManager());
        }

        for (int i : DimensionManager.getInstance().getRegisteredDimensions()) {
            PacketHandler.sendToDispatcher(new PacketDimInfo(i, DimensionManager.getInstance().getDimensionProperties(i)), event.getManager());
        }

        for (ISpaceObject spaceObject : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
            PacketHandler.sendToDispatcher(new PacketSpaceStationInfo(spaceObject.getId(), spaceObject), event.getManager());
        }
        PacketHandler.sendToDispatcher(new PacketDimInfo(0, DimensionManager.getInstance().getDimensionProperties(0)), event.getManager());
    }

    public void connectToServer(ClientConnectedToServerEvent event) {
        zmaster587.advancedRocketry.dimension.DimensionManager.getInstance().unregisterAllDimensions();
    }

    @SubscribeEvent
    public void worldLoadEvent(WorldEvent.Load event) {
        if (!event.getWorld().isRemote)
            AtmosphereHandler.registerWorld(event.getWorld().provider.getDimension());
        else if (ARConfiguration.getCurrentConfig().skyOverride)
            event.getWorld().provider.setSkyRenderer(new RenderPlanetarySky());
    }

    @SubscribeEvent
    public void worldUnloadEvent(WorldEvent.Unload event) {
        (event.getWorld().isRemote ? clientRocketListeners : serverRocketListeners)
                .removeIf(tile -> tile.getWorld() == event.getWorld());
        if (!event.getWorld().isRemote)
            AtmosphereHandler.unregisterWorld(event.getWorld().provider.getDimension());
    }

    //Handle fog density and color
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void fogColor(FogColors event) {

        IBlockState state = ActiveRenderInfo.getBlockStateAtEntityViewpoint(event.getEntity().world, event.getEntity(), (float) event.getRenderPartialTicks());

        if (state.getMaterial() == Material.WATER)
            return;

        DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(event.getEntity().dimension);
        if (properties != null) {
            if (event.getEntity().world.provider instanceof IPlanetaryProvider) {
                Vec3d color = event.getEntity().world.provider.getFogColor(event.getEntity().world.getCelestialAngle((float) event.getRenderPartialTicks()), (float) event.getRenderPartialTicks());
                event.setRed((float) Math.min(color.x, 1f));
                event.setGreen((float) Math.min(color.y, 1f));
                event.setBlue((float) Math.min(color.z, 1f));

                //Make sure fog doesn't happen on zero atmospheres
                if (properties.getAtmosphereDensity() == 0) {
                    event.setRed(0);
                    event.setGreen(0);
                    event.setBlue(0);
                }
            }

            if (endTime > 0) {
                double amt = (endTime - Minecraft.getMinecraft().world.getTotalWorldTime()) / (double) duration;
                if (amt < 0) {
                    endTime = 0;
                } else {
                    event.setRed((float) amt);
                    event.setGreen((float) amt);
                    event.setBlue((float) amt);
                }
            }
        }
    }

    @SubscribeEvent
    public void handleSourcePlacement(BlockEvent.CreateFluidSourceEvent event) {
        List<watersourcelocked> source_lock_list = DimensionManager.getInstance().getDimensionProperties(event.getWorld().provider.getDimension()).water_source_locked_positions;
        HashedBlockPosition hp = new HashedBlockPosition(event.getPos().getX(),event.getPos().getY(),event.getPos().getZ());

        for (watersourcelocked i:source_lock_list){
            if (i.pos.equals(hp))
                event.setResult(Result.DENY);
        }
    }

    @SubscribeEvent
    public void serverTickEvent(TickEvent.WorldTickEvent event) {}

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        DimensionManager.getInstance().getDimensionProperties(event.getWorld().provider.getDimension()).add_chunk_to_terraforming_list(event.getChunk());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void fogColor(RenderFogEvent event) {

        if (event.getFogMode() == -1) {
            return;
        }
        DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(event.getEntity().dimension);
        if (properties != null && event.getState().getBlock() != Blocks.WATER && event.getState().getBlock() != Blocks.LAVA) {//& properties.atmosphereDensity > 125) {
            GlStateManager.setFog(GlStateManager.FogMode.LINEAR);

            float f1 = event.getFarPlaneDistance();
            float near;
            float far;

            int atmosphere = Math.min(properties.getAtmosphereDensity(), 200);
            ItemStack armor = Minecraft.getMinecraft().player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);

            if (!armor.isEmpty() && armor.getItem() instanceof IModularArmor) {
                for (ItemStack i : ((IModularArmor) armor.getItem()).getComponents(armor)) {
                    if (i.isItemEqual(component)) {
                        atmosphere = Math.min(atmosphere, 100);
                        break;
                    }
                }
            }

            //Check environment
            if (AtmosphereHandler.currentPressure != -1) {
                atmosphere = Math.min(AtmosphereHandler.currentPressure, 200);
            }

            if (atmosphere > 100) {
                near = 0.75f * f1 * (2.00f - atmosphere * atmosphere / 10000f);
                far = f1;
            } else {
                near = 0.75f * f1 * (2.00f - atmosphere / 100f);
                far = f1 * (2.002f - atmosphere / 100f);
            }
            GlStateManager.setFogStart(near);
            GlStateManager.setFogEnd(far);
            GlStateManager.setFogDensity(0);
        }
    }

    //Saves NBT data
    @SubscribeEvent
    public void worldSaveEvent(WorldEvent.Save event) {
        //TODO: save only the one dimension
        if (event.getWorld().provider.getDimension() == 0)
            try {
                DimensionManager.getInstance().saveDimensions(DimensionManager.workingPath);
            } catch (Exception e) {
                AdvancedRocketry.logger.fatal("An error has occurred saving planet data, this can happen if another mod causes the game to crash during game load.  If the game has fully loaded, then this is a serious error, Advanced Rocketry data has not been saved.");
                e.printStackTrace();
            }
    }

    //Make sure the player doesn't die on low gravity worlds
    @SubscribeEvent
    public void fallEvent(LivingFallEvent event) {
        if (event.getEntity().world.provider instanceof IPlanetaryProvider) {
            IPlanetaryProvider planet = (IPlanetaryProvider) event.getEntity().world.provider;
            event.setDistance((float) (event.getDistance() * planet.getGravitationalMultiplier(event.getEntity().getPosition())));
        }
    }
}
