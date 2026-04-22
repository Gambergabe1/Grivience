package io.papermc.Grivience.terrain;

public final class MountainProfile {
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final int baseY;
    private final int peakY;
    private final long seed;
    private final double centerX;
    private final double centerZ;
    private final double radiusX;
    private final double radiusZ;

    public MountainProfile(int minX, int maxX, int minZ, int maxZ, int baseY, int peakY, long seed) {
        if (maxX < minX) {
            throw new IllegalArgumentException("maxX must be >= minX");
        }
        if (maxZ < minZ) {
            throw new IllegalArgumentException("maxZ must be >= minZ");
        }
        if (peakY < baseY) {
            throw new IllegalArgumentException("peakY must be >= baseY");
        }

        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.baseY = baseY;
        this.peakY = peakY;
        this.seed = seed;
        this.centerX = (minX + maxX) / 2.0;
        this.centerZ = (minZ + maxZ) / 2.0;
        this.radiusX = Math.max(0.5D, (maxX - minX) / 2.0D);
        this.radiusZ = Math.max(0.5D, (maxZ - minZ) / 2.0D);
    }

    public int baseY() {
        return baseY;
    }

    public int peakY() {
        return peakY;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }

    public long seed() {
        return seed;
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public int heightRange() {
        return peakY - baseY;
    }

    public int heightAt(int x, int z) {
        ensureInsideFootprint(x, z);
        if (peakY == baseY) {
            return baseY;
        }
        if (isPeakColumn(x, z)) {
            return peakY;
        }

        double normalizedX = (x - centerX) / radiusX;
        double normalizedZ = (z - centerZ) / radiusZ;
        double shape = sampleShape(normalizedX, normalizedZ, x, z);
        int height = baseY + (int) Math.round(heightRange() * shape);
        return clamp(height, baseY, peakY);
    }

    public double steepnessAt(int x, int z) {
        ensureInsideFootprint(x, z);

        int west = heightAt(clamp(x - 1, minX, maxX), z);
        int east = heightAt(clamp(x + 1, minX, maxX), z);
        int north = heightAt(x, clamp(z - 1, minZ, maxZ));
        int south = heightAt(x, clamp(z + 1, minZ, maxZ));

        int span = Math.max(1, heightRange());
        double horizontalGradient = Math.max(Math.abs(east - west), Math.abs(south - north));
        return horizontalGradient / (double) span;
    }

    private double sampleShape(double normalizedX, double normalizedZ, int x, int z) {
        double absoluteX = Math.abs(normalizedX);
        double absoluteZ = Math.abs(normalizedZ);
        double radialDistance = Math.max(absoluteX, absoluteZ);
        if (radialDistance >= 1.0D) {
            return 0.0D;
        }

        double angle = Math.atan2(normalizedZ, normalizedX);
        double circularDistance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);
        double primaryNoise = valueNoise(x * 0.0625D, z * 0.0625D, seed ^ 0x632BE59BD9B4E019L);
        double shoulderNoise = valueNoise(x * 0.035D, z * 0.035D, seed ^ 0x9E3779B97F4A7C15L);
        double ridgeNoise = 1.0D - Math.abs(valueNoise(x * 0.125D, z * 0.125D, seed ^ 0x94D049BB133111EBL) * 2.0D - 1.0D);

        double ridgeWarp = 1.0D + 0.14D * Math.sin(angle * 3.0D + primaryNoise * Math.PI * 2.0D);
        double shoulderWarp = 0.92D + 0.14D * shoulderNoise;
        double distortedDistance = radialDistance * ridgeWarp / shoulderWarp;

        double edgeFade = smoothClamp((1.0D - radialDistance) / 0.18D);
        double radialFalloff = clamp01(1.0D - Math.pow(Math.min(1.0D, distortedDistance), 1.45D));
        double erosion = 0.84D + 0.20D * shoulderNoise;
        double ruggedness = 0.76D + 0.24D * ridgeNoise;
        double peakBoost = Math.exp(-circularDistance * circularDistance * 7.5D) * 0.22D;

        return clamp01(radialFalloff * edgeFade * erosion * ruggedness + peakBoost);
    }

    private double valueNoise(double x, double z, long salt) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double localX = x - x0;
        double localZ = z - z0;
        double fadeX = smoothClamp(localX);
        double fadeZ = smoothClamp(localZ);

        double n00 = latticeNoise(x0, z0, salt);
        double n10 = latticeNoise(x1, z0, salt);
        double n01 = latticeNoise(x0, z1, salt);
        double n11 = latticeNoise(x1, z1, salt);

        double nx0 = lerp(n00, n10, fadeX);
        double nx1 = lerp(n01, n11, fadeX);
        return lerp(nx0, nx1, fadeZ);
    }

    private double latticeNoise(int x, int z, long salt) {
        long hash = salt;
        hash ^= mix64((long) x * 0x632BE59BD9B4E019L);
        hash ^= mix64((long) z * 0x9E3779B97F4A7C15L);
        hash = mix64(hash);
        return ((hash >>> 11) & ((1L << 53) - 1)) * 0x1.0p-53;
    }

    private boolean isPeakColumn(int x, int z) {
        return Math.abs(x - centerX) <= 0.5D && Math.abs(z - centerZ) <= 0.5D;
    }

    private void ensureInsideFootprint(int x, int z) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            throw new IllegalArgumentException("Coordinates are outside of the selected footprint");
        }
    }

    private static int fastFloor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private static double smoothClamp(double value) {
        double clamped = clamp01(value);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private static double clamp01(double value) {
        if (value <= 0.0D) {
            return 0.0D;
        }
        if (value >= 1.0D) {
            return 1.0D;
        }
        return value;
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return mixed;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
