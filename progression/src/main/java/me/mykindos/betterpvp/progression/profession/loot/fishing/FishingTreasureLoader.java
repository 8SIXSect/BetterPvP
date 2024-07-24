package me.mykindos.betterpvp.progression.profession.loot.fishing;

import me.mykindos.betterpvp.progression.profession.loot.ProfessionConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class FishingTreasureLoader implements ProfessionConfigLoader<FishingTreasureType> {

    @Override
    public String getTypeKey() {
        return "treasure";
    }

    @Override
    public @NotNull FishingTreasureType read(ConfigurationSection section) {
        return new FishingTreasureType(section.getName());
    }
}
