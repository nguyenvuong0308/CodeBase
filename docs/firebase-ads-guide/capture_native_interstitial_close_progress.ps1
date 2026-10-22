param(
    [Parameter(Mandatory = $true)][string] $Serial,
    [string] $Adb = 'adb'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$images = Join-Path $PSScriptRoot 'native-interstitial-screenshots'
$evidenceDirectory = Join-Path $PSScriptRoot 'native-interstitial-evidence'
$previousSerial = $env:ANDROID_SERIAL
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments)
    & $Adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')" }
}

Push-Location $repositoryRoot
try {
    $model = (Invoke-Adb shell getprop ro.product.model).Trim()
    if ($model -ne 'Pixel 7a') { throw "Expected Pixel 7a, found $model" }
    $env:ANDROID_SERIAL = $Serial
    # Avoid the Windows short TEMP path problem when Java opens local sockets.
    New-Item -ItemType Directory -Force -Path '.gradle' | Out-Null
    $socketPath = Join-Path $repositoryRoot '.gradle'
    $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions `"-Djdk.net.unixdomain.tmpdir=$socketPath`"".Trim()
    Invoke-Adb shell input keyevent KEYCODE_WAKEUP
    Invoke-Adb shell wm dismiss-keyguard
    $methods = foreach ($version in 4..6) {
        "com.codebasetemplate.AdmobDeviceSmokeTest#testGuide_v${version}_dark_left_close"
        "com.codebasetemplate.AdmobDeviceSmokeTest#testHiddenIndicators_v$version"
    }
    & .\gradlew.bat :app:connectedDevDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$($methods -join ',')" --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Device tests failed; screenshots were not collected.' }
    New-Item -ItemType Directory -Force -Path $images, $evidenceDirectory | Out-Null
    foreach ($version in 4..6) {
        foreach ($hidden in @($false, $true)) {
            $caseId = "v$version-dark-left-close" + $(if ($hidden) { '-hidden-indicators' } else { '' })
            foreach ($orientation in @('portrait', 'landscape')) {
                $prefix = "guide-$caseId-$orientation"
                $remote = "/data/local/tmp/native-interstitial-$orientation"
                Invoke-Adb pull "$remote/$prefix.json" $evidenceDirectory
                $result = Get-Content -Raw -Encoding UTF8 (Join-Path $evidenceDirectory "$prefix.json") | ConvertFrom-Json
                if ($result.passed -ne $true -or $result.caseId -ne $caseId -or $result.orientation -ne $orientation -or $result.device -ne 'Pixel 7a' -or $result.config.hide_progress_count_down -ne $hidden -or $result.config.hide_text_count_down -ne $hidden) {
                    throw "Unexpected evidence: $prefix"
                }
                foreach ($state in @('waiting', 'ready')) {
                    Invoke-Adb pull "$remote/$prefix-$state.png" $images
                }
            }
        }
        Copy-Item -LiteralPath (Join-Path $images "guide-v$version-dark-left-close-portrait-ready.png") -Destination (Join-Path $PSScriptRoot "native-screenshots/full_interstitial_v$version.png")
    }
    $report = Join-Path $repositoryRoot 'app/build/outputs/androidTest-results/connected/debug/flavors/dev/TEST-Pixel 7a - 16-_app-dev.xml'
    Copy-Item -LiteralPath $report -Destination (Join-Path $evidenceDirectory 'device-tests-close-progress.xml')
    Write-Host 'Collected 24 screenshots and 12 passing evidence files on Pixel 7a.'
} finally {
    $env:ANDROID_SERIAL = $previousSerial
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    Pop-Location
}
