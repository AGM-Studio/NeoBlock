import os
import re
import json
import sys

if __name__ == "__main__":
    print("Started the translation check.")
else:
    sys.exit(-1)

# --- CONFIG ---
KEY_PATTERN = re.compile(r'"([^"]*\.neoblock\.[^"]*)"')  # captures any string containing '.neoblock.'
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

used_keys = set()
key_locations = {}  # key -> list of file:line

def format_location(full_path, line_number):
    """
    Convert a full file path into Gradle-style:
    :<module_path>:<filename>:L<line> (<package>)
    """
    full_path = os.path.normpath(os.path.abspath(full_path))
    rel_path = os.path.relpath(full_path, PROJECT_ROOT)  # relative to root
    parts = rel_path.split(os.sep)

    # Find 'src' folder to locate module root
    if "src" in parts:
        src_index = parts.index("src")
        module_path_parts = parts[:src_index]
        module_path = ":".join(module_path_parts)
        java_index = src_index + 2  # skip main/java
        rel_parts = parts[java_index:]
    else:
        module_path = "root"
        rel_parts = parts[-2:]

    filename = rel_parts[-1] if rel_parts else parts[-1]
    package_parts = rel_parts[:-1]
    package_name = ".".join(package_parts) if package_parts else "default"

    return f":{module_path}:{filename}:L{line_number} ({package_name})"


# --- SCAN ALL JAVA/KOTLIN SOURCE FILES ---
for root, _, files in os.walk(".."):
    for file in files:
        if file.endswith(".java") or file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                for lineno, line in enumerate(f, 1):
                    for match in KEY_PATTERN.finditer(line):
                        key = match.group(1)
                        used_keys.add(key)
                        key_locations.setdefault(key, []).append(format_location(path, lineno))

# --- LOAD ALL LANG JSON FILES ---
langs = {}  # filename -> set of keys
for root, _, files in os.walk(".."):
    if os.path.sep + "lang" + os.path.sep in root + os.path.sep:
        for f in files:
            if f.endswith(".json"):
                path = os.path.join(root, f)
                with open(path, "r", encoding="utf-8") as json_file:
                    try:
                        langs[f] = set(json.load(json_file).keys())
                    except Exception as e:
                        print(f"❌ Failed to parse {path}: {e}")
                        sys.exit(1)

# --- ENSURE en_us.json EXISTS ---
if "en_us.json" not in langs:
    print("❌ en_us.json not found in any lang folder")
    sys.exit(1)

defined_keys = langs["en_us.json"]

# --- COMPARE USED KEYS WITH en_us.json ---
missing = used_keys - defined_keys
unused = defined_keys - used_keys

# Print missing keys with file/line
if missing:
    print("❌ Missing keys in en_us.json:")
    for key in sorted(missing):
        print(f"  {key}")
        for loc in key_locations.get(key, []):
            print(f"    ↳ {loc}")

# Print unused keys
if unused:
    print("⚠️ Unused keys in en_us.json:")
    for key in sorted(unused):
        print(f"  {key}")

# --- CHECK OTHER LANGS AGAINST en_us.json ---
for lang, keys in langs.items():
    if lang == "en_us.json":
        continue
    missing_in_lang = defined_keys - keys
    if missing_in_lang:
        print(f"⚠️ {lang} is missing keys:")
        for key in sorted(missing_in_lang):
            print(f"  {key}")

# --- EXIT CODE ---
if missing:
    sys.exit(1)
else:
    print("✅ All used keys are present in en_us.json")
    sys.exit(0)
