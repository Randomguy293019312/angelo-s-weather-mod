package com.meteorologia.client;

import com.meteorologia.weather.Compass;
import com.meteorologia.weather.Supercell;
import com.meteorologia.weather.Tornado;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Tela do radar: ecos de refletividade das supercelulas, ganchos de tornado e varredura. */
public class RadarScreen extends Screen {
    private static final int[] RANGES = {1000, 2000, 4000, 8000};
    private static final int[] ECHO_COLORS = {
            0x9900AA00, 0xAA00DD00, 0xBBEEEE00, 0xCCFF9900, 0xDDFF2200, 0xEEDD00DD
    };
    private static final double[] ECHO_SCALE = {1.30, 1.00, 0.78, 0.58, 0.40, 0.22};

    private final BlockPos origin;
    private int rangeIdx = 1;

    public RadarScreen(BlockPos origin) {
        super(Component.literal("Radar Meteorologico"));
        this.origin = origin;
    }

    private int radarRadius() {
        return Math.max(60, Math.min(this.height - 50, this.width - 260) / 2);
    }

    private int centerX() {
        return 20 + radarRadius() + 6;
    }

    @Override
    protected void init() {
        int px = centerX() + radarRadius() + 24;
        addRenderableWidget(Button.builder(Component.literal("Alcance +"),
                b -> rangeIdx = Math.min(RANGES.length - 1, rangeIdx + 1)).bounds(px, 40, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Alcance -"),
                b -> rangeIdx = Math.max(0, rangeIdx - 1)).bounds(px, 64, 100, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void fillDisc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, color);
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int deg = 0; deg < 360; deg++) {
            double a = Math.toRadians(deg);
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);

        int R = radarRadius();
        int cx = centerX();
        int cy = this.height / 2;
        int range = RANGES[rangeIdx];
        double el = ClientStormData.elapsedTicks();

        g.fill(cx - R - 5, cy - R - 5, cx + R + 6, cy + R + 6, 0xFF0C1114);
        fillDisc(g, cx, cy, R, 0xFF04120A);

        g.enableScissor(cx - R, cy - R, cx + R + 1, cy + R + 1);

        // ---- ecos de supercelulas
        for (Supercell s : ClientStormData.supercells()) {
            double sx = (s.x + s.vx * el - origin.getX()) / range * R;
            double sz = (s.z + s.vz * el - origin.getZ()) / range * R;
            double rpx = s.radius * (0.4 + 0.6 * ClientStormData.strength(s, el)) / range * R;
            double ox = Math.cos(s.windAngle) * rpx * 0.25;
            double oz = Math.sin(s.windAngle) * rpx * 0.25;
            for (int i = 0; i < ECHO_COLORS.length; i++) {
                int rr = (int) Math.max(1, rpx * ECHO_SCALE[i]);
                double k = i / (double) (ECHO_COLORS.length - 1);
                fillDisc(g, cx + (int) (sx - ox * k), cy + (int) (sz - oz * k), rr, ECHO_COLORS[i]);
            }
        }

        // ---- tornados (assinatura de rotacao)
        for (Tornado t : ClientStormData.tornadoes()) {
            int tx = cx + (int) ((t.x + t.vx * el - origin.getX()) / range * R);
            int tz = cy + (int) ((t.z + t.vz * el - origin.getZ()) / range * R);
            ring(g, tx, tz, 5, 0xFFFFFFFF);
            ring(g, tx, tz, 3, 0xFFFF33FF);
            g.fill(tx - 1, tz - 1, tx + 2, tz + 2, 0xFFFF33FF);
            g.drawString(this.font, "EF" + t.ef, tx + 8, tz - 4, 0xFFFF77FF, false);
        }
        g.disableScissor();

        // ---- aneis, cruz e varredura
        for (int i = 1; i <= 4; i++) {
            ring(g, cx, cy, R * i / 4, 0x5533AA55);
        }
        g.fill(cx - R, cy, cx + R + 1, cy + 1, 0x4433AA55);
        g.fill(cx, cy - R, cx + 1, cy + R + 1, 0x4433AA55);

        double sweep = (System.nanoTime() / 1.0e9 * 1.3) % (Math.PI * 2.0);
        for (int k = 0; k < 22; k++) {
            double a = sweep - k * 0.035;
            int alpha = (int) (0xBB * (1.0 - k / 22.0));
            int color = (alpha << 24) | 0x44FF77;
            for (int i = 0; i < R; i += 2) {
                int x = cx + (int) (Math.cos(a) * i);
                int y = cy + (int) (Math.sin(a) * i);
                g.fill(x, y, x + 1, y + 1, color);
            }
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

        g.drawString(this.font, "N", cx - 2, cy - R - 16, 0xFFFFFFFF, false);
        g.drawString(this.font, "S", cx - 2, cy + R + 8, 0xFFFFFFFF, false);
        g.drawString(this.font, "L", cx + R + 10, cy - 4, 0xFFFFFFFF, false);
        g.drawString(this.font, "O", cx - R - 16, cy - 4, 0xFFFFFFFF, false);

        // ---- painel de informacoes
        int px = cx + R + 24;
        g.drawString(this.font, "RADAR METEOROLOGICO", px, 16, 0xFF55FF77, false);
        g.drawString(this.font, "Alcance: " + Compass.distance(range), px, 28, 0xFFAAAAAA, false);

        List<String> lines = new ArrayList<>();
        for (Tornado t : ClientStormData.tornadoes()) {
            lines.add("§cTornado EF" + t.ef + " §7" + describe(t.x + t.vx * el, t.z + t.vz * el));
        }
        for (Supercell s : ClientStormData.supercells()) {
            lines.add("§6Supercelula §7" + describe(s.x + s.vx * el, s.z + s.vz * el));
        }
        if (lines.isEmpty()) {
            lines.add("§aCeu limpo no alcance do radar.");
        }
        int y = 100;
        for (String line : lines) {
            g.drawString(this.font, line, px, y, 0xFFFFFFFF, false);
            y += 12;
        }
        if (ClientStormData.rain() >= 0.9F) {
            g.drawString(this.font, "§9Chuva forte em andamento", px, y + 6, 0xFFFFFFFF, false);
        }
    }

    private String describe(double x, double z) {
        double dx = x - origin.getX();
        double dz = z - origin.getZ();
        return Compass.distance(Math.sqrt(dx * dx + dz * dz)) + " " + Compass.direction(dx, dz);
    }
}
