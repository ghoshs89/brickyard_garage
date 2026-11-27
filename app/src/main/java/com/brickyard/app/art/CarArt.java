package com.brickyard.app.art;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

import com.brickyard.engine.cars.GarageCatalog;

import java.util.Map;

/**
 * Side view of a modular brick vehicle, assembled from whatever parts are snapped on.
 * Drawn in a 400 x 200 box (ground at y = 180). Missing slots are simply not drawn,
 * so the car visibly grows as repairs unlock its slots.
 */
public final class CarArt {
    private CarArt() {}

    public static void draw(Canvas c, GarageCatalog cat, String vehicle, Map<String, String> eq,
                            float x, float y, float w, float wheelAngle, float t) {
        draw(c, cat, vehicle, eq, 0, 0, x, y, w, wheelAngle, t);
    }

    public static void draw(Canvas c, GarageCatalog cat, String vehicle, Map<String, String> eq,
                            int carNumber, int stripeStyle,
                            float x, float y, float w, float wheelAngle, float t) {
        c.save();
        c.translate(x, y);
        c.scale(w / 400f, w / 400f);
        String chassis = eq == null ? null : eq.get("CHASSIS");
        if (chassis == null) {
            // Just a dust sheet over a shape.
            Path sheet = new Path();
            sheet.moveTo(60, 178); sheet.cubicTo(70, 80, 330, 80, 340, 178); sheet.close();
            Draw.path(c, sheet, 0xFFD9D2C3, 4);
            for (int k = 0; k < 5; k++) c.drawLine(110 + k * 45, 110, 100 + k * 48, 175, Draw.line(0x33000000, 3));
            c.restore();
            return;
        }
        int bodyColor = Pal.carColor(partName(cat, eq.get("COLOR")));
        if (eq.get("COLOR") == null) bodyColor = 0xFFB8B0A0; // bare primer until the paint booth is fixed
        int chassisLook = look(cat, chassis);
        int wheelLook = look(cat, eq.get("WHEELS"));
        boolean hasWheels = eq.get("WHEELS") != null;
        Draw.shadowEllipse(c, 200, 182, 330, 18);

        float lift = vehicle.equals("trailblazer") ? 22 + chassisLook * 8 : 0;
        float wheelR = vehicle.equals("trailblazer") ? 36 + wheelLook * 3 : vehicle.equals("apex") ? 28 : 31;
        float axleY = 180 - wheelR;
        float bodyBottom = axleY - 6 - lift * 0.4f;

        // ---- body silhouette per vehicle
        Path body = new Path();
        if (vehicle.equals("comet")) {
            body.moveTo(40, bodyBottom);
            body.lineTo(40, bodyBottom - 40);
            body.lineTo(120, bodyBottom - 52);
            body.lineTo(160, bodyBottom - 96);
            body.lineTo(268, bodyBottom - 96);
            body.lineTo(310, bodyBottom - 52);
            body.lineTo(362, bodyBottom - 44);
            body.lineTo(362, bodyBottom);
            body.close();
        } else if (vehicle.equals("trailblazer")) {
            body.moveTo(46, bodyBottom);
            body.lineTo(46, bodyBottom - 62);
            body.lineTo(110, bodyBottom - 66);
            body.lineTo(130, bodyBottom - 118);
            body.lineTo(300, bodyBottom - 118);
            body.lineTo(312, bodyBottom - 66);
            body.lineTo(356, bodyBottom - 60);
            body.lineTo(356, bodyBottom);
            body.close();
        } else {
            body.moveTo(30, bodyBottom);
            body.lineTo(34, bodyBottom - 30);
            body.lineTo(150, bodyBottom - 50);
            body.lineTo(206, bodyBottom - 80);
            body.lineTo(262, bodyBottom - 76);
            body.lineTo(352, bodyBottom - 40);
            body.lineTo(372, bodyBottom - 16);
            body.lineTo(368, bodyBottom);
            body.close();
        }
        Draw.path(c, body, bodyColor, 5);
        // brick seams and studs along the roof line make it read as built from bricks
        Path clip = new Path(body);
        c.save();
        c.clipPath(clip);
        for (int k = 0; k < 12; k++) c.drawLine(30 + k * 32, bodyBottom - 130, 30 + k * 32, bodyBottom, Draw.line(Pal.darker(bodyColor, 0.85f), 2));
        // Custom brick stripe decals
        if (stripeStyle == 1) { // double racing stripe
            c.drawRect(0, bodyBottom - 36, 400, bodyBottom - 26, Draw.fill(0xFFF4F1E8));
            c.drawRect(0, bodyBottom - 22, 400, bodyBottom - 18, Draw.fill(0xFFF4F1E8));
        } else if (stripeStyle == 2) { // checkered rally band
            for (int k = 0; k < 25; k++) {
                c.drawRect(k * 16, bodyBottom - 34, k * 16 + 16, bodyBottom - 24, Draw.fill(k % 2 == 0 ? 0xFFF4F1E8 : 0xFF222222));
                c.drawRect(k * 16, bodyBottom - 24, k * 16 + 16, bodyBottom - 14, Draw.fill(k % 2 == 1 ? 0xFFF4F1E8 : 0xFF222222));
            }
        } else if (stripeStyle == 3) { // lightning bolt
            Path bolt = new Path();
            bolt.moveTo(70, bodyBottom - 26); bolt.lineTo(185, bodyBottom - 44); bolt.lineTo(175, bodyBottom - 28);
            bolt.lineTo(310, bodyBottom - 36); bolt.lineTo(195, bodyBottom - 14); bolt.lineTo(205, bodyBottom - 26); bolt.close();
            Draw.path(c, bolt, Pal.GOLD, 2);
        }
        c.drawRect(0, bodyBottom - 18, 400, bodyBottom, Draw.fill(Pal.darker(bodyColor, 0.8f)));
        c.restore();
        if (carNumber > 0) {
            float nx = vehicle.equals("apex") ? 205 : 210, ny = bodyBottom - (vehicle.equals("trailblazer") ? 36 : 26);
            Draw.circle(c, nx, ny, 19, 0xFFFDFBF5, 3);
            android.graphics.Paint np = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            np.setColor(Pal.INK);
            np.setTextSize(21);
            np.setFakeBoldText(true);
            np.setTextAlign(android.graphics.Paint.Align.CENTER);
            c.drawText(String.valueOf(carNumber), nx, ny + 7, np);
        }
        float roofY = vehicle.equals("comet") ? bodyBottom - 96 : vehicle.equals("trailblazer") ? bodyBottom - 118 : bodyBottom - 80;
        float roofL = vehicle.equals("comet") ? 168 : vehicle.equals("trailblazer") ? 138 : 190;
        float roofR = vehicle.equals("comet") ? 260 : vehicle.equals("trailblazer") ? 292 : 250;
        int studs = (int) ((roofR - roofL) / 22);
        for (int k = 0; k < studs; k++) Draw.stud(c, roofL + 11 + k * 22, roofY - 2, 8, bodyColor);
        // windows
        Path win = new Path();
        if (vehicle.equals("comet")) {
            win.moveTo(170, bodyBottom - 56); win.lineTo(176, bodyBottom - 86); win.lineTo(258, bodyBottom - 86); win.lineTo(288, bodyBottom - 56); win.close();
        } else if (vehicle.equals("trailblazer")) {
            win.moveTo(140, bodyBottom - 70); win.lineTo(146, bodyBottom - 108); win.lineTo(290, bodyBottom - 108); win.lineTo(298, bodyBottom - 70); win.close();
        } else {
            win.moveTo(168, bodyBottom - 50); win.lineTo(210, bodyBottom - 72); win.lineTo(250, bodyBottom - 70); win.lineTo(300, bodyBottom - 46); win.close();
        }
        Draw.path(c, win, 0xFF9FD8F2, 4);
        c.drawLine(winX(vehicle) - 20, bodyBottom - 80, winX(vehicle), bodyBottom - 60, Draw.line(0x88FFFFFF, 6));
        // chassis upgrades: roll cage / rescue cage / ground-effect skirt
        if (chassisLook == 2) {
            if (vehicle.equals("apex")) Draw.rrect(c, 60, bodyBottom - 8, 340, bodyBottom + 4, 4, 0xFF333333, 3);
            else for (int k = 0; k < 3; k++) c.drawLine(180 + k * 36, bodyBottom - 56, 190 + k * 30, roofY + 6, Draw.line(Pal.STEEL_DARK, 5));
        } else if (chassisLook == 1) {
            c.drawLine(60, bodyBottom - 24, 340, bodyBottom - 24, Draw.line(Pal.lighter(bodyColor, 0.5f), 6));
        }

        // ---- engine (visible scoop / block)
        String engine = eq.get("ENGINE");
        if (engine != null) {
            int el = look(cat, engine);
            int ec = el == 2 ? Pal.GOLD : el == 1 ? 0xFF4A4A4A : Pal.STEEL;
            if (vehicle.equals("apex")) {
                Draw.rrect(c, 268, bodyBottom - 62, 318, bodyBottom - 44, 5, ec, 3);
                for (int k = 0; k < 2 + el; k++) Draw.circle(c, 276 + k * 12, bodyBottom - 53, 4, Pal.INK, 0);
            } else {
                float hx = vehicle.equals("comet") ? 60 : 62;
                Draw.brick(c, hx, bodyBottom - (vehicle.equals("comet") ? 64 : 84), hx + 56 + el * 6, bodyBottom - (vehicle.equals("comet") ? 44 : 64), ec, 2 + el, 3);
            }
            // exhaust puffs
            float px = vehicle.equals("apex") ? 26 : 36;
            for (int k = 0; k < 3; k++) {
                float ph = (t * 1.4f + k / 3f) % 1f;
                Draw.circle(c, px - ph * 40, bodyBottom - 14 - ph * 16, 6 + ph * 10, Pal.alpha(0xFFDDDDDD, (int) (160 * (1 - ph))), 0);
            }
        }

        // ---- aero
        String aero = eq.get("AERO");
        if (aero != null) {
            int al = look(cat, aero);
            if (vehicle.equals("trailblazer")) {
                if (al == 0) { Draw.rrect(c, 150, roofY - 22, 280, roofY - 12, 3, 0xFF555555, 3); c.drawLine(160, roofY - 12, 160, roofY, Draw.line(Pal.INK, 4)); c.drawLine(270, roofY - 12, 270, roofY, Draw.line(Pal.INK, 4)); }
                else if (al == 1) { Draw.rrect(c, 300, roofY - 30, 312, bodyBottom - 60, 4, 0xFF333333, 3); }
                else Draw.rrect(c, 330, bodyBottom - 20, 366, bodyBottom - 2, 4, 0xFF333333, 3);
            } else if (vehicle.equals("apex") && al == 0) {
                Path cone = new Path();
                cone.moveTo(352, bodyBottom - 40); cone.lineTo(392, bodyBottom - 8); cone.lineTo(368, bodyBottom); cone.close();
                Draw.path(c, cone, Pal.darker(bodyColor, 0.8f), 3);
            } else {
                float wy = vehicle.equals("comet") ? bodyBottom - 62 - al * 12 : bodyBottom - 54 - al * 14;
                float wx = vehicle.equals("comet") ? 330 : 40;
                c.drawLine(wx + 10, bodyBottom - 44, wx + 10, wy, Draw.line(Pal.INK, 5));
                Draw.rrect(c, wx - 10, wy - 10, wx + 46, wy + 2, 4, al == 2 ? Pal.INK : Pal.darker(bodyColor, 0.75f), 3);
                if (al == 2) Draw.rrect(c, wx - 10, wy - 26, wx + 46, wy - 16, 4, Pal.INK, 3);
            }
        }

        // ---- utility
        String util = eq.get("UTILITY");
        if (util != null) {
            int ul = look(cat, util);
            boolean glow = ((int) (t * 3)) % 2 == 0;
            if (ul == 1 && !vehicle.equals("apex")) {
                Draw.rrect(c, roofL + 4, roofY - 30, roofR - 4, roofY - 18, 4, 0xFF333333, 3);
                for (int k = 0; k < 4; k++) Draw.circle(c, roofL + 20 + k * (roofR - roofL - 40) / 3f, roofY - 24, 5, glow ? 0xFFFFF4A8 : 0xFFFFD23F, 0);
            } else if (vehicle.equals("trailblazer") && ul == 0) {
                c.drawArc(new RectF(352, bodyBottom - 30, 380, bodyBottom - 2), 270, 270, false, Draw.line(Pal.STEEL_DARK, 6));
            } else if (ul == 2 && vehicle.equals("comet")) {
                Draw.rrect(c, 190, bodyBottom - 50, 240, bodyBottom - 34, 7, 0xFF3FA9F5, 3);
                Draw.rrect(c, 196, bodyBottom - 34, 246, bodyBottom - 18, 7, 0xFF3FA9F5, 3);
            } else if (vehicle.equals("apex") && ul >= 1) {
                Draw.rrect(c, 8, bodyBottom - 34, 34, bodyBottom - 14, 6, Pal.STEEL_DARK, 3);
                Draw.circle(c, 8, bodyBottom - 24, 7 + (glow ? 3 : 0), 0xFF7FDBFF, 0);
            } else {
                float lx = vehicle.equals("apex") ? 350 : 352;
                Draw.circle(c, lx, bodyBottom - 26, 9, glow ? 0xFFFFF4A8 : 0xFFFFE066, 3);
            }
        }

        // ---- wheels (spin with wheelAngle)
        if (hasWheels) {
            float[] ax = vehicle.equals("apex") ? new float[]{96, 312} : vehicle.equals("trailblazer") ? new float[]{104, 300} : new float[]{100, 300};
            int rim = wheelLook == 2 ? Pal.GOLD : wheelLook == 1 ? 0xFFDDDDDD : Pal.STEEL;
            for (float wx : ax) {
                Draw.circle(c, wx, axleY, wheelR, 0xFF222222, 4);
                int treads = wheelLook == 1 || vehicle.equals("trailblazer") ? 12 : 0;
                for (int k = 0; k < treads; k++) {
                    double a = wheelAngle + k * Math.PI * 2 / treads;
                    c.drawLine(wx + (float) Math.cos(a) * (wheelR - 6), axleY + (float) Math.sin(a) * (wheelR - 6),
                            wx + (float) Math.cos(a) * wheelR, axleY + (float) Math.sin(a) * wheelR, Draw.line(0xFF555555, 5));
                }
                Draw.circle(c, wx, axleY, wheelR * 0.55f, rim, 3);
                for (int k = 0; k < 5; k++) {
                    double a = wheelAngle + k * Math.PI * 2 / 5;
                    c.drawLine(wx, axleY, wx + (float) Math.cos(a) * wheelR * 0.5f, axleY + (float) Math.sin(a) * wheelR * 0.5f, Draw.line(Pal.darker(rim, 0.6f), 4));
                }
                Draw.circle(c, wx, axleY, wheelR * 0.14f, Pal.INK, 0);
            }
        } else {
            // blocks under the axles until wheels are fitted
            Draw.brick(c, 80, 150, 130, 180, 0xFF777777, 2, 3);
            Draw.brick(c, 280, 150, 330, 180, 0xFF777777, 2, 3);
        }
        c.restore();
    }

