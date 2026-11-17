from pathlib import Path
import subprocess
root=Path(__file__).resolve().parents[2];tests=root/'tests/updater';classes=root/'build/updater-tests';classes.mkdir(parents=True,exist_ok=True)
package='cat/urysoft/widgets'
sources=list((tests/'src').rglob('*.java'))+[root/'app/src/main/java'/package/'UrySoftUpdater.java']
manifest=root/'app/src/main/java'/package/'UpdateManifest.java'
if manifest.exists():sources.append(manifest)
subprocess.run(['java','com.sun.tools.javac.Main','-source','8','-target','8','-encoding','UTF-8','-cp',str(tests/'json.jar'),'-d',str(classes),*[str(p) for p in sources]],check=True)
subprocess.run(['java','-cp',str(classes)+':'+str(tests/'json.jar'),'UpdaterStartupTest','Foldery'],check=True)
