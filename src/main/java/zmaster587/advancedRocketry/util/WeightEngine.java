package zmaster587.advancedRocketry.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;
import zmaster587.advancedRocketry.block.BlockBipropellantRocketMotor;
import zmaster587.advancedRocketry.block.BlockFuelTank;
import zmaster587.advancedRocketry.block.BlockPressurizedFluidTank;
import zmaster587.advancedRocketry.block.BlockRocketMotor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum WeightEngine {
    INSTANCE("config/advRocketry/weights.json");

    private static final double DEFAULT_ITEM_WEIGHT = 0.1D;
    private static final double DEFAULT_GENERIC_FLUID_WEIGHT = 0.001D;

    private static final double DEFAULT_ROCKET_TANK_WEIGHT = 0.2D;
    private static final double DEFAULT_ROCKET_MOTOR_WEIGHT = 2.0D;
    private static final double DEFAULT_PRESSURE_TANK_WEIGHT = 5.0D;
    private static final double DEFAULT_GUIDANCE_COMPUTER_WEIGHT = 1.8D;
    private static final double DEFAULT_SATELLITE_HATCH_WEIGHT = 5.0D;

    private static final double DEFAULT_INTAKE_WEIGHT = 5.0D;
    private static final double DEFAULT_DRILL_WEIGHT = 0.8D;

    private static final double DEFAULT_NUCLEARCORE_WEIGHT = 8.0D;

    private static final double DEFAULT_BIPROPELLANT_FUEL_WEIGHT = 2.0E-4D;
    private static final double DEFAULT_OXIDIZER_WEIGHT = 3.0E-4D;
    private static final double DEFAULT_MONOPROPELLANT_WEIGHT = 5.0E-4D;
    private static final double DEFAULT_NUCLEAR_WORKING_FLUID_WEIGHT = 5.0E-4D;

    private static final Map<String, Double> ROCKET_BLOCK_WEIGHTS = createRocketBlockWeights();

    private final String file;
    private Map<String, Double> weights;

    WeightEngine(String file) {
        this.file = file;
        load();
    }

    private static Map<String, Double> createRocketBlockWeights() {
        Map<String, Double> map = new HashMap<>();

        map.put("advancedrocketry:fueltank", DEFAULT_ROCKET_TANK_WEIGHT);
        map.put("advancedrocketry:bipropellantfueltank", DEFAULT_ROCKET_TANK_WEIGHT);
        map.put("advancedrocketry:oxidizerfueltank", DEFAULT_ROCKET_TANK_WEIGHT);
        map.put("advancedrocketry:nuclearfueltank", DEFAULT_ROCKET_TANK_WEIGHT);

        map.put("advancedrocketry:rocketmotor", DEFAULT_ROCKET_MOTOR_WEIGHT);
        map.put("advancedrocketry:advrocketmotor", DEFAULT_ROCKET_MOTOR_WEIGHT);
        map.put("advancedrocketry:bipropellantrocketmotor", DEFAULT_ROCKET_MOTOR_WEIGHT);
        map.put("advancedrocketry:advbipropellantrocketmotor", DEFAULT_ROCKET_MOTOR_WEIGHT);
        map.put("advancedrocketry:nuclearrocketmotor", DEFAULT_ROCKET_MOTOR_WEIGHT);

        map.put("advancedrocketry:liquidtank", DEFAULT_PRESSURE_TANK_WEIGHT);
        map.put("advancedrocketry:loader", DEFAULT_SATELLITE_HATCH_WEIGHT);
        map.put("advancedrocketry:guidancecomputer", DEFAULT_GUIDANCE_COMPUTER_WEIGHT);

        map.put("advancedrocketry:intake", DEFAULT_INTAKE_WEIGHT);
        map.put("advancedrocketry:drill", DEFAULT_DRILL_WEIGHT);
        map.put("advancedrocketry:nuclearcore", DEFAULT_NUCLEARCORE_WEIGHT);

        map.put("advancedrocketry:servicemonitor", DEFAULT_ITEM_WEIGHT);
        map.put("advancedrocketry:dataunit", DEFAULT_ITEM_WEIGHT);
        map.put("advancedrocketry:satellitepowersource", DEFAULT_ITEM_WEIGHT);
        map.put("advancedrocketry:seat", DEFAULT_ITEM_WEIGHT);
        map.put("advancedrocketry:satellite", DEFAULT_ITEM_WEIGHT);
        map.put("advancedrocketry:spacestationchip", DEFAULT_ITEM_WEIGHT);
        map.put("advancedrocketry:structuretower", DEFAULT_ITEM_WEIGHT);

        return map;
    }

    public float getWeight(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return 0;
        }

        String registryName = stack.getItem().getRegistryName().toString();

        Double rocketBlockWeight = getDefaultRocketBlockWeight(stack, registryName);
        if (rocketBlockWeight != null) {
            return (float) (rocketBlockWeight * stack.getCount());
        }

        double jsonWeight = weights.getOrDefault(registryName, -1.0D);
        if (jsonWeight >= 0) {
            return (float) (jsonWeight * stack.getCount());
        }

        weights.put(registryName, DEFAULT_ITEM_WEIGHT);
        return (float) (DEFAULT_ITEM_WEIGHT * stack.getCount());
    }

    private Double getDefaultRocketBlockWeight(ItemStack stack, String registryName) {
        Double hardcodedWeight = ROCKET_BLOCK_WEIGHTS.get(registryName);
        if (hardcodedWeight != null) {
            return hardcodedWeight;
        }

        if (!(stack.getItem() instanceof ItemBlock) || !registryName.startsWith("advancedrocketry:")) {
            return null;
        }
        Block block = ((ItemBlock) stack.getItem()).getBlock();

        if (block instanceof BlockFuelTank) {
            return DEFAULT_ROCKET_TANK_WEIGHT;
        }
        if (block instanceof BlockRocketMotor || block instanceof BlockBipropellantRocketMotor) {
            return DEFAULT_ROCKET_MOTOR_WEIGHT;
        }
        if (block instanceof BlockPressurizedFluidTank) {
            return DEFAULT_PRESSURE_TANK_WEIGHT;
        }
        return null;
    }

    public float getWeight(Collection<ItemStack> stacks) {
        return stacks.stream().map(this::getWeight).reduce(0.0F, Float::sum);
    }

    public float getWeight(World world, BlockPos pos) {
        return getWeight(world.getTileEntity(pos), world.getBlockState(pos).getBlock());
    }

    public float getWeight(FluidStack stack) {
        if (stack == null || stack.getFluid() == null || stack.amount <= 0) {
            return 0;
        }

        return getWeight(stack.getFluid(), stack.amount);
    }

    public float getWeight(Fluid fluid, float amount) {
        if (fluid == null || amount <= 0) {
            return 0;
        }
        String fluidName = fluid.getUnlocalizedName();

        double jsonWeight = weights.getOrDefault(fluidName, -1.0D);
        if (jsonWeight >= 0) {
            return (float) (jsonWeight * amount);
        }
        weights.put(fluidName, DEFAULT_GENERIC_FLUID_WEIGHT);

        return (float) (DEFAULT_GENERIC_FLUID_WEIGHT * amount);
    }

    public float getRocketPropellantWeight(FuelType fuelType, float amount) {
        if (amount <= 0) {
            return 0;
        }
        return (float) (getDefaultRocketPropellantWeight(fuelType) * amount);
    }

    private static double getDefaultRocketPropellantWeight(FuelType fuelType) {
        if (fuelType == FuelType.LIQUID_MONOPROPELLANT) {
            return DEFAULT_MONOPROPELLANT_WEIGHT;
        }

        if (fuelType == FuelType.LIQUID_BIPROPELLANT) {
            return DEFAULT_BIPROPELLANT_FUEL_WEIGHT;
        }

        if (fuelType == FuelType.LIQUID_OXIDIZER) {
            return DEFAULT_OXIDIZER_WEIGHT;
        }

        if (fuelType == FuelType.NUCLEAR_WORKING_FLUID) {
            return DEFAULT_NUCLEAR_WORKING_FLUID_WEIGHT;
        }

        return DEFAULT_MONOPROPELLANT_WEIGHT;
    }

    public float getTEWeight(TileEntity te) {
        if (!ARConfiguration.getCurrentConfig().advancedWeightSystemInventories) {
            return 0;
        }
        float weight = 0;
        if (te == null) {
            return weight;
        }

        IItemHandler capability = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (capability != null) {
            for (int i = 0; i < capability.getSlots(); i++) {
                weight += getWeight(capability.getStackInSlot(i));
            }
        }

        IFluidHandler fluidHandler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        if (fluidHandler != null) {
            for (IFluidTankProperties info : fluidHandler.getTankProperties()) {
                if (info != null && info.getContents() != null) {
                    weight += getWeight(info.getContents());
                }
            }
        }
        return weight;
    }

    public float getWeight(TileEntity te, Block blk) {
        if (blk == null) {
            if (te == null) {
                return 0;
            }
            blk = te.getBlockType();
        }
        float weight = getWeight(new ItemStack(blk));
        return weight + getTEWeight(te);
    }

    public float getWeight(World world, Collection<BlockPos> poses) {
        return poses.stream().map(pos -> getWeight(world, pos)).reduce(0.0F, Float::sum);
    }

    public void load() {
        File f = new File(file);
        if (!f.exists()) {
            weights = new HashMap<>();
            return;
        }
        try (Reader r = new FileReader(file)) {
            Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null || !root.has("individual") || !root.get("individual").isJsonObject()) {
                weights = new HashMap<>();
                return;
            }
            weights = GSON.fromJson(root.getAsJsonObject("individual"), HashMap.class);
            if (weights == null) {
                weights = new HashMap<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            weights = new HashMap<>();
            System.out.println("The weight config was wrong, could not be read, was broken, not there or something else! An empty weight config will be used");
        }
    }

    public void save() {
        try {
            File f = new File(file);
            File parent = f.getParentFile();

            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter w = new FileWriter(f)) {
                Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
                JsonObject json = new JsonObject();
                json.add("individual", GSON.toJsonTree(weights));
                w.write(GSON.toJson(json));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}