package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

/**
 * Provide changes to certain skills from champions if loaded and necessary
 */
@Singleton
@BPvPListener
public class ClansSkillListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClansSkillListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChampionsSkill(PlayerUseSkillEvent event) {
        if (!clanManager.canCast(event.getPlayer())) {
            UtilMessage.message(event.getPlayer(), "Restriction", "You cannot use this skill here.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGetPlayerPropertyEvent(GetEntityRelationshipEvent event) {
        if(event.getEntity() instanceof Player player && event.getTarget() instanceof Player target) {
            boolean isAlly = clanManager.isAlly(player, target);
            event.setEntityProperty(isAlly ? EntityProperty.FRIENDLY : EntityProperty.ENEMY);
        }
    }

    @EventHandler
    public void onFetchNearbyEntity(FetchNearbyEntityEvent<?> event) {
        if (!(event.getSource() instanceof Player player)) return;
        event.getEntities().forEach(entity -> {

            entity.setValue(EntityProperty.ENEMY);

            if (!(entity.getKey() instanceof Player target)) return;
            boolean isAlly = clanManager.isAlly(player, target);
            if(isAlly) {
                entity.setValue(EntityProperty.FRIENDLY);
            }
        });

        event.getEntities().removeIf(entity -> {
            if (entity.getKey() instanceof Player target) {
                if (target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR) {
                    return true;
                }

                if (clanManager.isInSafeZone(target)) {
                    return true;
                }

                if (event.getEntityProperty() != EntityProperty.ALL) {
                    return entity.getValue() != event.getEntityProperty();
                }
            }
            return false;
        });
    }

    @EventHandler
    public void disableLongshot(PlayerCanUseSkillEvent event) {
        if (!event.getSkill().getName().equals("Longshot")) return;
        Player player = event.getPlayer();

        if(player.getLocation().getBlockY() > 100) {
            event.cancel("Cannot use Longshot above 100Y");
            return;
        }

        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (playerClanOptional.isPresent() && locationClanOptional.isPresent()) {
            Clan playerClan = playerClanOptional.get();
            Clan locationClan = locationClanOptional.get();

            if (playerClan.equals(locationClan) || playerClan.isAllied(locationClan)) {
                event.cancel("Cannot use Longshot in own or allied territory");
            }
        }
    }

    @EventHandler
    public void disableSafezone(PlayerCanUseSkillEvent event) {
        if (!clanManager.canCast(event.getPlayer())) {
            UtilMessage.message(event.getPlayer(), "Restriction", "You cannot use this skill here.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void disableSafezoneItems(PlayerUseItemEvent event) {
        if (!clanManager.canCast(event.getPlayer()) && event.isDangerous()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onThrowableCollision(ThrowableHitEntityEvent event) {
        if (!(event.getCollision() instanceof Player target)) return;
        if (!(event.getThrowable().getThrower() instanceof Player thrower)) return;

        if (target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (event.getThrowable().isCanHitFriendlies()) {
            boolean isAlly = clanManager.isAlly(thrower, target);
            if (clanManager.isInSafeZone(target) && isAlly) {
                event.setCancelled(true);
                return;
            } else if (isAlly) {
                return;
            }
        }

        if (!clanManager.canHurt(thrower, target)) {
            event.setCancelled(true);
        }

    }
}
