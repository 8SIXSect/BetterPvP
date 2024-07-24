package me.mykindos.betterpvp.progression.profession.loot.woodcutting;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.events.items.SpecialItemDropEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.loot.type.WoodcuttingLootType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;

@Data
@CustomLog
public class WoodcuttingTreasureType implements WoodcuttingLootType {

    private static final Random RANDOM = new Random();

    @Getter(AccessLevel.NONE)
    private final String key;

    private int frequency;
    private Material material;
    private int minAmount;
    private int maxAmount;
    private @Nullable Integer customModelData;

    @Override
    public @NotNull String getName() {
        return "Treasure";
    }

    @Override
    public WoodcuttingLoot generateLoot() {
        return new WoodcuttingLoot() {
            @Override
            public WoodcuttingLootType getType() {
                return WoodcuttingTreasureType.this;
            }

            @Override
            public void processRemovedLeaf(Player player, Location location) {
                final int count = randomIntWithinRange(RANDOM, minAmount, maxAmount);
                final ItemStack itemStack = new ItemStack(material, count);
                itemStack.editMeta(meta -> meta.setCustomModelData(customModelData));

                final Item item = player.getWorld().dropItem(location, itemStack);
                item.setItemStack(itemStack);

                UtilServer.callEvent(new SpecialItemDropEvent(item, "Woodcutting"));

                log.info("{} found {}x {}.", player.getName(), count, material.name().toLowerCase())
                        .addClientContext(player).addLocationContext(item.getLocation()).submit();
            }
        };
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.frequency = config.getOrSaveInt("woodcutting.loot." + key + ".frequency", 1);
        this.minAmount = config.getOrSaveInt("woodcutting.loot." + key + ".minAmount", 1);
        this.maxAmount = config.getOrSaveInt("woodcutting.loot." + key + ".maxAmount", 1);
        this.customModelData = config.getObject("woodcutting.loot." + key + ".customModelData", Integer.class, null);
        this.material = getMaterialFromConfig(config, key, "woodcutting");
    }
}
