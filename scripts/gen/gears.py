"""Generates content/gears/*.json. Edit here, then run:
  ./gradlew :tools:run --args="gears content"   (proves each puzzle and records a witness)
Coordinates are half-units on the pegboard. Radius in half-units = teeth / 8.
Belts join fixed gears by their index among the non-axle fixed gears.
"""
import json, os
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "content", "gears")

def g(x, y, teeth, role="fixed", wantDir="", wantSpeed=0, label=""):
    d = {"x": x, "y": y, "teeth": teeth, "role": role}
    if wantDir: d["wantDir"] = wantDir
    if wantSpeed: d["wantSpeed"] = wantSpeed
    if label: d["label"] = label
    return d

P = []
def puzzle(k, bay, title, prompt, lesson, bo, fixed, tray, maxPlace, belts=(), blocked=(), w=18, h=10, tool=None, coins=15):
    P.append({"id": f"gear-{k:02d}", "index": k, "bay": bay, "title": title, "prompt": prompt, "lesson": lesson,
              "boNote": bo, "w": w, "h": h, "fixed": fixed, "tray": list(tray), "maxPlace": maxPlace,
              "belts": [{"a": a, "b": b, "crossed": c} for a, b, c in belts],
              "blocked": [list(r) for r in blocked], "rewardCoins": coins})

puzzle(1, 1, "First Gear", "Make the lift wheel turn the SAME way as the motor.",
       "Gears that touch turn opposite ways. One gear in between makes the wheel match the motor.",
       "Every machine in this garage starts with one gear touching another.",
       [g(3, 5, 16, "motor", label="Motor"), g(11, 5, 16, "output", "CW", label="Lift wheel")], (2, 1, 0), 2)

puzzle(2, 1, "Turn It Around", "Make the wheel turn the OPPOSITE way to the motor.",
       "Each gear in the chain flips the direction. Count the flips!",
       "A big gear can bridge the gap, but which way will the wheel turn then?",
       [g(3, 5, 16, "motor", label="Motor"), g(13, 5, 16, "output", "CCW", label="Paint fan")], (1, 1, 1), 2)

puzzle(3, 1, "Twice as Fast", "Pick a gear for the empty axle so it spins TWICE as fast as the motor.",
       "A small gear driven by a bigger one spins faster. 16 teeth driving 8 teeth = twice as fast.",
       "Count the teeth. The motor has 16.",
       [g(3, 5, 16, "motor", label="Motor"), g(10, 5, 8, "axle", wantSpeed=2, label="Sign spinner")], (1, 1, 1), 2)

puzzle(4, 2, "Slow and Strong", "Pick a gear for the winch axle so it turns at HALF the motor's speed.",
       "A big gear driven by a small one turns slower but stronger. That's how a winch lifts an engine.",
       "Heavy jobs want slow, strong gears. Fast is not always better.",
       [g(3, 5, 8, "motor", label="Motor"), g(10, 5, 8, "axle", wantSpeed=0.5, label="Winch drum")], (1, 2, 1), 2)

puzzle(5, 2, "Around the Block", "The toolbox is in the way. Get the drive around it to the crane.",
       "When the straight path is blocked, a chain of gears can go around. Direction still flips at every gear.",
       "I once spent a whole afternoon on this. Then I tried going underneath.",
       [g(3, 2, 16, "motor", label="Motor"), g(15, 2, 16, "output", label="Crane hook")], (2, 4, 1), 4,
       blocked=[(6, 0, 12, 4)])

puzzle(6, 2, "Crossed Belt", "The belt is crossed. Connect the motor and the floodlight so the light turns CLOCKWISE.",
       "A straight belt keeps the direction. A crossed belt flips it, just like a gear.",
       "Belts are like gears that can reach far away.",
       [g(3, 5, 16, "motor", label="Motor"), g(7, 2, 8, "fixed", label="Pulley A"), g(12, 2, 8, "fixed", label="Pulley B"),
        g(15, 6, 16, "output", "CW", label="Floodlight")], (2, 2, 1), 3, belts=[(1, 2, True)])

puzzle(7, 3, "Diagonal Teeth", "Gears can mesh on a slant too. Reach the dyno roller.",
       "Gears mesh when their centres are exactly the sum of their sizes apart, even on a diagonal (3 across, 4 up makes 5).",
       "Straight lines aren't the only lines. Try a slant.",
       [g(3, 3, 16, "motor", label="Motor"), g(14, 10, 16, "output", label="Dyno roller")], (2, 2, 2), 3,
       blocked=[(5, 0, 18, 4)], h=12)

puzzle(8, 3, "Two Wheels", "Drive BOTH wind-tunnel fans: the left one clockwise, the right one anticlockwise.",
       "One motor can drive many gears. Each branch flips direction on its own.",
       "Two jobs from one motor. Plan the branches.",
       [g(9, 5, 16, "motor", label="Motor"), g(2, 5, 8, "output", "CW", label="Left fan"),
        g(16, 5, 8, "output", "CCW", label="Right fan")], (2, 2, 2), 3)

puzzle(9, 3, "Grand Gearbox", "Make the show turntable spin THREE times as fast as the motor, clockwise.",
       "24 teeth driving 8 teeth is three times as fast. Idler gears in between flip direction but don't change speed.",
       "My last puzzle for you. Speed, direction and a tight squeeze, all at once.",
       [g(3, 5, 24, "motor", label="Motor"), g(13, 5, 8, "axle", "CW", 3, label="Turntable")], (2, 3, 2), 4,
       blocked=[(6, 0, 10, 4)], h=12)

os.makedirs(OUT, exist_ok=True)
for p in P:
    path = os.path.join(OUT, p["id"] + ".json")
    if os.path.exists(path):
        old = json.load(open(path))
        if "witness" in old: p["witness"] = old["witness"]
    with open(path, "w") as f:
        json.dump(p, f, indent=1)
        f.write("\n")
print(len(P), "gear puzzles")
