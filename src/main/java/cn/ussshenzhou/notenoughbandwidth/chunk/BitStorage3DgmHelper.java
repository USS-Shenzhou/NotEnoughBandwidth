package cn.ussshenzhou.notenoughbandwidth.chunk;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SimpleBitStorage;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * @author USS_Shenzhou, Argon4W
 */
public class BitStorage3DgmHelper {
    private static final int SIZE_X = 16;
    private static final int SIZE_Y = 16;
    private static final int SIZE_Z = 16;
    private static final int SIZE_XZ = SIZE_X * SIZE_Z;
    private static final int SIZE_XYZ = SIZE_X * SIZE_Y * SIZE_Z;
    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    public static void write(FriendlyByteBuf buf, SimpleBitStorage storage) {
        var context = CONTEXT.get();
        context.reset();
        storage.unpack(context.ids);
        for (var i = 0; i < SIZE_XYZ; i++) {
            var x = context.cuboidsWritePointer % SIZE_X;
            var y = context.cuboidsWritePointer / SIZE_XZ;
            var z = (context.cuboidsWritePointer / SIZE_X) % SIZE_Z;

            var self = context.cuboids[i];
            self.reset(context.ids[i]);
            var candidate = context.get(x - 1, y, z);
            if (candidate != null && candidate.valid && candidate.value == self.value && candidate.lengthY == 1 && candidate.lengthZ == 1) {
                self.from(candidate);
                self.lengthX++;
                candidate.valid = false;
            }
            candidate = context.get(x, y, z - 1);
            if (candidate != null && candidate.valid && candidate.value == self.value && candidate.lengthX == self.lengthX && candidate.lengthY == 1) {
                self.from(candidate);
                self.lengthZ++;
                candidate.valid = false;
            }
            candidate = context.get(x, y - 1, z);
            if (candidate != null && candidate.valid && candidate.value == self.value && candidate.lengthX == self.lengthX && candidate.lengthZ == self.lengthZ) {
                self.from(candidate);
                self.lengthY++;
                candidate.valid = false;
            }
            context.cuboidsWritePointer++;
        }

        for (var i = 0; i < context.cuboidsWritePointer; i++) {
            var x = i % SIZE_X;
            var y = i / SIZE_XZ;
            var z = (i / SIZE_X) % SIZE_Z;
            var cuboid = context.cuboids[i];
            if (!cuboid.valid) {
                continue;
            }
            int fromX = x - (cuboid.lengthX - 1);
            int fromY = y - (cuboid.lengthY - 1);
            int fromZ = z - (cuboid.lengthZ - 1);
            if (cuboid.lengthX * cuboid.lengthY * cuboid.lengthZ > 8) {
                context.compressedBuf.writeMedium(
                        fromX << 20 | fromY << 16 | fromZ << 12 | (cuboid.lengthX - 1) << 8 | (cuboid.lengthY - 1) << 4 | (cuboid.lengthZ - 1)
                );
                context.compressedBuf.writeVarInt(cuboid.value);
                for (int dy = fromY; dy <= y; dy++) {
                    for (int dz = fromZ; dz <= z; dz++) {
                        for (int dx = fromX; dx <= x; dx++) {
                            context.compressed[getIndex(dx, dy, dz)] = true;
                        }
                    }
                }
            }
        }
        long mask = (1L << storage.bits) - 1;
        for (int i = 0; i < SIZE_XYZ; i++) {
            if (context.compressed[i]) {
                continue;
            }
            context.currentLong |= (context.ids[i] & mask) << context.bitOffset;
            context.bitOffset += storage.bits;
            if (context.bitOffset >= 64) {
                context.plainBuf.writeLong(context.currentLong);
                context.bitOffset -= 64;
                context.currentLong = context.bitOffset > 0 ? (context.ids[i] & mask) >>> (storage.bits - context.bitOffset) : 0;
            }
        }
        if (context.bitOffset > 0) {
            context.plainBuf.writeLong(context.currentLong);
        }

        buf.writeVarInt(context.compressedBuf.readableBytes());
        buf.writeBytes(context.compressedBuf);
        buf.writeVarInt(context.plainBuf.readableBytes());
        buf.writeBytes(context.plainBuf);
    }

