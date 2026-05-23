package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.TremorData;
import me.mykindos.betterpvp.champions.champions.skills.types.AreaOfEffectSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Tremor extends Skill implements InteractSkill, CooldownSkill, OffensiveSkill, DamageSkill, AreaOfEffectSkill {

    private final WeakHashMap<Player, TremorData> tremors = new WeakHashMap<>();

    private double damage;
    private double damageIncreasePerLevel;
    private double maxRadius;
    private double radiusIncreasePerTick;

    @Inject
    public Tremor(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Tremor";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Send out a shockwave in a " + getValueString(this::getRadius, level) + " block radius,",
                "dealing " + getValueString(this::getDamage, level) + " damage to enemies hit",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getRadius(int level) {
        return maxRadius;
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public boolean activate(Player player, int level) {
        tremors.put(player, new TremorData(player.getLocation().clone()));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.6f);
        return true;
    }

    @UpdateEvent
    public void processTremor() {
        final Iterator<Map.Entry<Player, TremorData>> iterator = tremors.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, TremorData> entry = iterator.next();
            final Player player = entry.getKey();
            final TremorData tremor = entry.getValue();
            if (player == null || !player.isValid() || tremor == null) {
                iterator.remove();
                continue;
            }

            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            tremor.increaseRadius(radiusIncreasePerTick);
            final double currentRadius = tremor.getCurrentRadius();
            final Location origin = tremor.getOrigin();
            drawShockwave(origin, currentRadius);

            for (LivingEntity enemy : UtilEntity.getNearbyEnemies(player, origin, currentRadius)) {
                if (tremor.getHitEntities().contains(enemy) || !player.hasLineOfSight(enemy)) {
                    continue;
                }

                tremor.getHitEntities().add(enemy);
                UtilDamage.doDamage(new DamageEvent(enemy,
                        player,
                        null,
                        new SkillDamageCause(this),
                        getDamage(level),
                        getName()));
            }

            if (currentRadius >= maxRadius) {
                iterator.remove();
            }
        }
    }

    private void drawShockwave(Location origin, double radius) {
        if (origin.getWorld() == null || radius <= 0) {
            return;
        }

        int points = Math.max(8, (int) (radius * 8));
        for (int point = 0; point < points; point++) {
            double angle = (Math.PI * 2 * point) / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLocation = origin.clone().add(x, 0.15, z);
            origin.getWorld().spawnParticle(Particle.CLOUD, particleLocation, 0, 0, 0.03, 0, 0.01);
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        maxRadius = getConfig("radius", 5.0, Double.class);
        radiusIncreasePerTick = getConfig("radiusIncreasePerTick", 1.0, Double.class);
    }
}
