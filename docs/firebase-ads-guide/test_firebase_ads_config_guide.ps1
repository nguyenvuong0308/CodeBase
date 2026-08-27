$ErrorActionPreference = 'Stop'

function Assert-Guide {
    param(
        [Parameter(Mandatory = $true)]
        [bool] $Condition,
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Condition) {
        throw "[FAIL] $Message"
    }
}

$guideDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = Resolve-Path (Join-Path $guideDirectory '..\..')
$guidePath = Join-Path $guideDirectory 'firebase_ads_config_guide.html'
$captureScriptPath = Join-Path $guideDirectory 'capture_native_gallery.ps1'
$configParamPath = Join-Path $repositoryRoot 'core\config\src\main\java\com\core\config\data\helper\ConfigParam.kt'
$nativeTemplatePath = Join-Path $repositoryRoot 'core\config\src\main\java\com\core\config\domain\data\NativeTemplateSize.kt'
$modelDirectory = Join-Path $repositoryRoot 'core\config\src\main\java\com\core\config\data\model'

Assert-Guide (Test-Path -LiteralPath $guidePath -PathType Leaf) 'Guide HTML does not exist.'
Assert-Guide (Test-Path -LiteralPath $captureScriptPath -PathType Leaf) 'Native gallery capture script does not exist.'
Assert-Guide (Test-Path -LiteralPath $configParamPath -PathType Leaf) 'ConfigParam.kt was not found.'
Assert-Guide (Test-Path -LiteralPath $nativeTemplatePath -PathType Leaf) 'NativeTemplateSize.kt was not found.'
Assert-Guide (Test-Path -LiteralPath $modelDirectory -PathType Container) 'Config model directory was not found.'

$html = Get-Content -Raw -Encoding UTF8 -LiteralPath $guidePath

$requiredSections = @(
    'overview',
    'guide-1',
    'keys',
    'ad-place',
    'ab-testing',
    'native',
    'banner',
    'fullscreen',
    'app-open',
    'other',
    'onboarding-ui',
    'gallery'
)

$sectionIds = [regex]::Matches($html, '<section\s+id="([^"]+)"') |
    ForEach-Object { $_.Groups[1].Value }
$duplicateSections = $sectionIds | Group-Object | Where-Object Count -gt 1
Assert-Guide ($duplicateSections.Count -eq 0) "Duplicate section ids: $($duplicateSections.Name -join ', ')"

foreach ($section in $requiredSections) {
    Assert-Guide ($sectionIds -contains $section) "Required section is missing: #$section."
}

$tocMatch = [regex]::Match($html, '<ul class="toc">([\s\S]*?)</ul>')
Assert-Guide $tocMatch.Success 'Table of contents .toc was not found.'
$tocTargets = [regex]::Matches($tocMatch.Groups[1].Value, 'href="#([^"]+)"') |
    ForEach-Object { $_.Groups[1].Value }
foreach ($target in $tocTargets) {
    Assert-Guide ($sectionIds -contains $target) "TOC points to a missing section: #$target."
}
Assert-Guide ($tocTargets.Count -eq $requiredSections.Count) 'TOC does not cover all required sections.'

foreach ($tag in @('main', 'article', 'section', 'table', 'pre')) {
    $openCount = [regex]::Matches($html, "<$tag(?:\s|>)").Count
    $closeCount = [regex]::Matches($html, "</$tag>").Count
    Assert-Guide ($openCount -eq $closeCount) "Unbalanced <$tag> tags ($openCount/$closeCount)."
}

$keyTableMatch = [regex]::Match($html, '<table id="key-table">([\s\S]*?)</table>')
Assert-Guide $keyTableMatch.Success '#key-table was not found.'
$keyTableHtml = $keyTableMatch.Groups[1].Value
$configParamText = Get-Content -Raw -Encoding UTF8 -LiteralPath $configParamPath
$configKeys = [regex]::Matches($configParamText, 'override val key = "([^"]+)"') |
    ForEach-Object { $_.Groups[1].Value } |
    Sort-Object -Unique
$missingKeys = $configKeys | Where-Object { $keyTableHtml -notmatch [regex]::Escape($_) }
Assert-Guide ($missingKeys.Count -eq 0) "Parameter table is missing ConfigParam keys: $($missingKeys -join ', ')"
Assert-Guide ($keyTableHtml -match 'ad_place_ab_&lt;place_name&gt;') 'Dynamic A/B key pattern is missing.'

$contextualKeys = [regex]::Matches($html, 'data-config-key="([^"]+)"') |
    ForEach-Object { [System.Net.WebUtility]::HtmlDecode($_.Groups[1].Value) -split ' ' } |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    Sort-Object -Unique
$missingContextualKeys = $configKeys | Where-Object { $contextualKeys -notcontains $_ }
Assert-Guide ($missingContextualKeys.Count -eq 0) "Config keys missing contextual labels: $($missingContextualKeys -join ', ')"

$adPlaceSectionMatch = [regex]::Match($html, '<section id="ad-place">([\s\S]*?)</section>')
Assert-Guide $adPlaceSectionMatch.Success '#ad-place section was not found.'
$adPlaceSectionHtml = $adPlaceSectionMatch.Groups[1].Value
foreach ($providerMarker in @(
    'AppAdPlacesRemoteConfigKeyProvider.kt',
    'AdPlacesRemoteConfigKeyProvider',
    'bannerNativeAdPlacesKey',
    'appOpenAdPlacesKey',
    'rewardedRewardedInterInterAdPlacesKey',
    'provideAdPlacesRemoteConfigKeyProvider',
    '@IntoSet',
    '<div class="note">'
)) {
    Assert-Guide ($adPlaceSectionHtml.Contains($providerMarker)) "App key replacement guide is missing: $providerMarker"
}

