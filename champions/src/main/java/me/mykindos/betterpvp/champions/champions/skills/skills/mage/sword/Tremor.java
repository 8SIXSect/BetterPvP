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
import me.mykindos.betterpvp.core.effects.EffectTypes;
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
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Tremor extends Skill implements InteractSkill, CooldownSkill, OffensiveSkill, DamageSkill, AreaOfEffectSkill {

    private final WeakHashMap<Player, TremorData> tremors = new WeakHashMap<>();

    private double damage;
    private double damageIncreasePerLevel;
    private double radius;
    private double radiusIncreasePerTick;
    private double weightedDuration;

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
                "Send out a shockwave dealing " + getValueString(this::getDamage, level) + " damage",
                "If the hit enemy is already **Slowed**, they will receive the **Weighted** effect.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.WEIGHTED.getDescription(0)
        };
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
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
        TremorData tremorData = new TremorData();
        tremorData.setOrigin(player.getLocation().clone());
        tremors.put(player, tremorData);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.6f);
        return true;
    }

    @UpdateEvent
    public void processTremor() {
        final Iterator<Map.Entry<Player, TremorData>> iterator = tremors.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, TremorData> entry = iterator.next();
            final @NotNull Player player = entry.getKey();
            final @NotNull TremorData tremor = entry.getValue();
            if (!player.isOnline() || !player.isValid() || player.isDead()) {
                iterator.remove();
                continue;
            }

            final int level = getLevel(player);
            if (level <= 0) {
                iterator.remove();
                continue;
            }

            final double previousRadius = tremor.getCurrentRadius();
            tremor.setCurrentRadius(tremor.getCurrentRadius() + radiusIncreasePerTick);
            final double currentRadius = tremor.getCurrentRadius();
            final Location origin = tremor.getOrigin();
            drawShockwave(origin, currentRadius);

            for (LivingEntity enemy : UtilEntity.getNearbyEnemies(player, origin, currentRadius)) {
                if (tremor.getHitEntities().contains(enemy)) {
                    continue;
                }

                final double enemyDistance = enemy.getLocation().distance(origin);
                if (enemyDistance <= previousRadius) {
                    continue;
                }

                tremor.getHitEntities().add(enemy);
                UtilDamage.doDamage(new DamageEvent(enemy,
                        player,
                        null,
                        new SkillDamageCause(this),
                        getDamage(level),
                        getName()));

                if (championsManager.getEffects().hasEffect(enemy, EffectTypes.SLOWNESS)) {
                    championsManager.getEffects().addEffect(enemy, player, EffectTypes.WEIGHTED, getName(), 1,
                            weightedDuration, true);
                }

                enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
            }

            if (currentRadius >= radius) {
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
        radius = getConfig("radius", 5.0, Double.class);
        radiusIncreasePerTick = getConfig("radiusIncreasePerTick", 0.5, Double.class);
        weightedDuration = getConfig("weightedDuration", 5.0, Double.class);
    }
}
