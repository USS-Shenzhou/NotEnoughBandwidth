package cn.ussshenzhou.notenoughbandwidth.aggregation;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

    static {
        VANILLA_TO_ID.defaultReturnValue(-1);
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
        IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec = (IdDispatchCodec) protocolInfo.codec();
        updateVanillaIdMap(vanillaCodec);
        if (handleVanilla(context, vanillaCodec)) {
            return;
        }
        handleCustom(context, vanillaCodec);
    }

    private boolean handleVanilla(IPayloadContext context, IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec) {
        var id = VANILLA_TO_ID.getInt(type);
        if (id == -1) {
            return false;
        }
        var entry = vanillaCodec.byId.get(id);
        var codec = (StreamCodec<ByteBuf, Packet<?>>) entry.serializer();
        var truePacket = (Packet<ICommonPacketListener>) codec.decode(data);
        context.enqueueWork(() -> truePacket.handle(context.listener()));
        return true;
    }

    private void handleCustom(IPayloadContext context, IdDispatchCodec<ByteBuf, Packet<?>, PacketType> vanillaCodec) {
        var codec = (StreamCodec<ByteBuf, CustomPacketPayload>) NetworkRegistry.getCodec(type, ConnectionProtocol.PLAY, context.flow());
        if (codec == null) {
            LogUtils.getLogger().error("Skipped: Failed to handle packet " + type + ", failed to find a codec for it.");
            return;
        }
        try {
            var truePacket = codec.decode(data);
            if (context.listener() instanceof ServerCommonPacketListener listener) {
                listener.handleCustomPayload(new ServerboundCustomPayloadPacket(truePacket));
            } else if (context.listener() instanceof ClientCommonPacketListener listener) {
                listener.handleCustomPayload(new ClientboundCustomPayloadPacket(truePacket));
            }
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
