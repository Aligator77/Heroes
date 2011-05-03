package com.herocraftonline.dev.heroes.classes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.util.config.Configuration;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass.ArmorItems;
import com.herocraftonline.dev.heroes.classes.HeroClass.ArmorType;
import com.herocraftonline.dev.heroes.classes.HeroClass.ExperienceType;
import com.herocraftonline.dev.heroes.classes.HeroClass.WeaponItems;
import com.herocraftonline.dev.heroes.classes.HeroClass.WeaponType;
import com.herocraftonline.dev.heroes.command.skill.OutsourcedSkill;

public class ClassManager {

    private final Heroes plugin;
    private Set<HeroClass> classes;
    private HeroClass defaultClass;

    public ClassManager(Heroes plugin) {
        this.plugin = plugin;
        this.classes = new HashSet<HeroClass>();
    }

    public HeroClass getClass(String name) {
        for (HeroClass c : classes) {
            if (name.equalsIgnoreCase(c.getName())) {
                return c;
            }
        }
        return null;
    }

    public boolean addClass(HeroClass c) {
        return classes.add(c);
    }

    public boolean removeClass(HeroClass c) {
        return classes.remove(c);
    }

    public Set<HeroClass> getClasses() {
        return classes;
    }

    public void loadClasses(File file) {
        Configuration config = new Configuration(file);
        config.load();
        List<String> classNames = config.getKeys("classes");
        if(classNames==null){
            plugin.log(Level.WARNING, "You have no Classes defined in your setup!");
            return;
        }
        for (String className : classNames) {
            HeroClass newClass = new HeroClass(className);

            List<String> defaultType = new ArrayList<String>();
            defaultType.add("DIAMOND");

            StringBuilder aLimits = new StringBuilder();
            StringBuilder wLimits = new StringBuilder();

            List<String> armor = config.getStringList("classes." + className + ".permitted-armor", defaultType);
            for (String a : armor) {
                // If it's a generic type like 'DIAMOND' or 'LEATHER' we add all the possible entries.
                if (!(a.contains("_"))) {
                    try {
                        ArmorType aType = ArmorType.valueOf(a);
                        newClass.addAllowedArmor(aType + "_HELMET");
                        aLimits.append(" " + aType + "_HELMET");
                        newClass.addAllowedArmor(aType + "_CHESTPLATE");
                        aLimits.append(" " + aType + "_CHESTPLATE");
                        newClass.addAllowedArmor(aType + "_LEGGINGS");
                        aLimits.append(" " + aType + "_LEGGINGS");
                        newClass.addAllowedArmor(aType + "_BOOTS");
                        aLimits.append(" " + aType + "_BOOTS");
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid armor type (" + a + ") defined for " + className);
                    }
                } else {
                    String type = a.substring(0, a.indexOf("_"));
                    String item = a.substring(a.indexOf("_") + 1, a.length());
                    try {
                        ArmorType aType = ArmorType.valueOf(type);
                        ArmorItems aItem = ArmorItems.valueOf(item);
                        newClass.addAllowedArmor(aType + "_" + aItem);
                        aLimits.append(" " + aType + "_" + aItem);
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid armor type (" + type + "_" + item + ") defined for " + className);
                    }
                }
            }

            List<String> weapon = config.getStringList("classes." + className + ".permitted-weapon", defaultType);
            for (String w : weapon) {
                // A BOW has no ItemType so we just add it straight away.
                if (w.equalsIgnoreCase("BOW")) {
                    newClass.addAllowedWeapon("BOW");
                    wLimits.append(" BOW");
                    continue;
                }
                // If it's a generic type like 'DIAMOND' or 'LEATHER' we add all the possible entries.
                if (!(w.contains("_"))) {
                    try {
                        WeaponType wType = WeaponType.valueOf(w);
                        newClass.addAllowedWeapon(wType + "_PICKAXE");
                        wLimits.append(" " + wType + "_PICKAXE");
                        newClass.addAllowedWeapon(wType + "_AXE");
                        wLimits.append(" " + wType + "_AXE");
                        newClass.addAllowedWeapon(wType + "_HOE");
                        wLimits.append(" " + wType + "_HOE");
                        newClass.addAllowedWeapon(wType + "_SPADE");
                        wLimits.append(" " + wType + "_SPADE");
                        newClass.addAllowedWeapon(wType + "_SWORD");
                        wLimits.append(" " + wType + "_SWORD");
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid weapon type (" + w + ") defined for " + className);
                    }
                } else {
                    String type = w.substring(0, w.indexOf("_"));
                    String item = w.substring(w.indexOf("_") + 1, w.length());
                    try {
                        WeaponType wType = WeaponType.valueOf(type);
                        WeaponItems wItem = WeaponItems.valueOf(item);
                        newClass.addAllowedWeapon(wType + "_" + wItem);
                        wLimits.append(" - " + wType + "_" + wItem);
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid weapon type (" + type + "_" + item + ") defined for " + className);
                    }
                }

            }
            plugin.debugLog(Level.INFO, "Allowed Weapons - " + wLimits.toString());
            plugin.debugLog(Level.INFO, "Allowed Armor - " + aLimits.toString());

            List<String> skillNames = config.getKeys("classes." + className + ".permitted-skills");
            if (skillNames != null) {
                for (String skill : skillNames) {
                    try {
                        int reqLevel = config.getInt("classes." + className + ".permitted-skills." + skill + ".level", 1);
                        int manaCost = config.getInt("classes." + className + ".permitted-skills." + skill + ".mana", 0);
                        int cooldown = config.getInt("classes." + className + ".permitted-skills." + skill + ".cooldown", 0);
                        newClass.addSkill(skill, reqLevel, manaCost, cooldown);
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid skill (" + skill + ") defined for " + className + ". Skipping this skill.");
                    }
                }
            } else {
                plugin.log(Level.WARNING, className + " has no Skills defined!");
            }

            List<String> permissionSkillNames = config.getKeys("classes." + className + ".permission-skills");
            if (permissionSkillNames != null) {
                for (String skill : permissionSkillNames) {
                    try {
                        int reqLevel = config.getInt("classes." + className + ".permission-skills." + skill + ".level", 1);
                        newClass.addSkill(skill, reqLevel, 0, 0);

                        String usage = config.getString("classes." + className + ".permission-skills." + skill + ".usage", "");
                        String[] permissions = config.getStringList("classes." + className + ".permission-skills." + skill + ".permissions", null).toArray(new String[0]);
                        OutsourcedSkill oSkill = new OutsourcedSkill(plugin, skill, permissions, usage);
                        plugin.getCommandManager().addCommand(oSkill);
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid permission skill (" + skill + ") defined for " + className + ". Skipping this skill.");
                    }
                }
            }

            List<String> experienceNames = config.getStringList("classes." + className + ".experience-sources", null);
            Set<ExperienceType> experienceSources = new HashSet<ExperienceType>();
            if(experienceNames!=null){
                for (String experience : experienceNames) {
                    try {
                        boolean added = experienceSources.add(ExperienceType.valueOf(experience));
                        if (!added) {
                            plugin.log(Level.WARNING, "Duplicate experience source (" + experience + ") defined for " + className + ".");
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.log(Level.WARNING, "Invalid experience source (" + experience + ") defined for " + className + ". Skipping this source.");
                    }
                }
            }
            newClass.setExperienceSources(experienceSources);

            int maxSummon = config.getInt("classes." + className + ".tameMax", 0);
            try {
                newClass.setTameMax(maxSummon);
            } catch (Exception e) {
                plugin.log(Level.WARNING, "summon-max not set correctly -  (" + maxSummon + ") is not a number for - " + className);
            }

            boolean added = addClass(newClass);
            if (!added) {
                plugin.log(Level.WARNING, "Duplicate class (" + className + ") found. Skipping this class.");
            } else {
                plugin.log(Level.INFO, "Loaded class: " + className);
                if (config.getBoolean("classes." + className + ".default", false)) {
                    plugin.log(Level.INFO, "Default class found: " + className);
                    defaultClass = newClass;
                }
            }
        }

        for (HeroClass unlinkedClass : classes) {
            String className = unlinkedClass.getName();
            String parentName = config.getString("classes." + className + ".parent");
            if (parentName != null && (!parentName.isEmpty() || parentName.equals("null"))) {
                HeroClass parent = getClass(parentName);
                parent.getSpecializations().add(unlinkedClass);
                unlinkedClass.setParent(parent);
            }
        }
    }

    public void setDefaultClass(HeroClass defaultClass) {
        this.defaultClass = defaultClass;
    }

    public HeroClass getDefaultClass() {
        return defaultClass;
    }

}
