package com.nitnelave.CreeperHeal;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.nitnelave.CreeperHeal.config.CreeperConfig;
import com.nitnelave.CreeperHeal.config.WorldConfig;
import com.nitnelave.CreeperHeal.utils.CreeperMessenger;
import com.nitnelave.CreeperHeal.utils.CreeperPermissionManager;
import com.nitnelave.CreeperHeal.utils.CreeperPlayer;

public class GriefListener implements Listener {

    /**
     * Listener for the BlockPlaceEvent. If the player does not have the rights
     * to place a block, the event is cancelled, and the appropriate warnings
     * are fired.
     * 
     * @param event
     *            The BlockPlaceEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace (BlockPlaceEvent event) {
        Player player = event.getPlayer ();
        WorldConfig world = CreeperConfig.loadWorld (player.getWorld ());
        if (event.getBlockPlaced ().getType () == Material.TNT && !CreeperPermissionManager.checkPermissions (player, false, "bypass.place-tnt"))
        {
            boolean blocked = world.blockTNT;
            if (blocked)
                event.setCancelled (true);
            if (world.warnTNT)
                CreeperMessenger.warn (CreeperPlayer.WarningCause.TNT, player, blocked, null);
        }
        else if (world.isGriefBlackListed (event.getBlock ()) && !CreeperPermissionManager.checkPermissions (player, false, "bypass.place-blacklist"))
        {
            boolean blocked = world.griefBlockList;
            if (blocked)
                event.setCancelled (true);
            if (world.warnBlackList)
                CreeperMessenger.warn (CreeperPlayer.WarningCause.BLACKLIST, player, blocked, event.getBlockPlaced ().getType ().toString ());
        }

    }

    /**
     * Listener for the BlockIgniteEvent. If fire spreading or fire from lava is
     * disabled, cancel the event.
     * 
     * @param event
     *            The BlockIgniteEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockIgnite (BlockIgniteEvent event) {
        WorldConfig world = CreeperConfig.loadWorld (event.getBlock ().getWorld ());

        if (event.getCause () == IgniteCause.SPREAD && world.preventFireSpread)
            event.setCancelled (true);
        else if (event.getCause () == IgniteCause.LAVA && world.preventFireLava)
            event.setCancelled (true);
    }

    /**
     * Listener for the BlockSpreadEvent. If the event concerns fire and fire
     * spreading is disabled, cancel the event.
     * 
     * @param event
     *            The BlockSpreadEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockSpread (BlockSpreadEvent event) {
        if (!event.getBlock ().getType ().equals (Material.FIRE))
            return;
        WorldConfig world = CreeperConfig.loadWorld (event.getBlock ().getWorld ());

        event.getBlock ().setTypeId (0);
        event.getSource ().setTypeId (0);

        if (world.preventFireSpread)
            event.setCancelled (true);
    }

    /**
     * Listener for the EntityDamageEvent. Control PVP and check for destroyed
     * paintings.
     * 
     * @param event
     *            The EntityDamageEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage (EntityDamageEvent event) {
        if (event.getEntity () instanceof Player)
        {
            Player attacked = (Player) event.getEntity ();
            Player offender = null;
            String message = attacked.getDisplayName ();
            Entity attacker;
            switch (event.getCause ())
            {
                case ENTITY_ATTACK:
                    attacker = ((EntityDamageByEntityEvent) event).getDamager ();
                    if (attacker instanceof Player)
                        offender = (Player) attacker;
                    break;
                case PROJECTILE:
                case MAGIC:
                    Entity damager = ((EntityDamageByEntityEvent) event).getDamager ();
                    if (damager instanceof Projectile)
                    {
                        Projectile projectile = (Projectile) damager;
                        attacker = projectile.getShooter ();
                        if (attacker instanceof Player)
                            offender = (Player) attacker;
                    }
                    break;
                default:
            }
            if (offender != null && !offender.equals (attacked) && !CreeperPermissionManager.checkPermissions (offender, true, "bypass.pvp"))
            {
                WorldConfig world = CreeperConfig.loadWorld (event.getEntity ().getWorld ());
                boolean blocked = world.blockPvP;
                if (blocked)
                    event.setCancelled (true);
                if (world.warnPvP)
                    CreeperMessenger.warn (CreeperPlayer.WarningCause.PVP, offender, blocked, message);
            }
        }
    }

    /**
     * Listener for the PlayerBucketEmptyEvent. Check for lava placement.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerBucketEmpty (PlayerBucketEmptyEvent event) {
        WorldConfig world = CreeperConfig.loadWorld (event.getPlayer ().getWorld ());

        Player player = event.getPlayer ();
        if (event.getBucket () == Material.LAVA_BUCKET && !CreeperPermissionManager.checkPermissions (player, true, "bypass.place-lava"))
        {
            if (world.blockLava)
                event.setCancelled (true);
            if (world.warnLava)
                CreeperMessenger.warn (CreeperPlayer.WarningCause.LAVA, player, world.blockLava, null);
        }
    }

    /**
     * Listener for the PlayerInteractEvent. Check for monster egg use, and
     * block ignition.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract (PlayerInteractEvent event) {
        ItemStack item = event.getItem ();
        if (item == null)
            return;

        Player player = event.getPlayer ();
        WorldConfig world = CreeperConfig.loadWorld (player.getWorld ());

        if (item.getType () == Material.MONSTER_EGG && !CreeperPermissionManager.checkPermissions (player, true, "bypass.spawnEgg"))
        {
            String entityType = EntityType.fromId (event.getItem ().getData ().getData ()).getName ();

            if (world.blockSpawnEggs)
                event.setCancelled (true);
            if (world.warnSpawnEggs)
                CreeperMessenger.warn (CreeperPlayer.WarningCause.SPAWN_EGG, player, world.blockSpawnEggs, entityType);
        }
        else if (item.getType () == Material.FLINT_AND_STEEL && !CreeperPermissionManager.checkPermissions (player, true, "bypass.ignite"))
        {
            if (world.blockIgnite)
                event.setCancelled (true);
            if (world.warnIgnite)
                CreeperMessenger.warn (CreeperPlayer.WarningCause.FIRE, player, world.blockIgnite, null);
        }
    }

    /**
     * Listener for the PlayerJoinEvent. Add when appropriate to the warning
     * list.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin (PlayerJoinEvent event) {
        CreeperMessenger.registerPlayer (new CreeperPlayer (event.getPlayer ()));
    }

    /**
     * Listener for the PlayerQuitEvent. Remove when appropriate from the
     * warning list.
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit (PlayerQuitEvent event) {
        CreeperMessenger.removeFromWarnList (new CreeperPlayer (event.getPlayer ()));
    }

}
