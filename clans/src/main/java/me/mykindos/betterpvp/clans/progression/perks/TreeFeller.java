package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.loot.woodcutting.WoodcuttingLoot;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.NoMoreLeaves;
import me.mykindos.betterpvp.progression.profession.skill.woodcutting.TreeFellerSkill;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class TreeFeller implements Listener {

    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;
    private final WoodcuttingHandler woodcuttingHandler;
    private final TreeFellerSkill treeFellerSkill;
    private final NoMoreLeaves noMoreLeaves;

    @Inject
    public TreeFeller(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
        this.woodcuttingHandler = progression.getInjector().getInstance(WoodcuttingHandler.class);
        this.treeFellerSkill = progression.getInjector().getInstance(TreeFellerSkill.class);
        this.noMoreLeaves = progression.getInjector().getInstance(NoMoreLeaves.class);

    }

    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {
        if (event.isCancelled()) return;

        Optional<ProgressionSkill> progressionSkillOptional = progressionSkillManager.getSkill("Tree Feller");
        if(progressionSkillOptional.isEmpty()) return;

        Player player = event.getPlayer();
        if (!player.getInventory().getItemInMainHand().getType().name().contains("_AXE")) return;

        ProgressionSkill skill = progressionSkillOptional.get();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {

            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession == null) return;

            int skillLevel = profession.getBuild().getSkillLevel(skill);
            if (skillLevel <= 0) return;

            Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);

            if (treeFellerSkill.getCooldownManager().hasCooldown(player, treeFellerSkill.getName())) {
                treeFellerSkill.whenPlayerCantUseSkill(player);
                return;
            }

            event.setCancelled(true);
            fellTree(player, playerClan, event.getChoppedLogBlock(), event, true);

            treeFellerSkill.whenPlayerUsesSkill(player, skillLevel);
        });
    }

    /**
     * Removes the logs of the tree
     * <br>
     * Removes the leaves of the tree if the player has <b>No More Leaves</b>
     *
     * @param player the player who activated Tree Feller
     * @param playerClan the Clan of the player who activated Tree Feller
     * @param block the current log or leaf block
     * @param event the PlayerChopLogEvent instance
     * @param initialBlock the initial log block that was chopped
     */
    public void fellTree(Player player, Clan playerClan, Block block, PlayerChopLogEvent event, boolean initialBlock) {
        if (!initialBlock && woodcuttingHandler.didPlayerPlaceBlock(block)) return;

        if (UtilBlock.isLeaves(block.getType())) {
            if (noMoreLeaves.doesPlayerHaveSkill(player)) {
                final WoodcuttingLoot woodcuttingLoot = noMoreLeaves.woodcuttingLootCache.get(player);
                woodcuttingLoot.processRemovedLeaf(player, block.getLocation());
            }
        } else {
            event.setAmountChopped(event.getAmountChopped() + 1);
        }

        block.breakNaturally();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, 1, z);

                if (UtilBlock.isLog(targetBlock.getType()) || UtilBlock.isLeaves(targetBlock.getType())) {
                    Optional<Clan> targetBlockLocationClanOptional = clanManager.getClanByLocation(targetBlock.getLocation());
                    if (targetBlockLocationClanOptional.isPresent()) {
                        if (!targetBlockLocationClanOptional.get().equals(playerClan)) continue;
                    }

                    fellTree(player, playerClan, targetBlock, event, false);
                }
            }
        }
    }
}
