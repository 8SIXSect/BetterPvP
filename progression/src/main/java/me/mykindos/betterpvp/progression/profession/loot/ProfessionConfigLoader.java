package me.mykindos.betterpvp.progression.profession.loot;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface ProfessionConfigLoader<T> {

    /**
     * The config key for this loot type
     * @return The config key
     */
    String getTypeKey();

    @NotNull T read(ConfigurationSection section);

}
