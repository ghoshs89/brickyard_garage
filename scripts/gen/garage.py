"""Generates content/garage.json. Edit this script, not the JSON (same rule as AALOPP generators)."""
import json, os

def fixture(fid, name, repair, designs):
    return {"id": fid, "name": name, "repair": repair,
            "designs": [{"id": f"{fid}-{i}", "name": n, "price": p} for i, (n, p) in enumerate(designs)]}

bays = [
  {"id": "bay1", "name": "Classic Pit Bay", "vehicle": "comet",
   "intro": "Grandpa Bo's old pit bay. Dusty, quiet, and full of good ideas.",
   "outro": "The Comet sits on the lift, gleaming. Bay 1 is back in business!",
   "repairs": [
     {"id": "b1-r1", "name": "Sweep out the pit bay", "stars": 1, "unlocksSlot": "CHASSIS,WHEELS",
      "desc": "Clear the dust and roll the Comet's chassis onto the lift.",
      "story": "Under the dust sheet: a brick rally chassis. Bo smiles. \"I started her in '78. Never finished.\""},
     {"id": "b1-r2", "name": "Fix the hydraulic lift", "stars": 2, "unlocksSlot": "ENGINE",
      "desc": "Get the lift pumping so we can fit an engine.",
      "story": "Juno wants to bash the pump. Bo listens to it first. \"Hear that hiss? A loose valve, not a broken pump.\""},
     {"id": "b1-r3", "name": "Rebuild the paint booth", "stars": 3, "unlocksSlot": "COLOR",
      "desc": "A clean booth means a clean coat of bricks.",
      "story": "Sprocket trots past with a golden 2x4 brick in his mouth. Again."},
     {"id": "b1-r4", "name": "Hang the tool wall", "stars": 4, "unlocksSlot": "AERO",
      "desc": "Every tool in its place, every spoiler within reach.",
      "story": "\"Measure twice, snap once,\" says Bo. Juno groans, then measures twice."},
     {"id": "b1-r5", "name": "Raise the bay door and light the sign", "stars": 5, "unlocksSlot": "UTILITY",
      "desc": "Open the big door and switch on the neon BRICKYARD sign.",
      "story": "The sign flickers on. People across the street stop and look."}],
   "fixtures": [
     fixture("b1-floor", "Floor", "b1-r1", [("Oil-Stained Concrete", 0), ("Checkered Tiles", 20), ("Studded Baseplate Floor", 40)]),
     fixture("b1-bench", "Workbench", "b1-r2", [("Old Oak Bench", 0), ("Red Steel Bench", 22), ("Brick-Built Bench", 45)]),
     fixture("b1-wall", "Tool Wall", "b1-r4", [("Pegboard", 0), ("Rolling Tool Chest", 22), ("Magnet Rail", 45)])]},
  {"id": "bay2", "name": "Off-Road & Rescue Rig Bay", "vehicle": "trailblazer",
   "intro": "The heavy bay. Mud on the floor, a crane in the roof, and a big job ahead.",
   "outro": "The Trailblazer rumbles off the ramp, ready for any hill.",
   "repairs": [
     {"id": "b2-r1", "name": "Dig out the heavy bay", "stars": 1, "unlocksSlot": "CHASSIS,WHEELS",
      "desc": "Shovel the mud and find the crawler chassis.",
      "story": "Bo tells a long story about a rally in the rain. Juno almost leaves. Then he says: \"...so we let the tyres down a little for grip.\""},
     {"id": "b2-r2", "name": "Restart the winch crane", "stars": 2, "unlocksSlot": "ENGINE",
      "desc": "Lift a big engine into the Trailblazer.",
      "story": "The crane hook is stuck. Sprocket is sitting on the lever. Of course."},
     {"id": "b2-r3", "name": "Repaint the rescue stripes", "stars": 3, "unlocksSlot": "COLOR",
      "desc": "Bright stripes so everyone can see the rescue rig.",
      "story": "Juno paints fast. Bo paints slow. Bo's stripes are straighter. Juno notices."},
     {"id": "b2-r4", "name": "Patch the roof skylights", "stars": 4, "unlocksSlot": "AERO",
      "desc": "Let the light in and keep the rain out.",
      "story": "\"Some jobs you can't rush,\" says Bo, holding the ladder. \"Glass is one of them.\""},
     {"id": "b2-r5", "name": "Fire up the floodlight tower", "stars": 5, "unlocksSlot": "UTILITY",
      "desc": "Light the yard for night rescues.",
      "story": "The floodlights snap on. The whole yard glows like a stadium."}],
   "fixtures": [
     fixture("b2-lift", "Vehicle Lift", "b2-r1", [("Two-Post Lift", 0), ("Scissor Lift", 22), ("Rescue Ramp", 50)]),
     fixture("b2-crane", "Crane", "b2-r2", [("Hand Winch", 0), ("Yellow Gantry", 24), ("Brick Crane", 50)]),
     fixture("b2-shelves", "Parts Shelves", "b2-r4", [("Wooden Crates", 0), ("Wire Racks", 20), ("Stacked Brick Bins", 45)])]},
  {"id": "bay3", "name": "Aero Prototype Lab", "vehicle": "apex",
   "intro": "Bo's secret lab. A wind tunnel, a dyno, and a covered shape on a stand.",
   "outro": "The Apex rolls onto the Car Show stage. The crowd goes quiet, then cheers.",
   "repairs": [
     {"id": "b3-r1", "name": "Dust off the wind tunnel", "stars": 1, "unlocksSlot": "CHASSIS,WHEELS",
      "desc": "Uncover the hypercar prototype.",
      "story": "Under the cover: the Apex. Bo's best design, never built. \"I needed someone fast with ideas,\" he says, \"and someone slow with checks.\""},
     {"id": "b3-r2", "name": "Rewire the dyno", "stars": 2, "unlocksSlot": "ENGINE",
      "desc": "Test engines on the rolling road.",
      "story": "Juno spots a crossed wire Bo missed. Bo grins. \"Fast eyes. Good.\""},
     {"id": "b3-r3", "name": "Seal the clean-room paint bay", "stars": 3, "unlocksSlot": "COLOR",
      "desc": "Dust-free paint for a perfect finish.",
      "story": "Sprocket is banned from the clean room. He watches through the window, very sad."},
     {"id": "b3-r4", "name": "Spin up the big tunnel fan", "stars": 4, "unlocksSlot": "AERO",
      "desc": "Test wings and nose cones in real wind.",
      "story": "Smoke streams over the Apex. \"See where it swirls?\" says Bo. \"That's drag. Fix that, win races.\""},
     {"id": "b3-r5", "name": "Light the Car Show stage", "stars": 5, "unlocksSlot": "UTILITY",
      "desc": "Build the stage for the town Car Show.",
      "story": "Show night. Three cars, one team, and a very proud dog."}],
   "fixtures": [
     fixture("b3-fan", "Tunnel Fan", "b3-r1", [("Two-Blade Fan", 0), ("Three-Blade Fan", 22), ("Turbo Ring Fan", 50)]),
     fixture("b3-dyno", "Dyno Rollers", "b3-r2", [("Steel Rollers", 0), ("Striped Rollers", 24), ("Glow Rollers", 50)]),
     fixture("b3-stand", "Showcase Plinth", "b3-r3", [("Brick Plinth", 0), ("Turntable", 22), ("Spotlight Stage", 50)])]},
]

