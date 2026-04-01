package cn.ussshenzhou.notenoughbandwidth.chunk;

public abstract class AR {

    private static final int SIZE_X = 16;
    private static final int SIZE_Y = 16;
    private static final int SIZE_Z = 16;
    private static final int SIZE_XZ = SIZE_X * SIZE_Z;
    private static final int SIZE_XYZ = SIZE_X * SIZE_Y * SIZE_Z;

    private final Cuboid[] cuboids;
    private int cursor;

    public AR() {
        this.cuboids = new Cuboid[SIZE_X * SIZE_Y * SIZE_Z];
        this.cursor = 0;
    }

    public void reset() {
        for (var i = 0; i < cursor; i++) {
            this.cuboids[i] = null;
        }

        cursor = 0;
    }

    public void insert(int[] value, int offset, int length) {
        for (var i = 0; i < length && i < SIZE_XYZ; i++) {
            insertOne(value[offset + i]);
        }
    }

    public void insertOne(int value) {
        if (cursor >= SIZE_XYZ) {
            return;
        }

        var x = cursor % SIZE_X;
        var y = cursor / SIZE_XZ;
        var z = (cursor / SIZE_X) % SIZE_Z;

        cursor++;

        var self = new Cuboid(value);

        set(x, y, z, self);

        var candidate = get(x - 1, y, z);

        if (candidate != null && candidate.value == self.value && candidate.lengthY == 1 && candidate.lengthZ == 1) {
            self = candidate;
            self.lengthX++;

            set(x - 1, y, z, null);
            set(x, y, z, self);
        }

        candidate = get(x, y, z - 1);

        if (candidate != null && candidate.value == self.value && candidate.lengthX == self.lengthX && candidate.lengthY == 1) {
            self = candidate;
            self.lengthZ++;

            set(x, y, z - 1, null);
            set(x, y, z, self);
        }

        candidate = get(x, y - 1, z);

        if (candidate != null && candidate.value == self.value && candidate.lengthX == self.lengthX && candidate.lengthZ == self.lengthZ) {
            self = candidate;
            self.lengthY++;

            set(x, y - 1, z, null);
            set(x, y, z, self);
        }
    }

    public void build() {
        for (var i = 0; i < cursor; i++) {
            var x = i % SIZE_X;
            var y = i / SIZE_XZ;
            var z = (i / SIZE_X) % SIZE_Z;

            var cuboid = cuboids[i];

            if (cuboid != null) {
                emitCuboid(x - (cuboid.lengthX - 1), y - (cuboid.lengthY - 1), z - (cuboid.lengthZ - 1), cuboid.lengthX, cuboid.lengthY, cuboid.lengthZ, cuboid.value);
            }
        }
    }

    public int getIndex(int x, int y, int z) {
        return x + z * SIZE_X + y * SIZE_XZ;
    }

    public Cuboid get(int x, int y, int z) {
        return x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z ? null : cuboids[getIndex(x, y, z)];
    }

    public void set(int x, int y, int z, Cuboid cuboid) {
        if (x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z) {
            cuboids[getIndex(x, y, z)] = cuboid;
        }
    }

    public abstract void emitCuboid(int fromX, int fromY, int fromZ, int lengthX, int lengthY, int lengthZ, int value);

    public static class Cuboid {

        private final int value;
        private int lengthX;
        private int lengthY;
        private int lengthZ;

        public Cuboid(int value) {
            this.value = value;
            this.lengthX = 1;
            this.lengthY = 1;
            this.lengthZ = 1;
        }
    }
}
