"""One-off helper: add Locale.ROOT to String.format / case folding in large desktop files."""
from pathlib import Path
import re


def add_locale_import(text: str) -> str:
    if "import java.util.Locale;" in text:
        return text
    lines = text.splitlines(keepends=True)
    insert_at = None
    for i, line in enumerate(lines):
        if line.startswith("import java.util.") and line.strip().endswith(";"):
            insert_at = i + 1
    if insert_at is None:
        raise ValueError("no java.util import")
    lines.insert(insert_at, "import java.util.Locale;\n")
    return "".join(lines)


def main() -> None:
    root = Path(__file__).resolve().parents[1] / "src/main/java"
    for rel in ("desktop/LeagueHomeView.java", "desktop/TeamDetailView.java"):
        path = root / rel
        text = path.read_text(encoding="utf-8")
        orig = text
        text = add_locale_import(text)
        if rel == "desktop/LeagueHomeView.java" and "DecimalFormatSymbols" not in text:
            text = text.replace(
                "import java.text.DecimalFormat;\n",
                "import java.text.DecimalFormat;\nimport java.text.DecimalFormatSymbols;\n",
            )
        text = re.sub(
            r'private static final DecimalFormat DF2 = new DecimalFormat\("#\.##"\);',
            'private static final DecimalFormat DF2 = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));',
            text,
        )
        text = re.sub(r"String\.format\(\s*(?!Locale\b)", "String.format(Locale.ROOT, ", text)
        text = re.sub(r"\.toUpperCase\(\)", ".toUpperCase(Locale.ROOT)", text)
        text = re.sub(r"\.toLowerCase\(\)", ".toLowerCase(Locale.ROOT)", text)
        if text != orig:
            path.write_text(text, encoding="utf-8")
            print("updated", path)


if __name__ == "__main__":
    main()
