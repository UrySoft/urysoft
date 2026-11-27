param([Parameter(Mandatory)][string]$Sdk,[Parameter(Mandatory)][string]$Jdk,[Parameter(Mandatory)][string]$Keystore,[string]$Alias='simbolos',[string]$OutDir=(Join-Path $env:TEMP 'urysoft-simbolos-build'))
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$bin=Join-Path $Sdk 'build-tools/36.0.0'
$platform=Join-Path $Sdk 'platforms/android-36/android.jar'
$env:JAVA_HOME=$Jdk
function Run($exe,[string[]]$arguments){ & $exe @arguments; if($LASTEXITCODE -ne 0){throw "Error al ejecutar $exe ($LASTEXITCODE)"} }
if(!$env:URY_SIGN_PASS){throw 'Define URY_SIGN_PASS para firmar'}
$build=Join-Path $OutDir ([guid]::NewGuid().ToString('N'))
$assets=Join-Path $build 'assets';$classes=Join-Path $build 'classes';$dex=Join-Path $build 'dex'
New-Item -ItemType Directory -Force $assets,$classes,$dex,"$assets/v1"|Out-Null
Copy-Item "$root/index.html","$root/ui.mjs" $assets
Copy-Item "$root/v1/codec.mjs","$root/v1/png.mjs","$root/v1/finders.mjs" "$assets/v1"
Run "$bin/aapt2.exe" @('link','-o',"$build/base.apk",'-I',$platform,'--manifest',"$PSScriptRoot/AndroidManifest.xml",'-A',$assets)
Run "$Jdk/bin/javac.exe" @('-encoding','UTF-8','-source','8','-target','8','-classpath',$platform,'-d',$classes,"$PSScriptRoot/MainActivity.java")
Run "$Jdk/bin/jar.exe" @('cf',"$build/app.jar",'-C',$classes,'.')
Run "$Jdk/bin/java.exe" @('-cp',"$bin/lib/d8.jar",'com.android.tools.r8.D8','--min-api','26','--lib',$platform,'--output',$dex,"$build/app.jar")
Run "$Jdk/bin/jar.exe" @('uf',"$build/base.apk",'-C',$dex,'classes.dex')
Run "$bin/zipalign.exe" @('-f','4',"$build/base.apk","$build/aligned.apk")
$apk="$OutDir/UrySoft-Simbolos-0.2.0.apk"
Run "$Jdk/bin/java.exe" @('-jar',"$bin/lib/apksigner.jar",'sign','--ks',$Keystore,'--ks-key-alias',$Alias,'--ks-pass','env:URY_SIGN_PASS','--out',$apk,"$build/aligned.apk")
Run "$Jdk/bin/java.exe" @('-jar',"$bin/lib/apksigner.jar",'verify','--verbose',$apk)
Write-Output $apk
