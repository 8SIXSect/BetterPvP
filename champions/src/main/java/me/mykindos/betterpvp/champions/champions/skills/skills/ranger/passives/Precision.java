package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Singleton
@BPvPListener
public class Precision extends Skill implements PassiveSkill, DamageSkill, OffensiveSkill {

    // Needed for arrow trail
    private final Set<Arrow> arrows = new HashSet<>();
    private double baseDamage;

    private double damageIncreasePerLevel;

    @Inject
    public Precision(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Precision";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your arrows deal " + getValueString(this::getDamage, level) + " bonus damage on hit"
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @UpdateEvent
    public void updateArrowTrail() {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow arrow = it.next();
            if (arrow == null || arrow.isDead() || !(arrow.getShooter() instanceof Player) || arrow.isInBlock()) {
                it.remove();
            } else {
                Location location = arrow.getLocation().add(new Vector(0, 0.25, 0));
                Particle.TOTEM_OF_UNDYING.builder()
                        .location(location)
                        .receivers(60)
                        .extra(0)
                        .spawn();
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;


        int level = getLevel(player);
        if (level > 0) arrows.add(arrow);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            event.setDamage(event.getDamage() + getDamage(level));
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
    }
}
