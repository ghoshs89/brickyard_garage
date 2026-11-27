package com.brickyard.tools;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Renders the launcher icon (a red 2x2 brick with a gold gear) at every density into res/mipmap-*. */
public final class IconRenderer {
    public static void main(String[] args) throws IOException {
        File res = new File(args.length > 0 ? args[0] : "app/src/main/res");
        String[] dens = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] px = {48, 72, 96, 144, 192};
        for (int k = 0; k < dens.length; k++) {
            File dir = new File(res, "mipmap-" + dens[k]);
            dir.mkdirs();
            ImageIO.write(render(px[k]), "png", new File(dir, "ic_launcher.png"));
        }
        System.out.println("icons written to " + res.getAbsolutePath());
    }

    static BufferedImage render(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 100.0, size / 100.0);
        Color ink = new Color(0x2B2118);
        // rounded square background: garage yellow
        g.setPaint(new GradientPaint(0, 0, new Color(0xFFD86B), 0, 100, new Color(0xF2B630)));
        g.fill(new RoundRectangle2D.Double(4, 4, 92, 92, 24, 24));
        g.setColor(ink);
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new RoundRectangle2D.Double(4, 4, 92, 92, 24, 24));
        // red brick
        g.setPaint(new GradientPaint(0, 40, new Color(0xF05545), 0, 82, new Color(0xB82A1F)));
        g.fill(new RoundRectangle2D.Double(16, 40, 68, 42, 8, 8));
        g.setColor(ink);
        g.draw(new RoundRectangle2D.Double(16, 40, 68, 42, 8, 8));
        for (int k = 0; k < 2; k++) {
            double x = 26 + k * 32;
            g.setColor(new Color(0x9C2119));
            g.fill(new Ellipse2D.Double(x, 28, 16, 16));
            g.setColor(new Color(0xF27A6E));
            g.fill(new Ellipse2D.Double(x + 1, 27, 14, 13));
            g.setColor(ink);
            g.draw(new Ellipse2D.Double(x, 28, 16, 16));
        }
        // gold gear on the brick face
        Path2D gear = new Path2D.Double();
        int teeth = 8, n = teeth * 4;
        for (int k = 0; k <= n; k++) {
            double a = k * 2 * Math.PI / n;
            int ph = k % 4;
            double r = (ph == 1 || ph == 2) ? 15 : 11.5;
            double x = 50 + Math.cos(a) * r, y = 61 + Math.sin(a) * r;
            if (k == 0) gear.moveTo(x, y); else gear.lineTo(x, y);
        }
        gear.closePath();
        g.setColor(new Color(0xF7D35C));
        g.fill(gear);
        g.setColor(ink);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(gear);
        g.fill(new Ellipse2D.Double(46, 57, 8, 8));
        g.dispose();
        return img;
    }
}
