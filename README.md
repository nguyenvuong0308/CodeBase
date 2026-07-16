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
    implementation("com.github.nguyenvuong0308:CodeBase:2.1.0")
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
- `utilities`
