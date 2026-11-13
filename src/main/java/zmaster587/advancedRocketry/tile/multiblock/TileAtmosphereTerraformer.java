package zmaster587.advancedRocketry.tile.multiblock;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import zmaster587.libVulpes.inventory.GuiModular;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.multiblock.TileMultiPowerConsumer;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.IconResource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

//This code is a complete mess. it should be rewritten just like the space laser, but it kinda works, so I'll leave it with this for now

public class TileAtmosphereTerraformer extends TileMultiPowerConsumer implements INetworkMachine, IGuiCallback {

    private static final byte SET_TARGET_PRESSURE = 2;

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
                    {null, null, null, null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null, null, null, null},
                    {null, null, null, null, null, "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", null, null, null, null, null},
                    {null, null, null, null, "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", null, null, null, null},
                    {null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null},
                    {Blocks.CLAY, null, null, "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", null, null, Blocks.CLAY},
                    {null, null, null, "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", null, null, null},
                    {Blocks.CLAY, null, null, "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", null, null, Blocks.CLAY},
                    {null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null},
                    {null, null, null, null, "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", null, null, null, null},
                    {null, null, null, null, null, "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", null, null, null, null, null},
                    {null, null, null, null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null, null, null, null},
                    {null, null, null, null, null, "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", null, null, null, null, null},
                    {null, null, null, null, "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", null, null, null, null},
                    {null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", Blocks.CLAY, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", null, null, null},
                    {Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", Blocks.CLAY, Blocks.CLAY, Blocks.CLAY},
                    {null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null},
                    {null, null, null, null, "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", null, null, null, null},
                    {null, null, null, null, null, "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", null, null, null, null, null},
                    {null, null, null, null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null}},

