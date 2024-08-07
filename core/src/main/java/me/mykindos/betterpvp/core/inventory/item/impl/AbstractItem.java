package me.mykindos.betterpvp.core.inventory.item.impl;

import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.window.AbstractWindow;
import me.mykindos.betterpvp.core.inventory.window.Window;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract implementation of the {@link Item} interface.
 */
public abstract class AbstractItem implements Item {
    
    private final Set<AbstractWindow> windows = new HashSet<>();
    
    @Override
    public void addWindow(AbstractWindow window) {
        windows.add(window);
    }
    
    @Override
    public void removeWindow(AbstractWindow window) {
        windows.remove(window);
    }
    
    @Override
    public Set<Window> getWindows() {
        return Collections.unmodifiableSet(windows);
    }
    
    @Override
    public void notifyWindows() {
        windows.forEach(w -> w.handleItemProviderUpdate(this));
    }
    
}
