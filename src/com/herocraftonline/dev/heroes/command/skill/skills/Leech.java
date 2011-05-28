package com.herocraftonline.dev.heroes.command.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.command.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.persistence.Hero;

public class Leech extends TargettedSkill{

    public Leech(Heroes plugin) {
        super(plugin);
        name = "Leech";
        description = "Steals mana from an opponant";
        usage = "/skill leech";
        minArgs = 0;
        maxArgs = 1;
        identifiers.add("skill leech");
    }
    
    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("transfer-amount", 20);
        return node;
    }
    
    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        Hero tHero = plugin.getHeroManager().getHero((Player) target);
        int transferamount = getSetting(hero.getHeroClass(), "transfer-amount", 20);
        if(hero.getMana() > transferamount){
            hero.setMana(hero.getMana() + transferamount);
            tHero.setMana(tHero.getMana() - transferamount);
            return true;
        }else{
            return false;
        }
    }

}

}
