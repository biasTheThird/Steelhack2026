$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$nativeBuild = Join-Path $projectRoot 'build\windows\native'
$launcher = Join-Path $nativeBuild 'Guess-A-Morph-launcher.exe'
$builderRaw = Join-Path $nativeBuild 'Build-Guess-A-Morph.raw.exe'
$launcherObject = Join-Path $nativeBuild 'GuessAMorphLauncher.obj'
$builderObject = Join-Path $nativeBuild 'BuildGuessAMorph.obj'
$builderFinal = Join-Path $projectRoot 'Build-Guess-A-Morph.exe'
$builderTemporary = Join-Path $projectRoot 'Build-Guess-A-Morph.exe.tmp'

$vswhere = 'C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe'
if (-not (Test-Path -LiteralPath $vswhere)) {
    throw 'Visual Studio Build Tools were not found.'
}
$visualStudio = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $visualStudio) {
    throw 'The Visual C++ x64 build tools were not found.'
}
$vcvars = Join-Path $visualStudio 'VC\Auxiliary\Build\vcvars64.bat'

New-Item -ItemType Directory -Force -Path $nativeBuild | Out-Null

$launcherSource = Join-Path $PSScriptRoot 'GuessAMorphLauncher.cpp'
$builderSource = Join-Path $PSScriptRoot 'BuildGuessAMorph.cpp'
$compile = 'call "{0}" && cl.exe /nologo /std:c++17 /O2 /EHsc /Fo:"{1}" "{2}" /link /SUBSYSTEM:WINDOWS /OUT:"{3}" shell32.lib user32.lib && cl.exe /nologo /std:c++17 /O2 /EHsc /Fo:"{4}" "{5}" /link /OUT:"{6}"' -f $vcvars, $launcherObject, $launcherSource, $launcher, $builderObject, $builderSource, $builderRaw
& $env:COMSPEC /d /s /c $compile
if ($LASTEXITCODE -ne 0) {
    throw "The native compiler exited with code $LASTEXITCODE."
}

$marker = [Text.Encoding]::ASCII.GetBytes("GAMLAUNCHER_V1`r`n")
$launcherBytes = [IO.File]::ReadAllBytes($launcher)
$length = [BitConverter]::GetBytes([UInt64]$launcherBytes.LongLength)

$input = [IO.File]::OpenRead($builderRaw)
try {
    $output = [IO.File]::Create($builderTemporary)
    try {
        $input.CopyTo($output)
        $output.Write($launcherBytes, 0, $launcherBytes.Length)
        $output.Write($marker, 0, $marker.Length)
        $output.Write($length, 0, $length.Length)
    }
    finally {
        $output.Dispose()
    }
}
finally {
    $input.Dispose()
}

Move-Item -LiteralPath $builderTemporary -Destination $builderFinal -Force
Write-Host "Created $builderFinal"
