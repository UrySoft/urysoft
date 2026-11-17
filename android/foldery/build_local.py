#!/usr/bin/env python3
"""Build the dependency-free Android app using the official SDK and Java 17+."""
from pathlib import Path
import argparse, os, shutil, subprocess, zipfile, tempfile, re

ROOT = Path(__file__).resolve().parent

def run(args, **kwargs):
    print('Running:', args[0], args[1] if len(args) > 1 else '', flush=True)
    subprocess.run([str(a) for a in args], check=True, **kwargs)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--sdk', default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    parser.add_argument('--output', default=str(ROOT / 'build' / 'UrySoft-Foldery.2.AI-1.6.3.apk'))
    args = parser.parse_args()
    if not args.sdk:
        raise SystemExit('Set ANDROID_HOME or pass --sdk /path/to/android-sdk')
    sdk = Path(args.sdk)
    tools = sdk / 'build-tools' / '36.0.0'
    if not tools.is_dir(): tools = sdk / 'build-tools' / 'android-16'
    android = sdk / 'platforms' / 'android-36' / 'android.jar'
    if not android.is_file(): android = sdk / 'platform' / 'android-36' / 'android.jar'
    build_base = ROOT / 'build'
    build_base.mkdir(exist_ok=True)
    build = Path(tempfile.mkdtemp(prefix='manual-', dir=build_base))
    generated, classes, dex = build / 'generated', build / 'classes', build / 'dex'
    for directory in (generated, classes, dex): directory.mkdir()
    source = ROOT / 'app' / 'src' / 'main'
    run([tools / 'aapt2', 'compile', '--dir', source / 'res', '-o', build / 'resources.zip'])
    manifest = build / 'AndroidManifest.xml'
    manifest.write_text((source / 'AndroidManifest.xml').read_text().replace('<manifest ', '<manifest package="cat.urysoft.widgets" ', 1))
    run([tools / 'aapt2', 'link', '-I', android, '--manifest', manifest, '--java', generated,
         '--min-sdk-version', '26', '--target-sdk-version', '36', '--version-code', '26', '--version-name', '1.6.3',
         '-o', build / 'resources.apk', build / 'resources.zip'])
    sources = sorted((source / 'java').rglob('*.java')) + sorted(generated.rglob('*.java'))
    run(['java', 'com.sun.tools.javac.Main', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-classpath', android, '-d', classes, *sources])
    with zipfile.ZipFile(build / 'classes.jar', 'w', zipfile.ZIP_DEFLATED) as jar:
        for f in classes.rglob('*.class'): jar.write(f, f.relative_to(classes))
    run(['java', '-cp', tools / 'lib' / 'd8.jar', 'com.android.tools.r8.D8', '--lib', android, '--min-api', '26', '--output', dex, build / 'classes.jar'])
    shutil.copy2(build / 'resources.apk', build / 'unsigned.apk')
    with zipfile.ZipFile(build / 'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as apk:
        for f in dex.glob('*.dex'): apk.write(f, f.name)
    run([tools / 'zipalign', '-P', '16', '-f', '4', build / 'unsigned.apk', build / 'aligned.apk'])
    signing = ROOT / 'signing'; signing.mkdir(exist_ok=True)
    props = signing / 'signing.properties'; keystore = signing / 'ury-widgets.jks'
    if not props.is_file() or not keystore.is_file():
        raise SystemExit('Original signing files are required. Refusing to create a new key.')
    properties = dict(line.strip().split('=', 1) for line in props.read_text().splitlines() if '=' in line)
    env = dict(os.environ, URY_WIDGETS_STORE_PASS=properties['storePassword'], URY_WIDGETS_KEY_PASS=properties['keyPassword'])
    output = Path(args.output).resolve(); output.parent.mkdir(parents=True, exist_ok=True)
    run(['java', '-jar', tools / 'lib' / 'apksigner.jar', 'sign', '--ks', keystore, '--ks-key-alias', 'urywidgets',
         '--ks-pass', 'env:URY_WIDGETS_STORE_PASS', '--key-pass', 'env:URY_WIDGETS_KEY_PASS', '--out', build / 'signed.apk', build / 'aligned.apk'], env=env)
    run(['java', '-jar', tools / 'lib' / 'apksigner.jar', 'verify', '--verbose', build / 'signed.apk'])
    certificate = subprocess.check_output(['java', '-jar', str(tools / 'lib' / 'apksigner.jar'), 'verify', '--print-certs', str(build / 'signed.apk')], text=True)
    match = re.search(r'certificate SHA-256 digest: ([0-9a-fA-F]+)', certificate)
    if not match or match.group(1).lower() != '61246e557cb0944efa09018ef1ba50725f461d51990a38548e1fa3985cc11d91':
        raise SystemExit('Signing certificate differs from Foldery 1.5.7; output refused.')

    temporary_output = output.with_name(output.name + '.tmp')
    with (build / 'signed.apk').open('rb') as source_apk, temporary_output.open('wb') as target_apk:
        shutil.copyfileobj(source_apk, target_apk)
        target_apk.flush(); os.fsync(target_apk.fileno())
    temporary_output.replace(output)
    run(['java', '-jar', tools / 'lib' / 'apksigner.jar', 'verify', '--verbose', output])
    tests = build / 'test-classes'; tests.mkdir(exist_ok=True)
    package = source / 'java' / 'cat' / 'urysoft' / 'widgets'
    run(['java', 'com.sun.tools.javac.Main', '-source', '8', '-target', '8', '-d', tests, package / 'LayoutPolicy.java', package / 'BadgeMath.java', package / 'LanguagePolicy.java', package / 'PremiumPolicy.java', ROOT / 'tests' / 'CoreTests.java', ROOT / 'tests' / 'PremiumTests.java'])
    run(['java', '-cp', tests, 'cat.urysoft.widgets.CoreTests'])
    run(['java', '-cp', tests, 'cat.urysoft.widgets.PremiumTests'])
    print('Built and verified:', output, flush=True)

if __name__ == '__main__': main()
