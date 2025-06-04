package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.HashSet;


@Singleton
@BPvPListener
public class MagneticSpear extends Skill implements CooldownToggleSkill, Listener, CooldownSkill, OffensiveSkill, DamageSkill {

    private final HashSet<Player> playersInWindUp = new HashSet<>();
    private final WeakHashMap<Trident, Player> spears = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double effectsDuration;
    private int concussedStrength;
    private double windUpDuration;

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

    private double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getEffectsDuration(int level) {
        return effectsDuration;
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
        int startingSlownessLevel = 5;
        double windUpDurationInTicks = windUpDuration * 20L;
        playersInWindUp.add(player);

        new BukkitRunnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if (tickCount >= windUpDurationInTicks || !player.isOnline()) {
                    championsManager.getEffects().removeEffect(player, EffectTypes.SLOWNESS);
                    playersInWindUp.remove(player);

                    cancel();
                    return;
                }

                // Calculate remaining strength: 5 → 0  && apply slowness effect
                double charge = tickCount / windUpDurationInTicks;
                int slownessLevel = (int) Math.ceil(startingSlownessLevel * (1 - charge));

                championsManager.getEffects().addEffect(player, player, EffectTypes.SLOWNESS, slownessLevel, -1);

                // Pitch increases as the wind up progresses
                float pitch = 1f + ((float) charge);
                player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);

                // Small swirl of black and yellow dust to indicate wind up
                Location loc = player.getLocation().add(0, 1.0, 0);
                Color[] colors = {Color.BLACK, Color.YELLOW};

                for (int i = 0; i < 2; i++) {
                    double angle = Math.toRadians(((double) System.currentTimeMillis() / 10 + i * 180) % 360);
                    double radius = 0.4;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Particle.DustOptions dust = new Particle.DustOptions(colors[i], 1.0F);
                    Location particleLoc = loc.clone().add(x, 0.2, z);
                    Particle.DUST.builder()
                            .location(particleLoc)
                            .count(0)
                            .offset(0, 0, 0)
                            .extra(0)
                            .data(dust)
                            .receivers(60)
                            .spawn();
                }

                tickCount++;
            }
        }.runTaskTimer(champions, 0L, 1L);


        // Do ability - have it run the tick after the wind up is done
        UtilServer.runTaskLater(champions, () -> {

            // do projectile sound
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, 1.1f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

            // spawn projectile
            Trident playerSpear = player.launchProjectile(Trident.class);
            playerSpear.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            playerSpear.setVelocity(player.getLocation().getDirection().multiply(2));
            playerSpear.setShooter(player);

            spears.put(playerSpear, player);
        }, (long) windUpDurationInTicks + 1L);
    }

    @EventHandler
    public void onPlayerUseSkillDuringWindUp(PlayerUseSkillEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        if (!playersInWindUp.contains(player)) return;
        if (event.getSkill().getType() == SkillType.SWORD || event.getSkill().getType() == SkillType.AXE) {
            UtilMessage.simpleMessage(player, "You cannot use another skill while winding up your spear!");
            event.setCancelled(true);
        }
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

                    Particle.DUST.builder()
                            .location(particlePosition)
                            .offset(0, 0, 0)
                            .receivers(60)
                            .color(color)
                            .count(0)
                            .extra(0)
                            .data(dustOptions)
                            .spawn();
                }
            }
        }
    }

    /**
     * This is where most of the logic happens for the ability.
     * <p>
     * Negative effects are applied to the enemy hit by the spear, damage is dealt, etc.
     */
    @EventHandler
    public void doDamageAndRemoveTridentOnSpearHitEnemy(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Trident trident)) return;
        if (!spears.containsKey(trident)) return;

        // Sound on successful hit
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.7f);
        event.getDamager().getWorld().playSound(event.getDamagee().getLocation(), Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);

        // Delete the trident from the world
        trident.remove();

        // Delete the trident from the code
        spears.remove(trident);

        // Deal damage to the enemy
        int level = getLevel(player);
        event.setDamage(getDamage(level));
        event.addReason(getName());
        event.setDamageDelay(0);

        // Apply negative effects to the enemy
        LivingEntity enemy = event.getDamagee();
        long duration = (long) (getEffectsDuration(level) * 1000L);
        championsManager.getEffects().addEffect(enemy, player, EffectTypes.CONCUSSED, concussedStrength, duration);
        championsManager.getEffects().addEffect(enemy, player, EffectTypes.SHOCK, duration);
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
        effectsDuration = getConfig("effectsDuration", 4.0, Double.class);
        concussedStrength = getConfig("concussedStrength", 1, Integer.class);
        windUpDuration = getConfig("windUpDuration", 1.2, Double.class);
    }
}