vehicles = [
  {"id": "comet", "name": "The Comet", "nickname": "Brick Rally Coupe", "bay": "bay1", "speed": 3, "grip": 3, "boost": 2},
  {"id": "trailblazer", "name": "The Trailblazer", "nickname": "4x4 Off-Road Brick Crawler", "bay": "bay2", "speed": 2, "grip": 4, "boost": 3},
  {"id": "apex", "name": "The Apex", "nickname": "Hypercar Prototype", "bay": "bay3", "speed": 4, "grip": 2, "boost": 3},
]

# (slot, [(name, price, speed, grip, boost, source)])  first entry is the free starter
P = {
 "comet": {
  "CHASSIS": [("Rally Frame", 0, 1, 1, 0, "starter"), ("Lightweight Frame", 14, 2, 0, 0, "shop"), ("Rally Roll Cage", 34, 1, 2, 1, "shop")],
  "WHEELS":  [("Street Wheels", 0, 0, 1, 0, "starter"), ("Gravel Tyres", 12, 0, 2, 0, "shop"), ("Slick Racers", 30, 2, 1, 0, "shop")],
  "ENGINE":  [("Flat-Four Engine", 0, 1, 0, 1, "starter"), ("Twin-Turbo Engine", 18, 2, 0, 2, "shop"), ("Golden Piston V6", 0, 2, 0, 3, "blueprint")],
  "AERO":    [("Short Spoiler", 0, 0, 1, 0, "starter"), ("Rally Spoiler", 12, 1, 1, 0, "shop"), ("Swan-Neck Wing", 30, 1, 2, 1, "shop")],
  "UTILITY": [("Fog Lamps", 0, 0, 0, 1, "starter"), ("Roof Light Bar", 12, 0, 1, 1, "shop"), ("Nitro Bottles", 0, 1, 0, 2, "gear")],
  "COLOR":   [("Rally Red", 0, 0, 0, 0, "starter"), ("Ocean Blue", 12, 0, 0, 0, "shop"), ("Gold Stud", 28, 0, 0, 0, "shop")]},
 "trailblazer": {
  "CHASSIS": [("Ladder Frame", 0, 0, 1, 1, "starter"), ("Lifted Frame", 14, 0, 2, 0, "shop"), ("Rescue Cage", 34, 1, 2, 1, "shop")],
  "WHEELS":  [("All-Terrain Tyres", 0, 0, 1, 0, "starter"), ("Mud Paddles", 12, 0, 3, 0, "shop"), ("Rock Crawlers", 32, 0, 3, 1, "shop")],
  "ENGINE":  [("Diesel Thumper", 0, 0, 0, 1, "starter"), ("Torque Monster V8", 18, 1, 0, 2, "shop"), ("Clockwork Hybrid", 0, 1, 1, 2, "blueprint")],
  "AERO":    [("Roof Rack", 0, 0, 0, 0, "starter"), ("Snorkel", 12, 0, 1, 1, "shop"), ("Air Dam", 28, 1, 1, 0, "shop")],
  "UTILITY": [("Tow Hook", 0, 0, 1, 0, "starter"), ("Front Winch", 12, 0, 1, 1, "shop"), ("Floodlight Bar", 0, 0, 1, 2, "gear")],
  "COLOR":   [("Rescue Orange", 0, 0, 0, 0, "starter"), ("Forest Green", 12, 0, 0, 0, "shop"), ("Desert Sand", 26, 0, 0, 0, "shop")]},
 "apex": {
  "CHASSIS": [("Carbon Tub", 0, 1, 0, 1, "starter"), ("Featherweight Tub", 14, 2, 0, 1, "shop"), ("Ground-Effect Tub", 36, 1, 2, 1, "shop")],
  "WHEELS":  [("Sport Wheels", 0, 0, 1, 0, "starter"), ("Grip Slicks", 12, 0, 2, 0, "shop"), ("Aero Discs", 32, 2, 1, 0, "shop")],
  "ENGINE":  [("Electric Twin Motors", 0, 1, 0, 1, "starter"), ("Quad Motor Pack", 20, 2, 0, 2, "shop"), ("Fusion Prototype Core", 0, 2, 0, 3, "blueprint")],
  "AERO":    [("Nose Cone", 0, 1, 0, 0, "starter"), ("Twin-Plane Wing", 14, 1, 1, 0, "shop"), ("Active Air Brake", 32, 1, 2, 1, "shop")],
  "UTILITY": [("Light Strip", 0, 0, 0, 1, "starter"), ("Rear Thruster", 14, 1, 0, 2, "shop"), ("Boost Capacitor", 0, 1, 0, 3, "gear")],
  "COLOR":   [("Silver Arrow", 0, 0, 0, 0, "starter"), ("Midnight Purple", 12, 0, 0, 0, "shop"), ("Lime Lightning", 26, 0, 0, 0, "shop")]},
}
BLURB = {"CHASSIS": "The frame everything snaps onto.", "WHEELS": "Grip for mud, speed for straights.",
         "ENGINE": "More power for ramps and hills.", "AERO": "Air pushes the car down so it grips.",
         "UTILITY": "Handy extras for big jobs.", "COLOR": "A fresh coat of bricks."}

