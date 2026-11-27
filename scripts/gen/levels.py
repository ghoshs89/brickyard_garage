"""Generates content/levels/*.json and content/campaign.json.

Edit this script, not the JSON. Move limits are NOT set here: after generating, run
  ./gradlew :tools:run --args="tune content"
which plays each level 600 times with the difficulty bot and writes the move limit that puts
the bot's win rate in the middle of the level's band. The tune step keeps existing "moves"
values in the JSON, so re-running this script resets them (re-tune afterwards).
"""
import json, os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "content")


def grid(rows=8, cols=8, ch="."):
    return [[ch] * cols for _ in range(rows)]


def put(g, cells, ch):
    for r, c in cells:
        g[r][c] = ch


def rows_of(g):
    return ["".join(r) for r in g]


def rect(r0, c0, r1, c1):
    return [(r, c) for r in range(r0, r1 + 1) for c in range(c0, c1 + 1)]


def mirror(cells, cols=8):
    out = set()
    for r, c in cells:
        out.add((r, c))
        out.add((r, cols - 1 - c))
    return sorted(out)


def ring(r0, c0, r1, c1):
    """Closed conveyor loop around a rectangle, clockwise."""
    top = [(r0, c) for c in range(c0, c1 + 1)]
    right = [(r, c1) for r in range(r0 + 1, r1 + 1)]
    bottom = [(r1, c) for c in range(c1 - 1, c0 - 1, -1)]
    left = [(r, c0) for r in range(r1 - 1, r0, -1)]
    return top + right + bottom + left


def G(t, color=None, count=None):
    g = {"type": t}
    if color: g["color"] = color
    if count is not None: g["count"] = count
    return g


LEVELS = []


def level(n, title, goals, layout, difficulty="WARMUP", colors=5, floor=None, overlay=None, walls=None,
          portals=None, dispensers=None, exits=None, parts=0, conveyors=None, tutorial="", bo=""):
    bay = (n - 1) // 15 + 1
    d = {
        "id": f"b{bay}-{(n - 1) % 15 + 1:02d}",
        "kind": "STORY", "bay": bay, "index": n, "title": title, "difficulty": difficulty,
        "moves": 30, "colors": colors, "seed": 1000 + n * 37, "baseCoins": 10,
        "layout": rows_of(layout),
    }
    if floor: d["floor"] = rows_of(floor)
    if overlay: d["overlay"] = rows_of(overlay)
    if walls: d["walls"] = [{"r": r, "c": c, "side": s} for r, c, s in walls]
    if portals: d["portals"] = [{"from": list(a), "to": list(b)} for a, b in portals]
    if dispensers: d["dispensers"] = [list(x) for x in dispensers]
    if exits: d["exits"] = [list(x) for x in exits]
    if parts: d["partsTotal"] = parts
    if conveyors: d["conveyors"] = [[list(x) for x in loop] for loop in conveyors]
    d["goals"] = goals
    if tutorial: d["tutorial"] = tutorial
    if bo: d["boNote"] = bo
    LEVELS.append(d)


def top_row(g):
    return [(0, c) for c in range(len(g[0])) if g[0][c] == "."]


def bottom_row(g):
    r = len(g) - 1
    return [(r, c) for c in range(len(g[0])) if g[r][c] not in "#"]


# ============================================================ Bay 1: Classic Pit Bay
L = grid()
level(1, "Rolling Start", [G("COLOR", "RED", 40)], L,
      tutorial="Swipe two neighbours to line up 3 matching pieces. Collect the Red Bricks!")

L = grid()
level(2, "Nitro Lesson", [G("COLOR", "BLUE", 45), G("COLOR", "YELLOW", 45)], L,
      tutorial="Line up 4 to build a Nitro Booster. Tap it to blast a whole line!")

L = grid(); put(L, [(0, 0), (0, 7), (7, 0), (7, 7)], "#")
level(3, "Buggy Bay", [G("COLOR", "GREEN", 60)], L,
      tutorial="Make a 2x2 square for an RC Brick Buggy. It drives itself to a goal!")

L = grid()
level(4, "Airbag Check", [G("COLOR", "RED", 50), G("COLOR", "ORANGE", 50)], L,
      tutorial="Match in an L or T shape to build a Shockwave Airbag. Boom!")

L = grid()
level(5, "Color Sprayer", [G("COLOR", "BLUE", 40), G("COLOR", "GREEN", 40), G("COLOR", "YELLOW", 40)], L,
      tutorial="Five in a row makes a Master Color Sprayer. Swap it with a piece to clear that whole colour!")

