package net.tapacywka.teyvatcartographia.shared;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tapacywka.teyvatcartographia.TeyvatCartographia;
import net.tapacywka.teyvatcartographia.marker.Marker;

import java.util.List;
import java.util.UUID;

/**
 * The two packets of the shared interactive pins, both optional (a side without the mod just does not have the
 * channel): the server sends every player with the mod the whole list ({@link Pins}) on joining and after each change;
 * an operator's client asks for changes ({@link Edit}), which the server checks and applies.
 */
public final class SharedPinsNetwork {
    private SharedPinsNetwork() {}

    private static final String VERSION = "1";

    /** Server to client: all shared pins of the world (every dimension). */
    public record Pins(List<Marker> pins) implements CustomPacketPayload {
        public static final Type<Pins> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeyvatCartographia.MOD_ID, "shared_pins"));
        private static final Codec<Pins> CODEC = RecordCodecBuilder.create(i -> i.group(
                Marker.CODEC.listOf().fieldOf("pins").forGetter(Pins::pins)
        ).apply(i, Pins::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pins> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

        @Override
        public Type<Pins> type() {
            return TYPE;
        }
    }

    /** Client to server: pins to add or change (by id) and pins to remove, from an operator. */
    public record Edit(List<Marker> put, List<UUID> remove) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TeyvatCartographia.MOD_ID, "edit_shared_pins"));
        private static final Codec<Edit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Marker.CODEC.listOf().optionalFieldOf("put", List.of()).forGetter(Edit::put),
                UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("remove", List.of()).forGetter(Edit::remove)
        ).apply(i, Edit::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

        @Override
        public Type<Edit> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(VERSION).optional();
        // The client handler names a client class; it only runs on the client, so a dedicated server never loads it.
        r.playToClient(Pins.TYPE, Pins.STREAM_CODEC, (p, ctx) -> net.tapacywka.teyvatcartographia.marker.SharedPins.receive(p.pins()));
        r.playToServer(Edit.TYPE, Edit.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) SharedPinsServer.edit(sp, p);
        });
    }
}
