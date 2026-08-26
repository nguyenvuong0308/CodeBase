param(
    [string] $Serial = ""
)

$ErrorActionPreference = 'Stop'

$guideDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = Resolve-Path (Join-Path $guideDirectory '..\..')
$screenshotDirectory = Join-Path $guideDirectory 'native-screenshots'
$packageName = 'com.highsecure.base.code.dev'
$activityName = 'com.codebasetemplate.features.feature_demo_banner_native.ui.NativeGalleryPreviewActivity'
$componentName = "$packageName/$activityName"
$remoteScreenshot = '/sdcard/native-gallery-preview.png'
$remoteHierarchy = '/sdcard/native-gallery-window.xml'

$previews = @(
    @{ Key = 'small'; File = 'small.png' },
    @{ Key = 'small_cta_top'; File = 'small_cta_top.png' },
    @{ Key = 'small_cta_bottom'; File = 'small_cta_bottom.png' },
    @{ Key = 'small_cta_right'; File = 'small_cta_right.png' },
    @{ Key = 'small_banner_cta_right'; File = 'small_banner_cta_right.png' },
    @{ Key = 'mini_cta_right'; File = 'mini_cta_right.png' },
    @{ Key = 'small_long'; File = 'small_long.png' },
    @{ Key = 'small_for_popup'; File = 'small_for_popup.png' },
    @{ Key = 'medium'; File = 'medium.png' },
    @{ Key = 'medium_cta_top'; File = 'medium_cta_top.png' },
    @{ Key = 'medium_cta_bottom'; File = 'medium_cta_bottom.png' },
    @{ Key = 'medium_short_cta_bottom'; File = 'medium_short_cta_bottom.png' },
    @{ Key = 'medium_cta_right_top'; File = 'medium_cta_right_top.png' },
    @{ Key = 'medium_cta_right'; File = 'medium_cta_right.png' },
    @{ Key = 'medium_media_right'; File = 'medium_media_right.png' },
    @{ Key = 'medium_media_left'; File = 'medium_media_left.png' },
    @{ Key = 'medium_media_left_cta_right'; File = 'medium_media_left_cta_right.png' },
    @{ Key = 'full_cta_bottom'; File = 'full_cta_bottom.png' },
    @{ Key = 'full_cta_bottom_onboarding'; File = 'full_cta_bottom_onboarding.png' },
    @{ Key = 'full_cta_top'; File = 'full_cta_top.png' },
    @{ Key = 'full_cta_right'; File = 'full_cta_right.png' },
    @{ Key = 'full_interstitial_v1'; File = 'full_interstitial_v1.png' },
    @{ Key = 'full_interstitial_v2'; File = 'full_interstitial_v2.png' },
    @{ Key = 'full_interstitial_v3'; File = 'full_interstitial_v3.png' },
    @{
        Key = 'medium_cta_bottom'
        File = 'medium_collapsible_cta_bottom.png'
        Collapsible = $true
        ExpandTemplate = 'native_expand_v1'
    },
    @{
        Key = 'medium_cta_bottom'
        File = 'medium_collapsible_cta_bottom_v2.png'
        Collapsible = $true
        ExpandTemplate = 'native_expand_v2'
    },
    @{ Key = 'native_pip'; File = 'native_pip.png' }
)

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments)

    $adbArguments = if ([string]::IsNullOrWhiteSpace($Serial)) {
        $Arguments
    } else {
        @('-s', $Serial) + $Arguments
    }
    $output = & adb @adbArguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: adb $($adbArguments -join ' ')"
    }
    return $output
}

function Wait-NativePreview {
    param([Parameter(Mandatory = $true)][string] $TemplateKey)

    $readyMarker = "native-gallery-ready:$TemplateKey"
    $errorMarker = "native-gallery-error:$TemplateKey"
    for ($attempt = 0; $attempt -lt 90; $attempt += 1) {
        $null = Invoke-Adb -Arguments @('shell', 'uiautomator', 'dump', $remoteHierarchy)
        $hierarchy = (Invoke-Adb -Arguments @('shell', 'cat', $remoteHierarchy)) -join "`n"
        if ($hierarchy.Contains($readyMarker)) {
            Start-Sleep -Milliseconds 1200
            return
        }
        if ($hierarchy.Contains($errorMarker)) {
            throw "Native preview reported an error for $TemplateKey."
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Timed out waiting for native preview: $TemplateKey."
}

Push-Location $repositoryRoot
try {
    New-Item -ItemType Directory -Force -Path $screenshotDirectory | Out-Null
    $devices = Invoke-Adb -Arguments @('devices')
    $connectedDevices = $devices | Where-Object { $_ -match '\sdevice(?:\s|$)' }
    if ($connectedDevices.Count -eq 0) {
        throw 'No authorized ADB device is connected.'
    }

    foreach ($preview in $previews) {
        $templateKey = $preview.Key
        Write-Host "[CAPTURE] $($preview.File)"
        $null = Invoke-Adb -Arguments @('shell', 'am', 'force-stop', $packageName)

        $startArguments = @(
            'shell', 'am', 'start', '-W', '-n', $componentName,
            '--es', 'template_key', $templateKey
        )
        if ($preview.Collapsible) {
            $startArguments += @(
                '--ez', 'collapsible', 'true',
                '--es', 'expand_template', $preview.ExpandTemplate
            )
        }
        $null = Invoke-Adb -Arguments $startArguments
        Wait-NativePreview -TemplateKey $templateKey

        $null = Invoke-Adb -Arguments @('shell', 'screencap', '-p', $remoteScreenshot)
        $destination = Join-Path $screenshotDirectory $preview.File
        $null = Invoke-Adb -Arguments @('pull', $remoteScreenshot, $destination)

        if (-not (Test-Path -LiteralPath $destination -PathType Leaf)) {
            throw "Screenshot was not created: $destination"
        }
    }

    Write-Host "[PASS] Captured $($previews.Count) native gallery screenshots."
} finally {
    $null = Invoke-Adb -Arguments @('shell', 'rm', '-f', $remoteScreenshot, $remoteHierarchy)
    Pop-Location
}