L = grid(); put(L, rect(5, 0, 5, 7), "1"); put(L, rect(6, 0, 7, 7), "2")
level(6, "Stud Crates", [G("CRATE")], L,
      tutorial="Stud Crates pop when you match right next to them.")

L = grid(); put(L, [(3, 3), (3, 4), (4, 3), (4, 4)], "#"); put(L, mirror([(6, 0), (6, 1), (7, 0), (7, 1), (7, 2)]), "2")
level(7, "Cutout Corner", [G("CRATE"), G("COLOR", "ORANGE", 16)], L,
      tutorial="Some crates have 2 layers of studs. Hit them twice!")

L = grid(); put(L, rect(5, 0, 7, 7), "2"); put(L, [(5, 3), (5, 4), (7, 3), (7, 4)], "3")
level(8, "Stacked Crates", [G("CRATE")], L, difficulty="CHALLENGE",
      bo="This stack took me five tries in '78. Pop the top row first, so pieces can fall in and help.")

L = grid(); O = grid(); put(O, rect(5, 0, 6, 7), "1"); put(O, [(3, 1), (3, 6), (4, 3), (4, 4)], "2")
level(9, "Wheel Clamps", [G("CLAMP")], L, overlay=O,
      tutorial="A Wheel Clamp locks a piece in place. Match the clamped piece to take the clamp off.")

L = grid(); put(L, rect(6, 0, 7, 7), "1"); O = grid(); put(O, rect(3, 1, 4, 6), "1")
level(10, "Clamp and Crate", [G("CLAMP"), G("CRATE")], L, overlay=O)

L = grid()
level(11, "Rare Parts", [G("PART", count=4)], L, dispensers=[(0, 2), (0, 5)], exits=bottom_row(L), parts=4,
      tutorial="Rare Car Parts drop from the dispensers at the top. Clear the pieces below a part to bring it down to the exit bay.")

L = grid(); put(L, [(6, 0), (6, 1), (6, 6), (6, 7), (7, 0), (7, 1), (7, 6), (7, 7)], "2")
level(12, "Parts Under Crates", [G("PART", count=4), G("CRATE")], L, difficulty="CHALLENGE",
      dispensers=[(0, 3), (0, 4)], exits=[(7, 2), (7, 3), (7, 4), (7, 5)], parts=4,
      bo="Parts only fall straight down, so clear a path under them. I always checked the path before I pulled a part.")

L = grid(); put(L, [(0, 0), (0, 1), (1, 0), (0, 6), (0, 7), (1, 7), (6, 0), (7, 0), (7, 1), (6, 7), (7, 6), (7, 7)], "#")
O = grid(); put(O, [(3, 2), (3, 5), (4, 2), (4, 5), (2, 3), (2, 4)], "2")
level(13, "Diamond Clamps", [G("CLAMP"), G("COLOR", "YELLOW", 40)], L, overlay=O,
      tutorial="Double clamps need two matches.")

L = grid(); O = grid(); put(O, [(4, 1), (4, 6), (5, 3), (5, 4)], "1")
level(14, "Clamped Path", [G("PART", count=3), G("CLAMP")], L, overlay=O, dispensers=[(0, 1), (0, 6)],
      exits=bottom_row(L), parts=3)

L = grid(); put(L, rect(6, 2, 7, 5), "2"); O = grid(); put(O, [(3, 0), (3, 7), (4, 1), (4, 6)], "1")
level(15, "Pit Bay Finale", [G("CRATE"), G("CLAMP"), G("PART", count=2)], L, difficulty="CHALLENGE", overlay=O,
      dispensers=[(0, 0), (0, 7)], exits=[(7, 0), (7, 1), (7, 6), (7, 7)], parts=2,
      bo="Last job in the pit bay! Parts come down the sides. Build a Nitro to clear a whole column for them.")

# ============================================================ Bay 2: Off-Road & Rescue Rig Bay
L = grid(); put(L, [(5, 0), (5, 3), (5, 4), (5, 7), (7, 1), (7, 6), (6, 2), (6, 5)], "~")
level(16, "Oil Spill", [G("OIL"), G("COLOR", "BLUE", 35)], L,
      tutorial="Oil spreads if you don't clean any up on your turn. Match next to it to mop it up!")

