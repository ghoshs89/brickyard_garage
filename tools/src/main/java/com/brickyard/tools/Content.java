package com.brickyard.tools;

import com.brickyard.engine.cars.GarageCatalog;
import com.brickyard.engine.gears.GearPuzzle;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.profile.Campaign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads everything under content/ exactly as the app does (same parsers, same files). */
public final class Content {
    public final Path dir;
    public final Map<String, Level> levels = new LinkedHashMap<>();
    public final Map<String, Path> levelFiles = new LinkedHashMap<>();
    public final Map<String, GearPuzzle> gears = new LinkedHashMap<>();
    public final Map<String, Path> gearFiles = new LinkedHashMap<>();
    public final Campaign campaign;
    public final GarageCatalog garage;
    public final List<String> rawTexts = new ArrayList<>();

    public Content(Path dir) throws IOException {
        this.dir = dir;
        for (String sub : new String[]{"levels", "blueprints"}) {
            Path d = dir.resolve(sub);
            if (!Files.isDirectory(d)) continue;
            for (Path f : sorted(d)) {
                String txt = read(f);
                rawTexts.add(txt);
                Level L;
                try {
                    L = Level.parse(txt);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(f.getFileName() + ": " + e.getMessage(), e);
                }
                if (levels.containsKey(L.id)) throw new IllegalArgumentException("duplicate level id " + L.id);
                levels.put(L.id, L);
                levelFiles.put(L.id, f);
            }
        }
        Path gd = dir.resolve("gears");
        if (Files.isDirectory(gd)) for (Path f : sorted(gd)) {
            String txt = read(f);
            rawTexts.add(txt);
            GearPuzzle p = GearPuzzle.parse(txt);
            if (gears.containsKey(p.id)) throw new IllegalArgumentException("duplicate gear puzzle " + p.id);
            gears.put(p.id, p);
            gearFiles.put(p.id, f);
        }
        String camp = read(dir.resolve("campaign.json"));
        String gar = read(dir.resolve("garage.json"));
        rawTexts.add(camp);
        rawTexts.add(gar);
        campaign = Campaign.parse(camp);
        garage = GarageCatalog.parse(gar);
    }

    static List<Path> sorted(Path d) throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(d, "*.json")) {
            for (Path p : ds) out.add(p);
        }
        out.sort(null);
        return out;
    }

    static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    public List<Level> story() {
        List<Level> out = new ArrayList<>();
        for (String id : campaign.story) out.add(levels.get(id));
        return out;
    }
}
