package me.mykindos.betterpvp.progression.profession.loot.woodcutting;

import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.loot.type.WoodcuttingLootType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface WoodcuttingLoot {

    /**
     * Get the type of this loot
     * @return The type of this loot
     */
    WoodcuttingLootType getType();

    /**
     * Called whenever a leaf block is removed
     */
    void processRemovedLeaf(Player player, Location location);
}
