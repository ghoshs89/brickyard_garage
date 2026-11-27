package com.brickyard.app.game;

import android.content.res.AssetManager;

import com.brickyard.engine.cars.GarageCatalog;
import com.brickyard.engine.gears.GearPuzzle;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.profile.Campaign;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads the bundled content with the same engine parsers the desktop tools verified. */
public final class ContentStore {
    public final Map<String, Level> levels = new LinkedHashMap<>();
    public final Map<String, GearPuzzle> gears = new LinkedHashMap<>();
    public final Campaign campaign;
    public final GarageCatalog garage;

    public ContentStore(AssetManager am) throws IOException {
        for (String dir : new String[]{"levels", "blueprints"}) {
            String[] files = am.list(dir);
            if (files == null) continue;
            for (String f : files) {
                if (!f.endsWith(".json")) continue;
                Level L = Level.parse(read(am, dir + "/" + f));
                levels.put(L.id, L);
            }
        }
        String[] gf = am.list("gears");
        if (gf != null) for (String f : gf) {
            if (!f.endsWith(".json")) continue;
            GearPuzzle p = GearPuzzle.parse(read(am, "gears/" + f));
            gears.put(p.id, p);
        }
        campaign = Campaign.parse(read(am, "campaign.json"));
        garage = GarageCatalog.parse(read(am, "garage.json"));
    }

    static String read(AssetManager am, String path) throws IOException {
        InputStream in = am.open(path);
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            return new String(o.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
