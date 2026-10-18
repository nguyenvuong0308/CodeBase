param(
    [Parameter(Mandatory = $true)][string] $Serial,
    [string] $Adb = 'adb',
    [string] $Python = 'python',
    [switch] $CollectOnly
)
$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if (-not $CollectOnly) {
    Push-Location $repositoryRoot
    $previousSerial = $env:ANDROID_SERIAL
    $previousStayAwake = $null
    try {
        & $Python (Join-Path $PSScriptRoot 'build_native_interstitial_guide.py')
        if ($LASTEXITCODE -ne 0) { throw 'Guide generation failed.' }
        $env:ANDROID_SERIAL = $Serial
        $previousStayAwake = (& $Adb -s $Serial shell settings get global stay_on_while_plugged_in).Trim()
        if ($LASTEXITCODE -ne 0) { throw 'Cannot read device stay-awake setting.' }
        & $Adb -s $Serial shell svc power stayon true
        if ($LASTEXITCODE -ne 0) { throw 'Cannot keep device awake during the build/tests.' }
        & $Adb -s $Serial shell input keyevent KEYCODE_WAKEUP
        if ($LASTEXITCODE -ne 0) { throw 'Device wake failed.' }
        & $Adb -s $Serial shell wm dismiss-keyguard
        if ($LASTEXITCODE -ne 0) { throw 'Device unlock failed.' }
        $methods = foreach ($version in 4..6) {
            foreach ($orientation in @('portrait','landscape')) {
                "com.codebasetemplate.AdmobDeviceSmokeTest#testThemeShuffle_v${version}_${orientation}"
            }
        }
        $methods += @('com.codebasetemplate.AdmobDeviceSmokeTest#testTemplateShuffle_portrait', 'com.codebasetemplate.AdmobDeviceSmokeTest#testTemplateShuffle_landscape')
        $filter = '-Pandroid.testInstrumentationRunnerArguments.class=' + ($methods -join ',')
        & .\gradlew.bat :app:connectedDevDebugAndroidTest $filter --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Device tests failed.' }
    } finally {
        if ($null -ne $previousStayAwake) {
            if ($previousStayAwake -eq 'null') {
                & $Adb -s $Serial shell settings delete global stay_on_while_plugged_in
            } else {
                & $Adb -s $Serial shell settings put global stay_on_while_plugged_in $previousStayAwake
            }
            if ($LASTEXITCODE -ne 0) { Write-Warning 'Could not restore the device stay-awake setting.' }
        }
        $env:ANDROID_SERIAL = $previousSerial
        Pop-Location
    }
}
foreach ($version in 4..6) {
    foreach ($orientation in @('portrait','landscape')) {
        foreach ($theme in @('light','dark')) {
            $prefix = "guide-v$version-theme-shuffle-$orientation-$theme"
            $remote = "/data/local/tmp/native-interstitial-$orientation"
            & $Adb -s $Serial pull "$remote/$prefix.json" (Join-Path $PSScriptRoot 'native-interstitial-evidence')
            if ($LASTEXITCODE -ne 0) { throw "Evidence pull failed: $prefix" }
            foreach ($state in @('waiting','ready')) {
                & $Adb -s $Serial pull "$remote/$prefix-$state.png" (Join-Path $PSScriptRoot 'native-interstitial-screenshots')
                if ($LASTEXITCODE -ne 0) { throw "Screenshot pull failed: $prefix-$state" }
            }
        }
    }
}
foreach ($version in 4..6) {
    foreach ($orientation in @('portrait','landscape')) {
        foreach ($theme in @('light','dark')) {
            $prefix = "guide-template-shuffle-$orientation-v$version-$theme"
            $remote = "/data/local/tmp/native-interstitial-$orientation"
            & $Adb -s $Serial pull "$remote/$prefix.json" (Join-Path $PSScriptRoot 'native-interstitial-evidence')
            if ($LASTEXITCODE -ne 0) { throw "Evidence pull failed: $prefix" }
            foreach ($state in @('waiting','ready')) {
                & $Adb -s $Serial pull "$remote/$prefix-$state.png" (Join-Path $PSScriptRoot 'native-interstitial-screenshots')
                if ($LASTEXITCODE -ne 0) { throw "Screenshot pull failed: $prefix-$state" }
            }
        }
    }
}
Write-Host 'Collected 48 shuffle screenshots and 24 evidence files. Run Python guide tests to verify correspondence.'
