package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.TeamSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.WorldSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Singleton
@BPvPListener
public class ArcticArmour extends ActiveToggleSkill implements EnergySkill, DefensiveSkill, TeamSkill, DebuffSkill, BuffSkill, WorldSkill {

    private final WorldBlockHandler blockHandler;

    @Getter
    private int radius;
    @Getter
    private double duration;

    private int resistanceStrength;
    private int slownessStrength;
    @Getter
    private double slowDuration;

    @Inject
    public ArcticArmour(Champions champions, ChampionsManager championsManager, WorldBlockHandler blockHandler) {
        super(champions, championsManager);
        this.blockHandler = blockHandler;
    }

    @Override
    public String getName() {
        return "Arctic Armour";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Drop your Sword / Axe to toggle",
                "",
                "Create a freezing area around",
                "you in a <val>" + getRadius() + "</val> Block radius",
                "",
                "Allies inside this area receive <effect>Resistance " + UtilFormat.getRomanNumeral(resistanceStrength) + "</effect>, and",
                "enemies hit by you receive <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> for",
                getSlowDuration() + " seconds",
                "",
                "Uses <val>" + getEnergyStartCost() + "</val> energy on activation",
                "Energy / Second: <val>" + getEnergy(),
                "",
                EffectTypes.RESISTANCE.getDescription(resistanceStrength)
        };
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public boolean process(Player player) {

        HashMap<String, Long> updateCooldowns = updaterCooldowns.get(player.getUniqueId());

        if (updateCooldowns.getOrDefault("audio", 0L) < System.currentTimeMillis()) {
            audio(player);
            updateCooldowns.put("audio", System.currentTimeMillis() + 1000);
        }

        if (updateCooldowns.getOrDefault("snowAura", 0L) < System.currentTimeMillis()) {
            snowAura(player);
            updateCooldowns.put("snowAura", System.currentTimeMillis() + 100);
        }

        return doArcticArmour(player);
    }

    private void audio(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.3F, 0.0F);
    }

    private boolean doArcticArmour(Player player) {
        final int distance = getRadius();
        if (!hasSkill(player)) {
            return false;
        }

        if (!championsManager.getEnergy().use(player, getName(), getEnergy() / 20, true)) {
            return false;
        }

        // Apply resistance and slow effects
        final List<KeyValue<Player, EntityProperty>> nearby = UtilPlayer.getNearbyPlayers(player, distance);
        nearby.add(new KeyValue<>(player, EntityProperty.FRIENDLY));
        for (KeyValue<Player, EntityProperty> nearbyEnt : nearby) {
            final Player target = nearbyEnt.getKey();
            final boolean friendly = nearbyEnt.getValue() == EntityProperty.FRIENDLY;

            if (friendly) {
                championsManager.getEffects().addEffect(target, EffectTypes.RESISTANCE, resistanceStrength, 1000);
            }
        }
        return true;
    }

    private void snowAura(Player player) {
        final int distance = getRadius();
        // Apply cue effects
        // Spin particles around the player in the radius
        final int angle = (int) ((System.currentTimeMillis() / 10) % 360);
        playEffects(player, distance, -angle);
        playEffects(player, distance, angle);
        playEffects(player, distance, -angle + 180f);
        playEffects(player, distance, angle + 180f);

        convertWaterToIce(player, getDuration(), distance);
    }

    private void playEffects(final Player player, float radius, float angle) {
        final Location reference = player.getLocation();
        reference.setPitch(0);

        final Location relative = UtilLocation.fromAngleDistance(reference, radius, angle);
        final Optional<Location> closestSurface = UtilLocation.getClosestSurfaceBlock(relative, 3, true);
        closestSurface.ifPresent(loc -> loc.add(0, 2.2, 0));
        final Location result = closestSurface.orElse(relative.add(0, 1.2, 0));
        Particle.CLOUD.builder().extra(0).location(result).receivers(60).spawn();
    }

    private void convertWaterToIce(Player player, double duration, int radius) {
        // Sort by height descending
        final HashMap<Block, Double> inRadius = UtilBlock.getInRadius(player.getLocation(), radius);
        Collection<Block> blocks = inRadius.keySet().stream()
                .sorted((b1, b2) -> b2.getLocation().getBlockY() - b1.getLocation().getBlockY())
                .toList();

        for (Block block : blocks) {
            if (block.getLocation().getY() > player.getLocation().getY()) {
                continue;
            }

            final boolean water = UtilBlock.isWater(block);
            if (!water && block.getType() != Material.ICE) {
                continue;
            }

            final Block top = block.getRelative(0, 1, 0);
            if (UtilBlock.isWater(top)) {
                continue;
            }

            if (blockHandler.isRestoreBlock(block, "Ice Prison") && !block.getWorld().getName().equalsIgnoreCase("world")) {
                continue;
            }

            final long expiryOffset = (long) (100 * (inRadius.get(block) * radius));
            final long delay = (long) Math.pow((1 - inRadius.get(block)) * radius, 2);
            blockHandler.scheduleRestoreBlock(player, block, Material.ICE, delay, ((long) duration * 1000) + expiryOffset, false);

            final double chance = Math.random();
            if (chance < 0.025) {
                Particle.SNOWFLAKE.builder().extra(0).location(block.getLocation()).receivers(60).spawn();
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public float getEnergy() {
        return (float) energy;
    }

    @Override
    public void toggleActive(Player player) {
        if (championsManager.getEnergy().use(player, getName(), getEnergyStartCost(), false)) {
            UtilMessage.message(player, getClassType().getName(), "Arctic Armour: <green>On");
        } else {
            cancel(player);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;

        if (hasSkill(player)) {
            championsManager.getEffects().addEffect(event.getDamagee(), player, EffectTypes.SLOWNESS, slownessStrength, 1000);
        }
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 4, Integer.class);
        duration = getConfig("duration", 2.0, Double.class);

        resistanceStrength = getConfig("resistanceStrength", 1, Integer.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
        slowDuration = getConfig("slowDuration", 2.0, Double.class);
    }


}