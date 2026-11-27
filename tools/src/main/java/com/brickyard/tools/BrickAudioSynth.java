package com.brickyard.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Synthesises every sound in the game at build time: original, royalty-free, deterministic.
 * Output: 16-bit mono WAV at 22050 Hz (plays everywhere down to API 22).
 *   sfx: brick snaps pitched up through a cascade (C5 D5 E5 G5 C6), swap, bounce, ratchet, lift hiss,
 *        buggy whirr, airbag thump, nitro whoosh, sprayer, gear mesh, engine rev, coin, star, win, retry,
 *        crate pop, oil mop, part chime, tap, unclamp, jam.
 *   music: Morning Workshop (FM electric piano + plucked bass), Blueprint Focus (marimba + soft clockwork),
 *          Car Show Parade (brassy saw lead + plucked guitar + drums). Each loops seamlessly.
 */
public final class BrickAudioSynth {
    static final int SR = 22050;
    static final Random NOISE = new Random(12345);

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "build/audio");
        Files.createDirectories(out);
        double[] notes = {523.25, 587.33, 659.25, 783.99, 1046.5};
        for (int k = 0; k < notes.length; k++) write(out, "snap_" + k, snap(notes[k]));
        write(out, "swap", swoosh(0.12, 600, 900, 0.25));
        write(out, "bounce", bounce());
        write(out, "ratchet", ratchet());
        write(out, "hiss", noiseEnv(0.6, 0.02, 0.5, 0.25, 4000));
        write(out, "whirr", whirr());
        write(out, "thump", thump());
        write(out, "nitro", swoosh(0.45, 300, 2400, 0.5));
        write(out, "spray", noiseEnv(0.5, 0.01, 0.4, 0.35, 7000));
        write(out, "mesh", mesh());
        write(out, "rev", rev());
        write(out, "coin", chime(new double[]{1318.5, 1975.5}, 0.08, 0.35));
        write(out, "star", chime(new double[]{1046.5, 1318.5, 1568.0, 2093.0}, 0.07, 0.6));
        write(out, "win", fanfare());
        write(out, "retry", gentle());
        write(out, "crate", cratePop());
        write(out, "oil", slurp());
        write(out, "part", chime(new double[]{880, 1108.7, 1318.5}, 0.09, 0.5));
        write(out, "tap", snap(1567.98));
        write(out, "unclamp", ratchetShort());
        write(out, "jam", jam());
        write(out, "music_workshop", workshop());
        write(out, "music_focus", focus());
        write(out, "music_show", show());
        System.out.println("audio written to " + out.toAbsolutePath());
    }

    // ------------------------------------------------------------------ building blocks

    static double[] buf(double seconds) {
        return new double[(int) (seconds * SR)];
    }

    static double env(double t, double attack, double decay) {
        if (t < 0) return 0;
        if (t < attack) return t / attack;
        return Math.exp(-(t - attack) / decay);
    }

    static double sine(double f, double t) {
        return Math.sin(2 * Math.PI * f * t);
    }

    /** Polycarbonate brick snap: a click transient plus a short pitched body. */
    static double[] snap(double f) {
        double[] b = buf(0.18);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double click = (NOISE.nextDouble() * 2 - 1) * env(t, 0.0005, 0.004);
            double body = (sine(f, t) + 0.3 * sine(f * 2.01, t) + 0.15 * sine(f * 3.98, t)) * env(t, 0.001, 0.045);
            b[i] = 0.55 * click + 0.45 * body;
        }
        return b;
    }

    static double[] swoosh(double dur, double f0, double f1, double amp) {
        double[] b = buf(dur);
        double lp = 0;
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR, u = t / dur;
            double cutoff = f0 + (f1 - f0) * u;
            double a = Math.min(1, 2 * Math.PI * cutoff / SR);
            lp += a * ((NOISE.nextDouble() * 2 - 1) - lp);
            b[i] = amp * 2.2 * lp * Math.sin(Math.PI * u);
        }
        return b;
    }

    static double[] bounce() {
        double[] b = buf(0.22);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double f = 330 + 120 * Math.sin(2 * Math.PI * 9 * t);
            b[i] = 0.35 * sine(f, t) * env(t, 0.005, 0.07);
        }
        return b;
    }

    static double[] ratchet() {
        double[] b = buf(0.5);
        for (int k = 0; k < 7; k++) add(b, snap(2200 + 90 * k), (int) (k * 0.06 * SR), 0.5);
        return b;
    }

    static double[] ratchetShort() {
        double[] b = buf(0.25);
        for (int k = 0; k < 3; k++) add(b, snap(1900 + 150 * k), (int) (k * 0.05 * SR), 0.55);
        return b;
    }

    static double[] noiseEnv(double dur, double attack, double decay, double amp, double cutoff) {
        double[] b = buf(dur);
        double lp = 0, a = Math.min(1, 2 * Math.PI * cutoff / SR);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            lp += a * ((NOISE.nextDouble() * 2 - 1) - lp);
            b[i] = amp * 1.6 * lp * env(t, attack, decay) * (1 - t / dur);
        }
        return b;
    }

    static double[] whirr() {
        double[] b = buf(0.6);
        double ph = 0;
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double f = 180 + 260 * Math.min(1, t / 0.3);
            ph += f / SR;
            double saw = 2 * (ph - Math.floor(ph + 0.5));
            b[i] = 0.22 * saw * env(t, 0.03, 0.35) * (0.8 + 0.2 * Math.sin(2 * Math.PI * 30 * t));
        }
        return b;
    }

    static double[] thump() {
        double[] b = buf(0.45);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double f = 140 * Math.exp(-t * 9) + 45;
            b[i] = 0.8 * Math.sin(2 * Math.PI * f * t) * env(t, 0.002, 0.12) + 0.25 * (NOISE.nextDouble() * 2 - 1) * env(t, 0.001, 0.03);
        }
        return b;
    }

    static double[] mesh() {
        double[] b = buf(0.4);
        for (int k = 0; k < 10; k++) add(b, snap(1400 + (k % 2) * 300), (int) (k * 0.035 * SR), 0.3);
        return b;
    }

    static double[] rev() {
        double[] b = buf(1.1);
        double ph = 0;
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double f = 55 + 110 * Math.sin(Math.PI * Math.min(1, t / 1.0)) + 8 * Math.sin(2 * Math.PI * 23 * t);
            ph += f / SR;
            double saw = 2 * (ph - Math.floor(ph + 0.5));
            double sq = Math.signum(Math.sin(2 * Math.PI * ph * 0.5));
            b[i] = 0.25 * (0.7 * saw + 0.3 * sq) * env(t, 0.05, 0.8);
        }
        return b;
    }

    static double[] chime(double[] fs, double gap, double tail) {
        double[] b = buf(gap * fs.length + tail + 0.1);
        for (int k = 0; k < fs.length; k++) add(b, bell(fs[k], tail), (int) (k * gap * SR), 0.45);
        return b;
    }

    static double[] bell(double f, double dur) {
        double[] b = buf(dur);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double mod = 2.5 * Math.exp(-t * 6) * sine(f * 3.5, t);
            b[i] = Math.sin(2 * Math.PI * f * t + mod) * env(t, 0.002, dur * 0.35);
        }
        return b;
    }

    static double[] fanfare() {
        double[] b = buf(1.6);
        double[][] seq = {{523.25, 0.0}, {659.25, 0.12}, {783.99, 0.24}, {1046.5, 0.40}};
        for (double[] n : seq) add(b, brass(n[0], n == seq[3] ? 1.1 : 0.2), (int) (n[1] * SR), 0.35);
        add(b, brass(659.25, 1.0), (int) (0.40 * SR), 0.2);
        add(b, brass(783.99, 1.0), (int) (0.40 * SR), 0.2);
        return b;
    }

    static double[] gentle() {
        // A warm "try again" two-note figure: never a buzzer.
        double[] b = buf(0.9);
        add(b, epiano(392.0, 0.6), 0, 0.4);
        add(b, epiano(523.25, 0.7), (int) (0.18 * SR), 0.4);
        return b;
    }

    static double[] cratePop() {
        double[] b = buf(0.25);
        add(b, noiseEnv(0.2, 0.001, 0.05, 0.6, 2500), 0, 1);
        add(b, snap(700), 0, 0.6);
        return b;
    }

    static double[] slurp() {
        double[] b = buf(0.35);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double f = 500 - 900 * t;
            b[i] = 0.3 * sine(Math.max(80, f), t) * env(t, 0.01, 0.12) * (0.6 + 0.4 * Math.sin(2 * Math.PI * 40 * t));
        }
        return b;
    }

    static double[] jam() {
        double[] b = buf(0.4);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            b[i] = 0.25 * Math.signum(sine(110, t)) * env(t, 0.005, 0.1) + 0.2 * (NOISE.nextDouble() * 2 - 1) * env(t, 0.001, 0.05);
        }
        return b;
    }

    // ------------------------------------------------------------------ instruments

    /** FM electric piano (Rhodes-like): modulator at 1:1 with a decaying index, plus a soft tine. */
    static double[] epiano(double f, double dur) {
        double[] b = buf(dur);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            double idx = 1.8 * Math.exp(-t * 4);
            double v = Math.sin(2 * Math.PI * f * t + idx * sine(f, t)) + 0.15 * sine(f * 14, t) * Math.exp(-t * 30);
            b[i] = v * env(t, 0.004, dur * 0.45);
        }
        return b;
    }

    /** Karplus-Strong plucked string (bass and guitar). */
    static double[] pluck(double f, double dur, double damp, long seed) {
        double[] b = buf(dur);
        int n = Math.max(2, (int) (SR / f));
        double[] ring = new double[n];
        Random r = new Random(seed);
        for (int i = 0; i < n; i++) ring[i] = r.nextDouble() * 2 - 1;
        int p = 0;
        for (int i = 0; i < b.length; i++) {
            double cur = ring[p], nxt = ring[(p + 1) % n];
            ring[p] = damp * 0.5 * (cur + nxt);
            b[i] = cur;
            p = (p + 1) % n;
        }
        return b;
    }

    static double[] marimba(double f, double dur) {
        double[] b = buf(dur);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            b[i] = (sine(f, t) * env(t, 0.002, 0.35) + 0.35 * sine(f * 3.93, t) * env(t, 0.001, 0.05)) * 0.8;
        }
        return b;
    }

    static double[] brass(double f, double dur) {
        double[] b = buf(dur);
        double ph = 0, lp = 0;
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            ph += f * (1 + 0.004 * Math.sin(2 * Math.PI * 5.5 * t)) / SR;
            double saw = 2 * (ph - Math.floor(ph + 0.5));
            double cutoff = 600 + 2600 * Math.min(1, t / 0.06) * Math.exp(-t * 1.5);
            double a = Math.min(1, 2 * Math.PI * cutoff / SR);
            lp += a * (saw - lp);
            double release = t > dur - 0.05 ? (dur - t) / 0.05 : 1;
            b[i] = lp * Math.min(1, t / 0.03) * release;
        }
        return b;
    }

    static double[] kick() {
        double[] b = buf(0.3);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            b[i] = Math.sin(2 * Math.PI * (50 + 90 * Math.exp(-t * 25)) * t) * env(t, 0.001, 0.12);
        }
        return b;
    }

    static double[] hat(double dur) {
        double[] b = buf(dur);
        double prev = 0;
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR, n = NOISE.nextDouble() * 2 - 1;
            b[i] = (n - prev) * env(t, 0.0005, 0.02);
            prev = n;
        }
        return b;
    }

    static double[] snare() {
        double[] b = buf(0.25);
        for (int i = 0; i < b.length; i++) {
            double t = i / (double) SR;
            b[i] = 0.6 * (NOISE.nextDouble() * 2 - 1) * env(t, 0.001, 0.06) + 0.3 * sine(190, t) * env(t, 0.001, 0.04);
        }
        return b;
    }

    // ------------------------------------------------------------------ music

    static double mtof(int midi) {
        return 440 * Math.pow(2, (midi - 69) / 12.0);
    }

    /** Morning Workshop: 92 bpm, warm FM piano chords and a plucked bass. 8 bars, loops. */
    static double[] workshop() {
        double bpm = 92, beat = 60 / bpm, bars = 8;
        double[] b = buf(bars * 4 * beat);
        int[][] chords = {{60, 64, 67, 71}, {57, 60, 64, 67}, {53, 57, 60, 64}, {55, 59, 62, 65}};
        int[] roots = {36, 33, 29, 31};
        for (int bar = 0; bar < bars; bar++) {
            int c = bar % 4;
            int start = (int) (bar * 4 * beat * SR);
            for (int k = 0; k < chords[c].length; k++) {
                add(b, epiano(mtof(chords[c][k]), 2 * beat), start + (int) (k * 0.02 * SR), 0.11);
                add(b, epiano(mtof(chords[c][k]), 1.6 * beat), start + (int) ((2.5 * beat + k * 0.02) * SR), 0.08);
            }
            add(b, pluck(mtof(roots[c]), 1.8 * beat, 0.996, bar), start, 0.35);
            add(b, pluck(mtof(roots[c] + 7), 0.9 * beat, 0.995, bar + 50), start + (int) (2 * beat * SR), 0.3);
            add(b, pluck(mtof(roots[c] + 12), 0.9 * beat, 0.995, bar + 90), start + (int) (3 * beat * SR), 0.25);
        }
        return loopFade(b);
    }

    /** Blueprint Focus: 76 bpm, calm marimba arpeggios and a soft clockwork tick. Low sensory load. */
    static double[] focus() {
        double bpm = 76, beat = 60 / bpm, bars = 8;
        double[] b = buf(bars * 4 * beat);
        int[][] arps = {{62, 65, 69, 72}, {60, 64, 67, 71}, {57, 60, 64, 67}, {59, 62, 65, 69}};
        for (int bar = 0; bar < bars; bar++) {
            int[] a = arps[bar % 4];
            for (int s = 0; s < 8; s++) {
                int start = (int) ((bar * 4 + s * 0.5) * beat * SR);
                add(b, marimba(mtof(a[s % 4] + (s >= 4 ? 12 : 0)), 0.9), start, s == 0 ? 0.22 : 0.15);
                add(b, hat(0.04), start, 0.03);
            }
        }
        return loopFade(b);
    }

    /** Car Show Parade: 118 bpm, brassy lead, plucked guitar chords and a simple beat. */
    static double[] show() {
        double bpm = 118, beat = 60 / bpm, bars = 8;
        double[] b = buf(bars * 4 * beat);
        int[][] chords = {{48, 55, 60, 64}, {53, 57, 60, 65}, {55, 59, 62, 67}, {48, 55, 60, 64}};
        int[] melody = {72, 74, 76, 79, 77, 76, 74, 72, 74, 76, 77, 79, 81, 79, 76, 74};
        for (int bar = 0; bar < bars; bar++) {
            int start = (int) (bar * 4 * beat * SR);
            int[] ch = chords[bar % 4];
            for (int s = 0; s < 4; s++) {
                int st = start + (int) (s * beat * SR);
                add(b, kick(), st, s % 2 == 0 ? 0.35 : 0.0);
                if (s % 2 == 1) add(b, snare(), st, 0.22);
                add(b, hat(0.05), st + (int) (0.5 * beat * SR), 0.06);
                for (int k = 0; k < ch.length; k++) add(b, pluck(mtof(ch[k]), 0.9 * beat, 0.993, bar * 7 + s * 3 + k), st + (int) (k * 0.012 * SR), 0.08);
            }
            for (int s = 0; s < 2; s++) {
                int note = melody[(bar * 2 + s) % melody.length];
                add(b, brass(mtof(note), 1.8 * beat), start + (int) (s * 2 * beat * SR), 0.14);
            }
        }
        return loopFade(b);
    }

    /** Tiny fade at both ends so the loop point never clicks. */
    static double[] loopFade(double[] b) {
        int n = (int) (0.01 * SR);
        for (int i = 0; i < n; i++) {
            double g = i / (double) n;
            b[i] *= g;
            b[b.length - 1 - i] *= g;
        }
        return b;
    }

    static void add(double[] dst, double[] src, int at, double gain) {
        for (int i = 0; i < src.length && at + i < dst.length; i++) if (at + i >= 0) dst[at + i] += gain * src[i];
    }

    // ------------------------------------------------------------------ WAV

    static void write(Path dir, String name, double[] b) throws IOException {
        double peak = 1e-9;
        for (double v : b) peak = Math.max(peak, Math.abs(v));
        double gain = peak > 0.95 ? 0.95 / peak : 1;
        ByteBuffer pcm = ByteBuffer.allocate(b.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : b) pcm.putShort((short) Math.round(Math.max(-1, Math.min(1, v * gain)) * 32767));
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + b.length * 2).put("WAVE".getBytes());
        h.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(SR).putInt(SR * 2).putShort((short) 2).putShort((short) 16);
        h.put("data".getBytes()).putInt(b.length * 2);
        o.write(h.array());
        o.write(pcm.array());
        Files.write(dir.resolve(name + ".wav"), o.toByteArray());
    }
}