    public static void read(SimpleBitStorage storage, FriendlyByteBuf buf, long[] result) {
        var context = CONTEXT.get();
        context.reset();
        int compressedSize = buf.readVarInt();
        context.compressedBuf.ensureWritable(compressedSize);
        buf.readBytes(context.compressedBuf, compressedSize);
        int plainSize = buf.readVarInt();
        context.plainBuf.ensureWritable(plainSize);
        buf.readBytes(context.plainBuf, plainSize);

        while (context.compressedBuf.isReadable()) {
            int packed = context.compressedBuf.readUnsignedMedium();
            int fromX = (packed >> 20) & 0xF;
            int fromY = (packed >> 16) & 0xF;
            int fromZ = (packed >> 12) & 0xF;
            int lengthX = ((packed >> 8) & 0xF) + 1;
            int lengthY = ((packed >> 4) & 0xF) + 1;
            int lengthZ = (packed & 0xF) + 1;
            int value = context.compressedBuf.readVarInt();

            for (int dy = fromY; dy < fromY + lengthY; dy++) {
                for (int dz = fromZ; dz < fromZ + lengthZ; dz++) {
                    for (int dx = fromX; dx < fromX + lengthX; dx++) {
                        int index = getIndex(dx, dy, dz);
                        context.ids[index] = value;
                        context.compressed[index] = true;
                    }
                }
            }
        }

        int bitsRemaining = 0;
        long mask = (1L << storage.bits) - 1;
        for (int i = 0; i < SIZE_XYZ; i++) {
            if (context.compressed[i]) {
                continue;
            }

            if (bitsRemaining >= storage.bits) {
                context.ids[i] = (int) (context.currentLong & mask);
                context.currentLong >>>= storage.bits;
                bitsRemaining -= storage.bits;
            } else {
                int partial = (int) context.currentLong;
                context.currentLong = context.plainBuf.readLong();
                int bitsFromNew = storage.bits - bitsRemaining;
                context.ids[i] = (int) ((partial | (context.currentLong << bitsRemaining)) & mask);
                context.currentLong >>>= bitsFromNew;
                bitsRemaining = 64 - bitsFromNew;
            }
        }

        int outputIndex = 0;
        int inputOffset;
        for (inputOffset = 0; inputOffset <= storage.size - storage.valuesPerLong; inputOffset += storage.valuesPerLong) {
            long packedValue = 0L;
            for (int indexInLong = storage.valuesPerLong - 1; indexInLong >= 0; indexInLong--) {
                packedValue <<= storage.bits;
                packedValue |= context.ids[inputOffset + indexInLong] & storage.mask;
            }
            result[outputIndex++] = packedValue;
        }

        int remainderCount = storage.size - inputOffset;
        if (remainderCount > 0) {
            long lastPackedValue = 0L;
            for (int indexInLong = remainderCount - 1; indexInLong >= 0; indexInLong--) {
                lastPackedValue <<= storage.bits;
                lastPackedValue |= context.ids[inputOffset + indexInLong] & storage.mask;
            }
            result[outputIndex] = lastPackedValue;
        }
    }

    private static int getIndex(int x, int y, int z) {
        return x + z * SIZE_X + y * SIZE_XZ;
    }

    private static class Context {
        private final Cuboid[] cuboids = IntStream.range(0, SIZE_XYZ).mapToObj(_ -> new Cuboid()).toArray(Cuboid[]::new);
        private int cuboidsWritePointer;
        private final int[] ids = new int[SIZE_XYZ];
        private final FriendlyByteBuf compressedBuf = new FriendlyByteBuf(Unpooled.buffer());
        private final FriendlyByteBuf plainBuf = new FriendlyByteBuf(Unpooled.buffer());
        private long currentLong;
        private int bitOffset;
        boolean[] compressed = new boolean[SIZE_XYZ];

        private void reset() {
            cuboidsWritePointer = 0;
            compressedBuf.clear();
            plainBuf.clear();
            currentLong = 0;
            bitOffset = 0;
            Arrays.fill(compressed, false);
        }

        @Nullable
        public Cuboid get(int x, int y, int z) {
            return x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z ? null : cuboids[getIndex(x, y, z)];
        }
    }

    private static class Cuboid {
        private boolean valid;
        private int value;
        private int lengthX, lengthY, lengthZ;

        private void reset(int value) {
            this.valid = true;
            this.value = value;
            this.lengthX = 1;
            this.lengthY = 1;
            this.lengthZ = 1;
        }

        private void from(Cuboid cuboid) {
            this.valid = cuboid.valid;
            this.value = cuboid.value;
            this.lengthX = cuboid.lengthX;
            this.lengthY = cuboid.lengthY;
            this.lengthZ = cuboid.lengthZ;
        }
    }
}
