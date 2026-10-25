<# External, opt-in JDK 21 profiling. Does not launch Minecraft or alter its options/world. #>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateRange(1,2147483647)][int]$ProcessId,
    [Parameter(Mandatory)][ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Scenario,
    [ValidateRange(10,300)][int]$Seconds = 90,
    [Parameter(Mandatory)][string]$SceneNotes,
    [string]$JdkBin = '',
    [string]$OptionsFile = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$jcmd = if ($JdkBin) { Join-Path $JdkBin 'jcmd.exe' } else { (Get-Command jcmd -ErrorAction Stop).Source }
$jfr = Join-Path (Split-Path $jcmd) 'jfr.exe'
if (!(Test-Path -LiteralPath $jfr)) { throw 'Use a JDK installation containing jcmd.exe and jfr.exe.' }
if (!$OptionsFile) { $OptionsFile = Join-Path $projectRoot 'run/options.txt' }
$process = Get-Process -Id $ProcessId -ErrorAction Stop
if ($process.ProcessName -notin @('java','javaw')) { throw 'Choose the Minecraft Java process, not its launcher.' }
# Identify the JVM without collecting command-line arguments, which can contain account tokens.
$identity = & $jcmd $ProcessId VM.version 2>&1
if ($LASTEXITCODE -ne 0) { throw "Cannot attach to the JVM: $identity" }
$directory = Join-Path $projectRoot ('build/performance/' + $Scenario + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Path $directory | Out-Null
$recording = Join-Path $directory 'recording.jfr'
$selectedOptions = @()
if (Test-Path -LiteralPath $OptionsFile) {
    $selectedOptions = @(Get-Content -LiteralPath $OptionsFile | Where-Object { $_ -match '^(renderDistance|simulationDistance|graphicsMode|enableVsync|maxFps|fullscreen|overrideWidth|overrideHeight|guiScale|particles|entityDistanceScaling|fov|mipmapLevels|biomeBlendRadius):' })
}
$jar = Join-Path $projectRoot 'build/libs/katzencraftmetals-1.0.0.jar'
$metadata = [ordered]@{
    scenario=$Scenario; seconds=$Seconds; startedUtc=[DateTime]::UtcNow.ToString('o')
    processId=$ProcessId; jvmVersion=($identity -join "`n"); notes=$SceneNotes
    options=$selectedOptions; jarSha256=if (Test-Path -LiteralPath $jar) { (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash } else { 'not built' }
    caution='Verify this is the Minecraft JVM. Record actual framebuffer resolution, mod/shader list, camera and frame times manually. JFR is CPU/allocation sampling, not a GPU or FPS benchmark.'
}
$metadata | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $directory 'metadata.json') -Encoding utf8
$name = 'foundry_' + [guid]::NewGuid().ToString('N')
# Timed capture returns immediately. JFR does the timed dump; no sleeping/helper window.
$result = & $jcmd $ProcessId JFR.start "name=$name" 'settings=profile' "duration=$($Seconds)s" "filename=$recording" 'dumponexit=true' 2>&1
$code = $LASTEXITCODE
$result | Set-Content -LiteralPath (Join-Path $directory 'jcmd-start.txt') -Encoding utf8
if ($code -ne 0) { throw "JFR.start failed; inspect $directory/jcmd-start.txt" }
Write-Output $result
Write-Output "Remain in the unchanged scene for $Seconds seconds; do not pause the integrated server."
Write-Output "Recording: $recording"
Write-Output "After completion: python tools/performance/summarize_foundry_jfr.py `"$recording`" --jfr `"$jfr`""
