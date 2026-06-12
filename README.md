# Gauge + Appium with BrowserStack App Automate

Run [Gauge](https://gauge.org/) + [Appium](https://appium.io/) (Java) mobile tests on real devices
in the [BrowserStack App Automate](https://app-automate.browserstack.com/) cloud, instrumented by the
[BrowserStack Java SDK](https://www.browserstack.com/docs/app-automate/appium/getting-started/java).

The SDK reads `browserstack.yml`, uploads/points at the app under test, provisions the device, and reports
each scenario's name and status to App Automate and Test Observability — no per-test capability code.

This sample currently ships the **`android/`** platform directory (Android, Samsung Galaxy S22 Ultra).

## Prerequisites

- A [BrowserStack](https://www.browserstack.com/) account (username + access key).
- **JDK 11+** (`java -version`).
- **Gradle 7+** (a wrapper can be generated with `gradle wrapper`).
- **Gauge CLI** with the Java plugin:
  ```bash
  brew install gauge        # macOS (or see https://docs.gauge.org/getting_started/installing-gauge.html)
  gauge install java
  gauge install html-report
  gauge install screenshot
  ```

## Setup

```bash
git clone <this-repo>
cd gauge-appium/android
```

Configure credentials — set them as environment variables (recommended) or edit `android/browserstack.yml`:

```bash
export BROWSERSTACK_USERNAME="YOUR_USERNAME"
export BROWSERSTACK_ACCESS_KEY="YOUR_ACCESS_KEY"
```

Resolve dependencies and stage the BrowserStack Java SDK agent:

```bash
gradle clean build copyBrowserStackAgent
```

`copyBrowserStackAgent` stages the resolved `browserstack-java-sdk` jar at `build/agent/browserstack-java-sdk.jar`.
The `-javaagent` is attached to the **Gauge Java runner JVM** via `env/default/java.properties`
(`gauge_jvm_args`) — Gauge spawns its own runner JVM, so the agent must be wired there (not on the Gradle
build JVM) for the Appium driver to be instrumented.

### App under test

`android/browserstack.yml` points at a pre-uploaded `WikipediaSample.apk`
(`app: bs://92d48b416632f2b1734259565ceab61b05ad0b24`). To use your own build, upload it and replace the value:

```bash
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
  -F "file=@/path/to/WikipediaSample.apk"
# -> use the returned "app_url" (bs://...) as the `app:` value
```

## Run Sample Test

Searches the Wikipedia sample app for "BrowserStack" and asserts results are listed.

```bash
cd android
gradle runSampleTest
# or run the whole suite:  gradle gauge
# or via the Gauge CLI:    gauge run specs/bstack-specs/bstack-sample.spec
```

## Run Local Test

Verifies the [BrowserStack Local](https://www.browserstack.com/local-testing/app-automate) tunnel is
connected. The SDK starts the tunnel automatically because `browserstackLocal: true` is set in
`browserstack.yml`.

```bash
cd android
gradle runLocalTest
# or via the Gauge CLI:  gauge run specs/bstack-specs/local-test.spec
```

## Notes / Dashboard

- View runs, video, device logs, and network logs at **https://app-automate.browserstack.com/**.
- With `testObservability: true`, builds also appear at **https://observability.browserstack.com/**.
- The `framework: gauge` token in `browserstack.yml` lets the Java SDK report each scenario's name and
  status to BrowserStack.
- The BrowserStack Gradle SDK plugin (`com.browserstack.gradle-sdk`) is declared per the BrowserStack
  SDK convention; the actual Appium-driver instrumentation for this Gauge/Java flow is performed by the
  `browserstack-java-sdk` `-javaagent` on the Gauge runner JVM (see Setup).

### Known issue (BrowserStack Java SDK + Gauge App Automate)

With the **published** `browserstack-java-sdk:1.59.7`, `framework: gauge` disables the SDK's normal
in-process flow and instead drives the run through the **Gradle Tooling API** (it re-invokes the
`gauge` task per device platform). On this version the SDK mis-resolves the Gradle installation to
`GRADLE_USER_HOME` (`~/.gradle`) and the Tooling-API connection fails:

```
GradleTaskExecutor - Using Gradle Installation /Users/<you>/.gradle
GradleTaskExecutor - Gradle connection failed for Platform Index: 0
[SDK-TRA-006] ... could not find relevant classes from gauge on your class path
```

The SDK then ends its run inside the `-javaagent` premain and the Gauge runner JVM exits before any
test (and therefore any Appium driver / device session) is created. Only the Observability/TestHub
build is created; no App Automate device session starts.

This reproduces with both `gauge run` and `gradle gauge`, with `gauge-java` 0.12.0 and 1.0.1, and with
or without the `framework` token. The known-working internal reference for this combo used **unpublished
local dev builds** (`browserstack-java-sdk-1.32.12` + `gradle-sdk` plugin `99.86.49`). Track the fix /
a published known-good SDK version with the BrowserStack Java SDK team before relying on this sample for
a live device run. The repo itself is complete and compiles; the blocker is in the SDK's Gauge App
Automate execution path, not in the test code.
