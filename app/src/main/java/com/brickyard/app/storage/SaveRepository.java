package com.brickyard.app.storage;

import android.content.Context;

import com.brickyard.engine.profile.Profile;
import com.brickyard.engine.util.Json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Crash-safe persistence: write to a temp file, fsync, keep the previous save as .bak, then rename.
 * A save interrupted at any point leaves either the old file or the new one intact, never half of one.
 * Files live in internal app storage, which survives APK updates signed with the same key.
 */
public final class SaveRepository {
    private final File file, tmp, bak;
    private final File exportDir;

    public SaveRepository(Context ctx) {
        File dir = ctx.getFilesDir();
        file = new File(dir, "brickyard_save_v1.json");
        tmp = new File(dir, "brickyard_save_v1.json.tmp");
        bak = new File(dir, "brickyard_save_v1.json.bak");
        exportDir = ctx.getExternalFilesDir(null);
    }

    public synchronized Profile load() {
        for (File f : new File[]{file, bak}) {
            if (!f.exists()) continue;
            try {
                return Profile.fromJson(Json.parseObject(readAll(f)));
            } catch (Exception ignored) {
                // fall through to the backup
            }
        }
        return new Profile();
    }

    public synchronized void save(Profile p) {
        try {
            writeAtomic(Json.write(p.toJson()));
        } catch (IOException ignored) {
            // A failed save keeps the previous good file; the next save will try again.
        }
    }

    private void writeAtomic(String text) throws IOException {
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.getFD().sync();
        } finally {
            out.close();
        }
        if (file.exists()) {
            if (bak.exists()) bak.delete();
            file.renameTo(bak);
        }
        if (!tmp.renameTo(file)) throw new IOException("rename failed");
    }

    /** Parent Settings: copy the save to the app's external files folder (reachable over USB). */
    public synchronized String exportBackup(Profile p) {
        if (exportDir == null) return null;
        try {
            File f = new File(exportDir, "brickyard-backup.json");
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(Json.pretty(p.toJson()).getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            } finally {
                out.close();
            }
            return f.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    public synchronized Profile importBackup() {
        if (exportDir == null) return null;
        File f = new File(exportDir, "brickyard-backup.json");
        if (!f.exists()) return null;
        try {
            return Profile.fromJson(Json.parseObject(readAll(f)));
        } catch (Exception e) {
            return null;
        }
    }

    public String backupPath() {
        return exportDir == null ? "(unavailable)" : new File(exportDir, "brickyard-backup.json").getAbsolutePath();
    }

    private static String readAll(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            byte[] b = new byte[(int) f.length()];
            int off = 0;
            while (off < b.length) {
                int n = in.read(b, off, b.length - off);
                if (n < 0) break;
                off += n;
            }
            return new String(b, 0, off, StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
