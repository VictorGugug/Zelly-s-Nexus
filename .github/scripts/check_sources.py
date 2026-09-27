# Copyright (c) 2026 Zar
#
# This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
#
# Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
# plus this project's additional terms. Both are included in full in the
# LICENSE file at the root of this repository.
#
# You may view, study, and modify this file for any noncommercial purpose.
# You may not use it, or any modified version of it, for commercial purposes,
# in a closed-source product, or for passive monetization. See LICENSE for
# the complete terms, including the rules on forks and independent projects.

import os
import re
import subprocess
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
LANG = ROOT / "common" / "src" / "main" / "resources" / "lang"
HEADER_LINES = ("This file is part of Zelly's Nexus", "PolyForm Noncommercial License 1.0.0")
HEADER_TYPES = {".java", ".kts", ".py", ".yml", ".properties"}
HEADER_EXEMPT = re.compile(r"^(gradle/wrapper/|\.github/ISSUE_TEMPLATE/)")
TEXT_TYPES = HEADER_TYPES | {".md", ".txt"}
BANNED = {
    chr(0x2014): "em dash",
    chr(0x2013): "en dash",
    chr(0x2018): "curly single quote",
    chr(0x2019): "curly single quote",
    chr(0x201c): "curly double quote",
    chr(0x201d): "curly double quote",
}


def tracked():
    out = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, capture_output=True, check=True).stdout
    return [ROOT / name for name in out.decode("utf-8").split("\0") if name]


def flatten(node, prefix=""):
    flat = {}
    for key, value in (node or {}).items():
        path = f"{prefix}.{key}" if prefix else str(key)
        if isinstance(value, dict):
            flat.update(flatten(value, path))
        else:
            flat[path] = value
    return flat


def yaml_syntax(files):
    problems = []
    for file in files:
        if file.suffix == ".yml":
            try:
                yaml.safe_load(file.read_text(encoding="utf-8"))
            except yaml.YAMLError as error:
                problems.append(f"{file.relative_to(ROOT).as_posix()}: {error}")
    return problems


def translation_parity(_):
    tables = {}
    for code in ("en", "es"):
        tables[code] = flatten(yaml.safe_load((LANG / f"translations_{code}.yml").read_text(encoding="utf-8")))
    problems = []
    for code, other in (("en", "es"), ("es", "en")):
        for key in sorted(tables[other].keys() - tables[code].keys()):
            problems.append(f"translations_{code}.yml is missing {key}")
        for key, value in sorted(tables[code].items()):
            if value is None or str(value).strip() == "":
                problems.append(f"translations_{code}.yml has a blank value for {key}")
    return problems


def typography(files):
    problems = []
    for file in files:
        if file.suffix not in TEXT_TYPES:
            continue
        for number, line in enumerate(file.read_text(encoding="utf-8").splitlines(), 1):
            for char, name in BANNED.items():
                if char in line:
                    problems.append(f"{file.relative_to(ROOT).as_posix()}:{number}: {name}")
    return problems


def license_headers(files):
    problems = []
    for file in files:
        if file.suffix not in HEADER_TYPES or HEADER_EXEMPT.search(file.relative_to(ROOT).as_posix()):
            continue
        head = "\n".join(file.read_text(encoding="utf-8").splitlines()[:15])
        if not all(line in head for line in HEADER_LINES):
            problems.append(file.relative_to(ROOT).as_posix())
    return problems


CHECKS = (
    ("YAML syntax", yaml_syntax),
    ("Translation parity (en, es)", translation_parity),
    ("Typography", typography),
    ("License headers", license_headers),
)


def main():
    files = tracked()
    failed = False
    rows = []
    for name, check in CHECKS:
        problems = check(files)
        failed |= bool(problems)
        status = "Failed" if problems else "Passed"
        rows.append(f"| {name} | {status} | {len(problems)} |")
        print(f"[{status.upper()}] {name}")
        for problem in problems:
            print(f"  {problem}")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as out:
            out.write("## Source checks\n\n| Check | Status | Problems |\n| :--- | :---: | ---: |\n")
            out.write("\n".join(rows) + "\n\n")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
