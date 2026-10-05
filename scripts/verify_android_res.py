#!/usr/bin/env python3
"""Offline sanity gate for Android resources (no SDK needed).

Checks, under src/main/res and src/main/java:
  * every XML resource file parses;
  * every @type/name reference in res XML resolves to a declared resource
    (drawable, color, string, dimen, style, font, layout, menu, array, id);
  * every R.<type>.<name> reference in Java resolves (same types), and every
    R.id.<name> used in Java is declared by some layout/menu (@+id or
    android:id on a <item>);
  * no raw hex colours in layouts/menus/drawables under res (token rule).

Exit status 1 on any finding. Run: python3 scripts/verify_android_res.py
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "res")
JAVA = os.path.join(ROOT, "src", "main", "java")

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
TYPES = ("drawable", "color", "string", "dimen", "style", "font", "layout",
         "menu", "array", "id", "mipmap", "xml", "raw", "integer", "bool",
         "plurals", "anim", "animator", "interpolator", "attr")
VALUE_TAGS = {"string": "string", "color": "color", "dimen": "dimen",
              "style": "style", "integer": "integer", "bool": "bool",
              "attr": "attr", "item": None, "string-array": "array",
              "integer-array": "array", "array": "array", "plurals": "plurals",
              "declare-styleable": None}

declared = {t: set() for t in TYPES}
problems = []


def declare(t, name):
    if t in declared:
        declared[t].add(name)


def walk_values(path):
    try:
        tree = ET.parse(path)
    except ET.ParseError as e:
        problems.append(f"XML parse error: {os.path.relpath(path, ROOT)}: {e}")
        return
    for el in tree.getroot():
        tag = el.tag
        name = el.get("name")
        if name is None:
            continue
        if tag == "item":
            t = el.get("type")
            if t:
                declare(t, name)
        elif tag == "declare-styleable":
            for a in el:
                if a.get("name"):
                    declare("attr", a.get("name"))
        elif tag in VALUE_TAGS and VALUE_TAGS[tag]:
            declare(VALUE_TAGS[tag], name)
        # attrs declared inside styles via <item name="..."> are not resources.


def collect_declarations():
    for d in sorted(glob.glob(os.path.join(RES, "*"))):
        base = os.path.basename(d)
        kind = base.split("-")[0]
        for f in sorted(glob.glob(os.path.join(d, "*"))):
            stem = os.path.splitext(os.path.basename(f))[0]
            if stem.endswith(".9"):
                stem = stem[:-2]
            if kind == "values":
                if f.endswith(".xml"):
                    walk_values(f)
            elif kind in declared:
                declare(kind, stem)
            if f.endswith(".xml") and kind != "values":
                collect_ids(f)


def collect_ids(path):
    try:
        tree = ET.parse(path)
    except ET.ParseError as e:
        problems.append(f"XML parse error: {os.path.relpath(path, ROOT)}: {e}")
        return
    for el in tree.iter():
        for k, v in el.attrib.items():
            if isinstance(v, str) and v.startswith("@+id/"):
                declare("id", v[5:])
            elif k == ANDROID_NS + "id" and v.startswith("@id/"):
                # menu <item android:id="@+id/..."> always uses @+id; plain
                # @id must already exist, which the reference pass checks.
                pass
        # menu items use @+id too; covered above.


# Resources provided by AppCompat / Material / the build script (resValue).
LIBRARY_PREFIXES = ("Theme.Material3", "ThemeOverlay.Material3", "Widget.Material3",
                    "TextAppearance.Material3", "ShapeAppearance.Material3",
                    "Theme.AppCompat", "ThemeOverlay.AppCompat", "Widget.AppCompat",
                    "TextAppearance.AppCompat", "Widget.MaterialComponents",
                    "ThemeOverlay.MaterialComponents", "Theme.MaterialComponents",
                    "Base.", "Animation.AppCompat")
LIBRARY_NAMES = {("string", "appbar_scrolling_view_behavior"),
                 ("string", "versionName"),
                 ("string", "bottom_sheet_behavior")}


def is_library(t, name):
    if (t, name) in LIBRARY_NAMES:
        return True
    return t == "style" and name.startswith(LIBRARY_PREFIXES)


REF_RE = re.compile(r"@(?:\+)?(?:android:)?([a-z\-]+)/([A-Za-z0-9_.]+)")
RAW_HEX_RE = re.compile(r'="#[0-9A-Fa-f]{3,8}"')


def check_references():
    for path in sorted(glob.glob(os.path.join(RES, "**", "*.xml"), recursive=True)):
        rel = os.path.relpath(path, ROOT)
        text = open(path, encoding="utf-8").read()
        for m in REF_RE.finditer(text):
            full = m.group(0)
            if "@android:" in full or full.startswith("@+"):
                continue
            t, name = m.group(1), m.group(2)
            if t == "id":
                if name not in declared["id"]:
                    problems.append(f"{rel}: @id/{name} is never declared with @+id")
                continue
            if t in declared and name not in declared[t] and not is_library(t, name):
                problems.append(f"{rel}: unresolved @{t}/{name}")
        folder = os.path.basename(os.path.dirname(path))
        if folder.split("-")[0] in ("layout", "menu", "drawable", "color"):
            for m in RAW_HEX_RE.finditer(text):
                problems.append(f"{rel}: raw colour {m.group(0)} (use a @color token)")


R_RE = re.compile(r"(?<![A-Za-z0-9_.])R\.([a-z]+)\.([A-Za-z0-9_]+)")


def check_java():
    for path in sorted(glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True)):
        rel = os.path.relpath(path, ROOT)
        text = open(path, encoding="utf-8").read()
        for m in R_RE.finditer(text):
            t, name = m.group(1), m.group(2)
            if t == "id":
                if name not in declared["id"]:
                    problems.append(f"{rel}: R.id.{name} is not declared in any layout/menu")
            elif t in declared and t not in ("attr",):
                if name not in declared[t] and not is_library(t, name):
                    problems.append(f"{rel}: unresolved R.{t}.{name}")


SCROLLERS = {"ScrollView", "androidx.core.widget.NestedScrollView", "HorizontalScrollView"}
LISTS = {"ListView", "ExpandableListView", "GridView", "androidx.recyclerview.widget.RecyclerView"}
NON_VIEW_TAGS = {"include", "merge", "requestFocus", "tag", "fragment", "view"}


def check_layout_structure():
    """Structural rules the aapt/lint pass would catch on a real build."""
    for path in sorted(glob.glob(os.path.join(RES, "layout*", "*.xml"))):
        rel = os.path.relpath(path, ROOT)
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue  # reported elsewhere

        def walk(el, ancestors):
            tag = el.tag
            if tag in NON_VIEW_TAGS:
                for child in el:
                    walk(child, ancestors + [tag])
                return
            w = el.get(ANDROID_NS + "layout_width")
            h = el.get(ANDROID_NS + "layout_height")
            parent = ancestors[-1] if ancestors else None
            # TableLayout/TableRow children get their size from the table.
            table_child = parent in ("TableLayout", "TableRow")
            if parent is not None and parent != "merge" and not table_child and (w is None or h is None):
                # Root inside <merge>/<include>-less layouts still needs both;
                # a style may provide them, so flag only when no style is set.
                if el.get("style") is None:
                    problems.append(f"{rel}: <{tag}> missing layout_width/layout_height")
            weight = el.get(ANDROID_NS + "layout_weight")
            if (w == "0dp" or h == "0dp") and weight is None:
                problems.append(f"{rel}: <{tag}> has a 0dp size but no layout_weight")
            if tag in LISTS and any(a in SCROLLERS for a in ancestors):
                problems.append(f"{rel}: <{tag}> nested inside a ScrollView")
            if tag in SCROLLERS and len(list(el)) > 1:
                problems.append(f"{rel}: <{tag}> must have exactly one child")
            for child in el:
                walk(child, ancestors + [tag])

        walk(root, [])


def main():
    collect_declarations()
    check_references()
    check_java()
    check_layout_structure()
    if problems:
        print("\n".join(problems))
        print(f"\n{len(problems)} problem(s)")
        return 1
    print(f"android resources OK: {len(declared['layout'])} layouts, "
          f"{len(declared['drawable'])} drawables, {len(declared['id'])} ids, "
          f"{len(declared['string'])} strings")
    return 0


if __name__ == "__main__":
    sys.exit(main())
