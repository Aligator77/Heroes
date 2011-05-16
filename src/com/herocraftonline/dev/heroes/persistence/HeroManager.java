package com.herocraftonline.dev.heroes.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.Configuration;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.command.BaseCommand;
import com.herocraftonline.dev.heroes.command.skill.OutsourcedSkill;
import com.herocraftonline.dev.heroes.command.skill.PassiveSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

/**
 * Player management
 * 
 * @author Herocraft's Plugin Team
 */
public class HeroManager {

    private Heroes plugin;
    private Set<Hero> heroes;
    private File playerFolder;
    private Timer effectTimer;
    private Timer manaTimer;
    private final static int effectInterval = 100;
    private final static int manaInterval = 5000; // Possible to be configurable?

    public HeroManager(Heroes plugin) {
        this.plugin = plugin;
        this.heroes = new HashSet<Hero>();
        playerFolder = new File(plugin.getDataFolder(), "players"); // Setup our Player Data Folder
        playerFolder.mkdirs(); // Create the folder if it doesn't exist.

        effectTimer = new Timer(false); // Maintenance thread only
        effectTimer.scheduleAtFixedRate(new EffectChecker(effectInterval, this), 0, effectInterval);

        manaTimer = new Timer(false); // Maintenance thread only
        manaTimer.scheduleAtFixedRate(new ManaUpdater(this), 0, manaInterval);
    }

    /**
     * Load the given Players Data file.
     * 
     * @param player
     */
    public void loadHeroFile(Player player) {
        File playerFile = new File(playerFolder, player.getName() + ".yml"); // Setup our Players Data File.
        // Check if it already exists, if so we load the data.
        if (playerFile.exists()) {
            Configuration playerConfig = new Configuration(playerFile); // Setup the Configuration
            playerConfig.load(); // Load the Config File

            // Grab the Data we need.
            Set<String> masteries = new HashSet<String>(playerConfig.getStringList("masteries", null));

            int exp = playerConfig.getInt("experience", 0);
            boolean verbose = playerConfig.getBoolean("verbose", true);

            HeroClass playerClass = null;
            // Grab the Players Class.
            if (playerConfig.getString("class") != null) {
                playerClass = plugin.getClassManager().getClass(playerConfig.getString("class")); // Grab the Players Class from the File.
                if (Heroes.Permissions != null && playerClass != plugin.getClassManager().getDefaultClass()) {
                    if (!Heroes.Permissions.has(player, "heroes.classes." + playerClass.getName().toLowerCase())) {
                        playerClass = plugin.getClassManager().getDefaultClass();
                    }
                }
            } else {
                playerClass = plugin.getClassManager().getDefaultClass(); // If no Class saved then revert to the Default Class.
            }

            // Lets sort out any items we need to recover.
            List<ItemStack> itemRecovery = new ArrayList<ItemStack>();
            List<String> itemKeys = playerConfig.getKeys("itemrecovery");
            if (itemKeys != null && itemKeys.size() > 0) {
                for (String item : itemKeys) {
                    try {
                        Short durability = Short.valueOf(playerConfig.getString("itemrecovery." + item, "0"));
                        Material type = Material.valueOf(item);
                        itemRecovery.add(new ItemStack(type, 1, durability));
                    } catch (IllegalArgumentException e) {
                        this.plugin.debugLog(Level.WARNING, "Either '" + item + "' doesn't exist or the durability is of an incorrect value!");
                        continue;
                    }
                }
            }

            Map<Material, String[]> binds = new HashMap<Material, String[]>();
            List<String> bindKeys = playerConfig.getKeys("binds");
            if (bindKeys != null && bindKeys.size() > 0) {
                for (String material : bindKeys) {
                    try {
                        Material item = Material.valueOf(material);
                        String bind = playerConfig.getString("binds." + material, "");
                        if (bind.length() > 0) {
                            binds.put(item, bind.split(" "));
                        }
                    } catch (IllegalArgumentException e) {
                        this.plugin.debugLog(Level.WARNING, material + " isn't a valid Item to bind a Skill to.");
                        continue;
                    }
                }
            }

            if (masteries.contains(playerClass.getName())) {
                exp = plugin.getConfigManager().getProperties().maxExp;
            }
            
            Set<String> suppressed = new HashSet<String>(playerConfig.getStringList("suppressed", null));

            // Create a New Hero
            Hero playerHero = new Hero(plugin, player, playerClass, exp, 0, verbose, masteries, itemRecovery, binds, suppressed);
            // Add the Hero to the Set.
            addHero(playerHero);

            // Handle OutsourcedSkills
            List<BaseCommand> commands = plugin.getCommandManager().getCommands();
            if (Heroes.Permissions != null) {
                for (BaseCommand cmd : commands) {
                    if (cmd instanceof OutsourcedSkill) {
                        OutsourcedSkill skill = (OutsourcedSkill) cmd;
                        if (playerClass.hasSkill(skill.getName())) {
                            skill.tryLearningSkill(playerHero);
                        }
                    }
                }
            }

            for (BaseCommand cmd : commands) {
                if (cmd instanceof PassiveSkill) {
                    PassiveSkill skill = (PassiveSkill) cmd;
                    if (playerClass.hasSkill(skill.getName())) {
                        skill.tryApplying(playerHero);
                    }
                }
            }

            plugin.log(Level.INFO, "Loaded hero: " + player.getName());
        } else {
            // Create a New Hero with the Default Setup.
            createNewHero(player);
            plugin.log(Level.INFO, "Created hero: " + player.getName());
        }
    }

