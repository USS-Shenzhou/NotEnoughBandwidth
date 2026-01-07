package cn.ussshenzhou.notenoughbandwidth.aggregation;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings({"UnstableApiUsage", "rawtypes", "unchecked"})
public class AggregatedDecodePacket {
    private final Identifier type;
    private final ByteBuf data;
    private static final Object2IntArrayMap<Identifier> VANILLA_TO_ID = new Object2IntArrayMap<>();
    private static VarHandle SERVERBOUND_HANDLERS, CLIENTBOUND_HANDLERS;

    static {
        VANILLA_TO_ID.defaultReturnValue(-1);
        try {
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(NetworkRegistry.class, MethodHandles.lookup());
            SERVERBOUND_HANDLERS = privateLookup.findStaticVarHandle(NetworkRegistry.class, "SERVERBOUND_HANDLERS", Map.class);
            CLIENTBOUND_HANDLERS = privateLookup.findStaticVarHandle(NetworkRegistry.class, "CLIENTBOUND_HANDLERS", Map.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public AggregatedDecodePacket(Identifier type, ByteBuf data) {
        this.type = type;
        this.data = data;
    }

    /**
     * @see IdDispatchCodec
     * @see net.neoforged.neoforge.network.registration.NetworkRegistry#isModdedPayload(CustomPacketPayload)
     */
    public void handle(ProtocolInfo<?> protocolInfo, IPayloadContext context) {
        //var packet = (Packet<ICommonPacketListener>) protocolInfo.codec().decode(data);
        //packet.handle(listener);
        //data.release();
        IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec = (IdDispatchCodec) protocolInfo.codec();
        updateVanillaIdMap(vanillaCodec);
        if ("minecraft".equals(type.getNamespace())) {
            handleVanilla(context, vanillaCodec);
        } else {
            handleCustom(context, vanillaCodec);
        }
    }

    private void handleVanilla(IPayloadContext context, IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec) {
        var id = VANILLA_TO_ID.getInt(type);
        if (id == -1) {
            LogUtils.getLogger().error("Skipped: Failed to handle packet " + type + ", which may not be a vanilla packet but has a minecraft namespace.");
            return;
        }
        var entry = vanillaCodec.byId.get(id);
        var codec = (StreamCodec<ByteBuf, Packet<?>>) entry.serializer();
        var truePacket = (Packet<ICommonPacketListener>) codec.decode(data);
        context.enqueueWork(() -> truePacket.handle(context.listener()));
    }

    private void handleCustom(IPayloadContext context, IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec) {
        var codec = (StreamCodec<ByteBuf, CustomPacketPayload>) NetworkRegistry.getCodec(type, ConnectionProtocol.PLAY, context.flow());
        if (codec == null) {
            LogUtils.getLogger().error("Skipped: Failed to handle packet " + type + ", failed to find a codec for it.");
            return;
        }
        try {
            var truePacket = codec.decode(data);
            var handlers = (Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>>) (context.flow() == PacketFlow.CLIENTBOUND ? CLIENTBOUND_HANDLERS.get() : SERVERBOUND_HANDLERS.get());
            var handler = (IPayloadHandler<CustomPacketPayload>) handlers.get(ConnectionProtocol.PLAY).get(type);
            handler.handle(truePacket, context);
        } catch (Exception e) {
            LogUtils.getLogger().error("Skipped: Failed to handle packet " + type, e);
        }
    }

    private void updateVanillaIdMap(IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec) {
        if (vanillaCodec.toId.size() == VANILLA_TO_ID.size()) {
            return;
        }
        VANILLA_TO_ID.clear();
        vanillaCodec.toId.forEach((t, i) -> VANILLA_TO_ID.put(t.id(), i));
    }

    public ByteBuf getData() {
        return data;
    }
}
