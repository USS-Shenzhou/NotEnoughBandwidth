package cn.ussshenzhou.notenoughbandwidth.indextype;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ConfigurationPacketTypes;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.handshake.HandshakePacketTypes;
import net.minecraft.network.protocol.login.LoginPacketTypes;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import net.minecraft.network.protocol.status.StatusPacketTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Instead of vanilla {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload#codec(CustomPacketPayload.FallbackProvider, List, ConnectionProtocol, PacketFlow)},
 * we here use such protocol to avoid putting a huge Identifier into bytebuf.
 * <p>
 * There is no more 8-bits header after 26.1-1.
 *
 * <h4>Indexed packet type</h4>
 * <pre>
 * - If not indexed:
 *
 *   ┌------------- N bytes ----------------
 *   │   Identifier (packet type) in UTF-8
 *   └--------------------------------------
 *
 *   When decoding, just try whether we can get a valid Identifier to determine indexed or not.
 *
 * - If indexed:
 *
 *   ┌--------- X byte ----------┬--------- Y byte ---------┐
 *   │   namespace-id (var int)  │     path-id (var int)    │
 *   └---------------------------┴--------------------------┘
 *
 * </pre>
 *
 * <h4>Then packet data.</h4>
 *
 * @author USS_Shenzhou, nutant233
 */
public class CustomPacketPrefixHelper {

    public static void write(Identifier type, FriendlyByteBuf buf) {
        if (NotEnoughBandwidthConfig.skipType(type.toString())) {
            buf.writeIdentifier(type);
        } else if (NamespaceIndexManager.contains(type)) {
            var index = NamespaceIndexManager.getCheckedIndex(type);
            buf.writeVarInt(index.getA());
            buf.writeVarInt(index.getB());
        } else {
            buf.writeIdentifier(type);
        }
    }

    @Nullable
    public static Identifier read(FriendlyByteBuf buf) {
        try {
            var tryRead = new FriendlyByteBuf(buf.retainedDuplicate());
            var tryType = tryRead.readIdentifier();
            if (valid(tryType)) {
                return buf.readIdentifier();
            }
        } catch (Exception ignored) {
        }
        return NamespaceIndexManager.getIdentifier(buf.readVarInt(), buf.readVarInt());
    }

    private static final HashSet<String> VALID_PACKETS = new HashSet<>();

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    private static void getAllValidPackets() {
        ((Map<ConnectionProtocol, Map<Identifier, PayloadRegistration<?>>>) NamespaceIndexManager.PAYLOAD_REGISTRATIONS.get())
                .forEach((_, payloads) -> payloads.keySet().forEach(i -> VALID_PACKETS.add(i.toString())));
        ((Map<Identifier, StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload>>) NamespaceIndexManager.BUILTIN_PAYLOADS.get())
                .forEach((identifier, _) -> VALID_PACKETS.add(identifier.toString()));
        getFromFields(GamePacketTypes.class);
        getFromFields(StatusPacketTypes.class);
        getFromFields(PingPacketTypes.class);
        getFromFields(CookiePacketTypes.class);
        getFromFields(CommonPacketTypes.class);
        getFromFields(LoginPacketTypes.class);
        getFromFields(HandshakePacketTypes.class);
        getFromFields(ConfigurationPacketTypes.class);
    }

    private static void getFromFields(Class<?> clazz) {
        for (var field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || !PacketType.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                var value = (PacketType<?>) field.get(null);
                VALID_PACKETS.add(value.id().toString());
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    private static boolean valid(Identifier type) {
        if (VALID_PACKETS.isEmpty()) {
            getAllValidPackets();
        }
        return VALID_PACKETS.contains(type.toString());
    }
}
