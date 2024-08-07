package me.mykindos.betterpvp.core.inventory.window;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.InventoryAccess;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.abstraction.inventory.CartographyInventory;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.ComponentWrapper;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.map.MapIcon;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.map.MapPatch;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.inventory.util.MathUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An {@link me.mykindos.betterpvp.core.inventory.window.AbstractSplitWindow} that uses a {@link CartographyInventory} as the upper inventory
 * and the player inventory as the lower inventory.
 * <p>
 * Use the builder obtained by {@link me.mykindos.betterpvp.core.inventory.window.CartographyWindow#split()}, to get an instance of this class.
 */
final class CartographySplitWindowImpl extends AbstractSplitWindow implements CartographyWindow {
    
    private final CartographyInventory cartographyInventory;
    private int mapId;
    
    public CartographySplitWindowImpl(
        @NotNull Player player,
        @Nullable ComponentWrapper title,
        @NotNull AbstractGui upperGui,
        @NotNull AbstractGui lowerGui,
        boolean closeable
    ) {
        super(player, title, createWrappingGui(upperGui), lowerGui, null, closeable);
        
        cartographyInventory = InventoryAccess.createCartographyInventory(player, title != null ? title.localized(player) : null);
        upperInventory = cartographyInventory.getBukkitInventory();
        
        resetMap();
    }
    
    private static AbstractGui createWrappingGui(Gui upperGui) {
        if (upperGui.getWidth() != 2 || upperGui.getHeight() != 1)
            throw new IllegalArgumentException("Gui has to be 2x1");
        
        Gui wrapperGui = Gui.empty(3, 1);
        wrapperGui.fillRectangle(1, 0, upperGui, true);
        return (AbstractGui) wrapperGui;
    }
    
    @Override
    public void updateMap(@Nullable MapPatch patch, @Nullable List<MapIcon> icons) {
        InventoryAccess.getPlayerUtils().sendMapUpdate(getViewer(), mapId, (byte) 0, false, patch, icons);
    }
    
    @Override
    @SuppressWarnings({"deprecation", "DuplicatedCode"})
    public void resetMap() {
        mapId = -MathUtils.RANDOM.nextInt(Integer.MAX_VALUE);
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) map.getItemMeta();
        mapMeta.setMapId(mapId);
        map.setItemMeta(mapMeta);
        getGuis()[0].setItem(0, new SimpleItem(map));
    }
    
    @Override
    protected void openInventory(@NotNull Player viewer) {
        cartographyInventory.open();
    }
    
    public static final class BuilderImpl
        extends AbstractBuilder<me.mykindos.betterpvp.core.inventory.window.CartographyWindow, me.mykindos.betterpvp.core.inventory.window.CartographyWindow.Builder.Split>
        implements me.mykindos.betterpvp.core.inventory.window.CartographyWindow.Builder.Split
    {
        
        @Override
        public @NotNull CartographyWindow build(Player viewer) {
            if (viewer == null)
                throw new IllegalStateException("Viewer is not defined.");
            if (upperGuiSupplier == null)
                throw new IllegalStateException("Upper Gui is not defined.");
            
            var window = new CartographySplitWindowImpl(
                viewer,
                title,
                (AbstractGui) upperGuiSupplier.get(),
                (AbstractGui) lowerGuiSupplier.get(),
                closeable
            );
            
            applyModifiers(window);
            
            return window;
        }
        
    }
    
}