    private static float winX(String v) {
        return v.equals("comet") ? 200 : v.equals("trailblazer") ? 180 : 220;
    }

    static int look(GarageCatalog cat, String partId) {
        if (partId == null) return 0;
        GarageCatalog.Part p = cat.parts.get(partId);
        return p == null ? 0 : p.look;
    }

    static String partName(GarageCatalog cat, String partId) {
        if (partId == null) return null;
        GarageCatalog.Part p = cat.parts.get(partId);
        return p == null ? null : p.name;
    }

    /** Small icon for a slot (used on the Brick Bench). 100 x 100 box. */
    public static void slotIcon(Canvas c, String slot, float x, float y, float size, int color) {
        c.save();
        c.translate(x, y);
        c.scale(size / 100f, size / 100f);
        switch (slot) {
            case "CHASSIS": Draw.rrect(c, 10, 40, 90, 60, 4, Pal.STEEL, 3); Draw.circle(c, 25, 66, 10, Pal.INK, 0); Draw.circle(c, 75, 66, 10, Pal.INK, 0); break;
            case "WHEELS": Draw.circle(c, 50, 50, 36, 0xFF222222, 3); Draw.circle(c, 50, 50, 18, Pal.STEEL, 3); break;
            case "ENGINE": Draw.brick(c, 18, 34, 82, 80, 0xFF555555, 3, 3); break;
            case "AERO": Draw.rrect(c, 10, 30, 90, 44, 4, color, 3); c.drawLine(30, 44, 30, 76, Draw.line(Pal.INK, 6)); c.drawLine(70, 44, 70, 76, Draw.line(Pal.INK, 6)); break;
            case "UTILITY": Draw.rrect(c, 14, 40, 86, 60, 6, 0xFF333333, 3); for (int k = 0; k < 3; k++) Draw.circle(c, 30 + k * 20, 50, 6, 0xFFFFE066, 0); break;
            default: Draw.brick(c, 16, 40, 84, 80, color, 2, 3);
        }
        c.restore();
    }
}