parts = []
for v, slots in P.items():
    for slot, items in slots.items():
        for i, (name, price, s, g, b, src) in enumerate(items):
            pid = f"{v}-{slot.lower()}-{i}"
            parts.append({"id": pid, "name": name, "slot": slot, "vehicle": v, "price": price,
                          "speed": s, "grip": g, "boost": b, "source": "starter" if src == "starter" else src,
                          "starter": src == "starter", "blurb": BLURB[slot]})

tracks = [
  {"id": "pit-lane-sprint", "name": "Pit Lane Sprint", "vehicle": "comet",
   "segments": [{"type": "ramp", "length": 18}, {"type": "straight", "length": 40}, {"type": "mud", "length": 12}, {"type": "hill", "length": 16}, {"type": "straight", "length": 30}]},
  {"id": "mud-mountain", "name": "Mud Mountain", "vehicle": "trailblazer",
   "segments": [{"type": "straight", "length": 20}, {"type": "mud", "length": 24}, {"type": "hill", "length": 24}, {"type": "ramp", "length": 16}, {"type": "mud", "length": 14}]},
  {"id": "wind-tunnel-strip", "name": "Wind Tunnel Strip", "vehicle": "apex",
   "segments": [{"type": "straight", "length": 50}, {"type": "ramp", "length": 20}, {"type": "straight", "length": 40}, {"type": "hill", "length": 12}, {"type": "mud", "length": 8}]},
]

