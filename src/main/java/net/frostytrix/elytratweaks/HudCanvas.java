package net.frostytrix.elytratweaks;

// The drawing calls FlightHud needs. Minecraft's GUI drawing class differs between versions
// (GuiGraphics / GuiGraphicsExtractor), so each version's ClientModEvents adapts it to this.
public interface HudCanvas {
    int width();

    int height();

    void fill(int x0, int y0, int x1, int y1, int argb);

    void text(String text, int x, int y, int argb);
}
