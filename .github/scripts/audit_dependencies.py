# Copyright (c) 2026 Zar
#
# This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.
#
# Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
# plus this project's additional terms. Both are included in full in the
# LICENSE file at the root of this repository.
#!/usr/bin/env python3
import concurrent.futures
import json
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

if sys.stdout.encoding != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")


def parse_gradle_properties(repo_root):
    path = os.path.join(repo_root, "gradle.properties")
    props = {}
    supported_mc = []
    if not os.path.exists(path):
        return props, supported_mc

    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line_s = line.strip()
            if "=" in line_s and not line_s.startswith("#"):
                k, v = line_s.split("=", 1)
                props[k.strip()] = v.strip()
            elif "Supported on nexusVersion" in line_s:
                parts = line_s.split(":", 1)
                if len(parts) > 1:
                    supported_mc = [x.strip() for x in parts[1].split(",") if x.strip()]

    return props, supported_mc


def get_plugin_version_info(props, supported_mc):
    base_version = props.get("nexusVersion", "Unknown")
    suffix = props.get("versionSuffix", "").strip()
    full_version = f"{base_version}-{suffix}" if suffix else base_version
    latest_mc = supported_mc[-1] if supported_mc else "Unknown"
    mc_range = f"{supported_mc[0]} - {latest_mc}" if len(supported_mc) > 1 else latest_mc
    java_target = props.get("targetJavaVersion", "21")
    return {
        "version": full_version,
        "base_version": base_version,
        "suffix": suffix,
        "latest_mc": latest_mc,
        "supported_mc": supported_mc,
        "mc_range": mc_range,
        "java_target": java_target,
    }


def fetch_maven_latest(url):
    headers = {"User-Agent": "ZellysNexus-CI-Checker (Mozilla/5.0)"}
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=12) as resp:
        root = ET.fromstring(resp.read())
        release = root.findtext("./versioning/release")
        if release:
            return release.strip()
        latest = root.findtext("./versioning/latest")
        if latest:
            return latest.strip()
        versions = [v.text.strip() for v in root.findall(".//version") if v.text]
        if versions:
            return versions[-1]
    return "Unknown"


