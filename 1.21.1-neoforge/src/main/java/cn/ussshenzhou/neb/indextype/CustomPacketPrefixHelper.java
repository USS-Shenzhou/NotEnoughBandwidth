package cn.ussshenzhou.neb.indextype;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Instead of vanilla {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload#codec(CustomPacketPayload.FallbackProvider, List, ConnectionProtocol, PacketFlow)},
 * we here use such protocol to avoid putting a huge ResourceLocation into bytebuf.
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
 *   │ ResourceLocation (packet type) in UTF-8
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
    private static final ThreadLocal<CustomPacketPrefixHelper> INSTANCES = ThreadLocal.withInitial(CustomPacketPrefixHelper::new);

    private int prefix = 0;
    private ResourceLocation type = null;

    private CustomPacketPrefixHelper() {
    }

    public static CustomPacketPrefixHelper get() {
        var instance = INSTANCES.get();
        instance.prefix = 0;
        instance.type = null;
        return instance;
    }

    public CustomPacketPrefixHelper index(ResourceLocation type) {
        int index = NamespaceIndexManager.getNebIndex(type);
        if (index == 0) {
            this.type = type;
            return this;
        }
        this.type = type;
        prefix |= index;
        return this;
    }

    public void save(FriendlyByteBuf buf) {
        if (prefix >>> 31 == 0) {
            buf.writeByte(prefix >>> 24);
            buf.writeResourceLocation(type);
        }
        if (prefix >>> 31 == 1) {
            if ((prefix >>> 30 & 1) == 1) {
                buf.writeMedium(prefix >>> 8);
            } else {
                buf.writeInt(prefix);
            }
        }
    }

    @Nullable
    public static ResourceLocation getType(FriendlyByteBuf buf) {
        int fixed = buf.readUnsignedByte() & 0xff;
        if (fixed >>> 7 == 0) {
            return buf.readResourceLocation();
        } else {
            if (fixed >>> 6 == 0) {
                return NamespaceIndexManager.getResourceLocation(buf.readUnsignedMedium(), false);
            } else {
                return NamespaceIndexManager.getResourceLocation(buf.readUnsignedShort(), true);
            }
        }
    }
}
