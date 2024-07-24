package me.mykindos.betterpvp.progression.profession.loot.fishing;

import me.mykindos.betterpvp.progression.profession.loot.ProfessionConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SwimmerLoader implements ProfessionConfigLoader<SwimmerType> {
    @Override
    public String getTypeKey() {
        return "entity";
    }

    @Override
    public @NotNull SwimmerType read(ConfigurationSection section) {
        return new SwimmerType(section.getName());
    }
}
