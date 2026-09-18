#!/usr/bin/env python3
"""
Android UI snapshot harness (Wave 0 of the Android UI redesign).

Installs a debug APK on a connected device/emulator, plays through the new-game
wizard by locating UI nodes by text (uiautomator dump), then sweeps the drawer
destinations, capturing a PNG per screen via `adb exec-out screencap`.

Usage:
    python scripts/android_snapshots.py [--apk path.apk] [--out dir] [--label name]

Output: preview/android-snapshots/<label>/NN_name.png (+ harness.log).
The "before" baseline is captured with --label before; later waves re-run with
their own label to produce before/after evidence.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "antdroid.cfbcoach"
HOME_ACTIVITY = "antdroid.cfbcoach/.Home"
MAIN_ACTIVITY = "antdroid.cfbcoach/.MainActivity"
WIZARD_DONE_MARKERS = ("Start Season", "Quick Actions", "Sim Through Postseason")

# Drawer destinations to sweep: (label, drawer text, occurrence). Occurrence
# matters because the Team and League groups reuse labels ("Team Stats" etc.).
# None = actions skipped on purpose (Save Game writes state; bulk sims advance
# the league; exit quits).
DRAWER_SWEEP = [
    ("roster", "Roster", 1),
    ("depth_chart", "Depth Chart", 1),
    ("redshirts", "Redshirts", 1),
    ("schemes", "Schemes", 1),
    ("my_coach", "My Coach", 1),
    ("player_stats", "Player Stats", 1),
    ("team_stats", "Team Stats", 1),
    ("schedule", "Schedule", 1),
    ("standings", "Standings", 1),
    ("rankings", "Rankings", 1),
    ("scores", "Scores", 1),
    ("player_search", "Player Search", 1),
    ("transfer_portal", "Transfer Portal", 1),
    ("news", "News", 1),
    ("league_team_stats", "Team Stats", 2),
    ("league_player_stats", "Player Stats", 2),
    ("awards", "Awards", 1),
    ("history_records", "History & Records", 1),
    ("coach_database", "Coach Database", 1),
    ("postseason", "Postseason", 1),
]


def find_adb():
    import shutil
    adb = shutil.which("adb")
    if adb:
        return adb
    local = os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk",
                         "platform-tools", "adb.exe")
    if os.path.exists(local):
        return local
    sys.exit("adb not found (PATH or LOCALAPPDATA/Android/Sdk)")


class Harness:
    def __init__(self, adb, outdir, log):
        self.adb = adb
        self.outdir = outdir
        self.log = log
        os.makedirs(outdir, exist_ok=True)
        self.shot_index = 0

    def run(self, *args, timeout=60, binary=False):
        result = subprocess.run([self.adb, *args], capture_output=True,
                                timeout=timeout)
        return result.stdout if binary else result.stdout.decode("utf-8", "replace").strip()

    def note(self, msg):
        line = f"[{time.strftime('%H:%M:%S')}] {msg}"
        print(line)
        self.log.write(line + "\n")
        self.log.flush()

    def wait_device(self):
        self.run("wait-for-device", timeout=120)
        for _ in range(30):
            boot = self.run("shell", "getprop", "sys.boot_completed")
            if boot.strip() == "1":
                return
            time.sleep(2)
        sys.exit("device never finished booting")

    def install(self, apk):
        out = self.run("install", "-r", "-t", apk, timeout=180)
        self.note(f"install: {out.splitlines()[-1] if out else '?'}")

    def launch(self, activity):
        self.run("shell", "am", "start", "-n", activity)
        time.sleep(2.5)

    def dump_nodes(self):
        """Return the current UI hierarchy as a list of dicts (text, desc, bounds, clickable)."""
        for attempt in range(3):
            self.run("shell", "uiautomator", "dump", "/sdcard/cfhc_ui.xml", timeout=30)
            raw = self.run("shell", "cat", "/sdcard/cfhc_ui.xml", timeout=15)
            if "<hierarchy" in raw:
                break
            time.sleep(1.5)
        else:
            return []
        try:
            root = ET.fromstring(raw)
        except ET.ParseError:
            return []
        nodes = []
        for el in root.iter("node"):
            nodes.append({
                "text": el.get("text", ""),
                "desc": el.get("content-desc", ""),
                "bounds": el.get("bounds", ""),
                "clickable": el.get("clickable") == "true",
                "class": el.get("class", ""),
            })
        return nodes

    @staticmethod
    def center(bounds):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if not m:
            return None
        x1, y1, x2, y2 = map(int, m.groups())
        return (x1 + x2) // 2, (y1 + y2) // 2

    @staticmethod
    def pick(nodes, contains, clickable_only=False):
        """Exact text match first, then substring — a plain contains scan hits
        dialog message text (e.g. 'default' inside the prestige prompt)."""
        for n in nodes:
            if n["text"] == contains:
                return n
        for n in nodes:
            if contains.lower() in n["text"].lower():
                if clickable_only and not (n["clickable"] or n["class"].endswith("Button")):
                    continue
                return n
        return None

    def find(self, contains=None, clickable_only=True, desc=False):
        """First node whose text (or content-desc) contains the given string."""
        for n in self.dump_nodes():
            hay = n["desc"] if desc else n["text"]
            if contains and contains.lower() in hay.lower():
                if not clickable_only or n["clickable"]:
                    c = self.center(n["bounds"])
                    if c:
                        return c
        return None

    def tap(self, x, y):
        self.run("shell", "input", "tap", str(x), str(y))

    def tap_text(self, contains, desc=False, required=True):
        nodes = [n for n in self.dump_nodes()
                 if (n["desc"] if desc else n["text"])]
        n = self.pick(nodes, contains)
        if n:
            c = self.center(n["bounds"])
            if c:
                self.tap(*c)
                time.sleep(1.6)
                return True
        if required:
            self.note(f"WARN: node with text '{contains}' not found")
        return False

    def tap_occurrence(self, contains, occurrence):
        """Tap the Nth exact-text match, ordered top to bottom (drawer groups
        reuse labels across sections)."""
        matches = [n for n in self.dump_nodes() if n["text"] == contains]
        matches.sort(key=lambda n: self.center(n["bounds"])[1])
        if len(matches) < occurrence:
            return False
        c = self.center(matches[occurrence - 1]["bounds"])
        if c:
            self.tap(*c)
            time.sleep(1.6)
            return True
        return False

    def back(self):
        self.run("shell", "input", "keyevent", "4")
        time.sleep(1.2)

    def screenshot(self, name):
        self.shot_index += 1
        path = os.path.join(self.outdir, f"{self.shot_index:02d}_{name}.png")
        data = self.run("exec-out", "screencap", "-p", binary=True)
        with open(path, "wb") as f:
            f.write(data)
        self.note(f"captured {os.path.basename(path)} ({len(data) // 1024} KB)")

    def foreground_is_main(self):
        out = self.run("shell", "dumpsys", "activity", "activities", timeout=30)
        return "antdroid.cfbcoach/.MainActivity" in out

    # --- new-game wizard ---------------------------------------------------

    def play_wizard(self):
        """Tap through the new-game chain until the dashboard markers appear."""
        # "Start New Career" is deliberately absent: main() taps it before the
        # wizard starts — matching it here would restart the loop if a step
        # ever lands back on Home. Texts are exact dialog/button labels.
        preference = ["DEFAULT", "Apply Settings", "Continue",
                      "Balanced Leader", "OK", "NEXT", "Next"]
        for step in range(40):
            nodes = self.dump_nodes()
            texts = [n["text"] for n in nodes if n["text"]]
            self.note(f"wizard step {step}: {texts[:5]}")
            if any(any(m.lower() in t.lower() for m in WIZARD_DONE_MARKERS) for t in texts):
                self.note(f"wizard complete after {step} steps")
                return True
            # team picker: rows look like "Big Ten:  Iowa  [68]" (any conference)
            picker_open = any("Choose Your Program" in t for t in texts)
            if picker_open:
                row = next((n for n in nodes if re.match(
                    r"[A-Za-z0-9 .&'-]+:\s+\S.*\[\d+\]", n["text"])), None)
                if row:
                    c = self.center(row["bounds"])
                    if c:
                        self.tap(*c)
                        time.sleep(6)  # league generation
                        continue
            # Generic "Choose Your X" wizard pages (schemes etc.): the title is
            # texts[0] and the options follow — tap the first option.
            if texts and texts[0].startswith("Choose Your") and len(texts) > 1:
                hit = next((n for n in nodes if n["text"] == texts[1]), None)
                if hit:
                    c = self.center(hit["bounds"])
                    if c:
                        self.tap(*c)
                        time.sleep(2.2)
                        continue

            tapped = False
            for want in preference:
                hit = self.pick(nodes, want)
                if hit:
                    c = self.center(hit["bounds"])
                    if c:
                        self.tap(*c)
                        time.sleep(2.2)
                        tapped = True
                        break
            if not tapped:
                # fall back to the first enabled button on screen
                for n in nodes:
                    if n["class"].endswith("Button") and n["clickable"]:
                        c = self.center(n["bounds"])
                        if c:
                            self.tap(*c)
                            time.sleep(2.2)
                            tapped = True
                            break
            if not tapped:
                # Dialog controls can sit below the fold inside their
                # ScrollView — scroll the dialog and retry before stalling.
                if step % 3 != 2:  # allow up to 2 recovery scrolls per stall
                    self.run("shell", "input", "swipe", "540", "1600", "540", "700", "400")
                    time.sleep(1.2)
                    continue
                self.screenshot("wizard_stalled")
                self.note(f"wizard stalled at step {step}; texts={texts[:6]}")
                return False
        self.screenshot("wizard_exhausted")
        return False

    # --- in-game sweep -----------------------------------------------------

    def open_drawer(self):
        for attempt in range(4):
            if self.find("Close navigation drawer", clickable_only=False, desc=True):
                return True
            c = self.find("Open navigation drawer", clickable_only=False, desc=True)
            if c:
                self.tap(*c)
                time.sleep(1.6)
                return True
            if attempt == 1:
                # singleTop: brings the existing task back, drawer reachable
                self.run("shell", "am", "start", "-n", MAIN_ACTIVITY)
                time.sleep(2.5)
            elif attempt == 2:
                # toolbar hamburger sits at a stable position in the app bar
                self.tap(74, 212)
                time.sleep(1.6)
            else:
                self.back()
                time.sleep(1.2)
        return self.find("Close navigation drawer", clickable_only=False, desc=True) is not None

    def close_overlay(self):
        """Dismiss whatever the last destination opened: prefer a footer
        button, fall back to BACK."""
        for label in ("Close", "OK", "Done", "Dismiss", "Back"):
            hit = self.pick(self.dump_nodes(), label)
            if hit and (hit["clickable"] or hit["class"].endswith("Button")):
                c = self.center(hit["bounds"])
                if c:
                    self.tap(*c)
                    time.sleep(1.2)
                    return True
        self.back()
        time.sleep(1.0)
        return False

    def sweep(self):
        self.screenshot("dashboard_home")
        for label, drawer_text, occurrence in DRAWER_SWEEP:
            if not self.foreground_is_main():
                self.note("left MainActivity unexpectedly; ending sweep")
                return
            if not self.open_drawer():
                self.note(f"drawer unreachable before {label}; skipping rest")
                return
            if not self.tap_occurrence(drawer_text, occurrence):
                found = False
                for scroll in (("368", "1700", "368", "1100"), ("368", "1900", "368", "600")):
                    self.run("shell", "input", "swipe", *scroll, "400")
                    time.sleep(1.2)
                    if self.tap_occurrence(drawer_text, occurrence):
                        found = True
                        break
                if not found:
                    self.note(f"SKIP {label}: '{drawer_text}' (occurrence {occurrence}) not found in drawer")
                    self.back()
                    continue
            time.sleep(1.6)
            self.screenshot(label)
            # dismiss whatever opened (dialog or list page), then go back home
            self.close_overlay()
            if not self.foreground_is_main():
                self.note(f"BACK left the app after {label}; stopping")
                return
            self.tap_text("Home", required=False)


def main():
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap = argparse.ArgumentParser()
    ap.add_argument("--apk", default=os.path.join(repo, "app/build/outputs/apk/debug/app-debug.apk"))
    ap.add_argument("--label", default="run")
    ap.add_argument("--out", default=None)
    ap.add_argument("--skip-install", action="store_true")
    args = ap.parse_args()

    outdir = args.out or os.path.join(repo, "preview", "android-snapshots", args.label)
    os.makedirs(outdir, exist_ok=True)
    log = open(os.path.join(outdir, "harness.log"), "w", encoding="utf-8")

    adb = find_adb()
    h = Harness(adb, outdir, log)
    h.note(f"snapshot run '{args.label}' -> {outdir}")
    h.wait_device()
    if not args.skip_install:
        h.install(args.apk)

    h.run("shell", "am", "force-stop", PKG)
    h.launch(HOME_ACTIVITY)
    time.sleep(3)
    # dismiss the first-run welcome dialog if present
    h.tap_text("Let's go", required=False)
    time.sleep(1.5)
    h.screenshot("home_menu")

    if h.tap_text("Start New Career", required=False):
        if h.play_wizard():
            h.sweep()
        else:
            h.note("wizard did not complete; captured home only")
    else:
        h.note("Start New Career not found (existing UI state?)")

    h.run("shell", "am", "force-stop", PKG)
    h.note(f"done: {h.shot_index} screenshots")


if __name__ == "__main__":
    main()
