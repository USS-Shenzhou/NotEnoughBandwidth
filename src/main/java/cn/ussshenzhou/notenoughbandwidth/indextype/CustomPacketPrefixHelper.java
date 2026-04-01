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
 * <h4>Fixed 8 bits header</h4>
 * <pre>
 * ┌------------- 1 byte (8 bits) ---------------┐
 * │               function flags                │
 * ├---┬---┬-------------------------------------┤
 * │ i │ t │      reserved (6 bits)              │
 * └---┴---┴-------------------------------------┘
 *
 * i = indexed (1 bit)
 * t = tight_indexed (1 bit, only valid if i=1)
 * reserved = 6 bits (for future use)
 *
 * </pre>
 *
 * <h4>Indexed packet type</h4>
 * <pre>
 * - If i=0 (not indexed):
 *
 *   ┌---------------- N bytes ----------------
 *   │ Identifier (packet type) in UTF-8
 *   └-----------------------------------------
 *
 * - If i=1 and t=0 (indexed, NOT tight):
 *
 *   ┌-------- 1 byte ---------┬-------- 1 byte --------┬-------- 1 byte --------┐
 *   ┌------------- 12 bits ---------------┬-------------- 12 bits --------------┐
 *   │    namespace-id (capacity 4096)     │       path-id (capacity 4096)       │
 *   └-------------------------------------┴-------------------------------------┘
 *
 * - If i=1 and t=1 (indexed, tight):
 *
 *   ┌--------- 1 byte ----------┬--------- 1 byte ---------┐
 *   ┌--------- 8 bits ----------┬--------- 8 bits ---------┐
 *   │namespace-id (capacity 256)│  path-id (capacity 256)  │
 *   └---------------------------┴--------------------------┘
 *
 * </pre>
 *
 * <h4>Then packet data.</h4>
 *
 * @author USS_Shenzhou
 */
public class CustomPacketPrefixHelper {

    public static void writeType(Identifier type, FriendlyByteBuf buf) {
        var index = NamespaceIndexManager.getPathIndex(type.getNamespace());
        if (index == null) {
            buf.writeByte(0);
            buf.writeIdentifier(type);
        } else {
            VarInt.write(buf, index.namespaceIndex);
            VarInt.write(buf, index.get(type.getPath()));
        }
    }

    @Nullable
    public static Identifier readType(FriendlyByteBuf buf) {
        byte b = buf.readByte();
        if (b == 0) {
            return buf.readIdentifier();
        } else {
            return NamespaceIndexManager.getIdentifier(read(b, buf),  VarInt.read(buf));
        }
    }

    private static int read(byte in, FriendlyByteBuf buf) {
        int out = 0;
        int bytes = 0;
        while (true) {
            out |= (in & 127) << bytes * 7;
            if ((in & 128) == 128) {
                if (bytes++ > 4) {
                    throw new RuntimeException("VarInt too big");
                }
                in = buf.readByte();
            } else {
                break;
            }
        }
        return out;
    }
}