def seg_speed(t, s, g, b):
    if t == "ramp": return 5 + 1.6*s + 0.8*b
    if t == "mud": return 2.5 + 1.5*g + 0.2*s
    if t == "hill": return 2.5 + 1.2*b + 0.6*g
    return 6 + 2.4*s + 0.3*b

def run_time(track, stats):
    s, g, b = (max(1, min(10, x)) for x in stats)
    return round(sum(seg["length"] / seg_speed(seg["type"], s, g, b) for seg in track["segments"]), 1)

# Medal times are calibrated from real builds: starter parts earn bronze, the best shop
# parts earn silver, and gold needs the best parts including reward parts. Keep these
# numbers in step with BrickVehicleModel.segmentSpeed (checked by the tools).
vbase = {v["id"]: (v["speed"], v["grip"], v["boost"]) for v in vehicles}
for t in tracks:
    v = t["vehicle"]
    def total(pick):
        s, g, b = vbase[v]
        for slot, items in P[v].items():
            name, price, ps, pg, pb, src = pick(items)
            s += ps; g += pg; b += pb
        return (s, g, b)
    # Bronze: reachable as soon as the first three slots (chassis, wheels, engine) are fitted,
    # so the very first test drives can earn a medal.
    s3, g3, b3 = vbase[v]
    for slot in ("CHASSIS", "WHEELS", "ENGINE"):
        it = P[v][slot][0]
        s3 += it[2]; g3 += it[3]; b3 += it[4]
    starter = run_time(t, (s3, g3, b3))
    best_shop = run_time(t, total(lambda items: max([x for x in items if x[5] in ("starter", "shop")], key=lambda x: x[2]+x[3]+x[4])))
    best_all = run_time(t, total(lambda items: max(items, key=lambda x: x[2]+x[3]+x[4])))
    t["medals"] = [round(best_all + 0.05, 1), round(best_shop + 0.05, 1), round(starter + 0.05, 1)]
    t["calibration"] = {"starter": starter, "bestShop": best_shop, "bestAll": best_all}

out = {"bays": bays, "vehicles": vehicles, "parts": parts, "tracks": tracks}
path = os.path.join(os.path.dirname(__file__), "..", "..", "content", "garage.json")
with open(path, "w") as f:
    json.dump(out, f, indent=1)
print("parts", len(parts), "repair stars", sum(r["stars"] for b in bays for r in b["repairs"]))
for t in tracks: print(t["id"], t["medals"], t["calibration"])
