#!/usr/bin/env python3
"""Check source files and references without installed Android SDK (not a build)."""
from pathlib import Path
from xml.etree import ElementTree
import re
import subprocess

root = Path(__file__).resolve().parents[1]
java_dir = root / 'app/src/main/java/pl/touchguard'
resources = root / 'app/src/main/res'

for file in (root / 'app/src/main').rglob('*.xml'):
    ElementTree.parse(file)
    print('XML OK:', file.relative_to(root))

for name in ['MainActivity', 'ToggleActivity', 'TouchLockService', 'TouchTileService', 'Prefs']:
    file = java_dir / (name + '.java')
    assert file.is_file(), f'Missing Java class: {file}'
    text = file.read_text()
    assert f'class {name}' in text, f'Class name mismatch: {file}'

for file in java_dir.glob('*.java'):
    data = file.read_text()
    for kind, resource in re.findall(r'R\.(layout|drawable|string|id)\.([A-Za-z0-9_]+)', data):
        if kind in ('layout', 'drawable'):
            assert list((resources / kind).glob(f'{resource}.*')), f'Missing {kind}/{resource} in {file}'
        elif kind == 'string':
            strings = (resources / 'values/strings.xml').read_text()
            assert f'name="{resource}"' in strings, f'Missing string: {resource}'
        else:
            layouts = ''.join(f.read_text() for f in (resources / 'layout').glob('*.xml'))
            assert f'@+id/{resource}' in layouts, f'Missing layout id: {resource}'

for script in ['build_apk_ubuntu.sh', 'install_apk_ubuntu.sh']:
    subprocess.run(['bash', '-n', str(root / script)], check=True)

manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
for value in ['SYSTEM_ALERT_WINDOW', 'FOREGROUND_SERVICE_SPECIAL_USE',
              'TYPE_APPLICATION_OVERLAY', 'ACTION_LOCK', 'ACTION_UNLOCK']:
    assert value in manifest + '\n' + '\n'.join(f.read_text() for f in java_dir.glob('*.java'))

print('Static checks passed. This does not compile or test the Android app.')