L = grid(); put(L, [(4, 0), (4, 7), (6, 2), (6, 5), (7, 0), (7, 7), (7, 3), (7, 4)], "~")
level(17, "Slippery Floor", [G("OIL"), G("COLOR", "GREEN", 45)], L)

L = grid(); F = grid(); put(F, rect(2, 0, 7, 7), "r")
level(18, "Rust Plates", [G("RUST")], L, floor=F,
      tutorial="Rust Plates are under the pieces. Clear the piece on top to polish the rust away.")

L = grid(); F = grid(); put(F, rect(1, 1, 6, 6), "r"); put(L, rect(7, 0, 7, 7), "2")
level(19, "Rusty Crates", [G("RUST"), G("CRATE")], L, floor=F)

L = grid(); F = grid(); put(F, rect(4, 0, 7, 7), "r"); put(L, [(1, 1), (1, 6), (2, 3), (2, 4), (0, 0), (0, 7)], "~")
level(20, "Mud and Rust", [G("RUST"), G("OIL"), G("COLOR", "YELLOW", 45)], L, difficulty="CHALLENGE", floor=F,
      bo="Rust at the bottom, oil at the top. Keep the oil busy every turn, or it will win the race.")

L = grid(); walls = [(3, c, "below") for c in range(8)]
level(21, "Steel Bulkheads", [G("COLOR", "ORANGE", 55), G("COLOR", "BLUE", 55)], L, walls=walls,
      tutorial="Steel Bulkheads block swaps and falling. New pieces drop in just under them.")

L = grid(); walls = [(3, c, "below") for c in range(8)]; F = grid(); put(F, rect(4, 0, 7, 7), "r"); put(F, rect(0, 2, 1, 5), "r")
level(22, "Drop Chute", [G("RUST")], L, walls=walls, floor=F, portals=[((3, 0), (4, 3)), ((3, 7), (4, 4))],
      tutorial="Drop Chutes send falling pieces from one section to another. Watch where they come out!")

L = grid(); walls = [(3, c, "below") for c in range(8)]
level(23, "Chute Delivery", [G("PART", count=4)], L, walls=walls, portals=[((3, 1), (4, 1)), ((3, 6), (4, 6))],
      dispensers=[(0, 1), (0, 6)], exits=bottom_row(L), parts=4)

L = grid(); walls = [(r, 3, "right") for r in range(8)]; put(L, [(0, 0), (1, 1), (0, 7), (1, 6), (2, 2), (2, 5)], "~"); O = grid(); put(O, rect(5, 0, 6, 7), "1"); put(O, [(7, 1), (7, 6)], "2")
level(24, "Split Garage", [G("OIL"), G("CLAMP")], L, difficulty="CHALLENGE", walls=walls, overlay=O,
      bo="A bulkhead down the middle means two jobs, not one. Do the left, then the right. Slow is fine.")

L = grid()
level(25, "Assembly Line", [G("COLOR", "RED", 60), G("COLOR", "YELLOW", 60)], L, conveyors=[ring(2, 2, 5, 5)],
      tutorial="Assembly Lines move everything on them one step after each move. Plan ahead!")

L = grid(); put(L, [(3, 3), (3, 4), (4, 3), (4, 4)], "2"); put(L, rect(7, 0, 7, 7), "2")
level(26, "Crates on the Line", [G("CRATE"), G("COLOR", "GREEN", 50)], L, conveyors=[ring(2, 2, 5, 5)])

L = grid(); put(L, [(1, 1), (1, 6), (6, 1), (6, 6), (3, 1), (4, 6), (0, 0), (0, 7), (7, 0), (7, 7)], "~")
level(27, "Oily Conveyor", [G("OIL"), G("COLOR", "ORANGE", 85)], L, difficulty="CHALLENGE", conveyors=[ring(2, 2, 5, 5)],
      bo="The line carries the oil around. Wait for a spill to ride next to a good match, then strike.")

L = grid(); F = grid(); put(F, rect(3, 0, 6, 7), "r")
level(28, "Rusty Delivery", [G("RUST"), G("PART", count=3)], L, floor=F, dispensers=[(0, 3), (0, 4)],
      exits=bottom_row(L), parts=3)

L = grid(); F = grid(); put(F, rect(5, 0, 7, 7), "r"); put(L, [(3, 0), (3, 7), (2, 3), (2, 4)], "~"); O = grid(); put(O, [(4, 2), (4, 3), (4, 4), (4, 5)], "2")
level(29, "Clean Sweep", [G("RUST"), G("OIL"), G("CLAMP")], L, floor=F, overlay=O)

