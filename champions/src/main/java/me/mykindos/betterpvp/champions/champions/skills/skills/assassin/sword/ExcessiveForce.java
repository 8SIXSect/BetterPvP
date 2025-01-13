package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ExcessiveForce extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();

    @Getter
    private double duration;


    @Inject
    public ExcessiveForce(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Excessive Force";
    }


    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "For the next <val>" + getDuration() + "</val> seconds",
                "your attacks deal standard knockback to enemies",
                "",
                "Does not ignore anti-knockback abilities",
                "",
                "Cooldown: <val>" + getCooldown()
        };
    }

    @Override
    public void activate(Player player) {
        active.put(player, System.currentTimeMillis() + (long) (getDuration() * 1000L));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1.7f);
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void setKnockback(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (active.containsKey(damager)) {
                event.setKnockback(true);
            }
        }
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, Long> next = it.next();
            Player player = next.getKey();
            if (next.getValue() - System.currentTimeMillis() <= 0) {
                it.remove();
                UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s</green> has ended.", getName()));
                continue;
            }

            if (!championsManager.getRoles().hasRole(next.getKey(), Role.ASSASSIN)) {
                it.remove();
            }

        }
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 3.0, Double.class);
    }


}
