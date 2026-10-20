package net.tapacywka.teyvatcartographia.marker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;
import net.tapacywka.teyvatcartographia.shared.SharedPinsNetwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Client side of the shared interactive pins: the list the server sends ({@code SharedPinsServer}), and the requests
 * an operator sends it. Only when the server has the mod ({@link #available}); {@link MarkerStore} shows these pins
 * with the player's own ones and sends changes of shared pins here. Main thread only.
 */
public final class SharedPins {
    private SharedPins() {}

    private static final List<Marker> pins = new ArrayList<>();
    /** True once the server sent its list in this world. */
    private static boolean received;
    /** Local interactive pins sent up by the one-time offer: removed locally once the server lists them. */
    private static final Set<UUID> uploading = new HashSet<>();
    /** The one-time offer was looked at in this world (shown, or not needed). */
    private static boolean offerChecked;

    /** True if the server keeps shared pins (it has the mod: its channel is there); false without a game (unit tests). */
    public static boolean available() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener c = mc == null ? null : mc.getConnection();
        return c != null && c.hasChannel(SharedPinsNetwork.Pins.TYPE);
    }

    /** True while the server has the mod but its list has not come yet (pins of it may be tracked meanwhile). */
    public static boolean pending() {
        return !received && available();
    }

    static List<Marker> pins() {
        return pins;
    }

    public static int count() {
        return pins.size();
    }

    /** The whole list from the server (on joining and after every change). */
    public static void receive(List<Marker> in) {
        pins.clear();
        for (Marker m : in) {
            m.shared = true;
            pins.add(m);
        }
        received = true;
        if (!uploading.isEmpty()) MarkerStore.dropUploaded(uploading, pins);
        MarkerStore.sharedChanged();
    }

    /** Leaving the world or the server. */
    static void reset() {
        pins.clear();
        received = false;
        uploading.clear();
        offerChecked = false;
    }

    /** Adds or changes a shared pin (the server checks the rights and the limit, then sends the new list). */
    static void put(Marker m) {
        PacketDistributor.sendToServer(new SharedPinsNetwork.Edit(List.of(m), List.of()));
    }

    static void putAll(List<Marker> list) {
        if (!list.isEmpty()) PacketDistributor.sendToServer(new SharedPinsNetwork.Edit(List.copyOf(list), List.of()));
    }

    static void remove(Marker m) {
        PacketDistributor.sendToServer(new SharedPinsNetwork.Edit(List.of(), List.of(m.id)));
    }

    /**
     * Once per world, for an operator on a server with shared pins who still has interactive pins of their own there
     * (made before the server had the mod): asks whether to move them to the shared ones. Called every client tick;
     * waits until the list came and no screen is open.
     */
    public static void tickOffer() {
        if (offerChecked || !received) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !MarkerType.isOperator()) return;
        offerChecked = true;
        Path folder = MarkerStore.worldFolder();
        List<Marker> local = MarkerStore.localInteractive();
        if (folder == null || local.isEmpty() || Files.exists(folder.resolve(OFFER_FILE))) return;
        mc.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                for (Marker m : local) uploading.add(m.id);
                putAll(local);
            }
            markOffered(folder);
            mc.setScreen(null);
        }, Component.translatable("teyvatcartographia.shared.offer.title"),
                Component.translatable("teyvatcartographia.shared.offer.message", local.size())));
    }

    /** Marks the offer as answered in this world's folder, so it is not asked again. */
    private static final String OFFER_FILE = "shared_offer_answered";

    private static void markOffered(Path folder) {
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(OFFER_FILE), "The offer to move the interactive pins to the shared ones was answered.\n");
        } catch (IOException e) {
            TeyvatCartographia.LOGGER.error("Could not save the answer to the shared pins offer", e);
        }
    }
}
