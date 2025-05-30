package me.mykindos.betterpvp.core.utilities;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.model.DropTable;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilItem {

    public static ItemStack convertType(@NotNull ItemStack itemStackIn, @NotNull Material to, @Nullable Integer toModel) {
        ItemStack itemStack = itemStackIn.clone();
        final Material from = itemStack.getType();
        if (itemStack.getItemMeta() != null) {
            final ItemMeta metaCopy = itemStack.getItemMeta().clone();
            itemStack.setType(to);
            final ItemMeta meta = itemStack.getItemMeta();
            meta.setCustomModelData(toModel);

            if (!metaCopy.hasDisplayName()) {
                final String key = from.translationKey();
                final Component name = Component.translatable(key, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
                meta.displayName(name);
            }

            // Durability
            if (metaCopy instanceof Damageable oldItem && meta instanceof Damageable newItem) {
                int maxDamage = oldItem.hasMaxDamage() ? oldItem.getMaxDamage() : itemStackIn.getType().getMaxDurability();
                int damage = oldItem.hasDamageValue() ? oldItem.getDamage() : 0;
                if (maxDamage == 0) {
                    if (damage > 0) {
                        log.warn("Trying to set damage to {} that has no max damage", to.name()).submit();
                    }
                    //do nothing
                } else {
                    newItem.setMaxDamage(maxDamage);
                    newItem.setDamage(damage);
                }
            }

            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static boolean isCosmeticShield(ItemStack item) {
        return item.getType() == Material.SHIELD && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(CoreNamespaceKeys.UNDROPPABLE_KEY, PersistentDataType.BOOLEAN)
                && Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer().get(CoreNamespaceKeys.UNDROPPABLE_KEY, PersistentDataType.BOOLEAN));
    }

    public static ItemStack createCosmeticShield(int modelData) {
        ItemStack shield = new ItemStack(Material.SHIELD);
        final ItemMeta meta = shield.getItemMeta();
        meta.setCustomModelData(modelData);
        meta.getPersistentDataContainer().set(CoreNamespaceKeys.UNDROPPABLE_KEY, PersistentDataType.BOOLEAN, true);
        shield.setItemMeta(meta);
        return shield;
    }

    /**
     * Updates an ItemStack, giving it a custom name and lore
     *
     * @param item ItemStack to modify
     * @param name Name to give the ItemStack
     * @param lore Lore to give the ItemStack
     * @return Returns the ItemStack with the newly adjusted name and lore
     */
    public static ItemStack setItemNameAndLore(ItemStack item, String name, String[] lore) {
        if (lore != null) {
            return setItemNameAndLore(item, name, Arrays.asList(lore));
        }

        return setItemNameAndLore(item, name, new ArrayList<>());

    }

    /**
     * Updates an ItemStack, giving it a custom name and lore
     *
     * @param item ItemStack to modify
     * @param name Name to give the ItemStack
     * @param lore Lore to give the ItemStack
     * @return Returns the ItemStack with the newly adjusted name and lore
     */
    public static ItemStack setItemNameAndLore(ItemStack item, String name, List<String> lore) {
        ItemMeta im = item.getItemMeta();
        im.displayName(Component.text(name));
        if (lore != null) {
            List<Component> components = new ArrayList<>();
            for (String loreEntry : lore) {
                components.add(Component.text(loreEntry));
            }
            im.lore(components);
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        item.setItemMeta(im);
        return item;
    }

    /**
     * Updates an ItemStack, giving it a custom name and lore
     *
     * @param item ItemStack to modify
     * @param name Name to give the ItemStack
     * @param lore Lore to give the ItemStack
     * @return Returns the ItemStack with the newly adjusted name and lore
     */
    public static ItemStack setItemNameAndLore(ItemStack item, Component name, List<Component> lore) {
        ItemMeta im = item.getItemMeta();
        im.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (lore != null) {
            im.lore(removeItalic(lore));
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        item.setItemMeta(im);
        return item;
    }

    /**
     * Removes attributes from an ItemStack (e.g. the +7 damage that is visible
     * on a diamond sword under the lore)
     *
     * @param item ItemStack to update
     * @return Returns an itemstack without its attributes
     */
    public static ItemStack removeAttributes(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(itemMeta);


        return item;
    }

    /**
     * Removes Enchants from an ItemStack (e.g. the `Sharpness iV` that is visible
     * on a diamond sword's lore)
     *
     * @param item ItemStack to update
     * @return Returns an itemstack without its attributes
     */
    public static ItemStack removeEnchants(ItemStack item) {
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            item.removeEnchantment(entry.getKey());
        }


        return item;
    }

    /**
     * Add the 'enchanted' glowing effect to any ItemStack
     *
     * @param meta Item to update
     */
    public static void addGlow(ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
    }

    /**
     * Check if a Material is a type of sword
     *
     * @param item Material to check
     * @return Returns true if the Material is a type of sword
     */
    public static boolean isSword(ItemStack item) {
        return item.getType().name().contains("_SWORD");
    }

    public static boolean isSimilar(ItemStack item, ItemStack other) {
        return (item.getType() == Material.AIR && other.getType() == Material.AIR) || (other.isSimilar(item));
    }

    /**
     * Check if a Material is a type of axe
     *
     * @param axeType Material to check
     * @return Returns true if the Material is a type of axe
     */
    public static boolean isAxe(ItemStack axeType) {
        return axeType.getType().name().contains("_AXE");
    }

    /**
     * Check if a Material is a type of pickaxe
     *
     * @param pickType Material to check
     * @return Returns true if the Material is a type of pickaxe
     */
    public static boolean isPickaxe(Material pickType) {
        return pickType.name().contains("_PICKAXE");
    }

    public static boolean isShovel(Material shovelType) {
        return shovelType.name().contains("_SHOVEL");
    }

    /**
     * Check if a Material is a type of hoe
     *
     * @param hoeType Material to check
     * @return Returns true if the Material is a type of hoe
     */
    public static boolean isHoe(Material hoeType) {
        return hoeType.name().contains("_HOE");
    }

    public static boolean isTool(ItemStack item) {
        return isPickaxe(item.getType()) || isHoe(item.getType()) || isShovel(item.getType()) || isAxe(item);
    }

    public static boolean isWeapon(ItemStack itemStack) {
        return isSword(itemStack) || isAxe(itemStack) || isRanged(itemStack);
    }

    public static boolean isArmour(Material material) {
        return material.name().contains("_CAP") || material.name().contains("_HELMET") || material.name().contains("_CHESTPLATE")
                || material.name().contains("_LEGGINGS") || material.name().contains("_BOOTS");
    }

    /**
     * Check if a Material is a type of ranged weapon
     *
     * @param wep Material to check
     * @return Returns true if the Material is a type of ranged weapon
     */
    public static boolean isRanged(ItemStack wep) {
        return (wep.getType() == Material.BOW || wep.getType() == Material.CROSSBOW);
    }

    public static void insert(Player player, ItemStack stack) {
        if (stack != null && stack.getType() != Material.AIR) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(stack).forEach((num, item) -> {
                    player.getWorld().dropItem(player.getLocation(), item);
                });
            } else {
                player.getWorld().dropItem(player.getLocation(), stack);
            }
        }
    }

    public static void insert(Player player, ItemStack stack, ItemHandler itemHandler) {
        if (stack != null && stack.getType() != Material.AIR) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(stack);
            } else {
                player.getWorld().dropItem(player.getLocation(), stack);
            }
        }
    }

    public static <T, Z> Z getOrSavePersistentData(ItemMeta itemMeta, NamespacedKey namespacedKey, PersistentDataType<T, Z> type, Z defaultValue) {
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        if (!dataContainer.has(namespacedKey, type)) {
            dataContainer.set(namespacedKey, type, defaultValue);
            return defaultValue;
        }
        return dataContainer.getOrDefault(namespacedKey, type, defaultValue);
    }

    public static int indexOf(String matchingText, List<Component> components) {
        for (int i = 0; i < components.size(); i++) {
            String componentText = PlainTextComponentSerializer.plainText().serialize(components.get(i));
            if (componentText.equalsIgnoreCase(matchingText)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Create a simple ItemStack with a specific model
     * @param material The material
     * @param customModelData The model ID
     * @return The ItemStack
     */
    public static ItemStack createItemStack(Material material, int amount, int customModelData) {
        var itemStack = new ItemStack(material, amount);
        if(customModelData > 0) {
            var itemMeta = itemStack.getItemMeta();
            itemMeta.setCustomModelData(customModelData);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public static ItemStack createItemStack(Material material, int customModelData) {
        return createItemStack(material, 1, customModelData);
    }

    // Create a function to remove TextDecoration.ITALIC from List<Component>
    public static List<Component> removeItalic(List<Component> components) {
        List<Component> newComponents = new ArrayList<>();
        for (Component component : components) {
            newComponents.add(component.decoration(TextDecoration.ITALIC, false));
        }
        return newComponents;
    }

    public static DropTable getDropTable(ItemHandler itemHandler, BPvPPlugin plugin, String config, String configKey) {
        return getDropTable(itemHandler, plugin.getConfig(config), configKey);
    }

    public static DropTable getDropTable(ItemHandler itemHandler, BPvPPlugin plugin, String configKey) {
        return getDropTable(itemHandler, plugin, "config", configKey);
    }

    public static DropTable getDropTable(ItemHandler itemHandler, ExtendedYamlConfiguration config, String configKey) {
        DropTable droptable = new DropTable(configKey);

        var configSection = config.getConfigurationSection(configKey);
        if (configSection == null) return droptable;

        parseDropTable(itemHandler, configSection, droptable);

        return droptable;
    }

    public static Map<String, DropTable> getDropTables(ItemHandler itemHandler, ExtendedYamlConfiguration config, String configKey) {
        Map<String, DropTable> droptableMap = new HashMap<>();

        var configSection = config.getConfigurationSection(configKey);
        if (configSection == null) return droptableMap;

        configSection.getKeys(false).forEach(key -> {
            var droptableSection = configSection.getConfigurationSection(key);
            if (droptableSection == null) return;
            DropTable droptable = new DropTable(key);
            parseDropTable(itemHandler, droptableSection, droptable);

            droptableMap.put(key, droptable);

            log.info("Droptable: " + key).submit();
            droptable.getAbsoluteElementChances().forEach((element, chance) -> {
                log.info(element + ": " + (chance * 100)).submit();
            });

        });

        return droptableMap;
    }

    private static void parseDropTable(ItemHandler itemHandler, ConfigurationSection droptableSection, WeighedList<ItemStack> droptable) {
        for (String key : droptableSection.getKeys(false)) {
            ItemStack itemStack = null;
            int weight = droptableSection.getInt(key + ".weight");
            int categoryWeight = droptableSection.getInt(key + ".category-weight");
            int amount = droptableSection.getInt(key + ".amount", 1);

            if (key.contains(":")) {
                BPvPItem item = itemHandler.getItem(key);
                if (item != null) {
                    if (!item.isEnabled()) continue;
                    itemStack = item.getItemStack(amount);
                }
            } else {
                Material item = Material.valueOf(key.toUpperCase());
                int modelId = droptableSection.getInt(key + ".model-id", 0);
                itemStack = UtilItem.createItemStack(item, amount, modelId);
            }

            if (itemStack == null) {
                log.warn(key + " is null").submit();
            }

            droptable.add(categoryWeight, weight, itemStack);
        }
    }

    /**
     * Parses a configuration section into a DropTable.
     *
     * @param itemHandler The ItemHandler to use for resolving items
     * @param droptableSection The configuration section to parse
     * @param droptable The DropTable to add items to
     */
    private static void parseDropTable(ItemHandler itemHandler, ConfigurationSection droptableSection, DropTable droptable) {
        parseDropTable(itemHandler, droptableSection, (WeighedList<ItemStack>) droptable);
    }

    /**
     * Get an item identifier for the supplied ItemStack
     * @param itemStack
     * @return
     */
    public static String getItemIdentifier(ItemStack itemStack) {
        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        if (dataContainer.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            return dataContainer.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING);
        }
        return itemStack.getType()
                    + (itemStack.getItemMeta().hasCustomModelData() ? "(" + itemStack.getItemMeta().getCustomModelData() + ")" : "");

    }

    /**
     * Damages the supplied item, breaking it if damage > maxDamage
     * @param player the player the item belongs to
     * @param itemStack the itemStack to damage
     * @param damage the amount of damage to apply
     */
    public static void damageItem(Player player, ItemStack itemStack, int damage) {
        if (itemStack.getType() == Material.AIR) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        ItemStack damagedItemStack = itemStack.damage(damage, player);
        if (damagedItemStack.getAmount() == 0) {
            breakItem(player, damagedItemStack);
        }
    }

    /**
     * Breaks the supplied item
     * @param player the player the item belongs to
     * @param itemStack the item to break
     */
    public static void breakItem(Player player, ItemStack itemStack) {
        UtilServer.callEvent(new PlayerItemBreakEvent(player, itemStack));
        itemStack.setAmount(0);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
    }

    public static void removeRecipe(Material material) {
        var iterator = Bukkit.getServer().recipeIterator();
        while(iterator.hasNext()) {
            Recipe recipe = iterator.next();

            if(recipe.getResult().getType() == material) {
                iterator.remove();
            }
        }
    }

    /**
     * Reserves an item for a time, setting thrower permanently, and owner for the reserveTime
     * @param item the item to reserve
     * @param player the player to reserve the item for
     * @param reserveTime the number of seconds to reserve this item
     * @return the reserved item
     */
    @Contract(value = "_, _, _ -> param1", mutates = "param1")
    public static Item reserveItem(@NotNull Item item, @NotNull Player player, double reserveTime) {
        item.setThrower(player.getUniqueId());
        item.setOwner(player.getUniqueId());
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () ->
                item.setOwner(null), (long) (reserveTime * 20L));
        return item;
    }

    public static Component getDisplayName(ItemStack itemstack) {
        if(itemstack.hasItemMeta() && itemstack.getItemMeta().hasDisplayName()) {
            return itemstack.getItemMeta().displayName();
        }
        return Component.text(itemstack.getType().name());
    }

    public static String getDisplayNameAsString(ItemStack itemStack) {
        return PlainTextComponentSerializer.plainText().serialize(getDisplayName(itemStack));
    }

    public static String serializeItemStackList(ArrayList<ItemStack> itemStacks) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Write the number of ItemStacks in the list
            dos.writeInt(itemStacks.size());

            // Serialize each ItemStack
            for (ItemStack item : itemStacks) {
                if (item == null) {
                    // Handle null ItemStacks by writing a length of 0
                    dos.writeInt(0);
                } else {
                    // Serialize the ItemStack to bytes
                    byte[] itemBytes = item.serializeAsBytes();
                    // Write the length of the byte array
                    dos.writeInt(itemBytes.length);
                    // Write the byte array itself
                    dos.write(itemBytes);
                }
            }

            // Convert the final byte array to Base64 for easy storage/transmission
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (IOException e) {
            log.error("Failed to serialize ItemStack list", e);
        }

        return "";
    }

    public static ArrayList<ItemStack> deserializeItemStackList(String base64Data) {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64Data));
             DataInputStream dis = new DataInputStream(bais)) {

            // Read the number of ItemStacks
            int size = dis.readInt();

            // Deserialize each ItemStack
            for (int i = 0; i < size; i++) {
                int length = dis.readInt();
                if (length == 0) {
                    itemStacks.add(null);
                } else {
                    // Read the byte array for this ItemStack
                    byte[] itemBytes = new byte[length];
                    dis.readFully(itemBytes);
                    // Deserialize the ItemStack
                    itemStacks.add(ItemStack.deserializeBytes(itemBytes));
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to deserialize ItemStack list", e);
        }
        return itemStacks;
    }

    public static String serializeItemStackMap(Map<Integer, ItemStack> itemMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Write the number of entries in the map
            dos.writeInt(itemMap.size());

            // Serialize each key-value pair
            for (Map.Entry<Integer, ItemStack> entry : itemMap.entrySet()) {
                // Write the key
                dos.writeInt(entry.getKey());

                ItemStack item = entry.getValue();
                if (item == null) {
                    // Handle null ItemStacks by writing a length of 0
                    dos.writeInt(0);
                } else {
                    // Serialize the ItemStack to bytes
                    byte[] itemBytes = item.serializeAsBytes();
                    // Write the length of the byte array
                    dos.writeInt(itemBytes.length);
                    // Write the byte array itself
                    dos.write(itemBytes);
                }
            }

            // Convert the final byte array to Base64 for easy storage/transmission
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (IOException e) {
           log.error("Failed to serialize ItemStack map", e);
        }

        return "";
    }

    public static Map<Integer, ItemStack> deserializeItemStackMap(String base64Data) {
        Map<Integer, ItemStack> itemMap = new Int2ObjectOpenHashMap<>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64Data));
             DataInputStream dis = new DataInputStream(bais)) {

            // Read the number of entries
            int size = dis.readInt();

            // Deserialize each key-value pair
            for (int i = 0; i < size; i++) {
                // Read the key
                int key = dis.readInt();

                // Read the length of the ItemStack byte array
                int length = dis.readInt();
                if (length == 0) {
                    itemMap.put(key, null);
                } else {
                    // Read the byte array for this ItemStack
                    byte[] itemBytes = new byte[length];
                    dis.readFully(itemBytes);
                    // Deserialize the ItemStack
                    itemMap.put(key, ItemStack.deserializeBytes(itemBytes));
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            log.error("Failed to deserialize ItemStack map", e);
        }

        return itemMap;
    }


}
