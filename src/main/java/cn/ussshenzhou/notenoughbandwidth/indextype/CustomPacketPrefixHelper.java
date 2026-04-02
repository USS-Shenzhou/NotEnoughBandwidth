package cn.ussshenzhou.notenoughbandwidth.indextype;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;

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
 *   ┌--------- 1 byte ----------┐
 *   ┌--------- 8 bits ----------┬------------ N bytes ----------------
 *   │             0             │  Identifier (packet type) in UTF-8
 *   └---------------------------┴-------------------------------------
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
        var index = NamespaceIndexManager.getPathIndex(type.getNamespace());
        if (index == null) {
            buf.writeByte(0);
            buf.writeIdentifier(type);
        } else {
            buf.writeVarInt(index.namespaceIndex);
            buf.writeVarInt(index.get(type.getPath()));
        }
    }

    @Nullable
    public static Identifier read(FriendlyByteBuf buf) {
        byte firstByte = buf.getByte(buf.readerIndex());
        if (firstByte == 0) {
            buf.readVarInt();
            return buf.readIdentifier();
        } else {
            return NamespaceIndexManager.getIdentifier(buf.readVarInt(), buf.readVarInt());
        }
    }
}
