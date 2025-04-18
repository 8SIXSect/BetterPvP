package me.mykindos.betterpvp.core.chat.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


/**
 * Useful for filtering out messages that have been ignored, or the target has chat disabled
 */
@Getter
@Setter
public class ChatReceivedEvent extends CustomCancellableEvent {

    private final Player player;
    private final Client client;
    private final Player target;
    private final ChatChannel channel;
    private boolean cancelled;
    private String cancelReason;
    private Component message;
    private Component prefix;

    public ChatReceivedEvent(Player player, Client client, Player target, ChatChannel channel, Component prefix, Component message) {
        super(true);
        this.player = player;
        this.client = client;
        this.target = target;
        this.channel = channel;
        this.prefix = prefix;
        this.message = message;
        this.cancelReason = "";
    }

}