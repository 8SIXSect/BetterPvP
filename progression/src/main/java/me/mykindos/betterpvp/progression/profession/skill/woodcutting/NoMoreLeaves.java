package me.mykindos.betterpvp.progression.profession.skill.woodcutting;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * This skill is causes leaves to be removed when the player uses <b>Tree Feller</b>.
 * <br />
 * Additionally, it grants the player a chance to receive special drops from the removed leaves.
 */
@Singleton
public class NoMoreLeaves extends WoodcuttingProgressionSkill {
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public NoMoreLeaves(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
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
}
