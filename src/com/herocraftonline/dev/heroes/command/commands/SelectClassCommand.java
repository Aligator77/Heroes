package com.herocraftonline.dev.heroes.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.ClassManager;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.command.BaseCommand;
import com.herocraftonline.dev.heroes.persistance.PlayerManager;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SelectClassCommand extends BaseCommand {

    public SelectClassCommand(Heroes plugin) {
        super(plugin);
        name = "Select Class";
        description = "Allows you to advance from a primary class to it's secondary";
        usage = "/class select §9<class>";
        minArgs = 1;
        maxArgs = 1;
        identifiers.add("class select");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerManager playerManager = plugin.getPlayerManager();
            ClassManager classManager = plugin.getClassManager();
            HeroClass playerClass = playerManager.getClass(player);
            if (playerClass.isPrimary()) {
                HeroClass subClass = classManager.getClass(args[0]);
                if (subClass != null) {
                    if (subClass.getParent() == playerClass) {
                        playerManager.setClass(player, subClass);
                        Messaging.send(player, "Well done $1!", subClass.getName());
                    } else {
                        Messaging.send(player, "Sorry, that class doesn't belong to $1.", playerClass.getName());
                    }
                } else {
                    Messaging.send(player, "Sorry, that isn't a class!");
                }
            } else {
                Messaging.send(player, "You have already selected a class!");
            }
        }

    }
}