    /**
     * Save the given Players Data to a file.
     * 
     * @param player
     */
    public void saveHeroFile(Player player) {
        File playerFile = new File(playerFolder, player.getName() + ".yml");
        Configuration playerConfig = new Configuration(playerFile);
        // Save the players stuff
        Hero hero = getHero(player);
        playerConfig.setProperty("class", hero.getHeroClass().toString());
        playerConfig.setProperty("experience", hero.getExp());
        playerConfig.setProperty("verbose", hero.isVerbose());
        playerConfig.setProperty("masteries", hero.getMasteries());
        playerConfig.setProperty("suppressed", hero.getSuppressedSkills());
        playerConfig.removeProperty("itemrecovery"); // Just a precaution, we'll remove any values before resaving the list.
        for (ItemStack item : getHero(player).getItems()) {
            String durability = Short.toString(item.getDurability());
            playerConfig.setProperty("itemrecovery." + item.getType().toString(), durability);
        }

        playerConfig.removeProperty("binds"); // Just a precaution.
        Map<Material, String[]> binds = getHero(player).getBinds();
        for (Material material : binds.keySet()) {
            String[] bindArgs = binds.get(material);
            StringBuilder bind = new StringBuilder();
            for (String arg : bindArgs) {
                bind.append(arg).append(" ");
            }
            playerConfig.setProperty("binds." + material.toString(), bind.toString().substring(0, bind.toString().length() - 1));
        }

        playerConfig.save();
        plugin.log(Level.INFO, "Saved hero: " + player.getName());
    }

    public boolean createNewHero(Player player) {
        Hero hero = new Hero(plugin, player, plugin.getClassManager().getDefaultClass());
        return addHero(hero);
    }

    public boolean addHero(Hero hero) {
        return heroes.add(hero);
    }

    public boolean removeHero(Hero hero) {
        return heroes.remove(hero);
    }

    public boolean containsPlayer(Player player) {
        return getHero(player) != null;
    }

    public Hero getHero(Player player) {
        for (Hero hero : heroes) {
            if (hero.getPlayer() == null) {
                heroes.remove(hero); // Seeing as it's null we might as well remove it.
                continue;
            }
            if (player.getName().equalsIgnoreCase(hero.getPlayer().getName())) {
                return hero;
            }
        }
        // If it gets to this stage then clearly the HeroManager doesn't have it so we create it...
        loadHeroFile(player);
        for (Hero hero : heroes) {
            if (player.getName().equalsIgnoreCase(hero.getPlayer().getName())) {
                return hero;
            }
        }
        return null;
    }

    public Hero[] getHeroes() {
        return heroes.toArray(new Hero[0]);
    }

    public Set<Hero> getHeroSet() {
        return heroes;
    }

    public void stopChecker() {
        effectTimer.cancel();
        manaTimer.cancel();
    }
}

class EffectChecker extends TimerTask {
    private final int interval;
    private final HeroManager manager;

    EffectChecker(int interval, HeroManager manager) {
        this.interval = interval;
        this.manager = manager;
    }

    @Override
    public void run() {
        Hero[] heroes = manager.getHeroes();
        for (Hero hero : heroes) {
            if (hero == null) {
                continue;
            }
            hero.getEffects().update(interval);
        }
    }
}

class ManaUpdater extends TimerTask {
    private final HeroManager manager;

    ManaUpdater(HeroManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        Hero[] heroes = manager.getHeroes();
        for (Hero hero : heroes) {
            if (hero == null) {
                continue;
            }
            int mana = hero.getMana();
            hero.setMana(mana > 100 ? mana : mana > 95 ? 100 : mana + 5); // Hooray for the ternary operator!
            if (mana != 100 && hero.isVerbose()) {
                Messaging.send(hero.getPlayer(), Messaging.createManaBar(hero.getMana()));
            }
        }
    }
}