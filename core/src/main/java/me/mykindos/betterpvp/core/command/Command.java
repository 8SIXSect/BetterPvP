package me.mykindos.betterpvp.core.command;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public abstract class Command implements ICommand {

    @Setter
    private boolean enabled;

    @Setter
    private Rank requiredRank;

    protected List<String> aliases;
    protected List<SubCommand> subCommands;

    public Command() {
        aliases = new ArrayList<>();
        subCommands = new ArrayList<>();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    public Optional<SubCommand> getSubCommand(String name) {
        return getSubCommands().stream().filter(subCommand -> subCommand.getName().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public List<String> processTabComplete(CommandSender sender, String[] args) {
        List<String> tabCompletions = new ArrayList<>();
        if(args.length == 0) return tabCompletions;

        if(getArgumentType(1).equals(ArgumentType.SUBCOMMAND.name())){
            Optional<SubCommand> subCommand = getSubCommand(args[0]);
            if(subCommand.isPresent()){
                return subCommand.get().processTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        String lowercaseArg = args[args.length - 1].toLowerCase();


        switch (getArgumentType(args.length)) {
            case "SUBCOMMAND" -> getSubCommands().forEach(subCommand -> {
                if(subCommand.getName().toLowerCase().startsWith(lowercaseArg)) {
                    tabCompletions.add(subCommand.getName());
                    tabCompletions.addAll(subCommand.getAliases());
                }
            });
            case "PLAYER" -> tabCompletions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().
                    startsWith(lowercaseArg)).toList());
            case "POSITION_X" -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getX() + "" : "0");
            case "POSITION_Y" -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getY() + "" : "0");
            case "POSITION_Z" -> tabCompletions.add(sender instanceof Player player ? player.getLocation().getZ() + "" : "0");
        }


        return tabCompletions;
    }

    @Override
    public Rank getRequiredRank() {
        return requiredRank;
    }


}