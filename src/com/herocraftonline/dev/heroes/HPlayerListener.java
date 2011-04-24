package com.herocraftonline.dev.heroes;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.herocraftonline.dev.heroes.inventory.HNetServerHandler;
import com.herocraftonline.dev.heroes.persistence.HeroManager;

public class HPlayerListener extends PlayerListener {
    private final Heroes plugin;

    public HPlayerListener(Heroes instance) {
        plugin = instance;
    }

    @Override
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        HeroManager heroManager = plugin.getHeroManager();
        heroManager.loadHeroFile(player);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        HeroManager heroManager = plugin.getHeroManager();
        heroManager.saveHeroFile(player);
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        CraftPlayer cp = (CraftPlayer) p;
        CraftServer cs = (CraftServer) Bukkit.getServer();

        if (!(cp.getHandle().netServerHandler instanceof HNetServerHandler)) {
            Location l = p.getLocation();
            HNetServerHandler handler = new HNetServerHandler(cs.getHandle().server, cp.getHandle().netServerHandler.networkManager, cp.getHandle());
            handler.a(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
            cp.getHandle().netServerHandler = handler;
        }
        this.plugin.inventoryCheck(p);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (plugin.getHeroManager().getHero(p).getBinds().containsKey(event.getMaterial())) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                String[] args = plugin.getHeroManager().getHero(p).getBinds().get(event.getMaterial());
                plugin.onCommand(p, null, args[0], Arrays.copyOf(args, 1));
            }
        }
    }
}
