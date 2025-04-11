package me.mykindos.betterpvp.champions.champions.builds.menus.buttons;

import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.combat.armour.ArmourManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.FlashingButton;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleShowPassiveButton extends FlashingButton<ClassSelectionMenu> {

    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;
    private final ArmourManager armourManager;

    private boolean currentlyShowingPassives;

    public ToggleShowPassiveButton(BuildManager buildManager, ChampionsSkillManager skillManager, ArmourManager armourManager,
                                   boolean currentlyShowingPassives) {
        super();
        this.buildManager = buildManager;
        this.skillManager = skillManager;
        this.armourManager = armourManager;
        this.currentlyShowingPassives = currentlyShowingPassives;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new ClassSelectionMenu(buildManager, skillManager, armourManager, null, !currentlyShowingPassives).show(player);
        SoundEffect.HIGH_PITCH_PLING.play(player);
    }

    @Override
    public ItemProvider getItemProvider(ClassSelectionMenu gui) {
        Material buttonMaterial = (currentlyShowingPassives) ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK;
        String displayName = (currentlyShowingPassives) ? "Hide Passive Descriptions" : "Show Passive Descriptions";
        NamedTextColor textColor = (currentlyShowingPassives) ? NamedTextColor.RED : NamedTextColor.GREEN;

        return ItemView.builder().material(buttonMaterial)
                .displayName(Component.text(displayName, textColor, TextDecoration.BOLD))
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .glow(currentlyShowingPassives)
                .build();
    }
}
