# CodeBase Core Libraries

Android core modules published with JitPack.

## Publish

1. Push the code to GitHub.
2. Create a Git tag or GitHub release, for example `1.0.0`.
3. Open `https://jitpack.io/#nguyenvuong0308/CodeBase` and request the tag build.

JitPack runs `./gradlew clean publishToMavenLocal -x test` from `jitpack.yml`.

## Install

Add JitPack to the consuming app:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = uri("https://jitpack.io"))
    }
}
```

Use the modules you need:

```kotlin
dependencies {
    implementation("com.github.nguyenvuong0308:CodeBase:3.3.0")
}
```

Available artifacts:

- `core`
- `ads`
- `analytics`
- `baseui`
- `billing`
- `config`
- `dimens`
- `preference`
- `rate`
- `startflow`
- `utilities`

## Documentation

- [Release notes 3.1.1-3.3.0](docs/release-notes-3.1.1-3.3.0.md)
- [Release notes 3.1.1](docs/release-notes-3.1.1.md)
- [Release notes 3.0.5–3.0.9](docs/release-notes-3.0.5-3.0.9.md)
- [Firebase Ads configuration guide](docs/firebase-ads-guide/firebase_ads_config_guide.html)
- [Ads A/B testing overrides](docs/ads-ab-testing-config.md)
- [Native collapsible remote config](docs/native-collapsible-config.md)
- [StartFlow Language V1/V2 UI customization](docs/startflow-language-ui-customization.md)
- [StartFlow Onboarding UI customization](docs/startflow-onboarding-ui-customization.md)

### Native ad text sizes

Native placements support optional text sizes configured in Firebase Remote Config. Values from `1` to `35` map to the matching responsive `@dimen/_Ndp` resource; omit a field or use a value outside that range to keep the template layout default.

```json
{
  "primary_text_size_dp": 16,
  "body_text_size_dp": 14,
  "cta_text_size_dp": 14
}
```
