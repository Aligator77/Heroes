package com.herocraftonline.dev.heroes.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.command.BaseCommand;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.persistence.HeroManager;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.nijiko.coelho.iConomy.iConomy;

public class SelectProfession extends BaseCommand {
    
    public SelectProfession(Heroes plugin) {
        super(plugin);
        name = "Select Profession";
        description = "Selects a new profession";
        usage = "/heroes profession §9<type>";
        minArgs = 1;
        maxArgs = 1;
        identifiers.add("heroes profession");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            HeroClass profession = plugin.getClassManager().getClass(args[0]);
            if (profession != null) {
                if (profession.isPrimary()) {
                    HeroManager heroManager = plugin.getHeroManager();
                    Hero hero = heroManager.getHero(player);
                    if (hero.getPlayerClass().equals(plugin.getClassManager().getDefaultClass())) {
                        hero.setPlayerClass(profession);
                    } else {
                        changeClass(hero, profession);
                    }
                    Messaging.send(player, "Welcome to the path of the $1!", profession.getName());
                } else {
                    Messaging.send(player, "Sorry, $1 isn't a profession!", profession.getName());
                }
            } else {
                Messaging.send(player, "Sorry, that isn't a profession!");
            }
        }
    }

    public void changeClass(Hero hero, HeroClass newClass) {
        if (plugin.getConfigManager().getProperties().iConomy == true) {
            String playerName = hero.getPlayer().getName();
            if (iConomy.getBank().getAccount(playerName).hasOver(plugin.getConfigManager().getProperties().swapcost)) {
                iConomy.getBank().getAccount(playerName).add(plugin.getConfigManager().getProperties().swapcost * -1);
            }
            hero.setPlayerClass(newClass);
        } else {
            hero.setPlayerClass(newClass);
        }
    }
}
