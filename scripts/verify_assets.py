"""
Verification script for CFHC graphics and assets audit.
Checks image format, dimensions, mode, transparency, and presence across Android, Desktop, and Web Preview.
"""
import os
import sys
from PIL import Image

EXPECTED = {
    # Desktop Assets
    "src/main/assets/cfhc_icon.png": {"size": (512, 512), "mode": "RGBA"},
    # Android Menu & Drawables
    "src/main/res/drawable/main_menu_logo.png": {"size": (512, 512), "mode": "RGBA"},
    "src/main/res/drawable/football_logo.png": {"size": (512, 512), "mode": "RGBA"},
    "src/main/res/drawable/football_icon.png": {"size": (512, 512), "mode": "RGBA"},
    # Android Launcher & Mipmaps
    "src/main/ic_launcher-web.png": {"size": (512, 512), "mode": "RGBA"},
    "src/main/res/mipmap-mdpi/ic_launcher.png": {"size": (48, 48), "mode": "RGBA"},
    "src/main/res/mipmap-hdpi/ic_launcher.png": {"size": (72, 72), "mode": "RGBA"},
    "src/main/res/mipmap-xhdpi/ic_launcher.png": {"size": (96, 96), "mode": "RGBA"},
    "src/main/res/mipmap-xxhdpi/ic_launcher.png": {"size": (144, 144), "mode": "RGBA"},
    "src/main/res/mipmap-xxxhdpi/ic_launcher.png": {"size": (192, 192), "mode": "RGBA"},
    # Web Preview UI Sprites
    "preview/sprites/ui/avatar_mason_harrison.png": {"mode": "RGBA"},
    "preview/sprites/ui/avatar_jalen_bryant.png": {"mode": "RGBA"},
    "preview/sprites/ui/crest_eagles.png": {"mode": "RGBA"},
    "preview/sprites/ui/trophy_crystal.png": {"mode": "RGBA"},
    "preview/sprites/pixel_stadium_banner.jpg": {"mode": "RGB"},
}

# Android drawable-nodpi sprites (broadcast-HUD component library, Wave 0 of
# the Android UI redesign). Palette PNGs; every file must stay under 40 KB.
ANDROID_SPRITES = {
    "src/main/res/drawable-nodpi/sprite_avatar_qb_green.png": (156, 166),
    "src/main/res/drawable-nodpi/sprite_avatar_lb_green.png": (156, 166),
    "src/main/res/drawable-nodpi/sprite_avatar_opponent_crimson.png": (156, 161),
    "src/main/res/drawable-nodpi/sprite_avatar_showcase_qb.png": (100, 112),
    "src/main/res/drawable-nodpi/sprite_avatar_showcase_lb.png": (90, 104),
    "src/main/res/drawable-nodpi/sprite_crest_bears.png": (220, 220),
    "src/main/res/drawable-nodpi/sprite_crest_bison.png": (220, 220),
    "src/main/res/drawable-nodpi/sprite_crest_eagles.png": (220, 220),
    "src/main/res/drawable-nodpi/sprite_crest_wildcats.png": (220, 220),
    "src/main/res/drawable-nodpi/sprite_trophy_crystal.png": (235, 235),
    "src/main/res/drawable-nodpi/sprite_trophy_heisman.png": (235, 255),
    "src/main/res/drawable-nodpi/sprite_trophy_ring.png": (255, 235),
}

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    errors = []
    print(f"Verifying assets in: {repo_root}")
    
    for rel_path, spec in EXPECTED.items():
        full_path = os.path.join(repo_root, rel_path)
        if not os.path.exists(full_path):
            errors.append(f"MISSING: {rel_path}")
            continue
        try:
            im = Image.open(full_path)
            im.verify()
            im = Image.open(full_path)
            if "size" in spec and im.size != spec["size"]:
                errors.append(f"SIZE MISMATCH: {rel_path} got {im.size}, expected {spec['size']}")
            if spec.get("mode") and im.mode != spec["mode"]:
                errors.append(f"MODE MISMATCH: {rel_path} got {im.mode}, expected {spec['mode']}")
            sz_kb = os.path.getsize(full_path) / 1024
            print(f"  [OK] {rel_path}: {im.format} {im.size} {im.mode} ({sz_kb:.1f} KB)")
        except Exception as e:
            errors.append(f"INVALID: {rel_path}: {e}")

    # Check that preview/sprites are optimized (none > 450 KB)
    sprites_dir = os.path.join(repo_root, "preview", "sprites")
    total_sz = 0
    for f in os.listdir(sprites_dir):
        if f.endswith(".jpg"):
            fp = os.path.join(sprites_dir, f)
            sz = os.path.getsize(fp)
            total_sz += sz
            if sz > 450 * 1024:
                errors.append(f"SPRITE TOO LARGE: {f} is {sz/1024:.1f} KB (> 450 KB)")

    print(f"\nTotal preview/sprites JPG size: {total_sz/1024/1024:.2f} MB (well below 10.5 MB original)")

    # Android sprites: palette mode, expected dimensions, < 40 KB budget each.
    android_total = 0
    for rel_path, expected_size in ANDROID_SPRITES.items():
        full_path = os.path.join(repo_root, rel_path)
        if not os.path.exists(full_path):
            errors.append(f"MISSING: {rel_path}")
            continue
        try:
            im = Image.open(full_path)
            im.verify()
            im = Image.open(full_path)
            sz_kb = os.path.getsize(full_path) / 1024
            android_total += sz_kb
            if im.mode != "P":
                errors.append(f"MODE MISMATCH: {rel_path} got {im.mode}, expected P (palette)")
            if im.size != expected_size:
                errors.append(f"SIZE MISMATCH: {rel_path} got {im.size}, expected {expected_size}")
            if sz_kb > 40:
                errors.append(f"ANDROID SPRITE TOO LARGE: {rel_path} is {sz_kb:.1f} KB (> 40 KB)")
        except Exception as e:
            errors.append(f"INVALID: {rel_path}: {e}")
    print(f"Android drawable-nodpi sprites: {len(ANDROID_SPRITES)} files, {android_total:.0f} KB total")

    # Bundled fonts (subsetted OFL builds).
    for font in ["inter_regular", "inter_medium", "inter_semibold", "inter_bold",
                 "jetbrains_mono_regular", "jetbrains_mono_bold", "caveat_variable"]:
        fp = os.path.join(repo_root, "src", "main", "res", "font", font + ".ttf")
        if not os.path.exists(fp):
            errors.append(f"MISSING FONT: {fp}")
    for fam in ["inter.xml", "jetbrains_mono.xml", "caveat.xml"]:
        fp = os.path.join(repo_root, "src", "main", "res", "font", fam)
        if not os.path.exists(fp):
            errors.append(f"MISSING FONT FAMILY: {fp}")
    if not os.path.exists(os.path.join(repo_root, "docs", "FONT_LICENSES.md")):
        errors.append("MISSING: docs/FONT_LICENSES.md")

    if errors:
        print("\nERRORS FOUND:")
        for err in errors:
            print(" -", err)
        sys.exit(1)
    else:
        print("\nALL GRAPHICS & ASSETS VERIFIED SUCCESSFULLY!")

if __name__ == "__main__":
    main()