def fetch_github_release_or_tag(repo):
    headers = {"User-Agent": "ZellysNexus-CI-Checker"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"

    url_release = f"https://api.github.com/repos/{repo}/releases/latest"
    try:
        req = urllib.request.Request(url_release, headers=headers)
        with urllib.request.urlopen(req, timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            tag = data.get("tag_name", "")
            if tag:
                return tag.lstrip("v").strip()
    except Exception:
        pass

    url_tags = f"https://api.github.com/repos/{repo}/tags"
    req_tags = urllib.request.Request(url_tags, headers=headers)
    with urllib.request.urlopen(req_tags, timeout=12) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        if data and isinstance(data, list) and len(data) > 0:
            tag = data[0].get("name", "")
            return tag.lstrip("v").strip()

    return "Unknown"


def parse_gradle_build_files(repo_root):
    """Extract the dependency versions declared in the Gradle build files."""
    patterns = {
        "paper": {
            "shadow": r'id\("com\.gradleup\.shadow"\)\s*version\s*"([^"]+)"',
            "run_paper": r'id\("xyz\.jpenilla\.run-paper"\)\s*version\s*"([^"]+)"',
            "junit_bom": r'platform\("org\.junit:junit-bom:([^"]+)"\)',
            "bcrypt": r'at\.favre\.lib:bcrypt:([^"]+)',
        },
        "common": {
            "snakeyaml": r'org\.yaml:snakeyaml:([^"]+)',
        },
    }
    versions = {}
    for module, module_patterns in patterns.items():
        path = os.path.join(repo_root, module, "build.gradle.kts")
        if not os.path.exists(path):
            continue
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        for key, pattern in module_patterns.items():
            m = re.search(pattern, content)
            if m:
                versions[key] = m.group(1).strip()
    return versions


def is_version_match(current, latest):
    c = current.strip().lstrip("v").lower()
    l = latest.strip().lstrip("v").lower()

    if c == l:
        return True

    if c.startswith(l) or l.startswith(c):
        return True

    try:
        def to_ints(v_str):
            clean = re.sub(r"[-+].*$", "", v_str)
            return [int(x) for x in re.findall(r"\d+", clean)]
        c_parts = to_ints(c)
        l_parts = to_ints(l)
        if c_parts and l_parts:
            if c_parts >= l_parts or (len(c_parts) >= 3 and len(l_parts) >= 3 and c_parts[:3] == l_parts[:3]):
                return True
    except Exception:
        pass

    return False


def run_target_check(item):
    category, name, current, ftype, target_endpoint = item
    try:
        if ftype == "maven":
            latest = fetch_maven_latest(target_endpoint)
        elif ftype == "github":
            latest = fetch_github_release_or_tag(target_endpoint)
        else:
            latest = "Unknown"
    except Exception as e:
        latest = f"Error: {e}"

    up_to_date = is_version_match(current, latest)
    status_indicator = "[v]" if up_to_date else "[x]"

    return {
        "category": category,
        "name": name,
        "current": current,
        "latest": latest,
        "up_to_date": up_to_date,
        "status": status_indicator,
    }


def main():
    props, supported_mc = parse_gradle_properties(REPO_ROOT)
    version_info = get_plugin_version_info(props, supported_mc)
    gv = parse_gradle_build_files(REPO_ROOT)

    audit_targets = [
        ("Plugin Integration", "LuckPerms", "5.5", "github", "LuckPerms/LuckPerms"),
        ("Plugin Integration", "Geyser", "2.7", "github", "GeyserMC/Geyser"),
        ("Plugin Integration", "Floodgate API", "2.2.3-SNAPSHOT", "maven", "https://repo.opencollab.dev/main/org/geysermc/floodgate/api/maven-metadata.xml"),
        ("Plugin Integration", "PlaceholderAPI", "2.12.3", "maven", "https://repo.extendedclip.com/content/repositories/placeholderapi/me/clip/placeholderapi/maven-metadata.xml"),

        ("Core Dependency", "BCrypt", gv.get("bcrypt", "unknown"), "maven", "https://repo1.maven.org/maven2/at/favre/lib/bcrypt/maven-metadata.xml"),
        ("Core Dependency", "SnakeYAML", gv.get("snakeyaml", "unknown"), "maven", "https://repo1.maven.org/maven2/org/yaml/snakeyaml/maven-metadata.xml"),

        ("Build & Test", "JUnit BOM", gv.get("junit_bom", "unknown"), "maven", "https://repo1.maven.org/maven2/org/junit/junit-bom/maven-metadata.xml"),
        ("Build & Test", "Shadow Plugin", gv.get("shadow", "unknown"), "maven", "https://plugins.gradle.org/m2/com/gradleup/shadow/com.gradleup.shadow.gradle.plugin/maven-metadata.xml"),
        ("Build & Test", "Run-Paper Plugin", gv.get("run_paper", "unknown"), "maven", "https://plugins.gradle.org/m2/xyz/jpenilla/run-paper/xyz.jpenilla.run-paper.gradle.plugin/maven-metadata.xml"),
    ]

    print("Zellys Nexus Dependency and Integration Version Audit")
    print(f"Plugin Version: {version_info['version']}")
    print(f"Latest Declared Minecraft Support: {version_info['latest_mc']} (Range: {version_info['mc_range']})")
    print(f"Java Baseline: Bytecode {version_info['java_target']} (JDK 25 Toolchain)")

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        future_to_item = {executor.submit(run_target_check, item): item for item in audit_targets}
        for future in concurrent.futures.as_completed(future_to_item):
            results.append(future.result())

    cat_order = {"Plugin Integration": 1, "Core Dependency": 2, "Build & Test": 3}
    results.sort(key=lambda r: (cat_order.get(r["category"], 99), r["name"].lower()))

    up_to_date_count = sum(1 for r in results if r["up_to_date"])
    outdated_count = len(results) - up_to_date_count
    total_count = len(results)

    print(f"Status   {'Dependency / Integration':<26} {'Category':<22} {'Current':<16} {'Latest':<16}")
    for r in results:
        print(f"{r['status']:<8} {r['name']:<26} {r['category']:<22} {r['current']:<16} {r['latest']:<16}")
    print(f"Total: {total_count} | Up-to-date: {up_to_date_count} [v] | Updates Available: {outdated_count} [x]")

    md_lines = []
    md_lines.append("## Dependency and Plugin Integration Audit\n")
    md_lines.append(f"> **Plugin Version:** `{version_info['version']}` &nbsp;|&nbsp; "
                    f"**Latest Supported Minecraft:** `{version_info['latest_mc']}` &nbsp;|&nbsp; "
                    f"**Range:** `{version_info['mc_range']}` &nbsp;|&nbsp; "
                    f"**Java:** `{version_info['java_target']}`\n")
    md_lines.append(f"- **Summary:** `{up_to_date_count}` up-to-date [v] &nbsp;•&nbsp; `{outdated_count}` updates available [x]\n")

    category_headings = {
        "Plugin Integration": "Plugin Integrations",
        "Core Dependency": "Core Dependencies",
        "Build & Test": "Build and Test Tooling",
    }

    current_cat = None
    for r in results:
        if r["category"] != current_cat:
            if current_cat is not None:
                md_lines.append("\n")
            current_cat = r["category"]
            heading = category_headings.get(current_cat, f"{current_cat}s")
            md_lines.append(f"### {heading}\n")
            md_lines.append("| Status | Component | Declared Version | Latest Version | Evaluation |")
            md_lines.append("| :---: | :--- | :---: | :---: | :--- |")

        evaluation = "Up to date" if r["up_to_date"] else "Update available"
        md_lines.append(f"| `{r['status']}` | **{r['name']}** | `{r['current']}` | `{r['latest']}` | {evaluation} |")

    md_content = "\n".join(md_lines) + "\n"

    step_summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary_path:
        try:
            with open(step_summary_path, "a", encoding="utf-8") as f:
                f.write("\n" + md_content)
        except Exception as e:
            print(f"Warning: Could not write to GITHUB_STEP_SUMMARY: {e}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
