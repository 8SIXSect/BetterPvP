package me.mykindos.betterpvp.progression.profession.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.loot.ProfessionConfigLoader;
import me.mykindos.betterpvp.progression.profession.loot.type.WoodcuttingLootType;
import me.mykindos.betterpvp.progression.profession.loot.woodcutting.WoodcuttingTreasureLoader;
import me.mykindos.betterpvp.progression.profession.woodcutting.leaderboards.TotalLogsChoppedLeaderboard;
import me.mykindos.betterpvp.progression.profession.woodcutting.repository.WoodcuttingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;


/**
 * This class's purpose is to listen for whenever a block is broken
 * and notify the WoodcuttingHandler appropriately.
 */
@Singleton
@CustomLog
@Getter
public class WoodcuttingHandler extends ProfessionHandler {
    private final WoodcuttingRepository woodcuttingRepository;
    private Map<Material, Long> experiencePerWood = new EnumMap<>(Material.class);
    private final LeaderboardManager leaderboardManager;

    private final WeighedList<WoodcuttingLootType> lootTypes = new WeighedList<>();
    private final ProfessionConfigLoader<?>[] lootLoaders = new ProfessionConfigLoader<?>[]{
            new WoodcuttingTreasureLoader(),
    };

    @Inject
    public WoodcuttingHandler(Progression progression, ProfessionProfileManager professionProfileManager, WoodcuttingRepository woodcuttingRepository, LeaderboardManager leaderboardManager) {
        super(progression, professionProfileManager, "Woodcutting");
        this.woodcuttingRepository = woodcuttingRepository;
        this.leaderboardManager = leaderboardManager;
    }

    /**
     * @param material The (type of) wood material that was mined by the player
     * @return The experience gained from mining said wood material
     */
    public long getExperienceFor(Material material) {
        return experiencePerWood.getOrDefault(material, 0L);
    }

    public boolean didPlayerPlaceBlock(Block block) {
        return UtilBlock.getPersistentDataContainer(block).has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
    }

    /**
     * This handles all the experience gaining and logging that happens when a
     * player chops a log (`block`)
     * @param experienceModifier represents a higher order function that modifies
     *                           the experience gained by the player here.
     */
    public void attemptToChopLog(Player player, Material originalBlockType, Block block, DoubleUnaryOperator experienceModifier,
                                 int amountChopped, int additionalLogsDropped) {
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) {
            return;
        }

        long experience = getExperienceFor(originalBlockType);
        if (experience <= 0) {
            return;
        }

        final double finalExperience = experienceModifier.applyAsDouble(experience) * amountChopped;

        if (didPlayerPlaceBlock(block)) {
            professionData.grantExperience(0, player);
            return;
        }

        professionData.grantExperience(finalExperience, player);
        woodcuttingRepository.saveChoppedLog(player.getUniqueId(), block.getType(), player.getLocation());

        log.info("{} chopped {} for {} experience", player.getName(), originalBlockType, finalExperience)
                .addClientContext(player).addBlockContext(block).addLocationContext(block.getLocation())
                .addContext("Experience", finalExperience + "").submit();

        long logsChopped = (long) professionData.getProperties().getOrDefault("TOTAL_LOGS_CHOPPED", 0L);
        professionData.getProperties().put("TOTAL_LOGS_CHOPPED", logsChopped + ((long) amountChopped));

        // Checking if >0 is probably not necessary, but it's here for clarity
        if (additionalLogsDropped > 0) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalBlockType, additionalLogsDropped));
        }

        leaderboardManager.getObject("Total Logs Chopped").ifPresent(leaderboard -> {
            TotalLogsChoppedLeaderboard totalLogsChoppedLeaderboard = (TotalLogsChoppedLeaderboard) leaderboard;

            // the purpose of this line is increment the value on the leaderboard
            totalLogsChoppedLeaderboard.add(player.getUniqueId(), ((long) amountChopped)).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to add chopped logs to leaderboard for player " + player.getName(), throwable).submit();
                }

                totalLogsChoppedLeaderboard.attemptAnnounce(player, result);
            });
        });
    }

    @Override
    public String getName() {
        return "Woodcutting";
    }


    private void loadLootTypes(Reflections reflections, ExtendedYamlConfiguration config) {
        Set<Class<? extends WoodcuttingLootType>> classes = reflections.getSubTypesOf(WoodcuttingLootType.class);
        classes.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        classes.removeIf(clazz -> clazz.isAnnotationPresent(Deprecated.class));

        for (var clazz : classes) {
            log.info("Class " + clazz.getName()).submit();
            WoodcuttingLootType type = progression.getInjector().getInstance(clazz);
            progression.getInjector().injectMembers(type);

            type.loadConfig(config);
            lootTypes.add(type.getFrequency(), 1, type);
        }


        ConfigurationSection woodcuttingLootSection = config.getConfigurationSection("woodcutting.loot");
        if (woodcuttingLootSection == null) {
            woodcuttingLootSection = config.createSection("woodcutting.loot");
        }

        for (String key : woodcuttingLootSection.getKeys(false)) {
            final ConfigurationSection section = woodcuttingLootSection.getConfigurationSection(key);
            final String type = Objects.requireNonNull(section).getString("type");

            boolean found = false;
            for (ProfessionConfigLoader<?> loader : lootLoaders) {
                if (loader.getTypeKey().equalsIgnoreCase(type)) {
                    final WoodcuttingLootType loaded = (WoodcuttingLootType) loader.read(section);
                    loaded.loadConfig(config);
                    lootTypes.add(loaded.getFrequency(), 1, loaded);
                    found = true;
                }
            }

            if (!found) {
                throw new IllegalArgumentException("Unknown woodcutting loot type: " + type);
            }
        }
        log.info("Loaded " + lootTypes.size() + " loot types for woodcutting").submit();
    }

    public void loadConfig() {
        super.loadConfig();

        // not entirely sure if this line is necessary
        experiencePerWood = new EnumMap<>(Material.class);
        var config = progression.getConfig();

        ConfigurationSection experienceSection = config.getConfigurationSection("woodcutting.experiencePerWood");
        if (experienceSection == null) {
            experienceSection = config.createSection("woodcutting.experiencePerWood");
        }

        for (String key : experienceSection.getKeys(false)) {

            Material woodLogMaterial = Material.getMaterial(key.toUpperCase());
            if (woodLogMaterial == null) continue;

            long experienceGiven = config.getLong("woodcutting.experiencePerWood." + key);
            experiencePerWood.put(woodLogMaterial, experienceGiven);
        }

        log.info("Loaded " + experiencePerWood.size() + " woodcutting blocks").submit();


        lootTypes.clear();
        Reflections classScan = new Reflections(this.getClass().getPackageName());
        loadLootTypes(classScan, progression.getConfig());
    }
}