            {{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, "concrete", Blocks.CLAY, 'L', Blocks.CLAY, "concrete", null, null, null, null, null, null},
                    {null, null, null, null, null, "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", null, null, null, null, null},
                    {null, null, null, null, "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", null, null, null, null},
                    {null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null},
                    {null, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, null},
                    {null, null, null, 'L', AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, 'L', null, null, null},
                    {null, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, Blocks.CLAY, Blocks.CLAY, Blocks.CLAY, null},
                    {null, null, null, "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", "concrete", null, null, null},
                    {null, null, null, null, "concrete", "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", "concrete", null, null, null, null},
                    {null, null, null, null, null, "concrete", "concrete", AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, AdvancedRocketryBlocks.blockFuelTank, "concrete", "concrete", null, null, null, null, null},
                    {null, null, null, null, null, null, "concrete", Blocks.CLAY, 'L', Blocks.CLAY, "concrete", null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, Blocks.CLAY, null, Blocks.CLAY, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}}};
    private ModuleTextBox targetPressureBox;
    private ModuleText text;

    private int targetPressure = 100;
    private int pressureDirection;
    private boolean targetPressureEdited;
    private boolean outOfFluid;
    private boolean workedThisTick;
    int requiredN2 = 0, requiredO2 = 0;

    public TileAtmosphereTerraformer() {
        super();

        completionTime = (int) (18000 * ARConfiguration.getCurrentConfig().terraformSpeed);
        text = new ModuleText(10, 100, "", 0x282828);
        powerPerTick = 1000;
    }

    @Override
    public boolean attemptCompleteStructure(IBlockState state) {
        boolean wasComplete = isComplete();
        // Assemble normally, without TileMultiPowerConsumer's automatic power-on.
        canRender = completeStructure = completeStructure(state);
        if (!world.isRemote && !wasComplete && completeStructure)
            setMachineEnabled(false);
        return completeStructure;
    }

    @Override
    public void update() {
        workedThisTick = false;
        if (!world.isRemote && getMachineEnabled() && hasReachedTarget(
                DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension())))
            setMachineEnabled(false);
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
            final List<String> targetPressureTooltip = Collections.singletonList(
                    LibVulpes.proxy.getLocalizedString("msg.terraformer.targetpressure.tooltip"));
            targetPressureBox = new ModuleTextBox(this, 40, 20, 80, 16, 16) {
                @Override
                @SideOnly(Side.CLIENT)
                public void onMouseClicked(GuiModular gui, int x, int y, int button) {
                    super.onMouseClicked(gui, x, y, button);
                    if (targetPressureEdited && (x <= offsetX || x >= offsetX + 80 || y <= offsetY || y >= offsetY + 16)) {
                        setText(BigDecimal.valueOf(targetPressure, 2).toPlainString());
                        targetPressureEdited = false;
                    }
                }

                @Override
                @SideOnly(Side.CLIENT)
                public void renderToolTip(int guiOffsetX, int guiOffsetY, int mouseX, int mouseY, float zLevel,
                                          GuiContainer gui, FontRenderer font) {
                    if (mouseX >= offsetX && mouseX < offsetX + 80 && mouseY >= offsetY && mouseY < offsetY + 16)
                        drawTooltip(gui, targetPressureTooltip, mouseX, mouseY, zLevel, font);
                }
            };
            targetPressureBox.setText(BigDecimal.valueOf(targetPressure, 2).toPlainString());
            targetPressureEdited = false;
            modules.add(targetPressureBox);
        }

        modules.add(new ModuleButton(40, 38, 1, LibVulpes.proxy.getLocalizedString("msg.terraformer.set"), this, TextureResources.buttonScan, 80, 16));
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
                if (pressureDirection > 0) {
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

        if (ARConfiguration.getCurrentConfig().terraformRequiresFluid && pressureDirection > 0) {

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
            if (!hasReachedTarget(properties))
                properties.setAtmosphereDensity(properties.getAtmosphereDensity() + pressureDirection);
            if (hasReachedTarget(properties))
                setMachineEnabled(false);
        }
    }

    private boolean hasReachedTarget(DimensionProperties properties) {
        return pressureDirection == 0 || (pressureDirection > 0
                ? properties.getAtmosphereDensity() >= targetPressure
                : properties.getAtmosphereDensity() <= targetPressure);
    }

    @Override
    public void readDataFromNetwork(ByteBuf in, byte packetId,
                                    NBTTagCompound nbt) {
        super.readDataFromNetwork(in, packetId, nbt);

        if (packetId == SET_TARGET_PRESSURE)
            nbt.setInteger("targetPressure", in.readInt());
    }

    @Override
    public void writeDataToNetwork(ByteBuf out, byte id) {
        super.writeDataToNetwork(out, id);

        if (id == SET_TARGET_PRESSURE)
            out.writeInt(targetPressure);
    }

    @Override
    public void setMachineEnabled(boolean enabled) {
        if (enabled) {
            completionTime = getCompletionTime();
            if (!world.isRemote) {
                pressureDirection = Integer.compare(targetPressure, DimensionManager.getInstance()
                        .getDimensionProperties(world.provider.getDimension()).getAtmosphereDensity());
                enabled = pressureDirection != 0;
            }
        }
        currentTime = 0;
        requiredN2 = requiredO2 = 0;
        outOfFluid = false;
        toggleSwitch.setToggleState(enabled);
        super.setMachineEnabled(enabled);
    }

    @Override
    public void useNetworkData(EntityPlayer player, Side side, byte id,
                               NBTTagCompound nbt) {
        if (id == SET_TARGET_PRESSURE && side == Side.SERVER) {
            int newTarget = MathHelper.clamp(nbt.getInteger("targetPressure"), 0, 1600);
            if (newTarget == targetPressure)
                return;

            targetPressure = newTarget;
            if (getMachineEnabled()) {
                int direction = Integer.compare(targetPressure, DimensionManager.getInstance()
                        .getDimensionProperties(world.provider.getDimension()).getAtmosphereDensity());
                // Keep the current cycle and gas payments unless the direction changes.
                if (direction != pressureDirection) {
                    setMachineEnabled(direction != 0);
                    return;
                }
            }
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        } else
            super.useNetworkData(player, side, id, nbt);
    }

    @Override
    public void onInventoryButtonPressed(int buttonId) {
        super.onInventoryButtonPressed(buttonId);
        if (buttonId == 1) {
            try {
                targetPressure = parseTargetPressure(targetPressureBox.getText());
            } catch (NumberFormatException e) {
                return;
            }
            targetPressureBox.setText(BigDecimal.valueOf(targetPressure, 2).toPlainString());
            targetPressureEdited = false;
            PacketHandler.sendToServer(new PacketMachine(this, SET_TARGET_PRESSURE));
        }
    }

    static int parseTargetPressure(String input) {
        String value = input.trim();
        if (!value.matches("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))
            throw new NumberFormatException(value);
        return new BigDecimal(value).max(BigDecimal.ZERO).min(BigDecimal.valueOf(16))
                .movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    @Override
    public void onModuleUpdated(ModuleBase module) {
        if (module == targetPressureBox)
            targetPressureEdited = true;
    }

    @Override
    protected void writeNetworkData(NBTTagCompound nbt) {
        super.writeNetworkData(nbt);
        nbt.setInteger("targetPressure", targetPressure);
        nbt.setInteger("pressureDirection", pressureDirection);
        nbt.setBoolean("oofluid", outOfFluid);
        //System.out.println("write oof:"+outOfFluid);
    }

    @Override
    protected void readNetworkData(NBTTagCompound nbt) {
        super.readNetworkData(nbt);
        targetPressure = nbt.hasKey("targetPressure")
                ? MathHelper.clamp(nbt.getInteger("targetPressure"), 0, 1600) : 100;
        pressureDirection = nbt.getInteger("pressureDirection");
        if (!nbt.hasKey("targetPressure")) {
            enabled = false;
            currentTime = 0;
        }
        toggleSwitch.setToggleState(enabled);
        outOfFluid = nbt.getBoolean("oofluid");
        //System.out.println("oof:"+outOfFluid);

        if (world != null && world.isRemote) {
            if (targetPressureBox != null && !targetPressureEdited)
                targetPressureBox.setText(BigDecimal.valueOf(targetPressure, 2).toPlainString());
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
