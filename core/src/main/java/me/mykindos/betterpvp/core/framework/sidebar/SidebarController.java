package me.mykindos.betterpvp.core.framework.sidebar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

@BPvPListener
@Singleton
@CustomLog
public class SidebarController implements Listener {

    private final @NotNull ClientManager clientManager;
    private final @NotNull Core core;
    @Getter
    private @NotNull Function<Gamer, Sidebar> defaultProvider = gamer -> null;

    @Inject
    private SidebarController(@NotNull ClientManager clientManager, @NotNull Core core) {
        this.clientManager = clientManager;
        this.core = core;
    }

    public void setDefaultProvider(@NotNull Function<@NotNull Gamer, @Nullable Sidebar> provider) {
        this.defaultProvider = provider;
    }

    public void resetSidebar(@NotNull Gamer gamer) {
        gamer.setSidebar(this.defaultProvider.apply(gamer));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        resetSidebar(this.clientManager.search().online(event.getPlayer()).getGamer());
    }

    // Client property because it's a global setting, but the sidebar is attached to the gamer
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSave(ClientPropertyUpdateEvent event) {
        if (!event.getProperty().equals(ClientProperty.SIDEBAR_ENABLED.name())) {
            return; // Skip if not the sidebar property
        }

        final Client client = event.getClient();
        final Gamer gamer = client.getGamer();
        final Sidebar sidebar = gamer.getSidebar();
        if (sidebar == null || !gamer.isOnline()) {
            return; // Skip if the gamer is not online
        }

        final Player player = Objects.requireNonNull(gamer.getPlayer());
        if ((boolean) event.getValue()) {
            UtilServer.runTaskAsync(core, () -> sidebar.addPlayer(player));
        } else {
            UtilServer.runTaskAsync(core, () -> sidebar.removePlayer(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Gamer gamer = this.clientManager.search().online(event.getPlayer()).getGamer();
        final Sidebar sidebar = gamer.getSidebar();
        if (sidebar != null) {
            sidebar.close();
        }
    }

}
