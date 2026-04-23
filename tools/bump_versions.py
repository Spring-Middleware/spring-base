import os
import re
def bump_minor(version_str):
    # Matches X.Y.Z
    match = re.match(r'^(\d+)\.(\d+)\.(\d+)(.*)$', version_str)
    if match:
        return f"{match.group(1)}.{int(match.group(2)) + 1}.{match.group(3)}{match.group(4)}"
    return version_str
def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    # 1. Update bom version 1.5.0 -> 1.6.0
    content = re.sub(
        r'(<artifactId>bom</artifactId>\s*<version>)1\.5\.0(</version>)',
        r'\g<1>1.6.0\g<2>',
        content
    )
    content = re.sub(
        r'(<artifactId>parent</artifactId>\s*<version>)1\.5\.0(</version>)',
        r'\g<1>1.6.0\g<2>',
        content
    )
    # 2. Update revision 1.5.0 -> 1.6.0
    content = re.sub(
        r'(<revision>)1\.5\.0(</revision>)',
        r'\g<1>1.6.0\g<2>',
        content
    )
    # 3. Update all core.*.version properties
    def replace_core_version(match):
        tag_open = match.group(1)
        version = match.group(2)
        tag_close = match.group(3)
        bumped = bump_minor(version)
        return f"{tag_open}{bumped}{tag_close}"
    content = re.sub(
        r'(<core\.[a-zA-Z0-9.-]+\.version>)([\d\.]+)(</core\.[a-zA-Z0-9.-]+\.version>)',
        replace_core_version,
        content
    )
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
def main():
    skip_dirs = ['node_modules', 'target', '.git']
    for root, dirs, files in os.walk('.'):
        dirs[:] = [d for d in dirs if d not in skip_dirs]
        for name in files:
            if name == 'pom.xml':
                filepath = os.path.join(root, name)
                process_file(filepath)
                print(f"Processed: {filepath}")
if __name__ == '__main__':
    main()