L = grid(); walls = [(3, c, "below") for c in range(8)]; F = grid(); put(F, rect(4, 0, 7, 7), "r"); put(L, [(1, 2), (1, 5), (0, 0), (0, 7), (2, 3)], "~")
level(30, "Rescue Rig Finale", [G("RUST"), G("OIL"), G("COLOR", "GREEN", 45)], L, difficulty="CHALLENGE", walls=walls, floor=F,
      portals=[((3, 3), (4, 3)), ((3, 4), (4, 4))],
      bo="Big finish! The chutes in the middle feed the bottom. Keep the middle busy and the rust will follow.")

# ============================================================ Bay 3: Aero Prototype Lab
L = grid(); F = grid(); put(F, [(7, 3), (7, 4)], "t")
level(31, "Track Baseplates", [G("TRACK", count=58)], L, floor=F,
      tutorial="Match on the checkered track to spread it to every piece in the match. Cover the floor!")

L = grid(); F = grid(); put(F, [(0, 0), (0, 7)], "t"); put(L, rect(6, 0, 7, 7), "2")
level(32, "Track and Crates", [G("TRACK", count=54), G("CRATE")], L, floor=F)

L = grid(); F = grid(); put(F, [(7, 0), (7, 7)], "t"); put(L, [(3, 2), (3, 5), (5, 3), (0, 0), (0, 7), (5, 4)], "~")
level(33, "Oily Track", [G("TRACK", count=52), G("OIL")], L, difficulty="CHALLENGE", floor=F,
      bo="Power-ups fired from the track paint every cell they hit. Build them on the checkered squares!")

L = grid(); F = grid(); put(F, [(3, 3), (3, 4)], "t")
level(34, "Parts on Track", [G("TRACK", count=48), G("PART", count=3)], L, floor=F, dispensers=[(0, 0), (0, 7)],
      exits=bottom_row(L), parts=3)

L = grid(); F = grid(); put(F, [(3, 3), (4, 4)], "t")
level(35, "Track on the Line", [G("TRACK", count=62), G("COLOR", "RED", 80)], L, floor=F, conveyors=[ring(2, 2, 5, 5)])

L = grid(); walls = [(3, c, "below") for c in range(8)]; F = grid(); put(F, rect(4, 0, 7, 7), "r"); put(F, rect(0, 0, 2, 7), "r")
level(36, "Wind Tunnel Chutes", [G("RUST"), G("COLOR", "BLUE", 45)], L, difficulty="CHALLENGE", walls=walls, floor=F,
      portals=[((3, 0), (4, 7)), ((3, 7), (4, 0))],
      bo="The chutes cross over. Pieces from the left come out on the right. Bo's favourite trick.")

L = grid(); walls = [(r, 3, "right") for r in range(4, 8)]; F = grid(); put(F, [(7, 0), (7, 7)], "t")
level(37, "Half Bulkhead", [G("TRACK", count=52)], L, walls=walls, floor=F)

L = grid(); put(L, [(7, 0), (7, 1), (7, 6), (7, 7)], "2"); O = grid(); put(O, [(3, 2), (3, 5), (5, 1), (5, 6)], "1")
level(38, "Six-Colour Clamps", [G("CLAMP"), G("CRATE"), G("COLOR", "PURPLE", 14)], L, colors=6, overlay=O)

L = grid(); F = grid(); put(F, [(7, 3)], "t"); put(L, [(2, 2), (2, 5), (0, 0), (0, 7), (1, 3), (1, 4)], "~")
level(39, "Track, Line and Oil", [G("TRACK", count=56), G("OIL"), G("COLOR", "YELLOW", 75)], L, difficulty="CHALLENGE", floor=F,
      conveyors=[ring(3, 2, 6, 5)],
      bo="Three jobs at once. Pick one, finish it, then the next. That's how we built the dyno.")

L = grid(); walls = [(3, c, "below") for c in range(8)]; F = grid(); put(F, rect(4, 0, 7, 7), "r")
level(40, "Prototype Delivery", [G("PART", count=3), G("RUST")], L, walls=walls, floor=F,
      portals=[((3, 2), (4, 2)), ((3, 5), (4, 5))], dispensers=[(0, 2), (0, 5)], exits=bottom_row(L), parts=3)

