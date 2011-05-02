package com.herocraftonline.dev.heroes.command.skill.skills;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.command.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.persistence.Hero;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillBandage extends TargettedSkill {

    private HashMap<Integer, Integer> playerSchedulers = new HashMap<Integer, Integer>();
    private int tickHealth;
    private int ticks;

    public SkillBandage(Heroes plugin) {
        super(plugin);
        name = "Bandage";
        description = "Skill - Bandage";
        usage = "/skill bandage";
        minArgs = 0;
        maxArgs = 0;
        identifiers.add("skill bandage");
        maxDistance = 5;
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = Configuration.getEmptyNode();
        node.setProperty("tick-health", 1);
        node.setProperty("ticks", 10);
        return node;
    }

    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player) {
            Player tPlayer = (Player) target;
            if (!(player.getItemInHand().getType() == Material.PAPER)) {
                Messaging.send(player, "You need paper to perform this.");
                return false;
            }

            if (playerSchedulers.containsKey(tPlayer.getEntityId())) {
                Messaging.send(player, "$1 is already being bandaged.", tPlayer.getName());
                return false;
            }

            tickHealth = config.getInt("tick-health", 1);
            ticks = config.getInt("ticks", 10);
            playerSchedulers.put(tPlayer.getEntityId(), plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new BandageTask(plugin, tPlayer), 20L, 20L));

            notifyNearbyPlayers(player.getLocation().toVector(), "$1 is bandaging $2.", player.getName(), tPlayer == player ? "himself" : tPlayer.getName());
            return true;
        }
        return false;
    }

    private class BandageTask implements Runnable {
        private JavaPlugin plugin;
        private Player target;
        private int timesRan = 0;

        public BandageTask(JavaPlugin plugin, Player target) {
            this.plugin = plugin;
            this.target = target;
        }

        @Override
        public void run() {
            int health = target.getHealth();
            if (timesRan == ticks && health == 20) {
                int id = playerSchedulers.remove(target.getEntityId());
                plugin.getServer().getScheduler().cancelTask(id);
            } else {
                timesRan++;
                target.setHealth(health + tickHealth);
            }
        }
    }
}
