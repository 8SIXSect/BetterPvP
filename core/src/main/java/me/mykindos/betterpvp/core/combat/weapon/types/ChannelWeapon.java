package me.mykindos.betterpvp.core.combat.weapon.types;

import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class ChannelWeapon extends Weapon implements IWeapon, Listener {

    protected final Set<UUID> activeUsageNotifications = new HashSet<>();
    protected final Set<UUID> active = new HashSet<>();

    public ChannelWeapon(BPvPPlugin plugin, String key) {
        super(plugin, key);
    }

    public ChannelWeapon(BPvPPlugin plugin, String key, List<Component> lore) {
        super(key, plugin, lore);
    }

    public abstract double getEnergy();

    public boolean useShield(Player player) {
        return false;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        active.remove(event.getEntity().getUniqueId());
        activeUsageNotifications.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
        activeUsageNotifications.remove(event.getPlayer().getUniqueId());
    }

}
