package zmaster587.advancedRocketry.tile.multiblock;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.block.BlockAtmosphereTerraformer;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.block.BlockTile;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.gui.CommonResources;
import zmaster587.libVulpes.inventory.TextureResources;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.multiblock.TileMultiPowerConsumer;
import zmaster587.libVulpes.tile.multiblock.TileMultiblockMachine;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.IconResource;

import java.util.LinkedList;
import java.util.List;

//This code is a complete mess. it should be rewritten just like the space laser, but it kinda works, so I'll leave it with this for now

public class TileAtmosphereTerraformer extends TileMultiPowerConsumer implements INetworkMachine {

    private static final Object[][][] structure = new Object[][][]{
            {{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, AdvancedRocketryBlocks.blockOxygenVent, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockOxygenVent, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, AdvancedRocketryBlocks.blockOxygenVent, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockOxygenVent, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, AdvancedRocketryBlocks.blockOxygenVent, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockOxygenVent, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, AdvancedRocketryBlocks.blockOxygenVent, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, AdvancedRocketryBlocks.blockOxygenVent, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, Blocks.CLAY, LibVulpesBlocks.blockAdvStructureBlock, Blocks.CLAY, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, Blocks.CLAY, LibVulpesBlocks.blockAdvStructureBlock, Blocks.CLAY, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null},
                    {null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null},
                    {null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null},
                    {null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {Blocks.CLAY, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, Blocks.CLAY},
                    {null, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, null},
                    {Blocks.CLAY, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, Blocks.CLAY},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {Blocks.CLAY, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, 'c', LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, Blocks.CLAY},
                    {null, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, null},
                    {Blocks.CLAY, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, Blocks.CLAY},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {Blocks.CLAY, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, Blocks.CLAY},
                    {null, null, null, null, null, null, null, 'P', LibVulpesBlocks.blockAdvStructureBlock, 'P', null, null, null, null, null, null, null},
                    {Blocks.CLAY, null, null, null, null, null, null, LibVulpesBlocks.blockAdvStructureBlock, 'P', LibVulpesBlocks.blockAdvStructureBlock, null, null, null, null, null, null, Blocks.CLAY},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {Blocks.CLAY, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, null, null, Blocks.CLAY},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {Blocks.CLAY, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, null, null, Blocks.CLAY},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null},
                    {null, null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null},
                    {null, null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, Blocks.CLAY, 'L', Blocks.CLAY, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {null, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, null},
                    {null, null, null, 'L', AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, 'L', null, null, null},
                    {null, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, null},
                    {null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null},
                    {null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null},
                    {null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockConcrete, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null},
                    {null, null, null, null, null, null, AdvancedRocketryBlocks.blockConcrete, Blocks.CLAY, 'L', Blocks.CLAY, AdvancedRocketryBlocks.blockConcrete, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}}};
    private ModuleToggleSwitch buttonIncrease, buttonDecrease;
    private ModuleRadioButton radioButton;
    private ModuleText text;

    private boolean outOfFluid;
    private boolean workedThisTick;
    private int last_mode;
    int requiredN2 = 0, requiredO2 = 0;

    boolean client_contructed = false;

    public TileAtmosphereTerraformer() {
        super();

        completionTime = (int) (18000 * ARConfiguration.getCurrentConfig().terraformSpeed);
        buttonIncrease = new ModuleToggleSwitch(40, 20, 1, LibVulpes.proxy.getLocalizedString("msg.terraformer.atminc"), this, TextureResources.buttonScan, 80, 16, true);
        buttonDecrease = new ModuleToggleSwitch(40, 38, 2, LibVulpes.proxy.getLocalizedString("msg.terraformer.atmdec"), this, TextureResources.buttonScan, 80, 16, false);
        text = new ModuleText(10, 100, "", 0x282828);
        powerPerTick = 1000;

        List<ModuleToggleSwitch> buttons = new LinkedList<>();
        buttons.add(buttonIncrease);
        buttons.add(buttonDecrease);
        radioButton = new ModuleRadioButton(this, buttons);

        outOfFluid = false;
        last_mode = radioButton.getOptionSelected();

    }

    @Override
    public void update() {
        workedThisTick = false;
        super.update();
        if (world.isRemote) {
            spawnGasParticles();
        } else {
            // Publish once after processing so cycle completion cannot flicker the active state.
            setMachineRunning(workedThisTick && getMachineEnabled());
        }
    }

    public boolean isWorking() {
        return world != null && isWorking(world.getBlockState(pos));
    }

    private static boolean isWorking(IBlockState state) {
        return state.getBlock() instanceof BlockAtmosphereTerraformer && state.getValue(BlockTile.STATE);
    }

    private int getCompletionTime() {
        return (int) (18000 * ARConfiguration.getCurrentConfig().terraformSpeed);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 320 * 320;
    }

    @Override
    public List<ModuleBase> getModules(int ID, EntityPlayer player) {
        List<ModuleBase> modules =  super.getModules(ID, player);

        //Backgrounds
        if (world.isRemote) {
            modules.add(new ModuleImage(173, 0, new IconResource(90, 0, 84, 88, CommonResources.genericBackground)));
        }

        modules.add(0, radioButton);
        modules.add(new ModuleProgress(30, 57, 0, zmaster587.advancedRocketry.inventory.TextureResources.terraformProgressBar, this));
        modules.add(text);


        setText();

        int i = 0;
        modules.add(new ModuleText(180, 10, "Gas Status", 0x282828));
        for (IFluidHandler tile : fluidInPorts) {
            modules.add(new ModuleLiquidIndicator(180 + i * 16, 30, tile));
            i++;
        }

        return modules;
    }

    private void setText() {

        String statusText;


        if (outOfFluid)
            statusText = LibVulpes.proxy.getLocalizedString("msg.terraformer.outofgas");

        else if (isWorking())
            statusText = LibVulpes.proxy.getLocalizedString("msg.terraformer.running");
        else
            statusText = LibVulpes.proxy.getLocalizedString("msg.terraformer.notrunning");

        text.setText(String.format("%s:\n%s\n\n%s: %.2f", LibVulpes.proxy.getLocalizedString("msg.terraformer.status"), statusText, LibVulpes.proxy.getLocalizedString("msg.terraformer.pressure"), DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getAtmosphereDensity() / 100f));

    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-15, -15, -15), pos.add(15, 15, 15));
    }

    @SideOnly(Side.CLIENT)
    private void spawnGasParticles() {
        if (Minecraft.getMinecraft().gameSettings.particleSetting < 2) {
            IBlockState state = world.getBlockState(pos);
            if (!isWorking(state))
                return;
            EnumFacing dir = RotatableBlock.getFront(state).getOpposite();
            BlockPos offsetPos = pos.offset(dir);

            for (int i = 0; i < 3; i++) {
                if (radioButton.getOptionSelected() == 0) {
                    float xMot = (float) ((world.rand.nextGaussian()) / 40f);
                    float zMot = (float) ((world.rand.nextGaussian()) / 40f);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmoke", world, offsetPos.getX() + 5, pos.getY() + 7, offsetPos.getZ() + 0.5, xMot, 0.02f, zMot);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmoke", world, offsetPos.getX() - 4, pos.getY() + 7, offsetPos.getZ() + 0.5, xMot, 0.02f, zMot);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmoke", world, offsetPos.getX() + 0.5f, pos.getY() + 7, offsetPos.getZ() - 4, xMot, 0.02f, zMot);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmoke", world, offsetPos.getX() + 0.5f, pos.getY() + 7, offsetPos.getZ() + 5, xMot, 0.02f, zMot);

                } else {
                    float xMot = (float) ((world.rand.nextGaussian()) / 4f);
                    float yMot = (float) (world.rand.nextGaussian() / 20f);
                    float zMot = (float) ((world.rand.nextGaussian()) / 4f);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmokeInverse", world, offsetPos.getX() + 5, pos.getY() + 7, offsetPos.getZ() + 0.5, xMot, 0.4f + yMot, zMot);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmokeInverse", world, offsetPos.getX() - 4, pos.getY() + 7, offsetPos.getZ() + 0.5, xMot, 0.4f + yMot, zMot);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmokeInverse", world, offsetPos.getX() + 0.5f, pos.getY() + 7, offsetPos.getZ() - 4, xMot, 0.4f + yMot, zMot);
                    AdvancedRocketry.proxy.spawnParticle("rocketSmokeInverse", world, offsetPos.getX() + 0.5f, pos.getY() + 7, offsetPos.getZ() + 5, xMot, 0.4f + yMot, zMot);
                }
            }
        }
    }

    @Override
    protected void onRunningPoweredTick() {
        if (world.isRemote || !isComplete())
            return;

        int mode = radioButton.getOptionSelected();
        if (last_mode != mode) {
            last_mode = mode;
            this.setProgress(0, 0);
        }
        if (ARConfiguration.getCurrentConfig().terraformRequiresFluid && mode == 0) {

            if (requiredN2 == 0 && requiredO2 == 0) {
                requiredN2 = ARConfiguration.getCurrentConfig().terraformliquidRate;
                requiredO2 = ARConfiguration.getCurrentConfig().terraformliquidRate;
            }

            FluidStack nitrogenRequest = null;
            FluidStack oxygenRequest = null;
            for (IFluidHandler handler : fluidInPorts) {
                if (requiredN2 == 0 && requiredO2 == 0)
                    break;

                if (requiredN2 > 0) {
                    if (nitrogenRequest == null)
                        nitrogenRequest = new FluidStack(AdvancedRocketryFluids.fluidNitrogen, requiredN2);
                    else
                        nitrogenRequest.amount = requiredN2;
                    FluidStack drained = handler.drain(nitrogenRequest, true);
                    if (drained != null)
                        requiredN2 -= drained.amount;
                }

                if (requiredO2 > 0) {
                    if (oxygenRequest == null)
                        oxygenRequest = new FluidStack(AdvancedRocketryFluids.fluidOxygen, requiredO2);
                    else
                        oxygenRequest.amount = requiredO2;
                    FluidStack drained = handler.drain(oxygenRequest, true);
                    if (drained != null)
                        requiredO2 -= drained.amount;
                }
            }
            setOOF(requiredN2 != 0 || requiredO2 != 0);
        } else {
            setOOF(false);
        }

        if (!outOfFluid) {
            /////////from the super method
            useEnergy(powerPerTick);
            workedThisTick = true;
            currentTime++;

            if (currentTime == completionTime)
                processComplete();
            /////////from the super method
        }
    }

    public SoundEvent getSound() {
        return AudioRegistry.machineLarge;
    }

    @Override
    public int getSoundDuration() {
        return 80;
    }

    @Override
    protected void playMachineSound(SoundEvent event) {
        world.playSound(getPos().getX(), getPos().getY() + 7, getPos().getZ(), event, SoundCategory.BLOCKS, Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.BLOCKS), 0.975f + world.rand.nextFloat() * 0.05f, false);
    }

    @Override
    public boolean isRunning() {

        // The server must keep retrying stalled work; clients only display its published result.
        if (world != null && world.isRemote)
            return isWorking();

        boolean bool = getMachineEnabled() &&
                //super.isRunning() &&
                zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig().enableTerraforming;

        if (!bool)
            currentTime = 0;

        return bool;
    }

    public void setOOF(boolean x) {
        if (!x && outOfFluid) {
            outOfFluid = false;
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 3);

            //System.out.println("s oof false");
        } else if (x && !outOfFluid) {
            outOfFluid = true;
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 3);

            //System.out.println("s oof true");
        }
    }

    @Override
    public void setMachineRunning(boolean running) {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockAtmosphereTerraformer && state.getValue(BlockTile.STATE) != running)
                super.setMachineRunning(running);
        }
    }

    @Override
    protected void processComplete() {
        super.processComplete();
        completionTime = getCompletionTime();

        DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension());
        if (!world.isRemote && properties != null && properties.getId() == world.provider.getDimension() && ((world.provider.getClass().equals(WorldProviderPlanet.class) &&
                properties.isNativeDimension) || ARConfiguration.getCurrentConfig().allowTerraformNonAR)) {
            if (buttonIncrease.getState() && properties.getAtmosphereDensity() < 1600) {
                properties.setAtmosphereDensity(properties.getAtmosphereDensity() + 1);
                if (buttonIncrease.getState() && properties.getAtmosphereDensity() >= 1600) {
                    this.setMachineEnabled(false);
                    this.setMachineRunning(false);
                    markDirty();
                    this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 3);

                }
            }
            if (buttonDecrease.getState() && properties.getAtmosphereDensity() > 0) {
                properties.setAtmosphereDensity(properties.getAtmosphereDensity() - 1);
                if (buttonDecrease.getState() && properties.getAtmosphereDensity() <= 0) {
                    this.setMachineEnabled(false);
                    this.setMachineRunning(false);
                    markDirty();
                    this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 3);

                }
            }
        }
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte packetId,
                                    NBTTagCompound nbt) {
        super.readDataFromNetwork(in, packetId, nbt);

        if (packetId == (byte) TileMultiblockMachine.NetworkPackets.TOGGLE.ordinal()) {
            radioButton.setOptionSelected(in.readByte());
        }
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte id) {
        super.writeDataToNetwork(out, id);

        if (id == (byte) TileMultiblockMachine.NetworkPackets.TOGGLE.ordinal()) {
            out.writeByte(radioButton.getOptionSelected());
        }
    }

    @Override
    public void setMachineEnabled(boolean enabled) {
        super.setMachineEnabled(enabled);

        if (getMachineEnabled())
            completionTime = getCompletionTime();
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id,
                               NBTTagCompound nbt) {
        super.useNetworkData(player, side, id, nbt);
        if(!world.isRemote) {
            setOOF(false);
            markDirty();
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos), this.world.getBlockState(this.pos), 3);

        }
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        //if (hasValidBiomeChanger()) {
        super.onInventoryButtonPressed(buttonId);
        if (buttonId == 1 || buttonId == 2) {
            PacketHandler.sendToServer(new PacketMachine(this, (byte) TileMultiblockMachine.NetworkPackets.TOGGLE.ordinal()));
        }
        //}
    }

    @Override
    protected void writeNetworkData(NBTTagCompound nbt) {
        super.writeNetworkData(nbt);
        nbt.setInteger("selected", radioButton.getOptionSelected());
        nbt.setBoolean("oofluid", outOfFluid);
        //System.out.println("write oof:"+outOfFluid);
    }

    @Override
    protected void readNetworkData(NBTTagCompound nbt) {
        super.readNetworkData(nbt);
        radioButton.setOptionSelected(nbt.getInteger("selected"));
        outOfFluid = nbt.getBoolean("oofluid");
        //System.out.println("oof:"+outOfFluid);

        if (world !=null && world.isRemote){
            //if (!client_contructed)
                //client_contructed = this.completeStructure(this.world.getBlockState(this.pos));

            setText();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        outOfFluid = false;
    }

    @Override
    public String getMachineName() {
        return AdvancedRocketryBlocks.blockAtmosphereTerraformer.getLocalizedName();
    }
}
