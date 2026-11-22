#!/usr/bin/env python3
"""WCAG contrast check for the Android colour tokens, per theme.

The dark (Midnight) palette is src/main/res/values/colors.xml; the light theme
overrides it from src/main/res/values-notnight/colors.xml (MainActivity & co.
force the night qualifier from the theme setting). This resolves @color
aliases, composites translucent tokens over the surface they are drawn on, and
prints the contrast ratio of each text/icon pair against its WCAG AA target:
4.5:1 for body text, 3:1 for large text, icons and fills.

    python3 scripts/check_theme_contrast.py            # light theme
    python3 scripts/check_theme_contrast.py --dark     # dark theme (informational)

Exits non-zero if a light-theme pair misses its target.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, "src/main/res/values/colors.xml")
LIGHT = os.path.join(ROOT, "src/main/res/values-notnight/colors.xml")

SURFACES = ["cf_canvas", "cf_sidebar", "cf_card", "cf_card_elevated", "cf_card_hover"]
TEXT = ["cf_text_primary", "cf_text_secondary", "cf_text_muted", "cf_emerald", "cf_gold",
        "cf_amber_text", "cf_positive", "cf_negative", "cf_warning", "cf_info", "cf_win", "cf_loss"]
# (foreground, background, surface under a translucent background, target)
EXTRA = [
    ("cf_on_emerald", "cf_emerald", None, 4.5),      # win / emerald buttons
    ("cf_on_emerald", "cf_green", None, 3.0),        # pressed win button (bold label)
    ("cf_on_gold", "cf_amber", None, 4.5),           # primary CTA
    ("cf_on_gold", "cf_amber_pressed", None, 4.5),   # pressed CTA
    ("cf_on_gold", "cf_gold_fill", None, 4.5),       # solid gold badge
    ("cf_on_crimson", "cf_crimson", None, 4.5),      # danger buttons, crimson pills
    ("cf_on_crimson", "cf_crimson_bright", None, 4.5),
    ("cf_emerald", "cf_emerald_dim", "cf_card", 4.5),     # emerald pill
    ("cf_gold", "cf_gold_glow", "cf_card", 4.5),          # gold pill
    ("cf_text_primary", "cf_crimson_glow", "cf_card", 4.5),  # crimson alert card
    ("cf_emerald", "cf_emerald_glow", "cf_sidebar", 4.5),    # checked drawer row
    ("cf_text_primary", "cf_footer", "cf_canvas", 4.5),      # bottom bar
    ("cf_emerald", "cf_footer", "cf_canvas", 4.5),
    ("cf_text_secondary", "cf_card", None, 3.0),     # drawer / menu icons
    ("cf_emerald", "cf_canvas", None, 3.0),          # accent bars, rating-tile stroke
    ("cf_gold", "cf_canvas", None, 3.0),             # gold bars, budget-tile stroke
    ("cf_star_empty", "cf_card", None, 1.3),         # empty star vs card (visible, not text)
]


def parse(path):
    with open(path, encoding="utf-8") as f:
        return dict((n, v.strip()) for n, v in re.findall(r'<color name="([^"]+)">([^<]+)</color>', f.read()))


def main():
    dark = "--dark" in sys.argv
    pal = parse(BASE)
    if not dark:
        pal.update(parse(LIGHT))

    def raw(name):
        v = pal[name]
        while v.startswith("@color/"):
            v = pal[v[len("@color/"):]]
        return v

    def rgba(h):
        h = h.lstrip("#")
        if len(h) == 6:
            h = "FF" + h
        return [int(h[i:i + 2], 16) for i in (0, 2, 4, 6)]

    def resolve(name, under=None):
        c = rgba(raw(name))
        if c[0] < 255:
            bg = resolve(under) if under else [255, 255, 255, 255]
            a = c[0] / 255.0
            c = [255] + [round(c[i] * a + bg[i] * (1 - a)) for i in (1, 2, 3)]
        return c

    def lum(c):
        def ch(x):
            x /= 255.0
            return x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4
        return 0.2126 * ch(c[1]) + 0.7152 * ch(c[2]) + 0.0722 * ch(c[3])

    def ratio(a, b):
        la, lb = lum(a), lum(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)

    pairs = [(t, s, None, 4.5) for t in TEXT for s in SURFACES] + EXTRA
    fails = 0
    print("theme:", "dark" if dark else "light")
    for fg, bg, under, need in pairs:
        if fg not in pal or bg not in pal:
            continue
        b = resolve(bg, under)
        f = resolve(fg, bg)
        r = ratio(f, b)
        ok = r >= need
        fails += 0 if ok else 1
        print(f"  {fg:18s} on {bg:18s} {r:5.2f}:1  (AA {need})  {'ok' if ok else 'FAIL'}")
    print(f"{fails} below target")
    return 1 if (fails and not dark) else 0


if __name__ == "__main__":
    sys.exit(main())
