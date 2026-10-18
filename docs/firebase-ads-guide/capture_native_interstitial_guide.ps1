param(
    [Parameter(Mandatory = $true)][string] $Serial,
    [string] $Adb = 'adb',
    [string] $Python = 'python',
    [ValidateSet('all', 'default', 'compact')][string] $ReadyStyle = 'all',
    [switch] $CollectOnly
)

$ErrorActionPreference = 'Stop'
$guideDirectory = $PSScriptRoot
$repositoryRoot = (Resolve-Path (Join-Path $guideDirectory '../..')).Path
$cases = Get-Content -Raw -Encoding UTF8 (Join-Path $guideDirectory 'native-interstitial-cases.json') | ConvertFrom-Json
if ($cases.Count -ne 72) { throw 'Expected 72 guide configs.' }
if ($ReadyStyle -ne 'all') { $cases = @($cases | Where-Object readyStyle -eq $ReadyStyle) }

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments)
    & $Adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')" }
}

if (-not $CollectOnly) {
    Push-Location $repositoryRoot
    $previousSerial = $env:ANDROID_SERIAL
    try {
        & $Python (Join-Path $guideDirectory 'build_native_interstitial_guide.py')
        if ($LASTEXITCODE -ne 0) { throw 'Guide generation failed.' }
        $cases = Get-Content -Raw -Encoding UTF8 (Join-Path $guideDirectory 'native-interstitial-cases.json') | ConvertFrom-Json
        if ($ReadyStyle -ne 'all') { $cases = @($cases | Where-Object readyStyle -eq $ReadyStyle) }
        $env:ANDROID_SERIAL = $Serial
        Invoke-Adb shell input keyevent KEYCODE_WAKEUP
        Invoke-Adb shell wm dismiss-keyguard
        $methods = $cases | ForEach-Object { 'com.codebasetemplate.AdmobDeviceSmokeTest#testGuide_' + ($_.id -replace '-', '_') }
        $filter = '-Pandroid.testInstrumentationRunnerArguments.class=' + ($methods -join ',')
        & .\gradlew.bat :app:connectedDevDebugAndroidTest $filter --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Device tests failed. Review the Gradle report before collecting.' }
    } finally {
        $env:ANDROID_SERIAL = $previousSerial
        Pop-Location
    }
}

$images = Join-Path $guideDirectory 'native-interstitial-screenshots'
$evidence = Join-Path $guideDirectory 'native-interstitial-evidence'
New-Item -ItemType Directory -Force $images, $evidence | Out-Null
foreach ($case in $cases) {
    foreach ($orientation in @('portrait', 'landscape')) {
        $prefix = "guide-$($case.id)-$orientation"
        $remote = "/data/local/tmp/native-interstitial-$orientation"
        Invoke-Adb pull "$remote/$prefix.json" $evidence
        $result = Get-Content -Raw -Encoding UTF8 (Join-Path $evidence "$prefix.json") | ConvertFrom-Json
        if ($result.passed -ne $true -or $result.caseId -ne $case.id -or $result.orientation -ne $orientation) {
            throw "Missing passing evidence: $prefix"
        }
        foreach ($state in @('waiting', 'ready')) {
            Invoke-Adb pull "$remote/$prefix-$state.png" $images
        }
    }
}
foreach ($version in @(4, 5, 6)) {
    if ($ReadyStyle -ne 'compact') {
        Copy-Item -LiteralPath (Join-Path $images "guide-v$version-dark-left-close-portrait-ready.png") `
            -Destination (Join-Path $guideDirectory "native-screenshots/full_interstitial_v$version.png")
    }
}
Write-Host "Collected $($cases.Count * 4) screenshots and $($cases.Count * 2) passing evidence JSON files. Run docs/tests/test_native_interstitial_guide.py to validate config correspondence."