L = grid(9, 9); put(L, [(4, 4)], "#"); F = grid(9, 9); put(F, [(8, 4)], "t"); put(L, rect(7, 0, 8, 2) + rect(7, 6, 8, 8), "2")
level(41, "Big Board", [G("TRACK", count=70), G("CRATE")], L, floor=F)

L = grid(); put(L, [(2, 1), (2, 6), (0, 3), (0, 4), (1, 0), (1, 7)], "~"); F = grid(); put(F, rect(5, 0, 7, 7), "r"); O = grid(); put(O, rect(3, 2, 4, 5), "2")
level(42, "Lab Clean-Up", [G("OIL"), G("RUST"), G("CLAMP")], L, difficulty="CHALLENGE", floor=F, overlay=O,
      bo="Clamps in the middle, rust at the bottom. A Sprayer here saves a lot of moves. Look for five.")

L = grid(); put(L, [(0, 3), (0, 4)], "#"); F = grid(); put(F, [(7, 0)], "t")
level(43, "Spotlight Track", [G("TRACK", count=36), G("COLOR", "BLUE", 16)], L, floor=F)

L = grid(); put(L, rect(6, 0, 7, 7), "2"); F = grid(); put(F, [(0, 0)], "t")
level(44, "Stage Building", [G("CRATE"), G("TRACK", count=44)], L, floor=F, conveyors=[ring(2, 2, 5, 5)])

L = grid(); walls = [(3, c, "below") for c in range(8)]; F = grid(); put(F, [(7, 3), (7, 4)], "t"); put(L, [(1, 1), (1, 6), (0, 3), (0, 4), (2, 0), (2, 7)], "~")
O = grid(); put(O, rect(5, 0, 5, 7), "2")
level(45, "Car Show Finale", [G("TRACK", count=46), G("OIL"), G("CLAMP")], L, difficulty="CHALLENGE", walls=walls, floor=F,
      overlay=O, portals=[((3, 3), (4, 3)), ((3, 4), (4, 4))],
      bo="Show night. Everything you've learned, all at once. Take your time. The crowd will wait.")

# ============================================================ write
os.makedirs(os.path.join(ROOT, "levels"), exist_ok=True)
existing_moves = {}
for d in LEVELS:
    path = os.path.join(ROOT, "levels", d["id"] + ".json")
    if os.path.exists(path):
        try:
            existing_moves[d["id"]] = json.load(open(path))["moves"]
        except Exception:
            pass
    if d["id"] in existing_moves:
        d["moves"] = existing_moves[d["id"]]
    with open(path, "w") as f:
        json.dump(d, f, indent=1)
        f.write("\n")

BP_AFTER = [3, 6, 9, 12, 15, 19, 22, 26, 30, 34, 38, 42]  # story level numbers
blueprints = [
    {"id": f"bp-{k:02d}", "unlockAfter": f"b{(BP_AFTER[k - 1] - 1) // 15 + 1}-{(BP_AFTER[k - 1] - 1) % 15 + 1:02d}",
     "rewardCoins": 15 + 5 * (k // 4), "rewardTool": ["WRENCH", "CLAW", "BROOM"][(k - 1) % 3]}
    for k in range(1, 13)
]
# Reward parts: the golden engines come from Blueprints 4, 8 and 12.
blueprints[3]["rewardPart"] = "comet-engine-2"
blueprints[7]["rewardPart"] = "trailblazer-engine-2"
blueprints[11]["rewardPart"] = "apex-engine-2"

gears = []
repair_for = ["b1-r1", "b1-r3", "b1-r5", "b2-r1", "b2-r3", "b2-r5", "b3-r1", "b3-r3", "b3-r5"]
gear_parts = {2: "comet-utility-2", 5: "trailblazer-utility-2", 8: "apex-utility-2"}
for k in range(1, 10):
    g = {"id": f"gear-{k:02d}", "unlockRepair": repair_for[k - 1], "rewardCoins": 15,
         "rewardTool": ["CLAW", "WRENCH", "BROOM"][(k - 1) % 3]}
    if k - 1 in gear_parts:
        g["rewardPart"] = gear_parts[k - 1]
    gears.append(g)

campaign = {"story": [d["id"] for d in LEVELS], "levelsPerBay": 15, "blueprints": blueprints, "gears": gears,
            "trackDayUnlock": "b3-r5"}
with open(os.path.join(ROOT, "campaign.json"), "w") as f:
    json.dump(campaign, f, indent=1)
    f.write("\n")
print(len(LEVELS), "levels;", "blueprints unlock after", [b["unlockAfter"] for b in blueprints])
