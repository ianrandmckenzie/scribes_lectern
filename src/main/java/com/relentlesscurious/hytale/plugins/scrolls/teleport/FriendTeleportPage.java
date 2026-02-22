package com.relentlesscurious.hytale.plugins.scrolls.teleport;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import com.relentlesscurious.hytale.plugins.scrolls.config.FriendTeleportConfig;

public class FriendTeleportPage extends InteractiveCustomUIPage<FriendTeleportPage.PageData> {

    private final FriendTeleportService service;
    private final Map<UUID, Player> onlinePlayers;
    private final FriendTeleportScrollListener listener;
    private final FriendTeleportConfig config;

    public FriendTeleportPage(PlayerRef playerRef, FriendTeleportService service,
                              Map<UUID, Player> onlinePlayers,
                              FriendTeleportScrollListener listener,
                              FriendTeleportConfig config) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.service = service;
        this.onlinePlayers = onlinePlayers;
        this.listener = listener;
        this.config = config;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder,
                      UIEventBuilder eventBuilder, Store<EntityStore> store) {
        commandBuilder.append("Pages/Scribes_FriendTeleportPage.ui");
        buildPlayerList(commandBuilder, eventBuilder);

        // Cancel button — always present in the footer
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelButton",
            new EventData()
                .append("Action", "Cancel")
                .append("TargetId", "")
        );
    }

    private void buildPlayerList(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.clear("#PlayerCards");
        UUID selfId = playerRef.getUuid();

        // 1. Pending requests
        List<FriendTeleportService.TeleportRequest> pending = service.getRequestsFor(selfId);
        int count = 0;

        // --- Pending Requests Section ---
        boolean hasPending = !pending.isEmpty() || config.debugUi;
        if (hasPending) {
            commandBuilder.append("#PlayerCards", "Pages/Scribes_FriendTeleportHeader.ui");
            commandBuilder.set("#PlayerCards[" + count + "] #HeaderText.Text", "Pending Requests");
            count++;

            for (FriendTeleportService.TeleportRequest req : pending) {
                String selector = "#PlayerCards[" + count + "]";
                commandBuilder.append("#PlayerCards", "Pages/Scribes_FriendTeleportPendingEntry.ui");
                commandBuilder.set(selector + " #RequesterName.Text", req.senderName());

                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #AcceptButton",
                    new EventData()
                        .append("Action", "Accept")
                        .append("TargetId", req.from().toString())
                );
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector + " #DenyButton",
                    new EventData()
                        .append("Action", "Deny")
                        .append("TargetId", req.from().toString())
                );
                count++;
            }

            if (config.debugUi) {
                // 1b. Fake pending requests for UI testing
                for (int i = 0; i < 3; i++) {
                    String selector = "#PlayerCards[" + count + "]";
                    commandBuilder.append("#PlayerCards", "Pages/Scribes_FriendTeleportPendingEntry.ui");
                    commandBuilder.set(selector + " #RequesterName.Text", "Pending Request " + (i + 1));

                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        selector + " #AcceptButton",
                        new EventData().append("Action", "Accept").append("TargetId", UUID.randomUUID().toString())
                    );
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        selector + " #DenyButton",
                        new EventData().append("Action", "Deny").append("TargetId", UUID.randomUUID().toString())
                    );
                    count++;
                }
            }
        }

        // --- Online Players Section ---
        boolean hasOnline = !onlinePlayers.isEmpty() || config.debugUi;
        if (hasOnline) {
            commandBuilder.append("#PlayerCards", "Pages/Scribes_FriendTeleportHeader.ui");
            commandBuilder.set("#PlayerCards[" + count + "] #HeaderText.Text", "Send Request");
            count++;

            for (Player p : onlinePlayers.values()) {
                UUID pId = listener.resolvePlayerUuid(p);
                if (pId == null || pId.equals(selfId)) continue;

                String selector = "#PlayerCards[" + count + "]";
                commandBuilder.append("#PlayerCards", "Pages/Scribes_FriendTeleportEntry.ui");
                commandBuilder.set(selector + " #PlayerName.Text", listener.resolvePlayerName(p));

                // The entry root IS the Button (#ActionButton), so bind directly to the index selector
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    new EventData()
                        .append("Action", "Request")
                        .append("TargetId", pId.toString())
                );
                count++;
            }

            if (config.debugUi) {
                // 3. Fake cards for UI testing (to ensure scrolling works)
                for (int i = 0; i < 15; i++) {
                    String selector = "#PlayerCards[" + count + "]";
                    commandBuilder.append("#PlayerCards", "Pages/Scribes_FriendTeleportEntry.ui");
                    commandBuilder.set(selector + " #PlayerName.Text", "Fake Player " + (i + 1));
                    count++;
                }
            }
        }
    }

    private void updateList() {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        buildPlayerList(commandBuilder, eventBuilder);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, PageData data) {
        if (data.action == null) return;

        // Cancel — just close the menu
        if ("Cancel".equals(data.action)) {
            this.close();
            return;
        }

        if (data.targetId == null) return;

        UUID targetId;
        try {
            targetId = UUID.fromString(data.targetId);
        } catch (IllegalArgumentException e) {
            return;
        }

        Player self = onlinePlayers.get(playerRef.getUuid());
        if (self == null) return;

        if ("Accept".equals(data.action)) {
            listener.acceptRequest(self, targetId);
            this.close();
        } else if ("Deny".equals(data.action)) {
            service.clearRequest(playerRef.getUuid(), targetId);
            updateList();
        } else if ("Request".equals(data.action)) {
            Player target = onlinePlayers.get(targetId);
            if (target != null) {
                String targetName = listener.resolvePlayerName(target);
                String selfName = listener.resolvePlayerName(self);
                service.addRequest(targetId, playerRef.getUuid(), selfName);
                // Confirmation toast shown to the scroll user (AC 5)
                self.sendMessage(Message.raw("Friend teleport request sent to " + targetName));
                // Notification to the receiver
                target.sendMessage(Message.raw(selfName + " wants to teleport to you. Use your Friend Teleport scroll to accept."));
            }
            this.close();
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (data, s) -> data.action = s, data -> data.action)
            .add()
            .append(new KeyedCodec<>("TargetId", Codec.STRING), (data, s) -> data.targetId = s, data -> data.targetId)
            .add()
            .build();

        private String action;
        private String targetId;

        public String getAction() { return action; }
        public String getTargetId() { return targetId; }
    }
}
