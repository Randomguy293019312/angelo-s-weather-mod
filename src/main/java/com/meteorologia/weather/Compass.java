package com.meteorologia.weather;

public final class Compass {
    private Compass() {}

    private static final String[] NAMES = {"N", "NE", "L", "SE", "S", "SO", "O", "NO"};

    /** Direcao cardeal de um vetor (dx = leste, dz = sul). Norte = -Z. */
    public static String direction(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) {
            deg += 360.0;
        }
        return NAMES[(int) Math.round(deg / 45.0) % 8];
    }

    public static String distance(double blocks) {
        if (blocks < 1000.0) {
            return (int) blocks + " m";
        }
        return String.format("%.1f km", blocks / 1000.0);
    }
}
