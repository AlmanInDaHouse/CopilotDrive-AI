param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) {
    $localJdk = Get-ChildItem -LiteralPath (Join-Path $repoRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'Instala JDK 17 y configura JAVA_HOME. Consulta docs/INSTALL_ANDROID.md.' }
if (-not $Tasks) { $Tasks = @(':core:test', ':testing:test', ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug') }
$previousPreference = $ErrorActionPreference
try {
    # Windows PowerShell 5 treats native stderr (including SDK warnings) as errors
    # when output is redirected. Gradle's exit code remains the source of truth.
    $ErrorActionPreference = 'Continue'
    & (Join-Path $repoRoot 'android/gradlew.bat') -p (Join-Path $repoRoot 'android') @Tasks --console=plain 2>&1 |
        ForEach-Object { $_.ToString() }
    $gradleExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousPreference
}
exit $gradleExitCode
