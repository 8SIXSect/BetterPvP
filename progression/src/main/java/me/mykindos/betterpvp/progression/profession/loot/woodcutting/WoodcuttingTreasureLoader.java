package me.mykindos.betterpvp.progression.profession.loot.woodcutting;

import me.mykindos.betterpvp.progression.profession.loot.ProfessionConfigLoader;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class WoodcuttingTreasureLoader implements ProfessionConfigLoader<WoodcuttingTreasureType> {
    @Override
    public String getTypeKey() {
        return "treasure";
    }

    @Override
    public @NotNull WoodcuttingTreasureType read(ConfigurationSection section) {
        return new WoodcuttingTreasureType(section.getName());
    }
}
