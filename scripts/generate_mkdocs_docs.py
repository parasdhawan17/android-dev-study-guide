#!/usr/bin/env python3
"""Generate MkDocs pages from the notes folder.

This keeps notes/ as the source of truth:
- Markdown notes are copied unchanged into docs/notes/.
- Kotlin files are wrapped in Markdown code fences in docs/notes/.
"""
from pathlib import Path
import re
import shutil
from typing import Optional

ROOT = Path(__file__).resolve().parents[1]
NOTES_DIR = ROOT / "notes"
DOCS_DIR = ROOT / "docs"
DOCS_NOTES_DIR = DOCS_DIR / "notes"


def day_sort_key(path: Path):
    match = re.match(r"Day(\d+)(?:-(\d+))?$", path.name)
    if match:
        return (int(match.group(1)), int(match.group(2) or match.group(1)))
    return (10_000, path.name)


def title_from_file(path: Path) -> str:
    stem = path.stem
    stem = re.sub(r"^\d+_", "", stem)
    stem = stem.replace("_", " ")
    stem = re.sub(r"(?<=[a-z])(?=[A-Z])", " ", stem)
    if stem.lower() == "cheatsheet":
        return "Cheat Sheet"
    return stem.strip() or path.stem


def nav_file_sort_key(path: Path):
    if path.stem.lower() == "cheatsheet":
        return (0, path.name)
    return (1, path.name)


