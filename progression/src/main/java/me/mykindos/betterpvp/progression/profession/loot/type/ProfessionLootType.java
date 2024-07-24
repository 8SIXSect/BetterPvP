package me.mykindos.betterpvp.progression.profession.loot.type;

import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public interface ProfessionLootType<L> extends ConfigAccessor {

    /**
     * @return The frequency of the loot. The higher the frequency, the more common the loot is.
     */
    int getFrequency();

    /**
     * Generate a loot object
     * @return The loot object
     */
    L generateLoot();

    /**
     * Get the name of the loot
     * @return The name of the loot
     */
    @NotNull String getName();

    /**
     * Get a random integer within a provided range
     * @param random the Random() instance
     * @param minValue the minimum value of the range
     * @param maxValue the maximum value of the range
     * @return a random integer within the provided range
     */
    default int randomIntWithinRange(Random random, int minValue, int maxValue) {
        return random.ints(minValue, maxValue + 1)
                .findFirst()
                .orElse(minValue);
    }

    /**
     * @throws IllegalArgumentException triggers when material key is invalid or null
     * @param config the YAML config
     * @param key the type of material like <b>DIRT</b> or <b>OAK_LOG</b>
     * @param profession i.e. fishing, mining, woodcutting...
     * @return a Material object for the given profession & key
     */
    default Material getMaterialFromConfig(ExtendedYamlConfiguration config, String key, String profession) {
        final String materialKey = config.getOrSaveString(profession + ".loot." + key + ".material", "STONE");

        if (materialKey == null) {
            throw new IllegalArgumentException("Material key cannot be null!");
        }

        try {
            return Material.valueOf(materialKey.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material key: " + materialKey, e);
        }
    }
}
