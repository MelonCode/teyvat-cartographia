package net.tapacywka.teyvatcartographia.shared;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;
import net.tapacywka.teyvatcartographia.marker.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server side of the shared interactive pins: kept in the world ({@code data/teyvatcartographia_pins.dat} of the
 * overworld), sent to every player with the mod on joining and after each change. Only operators (permission level 2,
 * what /tp needs) may change them, checked here for every request; only interactive pin types are taken.
 */
public final class SharedPinsServer {
    /** Shared pins per world, apart from each player's own (local) pins. */
    public static final int MAX_SHARED = 200;
    private static final int NAME_MAX = 48;
    private static final String FILE = TeyvatCartographia.MOD_ID + "_pins";

    /** The saved list. */
    private static final class Data extends SavedData {
        final List<Marker> pins = new ArrayList<>();

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            Tag list = tag.get("pins");
            if (list != null) {
                Marker.CODEC.listOf().parse(NbtOps.INSTANCE, list)
                        .resultOrPartial(e -> TeyvatCartographia.LOGGER.error("Problem in the shared pins: {}", e))
                        .ifPresent(d.pins::addAll);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            Marker.CODEC.listOf().encodeStart(NbtOps.INSTANCE, pins)
                    .resultOrPartial(e -> TeyvatCartographia.LOGGER.error("Could not save the shared pins: {}", e))
                    .ifPresent(t -> tag.put("pins", t));
            return tag;
        }
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), FILE);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) send(p, data(p.server).pins);
    }

    private static void send(ServerPlayer p, List<Marker> pins) {
        if (p.connection.hasChannel(SharedPinsNetwork.Pins.TYPE)) PacketDistributor.sendToPlayer(p, new SharedPinsNetwork.Pins(List.copyOf(pins)));
    }

    /** An operator's request: pins to add or change, pins to remove. Anything else is ignored. */
    static void edit(ServerPlayer player, SharedPinsNetwork.Edit edit) {
        if (!player.hasPermissions(2)) {
            TeyvatCartographia.LOGGER.warn("{} tried to change the shared pins without operator rights", player.getGameProfile().getName());
            return;
        }
        Data d = data(player.server);
        if (!apply(d.pins, edit)) return;
        d.setDirty();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) send(p, d.pins);
    }

    /**
     * An operator's edit applied to the shared pins: interactive pins only, names trimmed (at most NAME_MAX, not
     * empty), a change keeps the pin's place (only the name and the type), new pins up to MAX_SHARED and not a
     * repeat of one there (same dimension, place and name). Returns whether anything changed.
     */
    static boolean apply(List<Marker> pins, SharedPinsNetwork.Edit edit) {
        boolean changed = false;
        for (Marker in : edit.put()) {
            if (!in.type.operatorOnly()) continue; // shared pins are interactive pins only
            String name = in.name.trim();
            if (name.length() > NAME_MAX) name = name.substring(0, NAME_MAX);
            if (name.isEmpty()) continue;
            Marker old = find(pins, in.id);
            if (old != null) {
                // A change keeps the place: only the name and the type are editable.
                if (!old.name.equals(name) || old.type != in.type) {
                    old.name = name;
                    old.type = in.type;
                    changed = true;
                }
            } else if (pins.size() < MAX_SHARED && !repeat(pins, in, name)) {
                Marker m = new Marker(in.id, name, in.type, in.dimension, in.x, in.y, in.z, in.created, false);
                m.band = in.band;
                pins.add(m);
                changed = true;
            }
        }
        for (UUID id : edit.remove()) changed |= pins.removeIf(m -> m.id.equals(id));
        return changed;
    }

    private static Marker find(List<Marker> pins, UUID id) {
        for (Marker m : pins) if (m.id.equals(id)) return m;
        return null;
    }

    /** The same place and name as a pin already there (an upload done twice, a repeat in an import). */
    private static boolean repeat(List<Marker> pins, Marker in, String name) {
        for (Marker m : pins) {
            if (m.dimension == in.dimension && m.x == in.x && m.y == in.y && m.z == in.z && m.name.equals(name)) return true;
        }
        return false;
    }
}
