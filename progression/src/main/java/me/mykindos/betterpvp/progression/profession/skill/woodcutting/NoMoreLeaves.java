package me.mykindos.betterpvp.progression.profession.skill.woodcutting;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.loot.type.WoodcuttingLootType;
import me.mykindos.betterpvp.progression.profession.loot.woodcutting.WoodcuttingLoot;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * This skill is causes leaves to be removed when the player uses <b>Tree Feller</b>.
 * <br />
 * Additionally, it grants the player a chance to receive special drops from the removed leaves.
 */
@Singleton
public class NoMoreLeaves extends WoodcuttingProgressionSkill {
    private final ProfessionProfileManager professionProfileManager;
    private final WoodcuttingHandler woodcuttingHandler;

    @Getter
    private int baseMaxLeavesCount;

    /**
     * Global Map to prevent the player from chopping off too many leaves from a felled tree
     */
    public final HashMap<UUID, Integer> felledTreeLeavesMap = new HashMap<>();

    public final LoadingCache<Player, WoodcuttingLoot> woodcuttingLootCache = Caffeine.newBuilder()
            .weakKeys()
            .build(key -> getRandomLoot());

    @Inject
    public NoMoreLeaves(Progression progression, ProfessionProfileManager professionProfileManager, WoodcuttingHandler woodcuttingHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.woodcuttingHandler = woodcuttingHandler;
    }

    @Override
    public String getName() {
        return "No More Leaves";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Removes leaves when <green>Tree Feller</green> is used",
                "Grants a chance for special drops from the removed leaves"
        };
    }

    @Override
    public Material getIcon() {
        return Material.AZALEA_LEAVES;
    }

    /**
     * This function's purpose is to return a boolean that tells you if the player has the skill
     * <b>No More Leaves</b>
     */
    public boolean doesPlayerHaveSkill(Player player) {
        Optional<ProfessionProfile> profile = professionProfileManager.getObject(player.getUniqueId().toString());

        if (profile.isPresent()) {
            return getPlayerSkillLevel(profile.get()) >= 0;
        }

        return false;
    }

    /**
     * Gets a random loot type from WoodcuttingHandler
     * If the type is null, an empty loot type is returned
     * @return Gets a random loot type from WoodcuttingHandler
     */
    public WoodcuttingLoot getRandomLoot() {
        final WoodcuttingLootType type = woodcuttingHandler.getLootTypes().random();
        if (type == null) {
            return new WoodcuttingLoot() {
                @Override
                public WoodcuttingLootType getType() {
                    return null;
                }

                @Override
                public void processRemovedLeaf(Player player, Location location) {
                    UtilMessage.message(player, "Woodcutting", "<red>No loot type registered! Please report this to an admin!");
                }
            };
        }

        return type.generateLoot();
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        baseMaxLeavesCount = getConfig("baseMaxLeavesCount", 20, Integer.class);
    }
}
