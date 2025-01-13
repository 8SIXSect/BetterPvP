package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class DefensiveStance extends ChannelSkill implements CooldownSkill, InteractSkill, EnergyChannelSkill, DefensiveSkill {

    private final WeakHashMap<Player, Long> gap = new WeakHashMap<>();

    @Getter
    private double damage;
    @Getter
    private double damageReduction;
    private boolean blocksMelee;

    private boolean blocksArrow;

    @Inject
    public DefensiveStance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Defensive Stance";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "While active, you take <val>" + UtilFormat.formatNumber(getDamageReduction() * 100, 0) + "</val> reduced damage",
                "from all melee attacks in front of you",
                "",
                "Players who attack you receive <val>" + getDamage() + "</val> damage,",
                "and get knocked back",
                "",
                "Energy / Second: <val>" + getEnergy(),
                "Cooldown: <val>" + getCooldown(),
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (blocksMelee && blocksArrow) {
            if (!(event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() != DamageCause.PROJECTILE)) return;
        } else if (blocksMelee) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        } else if (blocksArrow) {
            if (event.getCause() != DamageCause.PROJECTILE) return;
        } else return;

        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        if (!gamer.isHoldingRightClick()) return;

        if (hasSkill(player)) {
            Vector look = player.getLocation().getDirection();
            look.setY(0);
            look.normalize();

            Vector from = UtilVelocity.getTrajectory(player, event.getDamager());
            from.normalize();
            if (player.getLocation().getDirection().subtract(from).length() > 0.6D) {
                return;
            }

            event.getDamager().setVelocity(event.getDamagee().getEyeLocation().getDirection().add(new Vector(0, 0.5, 0)).multiply(1));

            CustomDamageEvent customDamageEvent = new CustomDamageEvent(event.getDamager(), event.getDamagee(), null, DamageCause.CUSTOM, getDamage(), false, getName());
            UtilDamage.doCustomDamage(customDamageEvent);
            event.setDamage(event.getDamage() * (1.0 - getDamageReduction()));
            if (event.getDamage() <= 0) {
                event.cancel(getName());
            }
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 2.0F);
        }

    }

    @UpdateEvent
    public void useEnergy() {
        final Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                iterator.remove();
                continue;
            }

            Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
            if (!gamer.isHoldingRightClick()
                    || !championsManager.getEnergy().use(player, getName(), getEnergy() / 20, true)
                    || !hasSkill(player)
                    || !isHolding(player)) {

                iterator.remove();
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_STEP, 0.5F, 1.0F);
            }

        }

    }

    @Override
    public boolean isShieldInvisible() {
        return false;
    }

    @Override
    public boolean shouldShowShield(Player player) {
        return !championsManager.getCooldowns().hasCooldown(player, getName());
    }

    @Override
    public float getEnergy() {
        return energy;
    }

    @Override
    public void activate(Player player) {
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        damageReduction = getConfig("damageReduction", 1.0, Double.class);
        blocksMelee = getConfig("blocksMelee", true, Boolean.class);
        blocksArrow = getConfig("blocksArrow", false, Boolean.class);
    }


}