def yaml_quote(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def is_divider_comment(line: str) -> bool:
    stripped = line.strip()
    return bool(re.fullmatch(r"//\s*=+\s*", stripped))


def section_title_from_comment(line: str) -> Optional[str]:
    stripped = line.strip()
    match = re.fullmatch(r"//\s*=+\s*(.*?)\s*=+\s*", stripped)
    if not match:
        return None

    title = match.group(1).strip()
    if not title:
        return None
    return format_heading(title)


def plain_comment_text(line: str) -> Optional[str]:
    stripped = line.strip()
    if not stripped.startswith("//"):
        return None
    return stripped[2:].strip()


def format_heading(value: str) -> str:
    value = value.strip("= -")
    value = re.sub(r"\s+", " ", value)
    words = value.title()
    replacements = {
        " Mvvm": " MVVM",
        " Mvi": " MVI",
        " Anr": " ANR",
        " Ui": " UI",
        " Io": " IO",
        " Bfs": " BFS",
        " Dfs": " DFS",
        " Vs ": " vs ",
    }
    for old, new in replacements.items():
        words = words.replace(old, new)
    return words


def clean_block_comment(block_lines: list[str]) -> list[str]:
    cleaned: list[str] = []
    for line in block_lines:
        stripped = line.strip()
        if stripped in {"/*", "/**", "*/"}:
            continue
        if stripped.startswith("/*"):
            stripped = stripped[2:].strip()
        if stripped.endswith("*/"):
            stripped = stripped[:-2].rstrip()
        if stripped.startswith("*"):
            stripped = stripped[1:].lstrip()
        if stripped and set(stripped) == {"="}:
            continue
        cleaned.append(stripped)
    return trim_blank_edges(cleaned)


def trim_blank_edges(lines: list[str]) -> list[str]:
    while lines and not lines[0].strip():
        lines.pop(0)
    while lines and not lines[-1].strip():
        lines.pop()
    return lines


def render_text_block(lines: list[str]) -> list[str]:
    lines = trim_blank_edges(lines[:])
    if not lines:
        return []
    escaped = [
        line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        for line in lines
    ]
    return escaped + [""]


def render_code_block(lines: list[str]) -> list[str]:
    lines = trim_blank_edges(lines[:])
    if not lines:
        return []
    return ["````kotlin", *lines, "````", ""]


def render_kotlin_page(title: str, content: str) -> str:
    source_lines = content.rstrip().splitlines()
    rendered: list[str] = [f"# {title}", ""]
    pending_text: list[str] = []
    pending_code: list[str] = []
    i = 0

    def flush_text() -> None:
        nonlocal pending_text
        rendered.extend(render_text_block(pending_text))
        pending_text = []

    def flush_code() -> None:
        nonlocal pending_code
        rendered.extend(render_code_block(pending_code))
        pending_code = []

    def flush_all() -> None:
        flush_text()
        flush_code()

    while i < len(source_lines):
        line = source_lines[i]
        direct_title = section_title_from_comment(line)

        if direct_title:
            flush_all()
            rendered.extend([f"## {direct_title}", ""])
            i += 1
            continue

        if is_divider_comment(line):
            next_line = source_lines[i + 1] if i + 1 < len(source_lines) else ""
            next_text = plain_comment_text(next_line)
            after_next = source_lines[i + 2] if i + 2 < len(source_lines) else ""
            if next_text and is_divider_comment(after_next):
                flush_all()
                rendered.extend([f"## {format_heading(next_text)}", ""])
                i += 3
                continue
            i += 1
            continue

        if line.lstrip().startswith("/*") and line == line.lstrip():
            flush_code()
            block = [line]
            i += 1
            while i < len(source_lines) and "*/" not in block[-1]:
                block.append(source_lines[i])
                i += 1
            pending_text.extend(clean_block_comment(block))
            pending_text.append("")
            continue

        if line.strip().startswith("//") and line == line.lstrip():
            flush_code()
            text = plain_comment_text(line)
            if text:
                pending_text.append(text)
            else:
                pending_text.append("")
            i += 1
            continue

        flush_text()
        pending_code.append(line)
        i += 1

    flush_all()
    return "\n".join(trim_blank_edges(rendered)) + "\n"


def generate_docs() -> list[Path]:
    if not NOTES_DIR.exists():
        raise SystemExit("notes/ directory not found")

    DOCS_NOTES_DIR.mkdir(parents=True, exist_ok=True)
    generated_pages: list[Path] = []

    for day_dir in sorted([p for p in NOTES_DIR.iterdir() if p.is_dir()], key=day_sort_key):
        target_day_dir = DOCS_NOTES_DIR / day_dir.name
        target_day_dir.mkdir(parents=True, exist_ok=True)

        for source in sorted(day_dir.iterdir(), key=nav_file_sort_key):
            if source.suffix == ".md":
                target = target_day_dir / source.name
                shutil.copyfile(source, target)
                generated_pages.append(target.relative_to(DOCS_DIR))
            elif source.suffix == ".kt":
                target = target_day_dir / f"{source.stem}.md"
                title = title_from_file(source)
                content = source.read_text(encoding="utf-8").rstrip()
                target.write_text(render_kotlin_page(title, content), encoding="utf-8")
                generated_pages.append(target.relative_to(DOCS_DIR))

    return generated_pages


def write_index() -> None:
    day_dirs = sorted([p for p in NOTES_DIR.iterdir() if p.is_dir()], key=day_sort_key)
    lines = [
        "# Android Study Notes",
        "",
        '<section class="hero">',
        '  <p class="eyebrow">Android Interview Prep</p>',
        "  <h1>Study Kotlin, Android architecture, testing, and performance in one focused guide.</h1>",
        "  <p>A practical study guide for Android engineers preparing for technical interviews, with concise references, Kotlin examples, and practice-oriented topic breakdowns.</p>",
        '  <p><a class="md-button md-button--primary" href="notes/Day1-2/CheatSheet/">Start studying</a> <a class="md-button" href="#study-sections">Browse sections</a></p>',
        "</section>",
        "",
        "## Study Sections { #study-sections }",
        "",
        '<div class="study-grid">',
    ]
    for day_dir in day_dirs:
        label = day_dir.name.replace("Day", "Day ")
        file_count = len([p for p in day_dir.iterdir() if p.suffix in {".md", ".kt"}])
        lines.extend(
            [
                '  <a class="study-card" href="notes/{}/CheatSheet/">'.format(day_dir.name),
                f"    <span>{label}</span>",
                f"    <strong>{file_count} topics</strong>",
                "    <small>Cheat sheet, examples, and practice notes</small>",
                "  </a>",
            ]
        )
    lines.extend(
        [
            "</div>",
            "",
            "## What You'll Cover",
            "",
            "- Kotlin fundamentals and idiomatic language features.",
            "- Android lifecycle, UI, and background execution concepts.",
            "- Coroutines, Flow, synchronization, and threading.",
            "- Architecture patterns, dependency injection, and testing.",
            "- Performance, debugging, edge cases, and interview practice.",
            "",
        ]
    )
    DOCS_DIR.mkdir(parents=True, exist_ok=True)
    (DOCS_DIR / "index.md").write_text("\n".join(lines), encoding="utf-8")


def write_assets() -> None:
    styles_dir = DOCS_DIR / "stylesheets"
    styles_dir.mkdir(parents=True, exist_ok=True)
    (styles_dir / "extra.css").write_text(
        """/* Presentation-only polish for the study site. */
:root {
  --study-card-border: rgba(85, 103, 255, 0.18);
  --study-card-shadow: 0 14px 40px rgba(15, 23, 42, 0.08);
}

.md-typeset .hero {
  border: 1px solid var(--study-card-border);
  border-radius: 1.25rem;
  padding: 2rem;
  margin: 1rem 0 2rem;
  background:
    radial-gradient(circle at top left, rgba(83, 109, 254, 0.18), transparent 34rem),
    linear-gradient(135deg, rgba(83, 109, 254, 0.08), rgba(0, 200, 150, 0.08));
}

.md-typeset .hero h1 {
  max-width: 900px;
  margin: 0.2rem 0 0.7rem;
  font-weight: 800;
}

.md-typeset .hero p {
  max-width: 760px;
}

.md-typeset .eyebrow {
  margin: 0;
  color: var(--md-primary-fg-color);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.md-typeset .study-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin: 1rem 0 2rem;
}

.md-typeset .study-card {
  display: block;
  min-height: 8rem;
  padding: 1.1rem;
  border: 1px solid var(--study-card-border);
  border-radius: 1rem;
  background: var(--md-default-bg-color);
  box-shadow: var(--study-card-shadow);
  color: var(--md-default-fg-color);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.md-typeset .study-card:hover {
  border-color: var(--md-primary-fg-color);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.12);
  transform: translateY(-2px);
}

.md-typeset .study-card span,
.md-typeset .study-card strong,
.md-typeset .study-card small {
  display: block;
}

.md-typeset .study-card span {
  color: var(--md-primary-fg-color);
  font-size: 0.82rem;
  font-weight: 700;
}

.md-typeset .study-card strong {
  margin: 0.35rem 0;
  font-size: 1.2rem;
}

.md-typeset .study-card small {
  color: var(--md-default-fg-color--light);
}

.md-typeset pre > code {
  border-radius: 0.6rem;
}
""",
        encoding="utf-8",
    )


def write_mkdocs_config() -> None:
    day_dirs = sorted([p for p in NOTES_DIR.iterdir() if p.is_dir()], key=day_sort_key)
    lines = [
        "site_name: Android Study Notes",
        "site_description: Android technical interview study notes",
        "theme:",
        "  name: material",
        "  icon:",
        "    logo: material/android",
        "  palette:",
        "    - media: '(prefers-color-scheme: light)'",
        "      scheme: default",
        "      primary: indigo",
        "      accent: teal",
        "      toggle:",
        "        icon: material/weather-night",
        "        name: Switch to dark mode",
        "    - media: '(prefers-color-scheme: dark)'",
        "      scheme: slate",
        "      primary: indigo",
        "      accent: teal",
        "      toggle:",
        "        icon: material/weather-sunny",
        "        name: Switch to light mode",
        "  font:",
        "    text: Inter",
        "    code: JetBrains Mono",
        "  features:",
        "    - navigation.instant",
        "    - navigation.tracking",
        "    - navigation.tabs",
        "    - navigation.sections",
        "    - navigation.expand",
        "    - navigation.top",
        "    - navigation.footer",
        "    - search.suggest",
        "    - search.highlight",
        "    - content.code.copy",
        "    - content.code.annotate",
        "extra_css:",
        "  - stylesheets/extra.css",
        "markdown_extensions:",
        "  - admonition",
        "  - attr_list",
        "  - tables",
        "  - toc:",
        "      permalink: true",
        "  - pymdownx.highlight:",
        "      anchor_linenums: true",
        "  - pymdownx.inlinehilite",
        "  - pymdownx.superfences",
        "nav:",
        "  - Home: index.md",
        "  - Notes:",
    ]

    for day_dir in day_dirs:
        label = day_dir.name.replace("Day", "Day ")
        lines.append(f"      - {yaml_quote(label)}:")
        for source in sorted(day_dir.iterdir(), key=nav_file_sort_key):
            if source.suffix not in {".md", ".kt"}:
                continue
            title = title_from_file(source)
            target_name = source.name if source.suffix == ".md" else f"{source.stem}.md"
            nav_path = f"notes/{day_dir.name}/{target_name}"
            lines.append(f"          - {yaml_quote(title)}: {yaml_quote(nav_path)}")

    lines.append("")
    (ROOT / "mkdocs.yml").write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    generate_docs()
    write_index()
    write_assets()
    write_mkdocs_config()


if __name__ == "__main__":
    main()
