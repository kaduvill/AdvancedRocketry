package zmaster587.advancedRocketry.command.sub.planet;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.command.CommandTreeHelp;
import net.minecraftforge.server.command.TextComponentHelper;

public class PlanetCommand extends CommandTreeBase {
    public PlanetCommand() {
        addSubcommand(new PlanetResetCommand());
        addSubcommand(new PlanetListCommand());
        addSubcommand(new PlanetDeleteCommand());
        addSubcommand(new PlanetGenerateCommand());
        addSubcommand(new PlanetSetCommand());
        addSubcommand(new PlanetGetCommand());

        addSubcommand(new CommandTreeHelp(this) {
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (args.length == 1 && "reset".equals(args[0]) && getSubCommand("reset").checkPermission(server, sender)) {
                    sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.defaults")
                            .appendText("\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.defaults.description"))
                            .appendText("\n\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.atmosphereOverride"))
                            .appendText("\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.atmosphereOverride.description"))
                            .appendText("\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.restart"))
                            .appendText("\n\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.stop"))
                            .appendText("\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.stopped"))
                            .appendText("\n\n").appendSibling(TextComponentHelper.createComponentTranslation(sender, "commands.advancedrocketry.planet.reset.help.dimension")));
                    return;
                }
                super.execute(server, sender, args);
            }
        });
    }

    @Override
    public String getName() {
        return "planet";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.advancedrocketry.planet.usage";
    }
}
