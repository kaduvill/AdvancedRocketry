package zmaster587.advancedRocketry.command.sub.planet;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import zmaster587.advancedRocketry.command.sub.ARCommand;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.libVulpes.network.PacketHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class PlanetResetCommand extends ARCommand {
    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.advancedrocketry.planet.reset.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        boolean resetAtmosphereOverride = args.length > 0
                && ("atmosphereOverride".equalsIgnoreCase(args[args.length - 1])
                || "ao".equalsIgnoreCase(args[args.length - 1]));
        int dimensionArgs = args.length - (resetAtmosphereOverride ? 1 : 0);
        if (dimensionArgs > 1) {
            throw wrongUsage(sender);
        }
        int dimId;
        if (dimensionArgs == 0) {
            Entity entity = sender.getCommandSenderEntity();
            if (entity == null) {
                throw wrongUsage(sender);
            }
            dimId = entity.dimension;
        } else {
            dimId = parseInt(args[0]);
        }
        if (!DimensionManager.getInstance().isDimensionCreated(dimId)) {
            throw invalidValue("Planet with id", dimId);
        }
        DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(dimId);
        if (resetAtmosphereOverride) {
            if (properties == DimensionManager.defaultSpaceDimensionProperties) {
                throw invalidValue("Planet with id", dimId);
            }
            if (properties.hasAtmosphereOverride()) {
                properties.setAtmosphereOverride(false);
                PacketHandler.sendToAll(new PacketDimInfo(dimId, properties, false));
            }
            sender.sendMessage(new TextComponentTranslation("commands.advancedrocketry.planet.reset.atmosphereOverride.success",
                    properties.getName(), dimId));
            sender.sendMessage(new TextComponentTranslation("commands.advancedrocketry.planet.reset.atmosphereOverride.warning"));
            return;
        }
        properties.resetProperties();
        DimensionManager.getInstance().refreshTerraformingBiomes();
        PacketHandler.sendToAll(new PacketDimInfo(dimId, properties));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1 || args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "atmosphereOverride", "ao");
        }
        return Collections.emptyList();
    }
}