$appOpenSectionMatch = [regex]::Match($html, '<section id="app-open">([\s\S]*?)</section>')
Assert-Guide $appOpenSectionMatch.Success '#app-open section was not found.'
$appOpenSectionHtml = $appOpenSectionMatch.Groups[1].Value
Assert-Guide ($appOpenSectionHtml -match '<h2>Open app ads</h2>\s*<div class="config-keys" data-config-key="app_open_ad_places">') 'Open app ads must show app_open_ad_places.'
Assert-Guide ($appOpenSectionHtml -match '<h3>App open global config</h3>\s*<div class="config-keys" data-config-key="app_open_ad_config">') 'App open global config must show app_open_ad_config.'

$modelText = (Get-ChildItem -LiteralPath $modelDirectory -Filter '*.kt' -File |
    ForEach-Object { Get-Content -Raw -Encoding UTF8 -LiteralPath $_.FullName }) -join "`n"
$jsonFields = [regex]::Matches($modelText, '@Json\(name = "([^"]+)"\)') |
    ForEach-Object { $_.Groups[1].Value } |
    Sort-Object -Unique
$missingFields = $jsonFields | Where-Object { $html -notmatch [regex]::Escape($_) }
Assert-Guide ($missingFields.Count -eq 0) "Guide is missing config model JSON fields: $($missingFields -join ', ')"

$imageSources = [regex]::Matches($html, '<img[^>]+src="([^"]+)"') |
    ForEach-Object { $_.Groups[1].Value }
Assert-Guide ($imageSources.Count -gt 0) 'Gallery has no images.'
foreach ($source in $imageSources) {
    $imagePath = Join-Path $guideDirectory $source
    Assert-Guide (Test-Path -LiteralPath $imagePath -PathType Leaf) "Gallery image does not exist: $source"
}

$gallerySectionMatch = [regex]::Match($html, '<section id="gallery">([\s\S]*?)</section>')
Assert-Guide $gallerySectionMatch.Success '#gallery section was not found.'
$galleryHtml = $gallerySectionMatch.Groups[1].Value
$nativeTemplateText = Get-Content -Raw -Encoding UTF8 -LiteralPath $nativeTemplatePath
$builtInNativeTemplateKeys = [regex]::Matches(
    $nativeTemplateText,
    'object\s+\w+\s*:\s*NativeTemplateSize\(\)\s*\{\s*override val key = "([^"]+)"'
) | ForEach-Object { $_.Groups[1].Value }
$captureScriptText = Get-Content -Raw -Encoding UTF8 -LiteralPath $captureScriptPath
foreach ($templateKey in $builtInNativeTemplateKeys) {
    Assert-Guide ($galleryHtml -match "native-screenshots/$([regex]::Escape($templateKey))\.png") "Gallery is missing built-in template: $templateKey."
    Assert-Guide ($captureScriptText -match "Key\s*=\s*'$([regex]::Escape($templateKey))'") "Capture script is missing built-in template: $templateKey."
}
foreach ($specialImage in @(
    'medium_collapsible_cta_bottom.png',
    'medium_collapsible_cta_bottom_v2.png',
    'native_pip.png'
)) {
    Assert-Guide ($galleryHtml -match "native-screenshots/$([regex]::Escape($specialImage))") "Gallery is missing special native preview: $specialImage."
    $specialImagePath = Join-Path $guideDirectory "native-screenshots/$specialImage"
    Assert-Guide (Test-Path -LiteralPath $specialImagePath -PathType Leaf) "Special native screenshot does not exist: $specialImage."
}
Assert-Guide ($captureScriptText -match "Key\s*=\s*'native_pip'") 'Capture script is missing native_pip.'

$jsonExampleCount = 0
$codeBlocks = [regex]::Matches($html, '<pre><code>([\s\S]*?)</code></pre>')
foreach ($block in $codeBlocks) {
    $content = [System.Net.WebUtility]::HtmlDecode($block.Groups[1].Value).Trim()
    if ($content.StartsWith('{') -or $content.StartsWith('[')) {
        try {
            $null = $content | ConvertFrom-Json
            $jsonExampleCount += 1
        } catch {
            throw "[FAIL] Invalid JSON example: $($_.Exception.Message)"
        }
    }
}
Assert-Guide ($jsonExampleCount -ge 10) "Too few JSON examples were validated: $jsonExampleCount."

foreach ($selector in @('#key-search', '#key-table', '.copy-button', 'IntersectionObserver')) {
    Assert-Guide ($html.Contains($selector)) "JavaScript behavior marker is missing: $selector"
}

$scriptMatch = [regex]::Match($html, '<script>([\s\S]*?)</script>')
Assert-Guide $scriptMatch.Success 'Interactive JavaScript was not found.'
$tempScriptPath = [System.IO.Path]::GetTempFileName()
$tempJsPath = [System.IO.Path]::ChangeExtension($tempScriptPath, '.js')
try {
    Move-Item -LiteralPath $tempScriptPath -Destination $tempJsPath
    Set-Content -LiteralPath $tempJsPath -Value $scriptMatch.Groups[1].Value -Encoding UTF8
    & node --check $tempJsPath
    Assert-Guide ($LASTEXITCODE -eq 0) 'JavaScript syntax check failed.'
} finally {
    if (Test-Path -LiteralPath $tempScriptPath) {
        Remove-Item -LiteralPath $tempScriptPath
    }
    if (Test-Path -LiteralPath $tempJsPath) {
        Remove-Item -LiteralPath $tempJsPath
    }
}

Write-Host "[PASS] Firebase guide: $($configKeys.Count) static keys, $($jsonFields.Count) model fields, $jsonExampleCount JSON examples, $($imageSources.Count) images."
