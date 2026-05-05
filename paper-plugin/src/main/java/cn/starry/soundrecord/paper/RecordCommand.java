package cn.starry.soundrecord.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RecordCommand implements CommandExecutor, TabCompleter {
    private final RecordService recordService;

    public RecordCommand(RecordService recordService) {
        this.recordService = recordService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHeader(sender);
            return true;
        }
        if ("help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }
        if ("status".equalsIgnoreCase(args[0])) {
            handleStatus(sender, args);
            return true;
        }
        if ("stop".equalsIgnoreCase(args[0])) {
            handleStop(sender, args);
            return true;
        }
        handlePlay(sender, args);
        return true;
    }

    private void handlePlay(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sendHelp(sender);
            return;
        }
        String fileName = args[0];
        if (args.length == 2 && "@a".equalsIgnoreCase(args[1])) {
            try {
                recordService.playForAll(fileName);
                sender.sendMessage(playingMessage(fileName));
            } catch (IOException e) {
                sender.sendRichMessage("<red>Unable to play record: " + e.getMessage());
            }
            return;
        }
        Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendRichMessage("<red>Player is not online.");
            return;
        }
        try {
            recordService.playFor(fileName, target);
            sender.sendMessage(playingMessage(fileName));
        } catch (IOException e) {
            sender.sendRichMessage("<red>Unable to play record: " + e.getMessage());
        }
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sendHelp(sender);
            return;
        }
        if (args.length == 2 && "@a".equalsIgnoreCase(args[1])) {
            recordService.stop();
            sender.sendRichMessage("<red>SoundRecord playback stopped.");
            return;
        }
        Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendRichMessage("<red>Player is not online.");
            return;
        }
        recordService.stop(target);
        sender.sendRichMessage("<red>SoundRecord playback stopped.");
    }

    private void handleStatus(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendRichMessage("<red>Usage: /record status <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendRichMessage("<red>Player is not online.");
            return;
        }
        String current = recordService.status(target).orElse("nothing");
        sender.sendMessage(Component.text(target.getName(), NamedTextColor.AQUA)
                .append(Component.text(" is currently listening to ", NamedTextColor.WHITE))
                .append(Component.text(current, NamedTextColor.AQUA)));
    }

    private Component playingMessage(String fileName) {
        return Component.text("You are currently playing ", NamedTextColor.WHITE)
                .append(Component.text(RecordService.displayName(fileName), NamedTextColor.AQUA));
    }

    private void sendHeader(CommandSender sender) {
        sender.sendRichMessage("<gradient:#55FFFF:#555555:#FFFFFF>SoundRecord -by Stalyer @ 2026</gradient>");
        sender.sendRichMessage("<gradient:#FFFFFF:#FF55FF>Please enter /record help to find more information.</gradient>");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendRichMessage("<gradient:#55FFFF:#FFFFFF>/record <file> [player|@a] - play a SoundRecord file.</gradient>");
        sender.sendRichMessage("<gradient:#55FFFF:#FFFFFF>/record stop [player|@a] - stop playback.</gradient>");
        sender.sendRichMessage("<gradient:#55FFFF:#FFFFFF>/record status <player> - view current playback.</gradient>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> values = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String option : List.of("help", "status", "stop")) {
                if (option.startsWith(prefix)) {
                    values.add(option);
                }
            }
            for (String record : recordService.listRecords()) {
                if (record.toLowerCase().startsWith(prefix)) {
                    values.add(record);
                }
            }
            return values;
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            if ("@a".startsWith(prefix) && !"status".equalsIgnoreCase(args[0])) {
                values.add("@a");
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    values.add(player.getName());
                }
            }
        }
        return values;
    }
}
