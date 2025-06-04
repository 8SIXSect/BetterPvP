package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.AxeProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

@Singleton
@BPvPListener
public class MagneticSpear extends Skill implements CooldownToggleSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final Map<Player, List<AxeProjectile>> data = new HashMap<>();
    private final WeakHashMap<Trident, Player> spears = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double projectileDuration;
    private double shockDuration;
    private int concussedStrength;
    private double hitboxSize;
    private double speed;

    @Inject
    public MagneticSpear(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Magnetic Spear";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Charge and throw a spear that <effect>Shocks</effect>,",
                "<effect>Concusses</effect> and deals " + getValueString(this::getDamage, level) + " damage",
                "to enemies.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.CONCUSSED.getDescription(concussedStrength)
        };
    }

    public double getSpeed() {
        return speed;
    }

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getProjectileDuration(int level) {
        return projectileDuration;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }

    @Override
    public void toggle(Player player, int level) {
        // do wind up

        // spawn projectile
        Trident playerSpear = player.launchProjectile(Trident.class);
        playerSpear.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        playerSpear.setVelocity(player.getLocation().getDirection().multiply(2));
        playerSpear.setShooter(player);

        spears.put(playerSpear, player);
    }

    /**
     * This method is called periodically to update the spear particles.
     * The particles are spawned in a spiral pattern around the spear (kind of looks like dna).
     * <p>
     * Really, you only need to worry about the configuration section of this method.
     */
    @UpdateEvent
    public void updateSpearParticles() {
        if (spears.isEmpty()) return;

        Iterator<Map.Entry<Trident, Player>> iterator = spears.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Trident, Player> entry = iterator.next();
            Trident spear = entry.getKey();

            if (spear.isDead()) {
                iterator.remove();
            } else if (!spear.isInBlock()) {

                // Spiral Particles Configuration!
                Location center = spear.getLocation();
                double spiralRadius = 1.5;
                int spiralArms = 2;

                // Use time-based rotation angle
                double rotationAngle = (System.currentTimeMillis() % 1000) / 1000.0 * 2 * Math.PI;

                for (int armIndex = 0; armIndex < spiralArms; armIndex++) {
                    double angle = rotationAngle + (2 * Math.PI / spiralArms) * armIndex;

                    double offsetX = spiralRadius * Math.cos(angle);
                    double offsetY = armIndex * 0.1;
                    double offsetZ = spiralRadius * Math.sin(angle);

                    Location particlePosition = center.clone().add(offsetX, offsetY, offsetZ);

                    Color color = (armIndex % 2 == 0) ? Color.BLACK : Color.YELLOW;
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5F);

                    center.getWorld().spawnParticle(
                            Particle.DUST,
                            particlePosition,
                            0,
                            0, 0, 0,
                            0,
                            dustOptions
                    );
                }
            }
        }
    }

    @EventHandler
    public void doDamageAndRemoveTridentOnSpearHitEnemy(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Trident trident)) return;
        if (!spears.containsKey(trident)) return;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
        event.getDamager().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);

        // Delete the trident from the world
        trident.remove();

        // Delete the trident from the code
        spears.remove(trident);

        event.setDamage(getDamage(getLevel(player)));
        event.addReason(getName());
        event.setDamageDelay(0);
    }

    /**
     * The purpose of this event handler is to listen for when a trident hits
     * a block or simply whenever an entity is NOT hit.
     * That trident will then get despawned.
     */
    @EventHandler
    public void removeSpearOnHitBlock(ProjectileHitEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Trident trident)) return;

        // When an entity is hit, that is handled elsewhere
        if (event.getHitEntity() == null) {
            trident.remove();
        }
    }

    @EventHandler
    public void removeSpearOnPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        spears.values().removeIf(player -> player.getUniqueId().equals(playerId));
    }

    @EventHandler
    public void removeSpearOnPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getEntity().getUniqueId();
        spears.values().removeIf(player -> player.getUniqueId().equals(playerId));
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.5, Double.class);
        shockDuration = getConfig("shockDuration", 2.5, Double.class);
        concussedStrength = getConfig("concussedStrength", 1, Integer.class);
        projectileDuration = getConfig("projectileDuration", 10.0, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.4, Double.class);
        speed = getConfig("speed", 30.0, Double.class);
    }
}
