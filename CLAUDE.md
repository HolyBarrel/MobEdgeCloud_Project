# CLAUDE.md

Guidance for Claude Code and other coding agents working in this repository.

## Project

**DiscLab**, *A Motion-Based Disc Golf Training Simulator*. Always style the name exactly as "DiscLab": capital D and L, no space. This is a university project for **Mobile, Edge and Cloud Computing** at Sapienza University of Rome.

The Android phone is a motion controller for a virtual disc. The player:

1. aims using a physical landmark,
2. holds volume-down,
3. makes a throwing gesture,
4. releases the button at the moment of virtual release, without ever letting go of the phone.

An edge PC or a cloud backend analyzes the recorded motion, simulates the flight and returns a timed trajectory. The app plays the trajectory back on a top-down view of a virtual hole and scores it.

- **Priorities:** consistent power, motion-derived direction and meaningful hyzer/anhyzer control. The phone is a game controller, not a measuring instrument.
- **Guiding rule:** keep the implementation as simple as possible. The game motivates the computation; the distributed architecture and its measured evaluation satisfy the course.
- **Course requirements:** mobile sensing, edge processing connected to the cloud, a cloud API with storage, communication between tiers, and an experimental comparison of latency, execution time, bandwidth, energy or accuracy. These are as interpreted from the intro slides, which haven't been verified independently.
- **Project types:** the course offers Type A, three-tier (mobile ↔ edge ↔ cloud, with the student's own PC as edge), and Type B, two-tier (mobile ↔ cloud). DiscLab supports both.

## Status

- **Stage 0 is done** (26 Nov 2026). `MainActivity` hosts `MainScreen` (stateless composable), which renders `MainUiState` from `MainViewModel` (a `StateFlow`, collected with `collectAsStateWithLifecycle`).
  - Verified on the `Medium_Phone_API_37.0` emulator (API 37): build, unit tests, instrumented tests, cold launch, and state surviving rotation.
  - Stage 0 itself never ran on a phone. The app's first phone run came with #2 (below).
- **Stage 1 (Capture) is in progress** on branch `feature/stage1-capture`, as GitHub issues #2–#7 in the "Stage 1: Capture" milestone. Implement one issue at a time, and only when asked. Don't start later stages unless asked.
  - **#2, the volume-down throw button, is implemented** (27 Nov 2026) and replaced the Stage 0 tap counter. Its rules are under "Controller and calibration" below.
    - Verified on the emulator: build, unit tests, instrumented tests, lint, and scripted `adb` key injection (hold and release, repeats, the 5 s timeout, the notification shade, Home, screen off, rotation while armed, volume pass-through in Idle).
    - **On the user's physical phone** (27 Nov 2026, debug APK sideloaded), the user reported that it works: the button works, the app responds quickly, and the shortest press-to-release seen was about 50 ms. The interruption cancels and the comfort of a full throwing motion weren't reported separately.
  - Next: #3, sensor recording.
- Tests: `CaptureMachineTest` (JVM, the throw button's rules), `MainViewModelTest` (JVM, key consumption and the hold timer, using `MainDispatcherRule`) and `MainActivityTest` (instrumented: arms capture and injects a volume-down press through the system's input pipeline), plus the template examples.
- There is no backend code in the repo yet. When it's added, record its location and commands here.

## Sources of truth

When sources conflict, higher entries win:

1. The user's latest instructions.
2. `brainstorm-v2.md` (repo root) is the current incremental plan (v2, 26 Nov 2026). Read its relevant section before starting a stage.
3. This file, which digests brainstorm-v2 plus the decision history from the original ChatGPT design conversation.

The original `brainstorm.md` (v1) and the course slides (`01_INTRODUCTION_27.pdf`) are referenced by brainstorm-v2 but are **not** in this repo.

## Build and run

The host is Windows. Use `gradlew.bat` in PowerShell or cmd, and `./gradlew` in Git Bash.

| Task | Command |
|---|---|
| Debug APK | `./gradlew assembleDebug` |
| JVM unit tests | `./gradlew test`. One class: `./gradlew :app:testDebugUnitTest --tests "sapienz.allison.disclab.MainViewModelTest"` |
| Instrumented tests (device or emulator) | `./gradlew connectedAndroidTest` |
| Install on device | `./gradlew installDebug` |
| Install without USB (current way) | `./gradlew assembleDebug`, then send `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it. Wireless debugging (Android 11+) is the upgrade path |
| Launch | `adb shell am start -W -n sapienz.allison.disclab/.MainActivity` |
| Lint | `./gradlew lint` (report: `app/build/reports/lint-results-debug.html`) |
| Emulator | `emulator -avd Medium_Phone_API_37.0` (add `-no-window` for headless) |
| Simulate a throw (emulator) | `adb shell input keyevent --duration 800 KEYCODE_VOLUME_DOWN` holds volume-down for 0.8 s; `--longpress` instead sends one auto-repeat |

- All of these were run successfully on 26 Nov 2026, and again on 27 Nov 2026 for #2. A cold build takes about a minute.
- `connectedAndroidTest` **uninstalls the app** when it finishes, so run `installDebug` again before launching by hand.
- Emulator key injection:
  - Wait for the app's first frame (`am start -W`) before injecting keys. The first cold start after an install once took 14 s, and a key sent before the window has focus ends in an ANR.
  - Injected holds come out 0.2–0.7 s longer than `--duration`, because the shell injects them synchronously. The app shows the event times exactly as stamped.
  - On the API 37 emulator, the first volume press outside an armed capture only opens the volume panel, and the second changes the volume.
- Lint: 0 errors and 13 warnings: unused template colors, a redundant activity label, and newer versions available (including `kotlinx-coroutines-test`, which is pinned on purpose; see below). They're left alone under the no-migration rule below.
- `emulator` lives in the SDK's `emulator` folder; the SDK is at `%LOCALAPPDATA%\Android\Sdk`.
- `JAVA_HOME` points at Android Studio's bundled JBR (JDK 25). That matches the daemon toolchain in `gradle/gradle-daemon-jvm.properties`.
- `adb` isn't on `PATH`; it's in the Android SDK's `platform-tools`.

## Project configuration (preserve it)

- Single module `:app`. The namespace and applicationId are both `sapienz.allison.disclab`.
- Versions: AGP 9.5.3, Gradle 9.6.0, Kotlin 2.2.10, Compose BOM 2026.02.01. compileSdk and targetSdk are 37, minSdk is 26, and bytecode targets Java 11.
- Add dependencies through the version catalog, `gradle/libs.versions.toml`.
  - `lifecycle-runtime-compose` (added in Stage 0) shares the template's `lifecycleRuntimeKtx` version ref, because AndroidX Lifecycle releases them together. Bump both at once.
  - `kotlinx-coroutines-test` (tests only, added for #2) is pinned to the kotlinx-coroutines version that AndroidX resolves, which is 1.9.0 with Lifecycle 2.11.0. When bumping Lifecycle, check that version with `./gradlew :app:dependencies --configuration debugUnitTestRuntimeClasspath`, and ignore lint's newer-version warning for it.
  - Stage 0's `lifecycle-viewmodel-compose` was removed in #2. `MainActivity` now gets its ViewModel with `by viewModels()`, which `androidx.activity` 1.13 already provides.
- Kotlin uses AGP 9's built-in support. Only `com.android.application` and `org.jetbrains.kotlin.plugin.compose` are applied; don't add `org.jetbrains.kotlin.android`.
- R8 keep rules go in `app/src/main/keepRules/`; there is no `proguard-rules.pro`. Optimization is enabled for release builds, and the Gradle configuration cache is on.
- Don't migrate or upgrade the generated setup unless a feature needs it. When a stage needs a library, pick a compatible version from the official docs. Don't add dependencies for deferred features.

## Architecture (agreed)

### Android app

- Native Android with Kotlin and Jetpack Compose, using **lightweight MVVM**:
  - Compose screens render observable ViewModel state. Coroutines and StateFlow are the starting candidates.
  - Device and data services handle sensors, files and networking.
  - Pure functions hold the game rules and coordinate transforms.
- Use unidirectional data flow. Keep high-frequency raw sensor samples out of UI state.
- Use one app module with ordinary packages. Add repositories, interfaces, DI or more modules only when a working feature needs them. Don't create empty packages, speculative layers or full Clean Architecture.
- Gameplay is an explicit finite-state machine: a sealed class or enum plus transition logic, not one class per state. The phases are calibration → ready → recording → processing → awaiting playback → playback → result → completed, with cancel and retry handling. Only one throw can be active at a time.

### Edge and cloud

- The same motion-analysis and physics code runs on the edge PC and in the cloud. Deployment configuration chooses the service integrations.
  - **Two-tier:** Android → cloud for computation and persistence.
  - **Three-tier:** Android → PC for computation, then PC → cloud for persistence and retrieval.
- The PC does real computation; it doesn't just forward simulations to the cloud.
- The backend is a small modular monolith: HTTP handlers, pure numerical modules and storage integrations. No microservices, Kubernetes or custom auth.
- Fully offline, on-device simulation is outside the baseline.
- Use one **versioned** JSON-over-HTTP request/result schema:
  - Define only the fields the current stage needs, and keep Android-specific types out of the wire format.
  - Include throw IDs and timings from the first API call.
  - Document units, the clock basis and rotation conventions.
- These are candidates, not commitments: Python + FastAPI, Docker (one image on edge and cloud), Cloud Run, Firebase Anonymous Auth + Firestore, CameraX for the aim preview, and a conventional HTTP client.
- **Get approval before provisioning anything billable.**

### Routing, idempotency and persistence

- Start with a manually configured PC address, then add forced cloud, then automatic routing. The final modes are:
  - **force edge**
  - **force cloud**
  - **auto**: prefer the edge, fall back to the cloud.
- Forced modes never silently switch tiers.
- Use bounded timeouts, and don't probe an unavailable PC on every throw.
- Retry only connection and timeout failures. Invalid sensor input is never re-run on another tier.
- Reuse the same throw ID and frozen inputs across retries and fallback. Accept one result per throw, ignore stale responses, and never apply a score twice.
- A completed simulation is separate from a completed save, so playback can continue while a save is pending. For saves:
  - Store the accepted record locally before treating it as queued.
  - Pending saves must survive app restarts, and a small file-backed list is enough. Show their status as pending, saved or error.
  - Retry saves without re-simulating.
- Cloud writes deduplicate on owner, session and throw identity, and reject conflicting records instead of overwriting them.
- Authentication arrives together with cloud storage, as an anonymous but *authenticated* identity with server-side ownership checks. A random player ID alone is not authentication. Never fingerprint the device hardware.

## Domain rules

### Controller and calibration

- Pressing volume-down starts capture, and releasing it is the authoritative release:
  - Ignore repeat key events (`repeatCount > 0`) and cancel interrupted gestures.
  - Consume the key only during the throw; elsewhere volume works normally.
  - Test ergonomics on the real phone, and add an alternative button only if needed.
- **The throw button as implemented (#2)** lives in `capture/CaptureMachine.kt`: one pure transition function over sealed, immutable states, with no class per state.
  - Phases: **Idle** (capture off) ↔ **Ready** (armed by the on-screen control) → **Recording** (volume-down held). A throw ends as **Captured**(press, release) or **Cancelled**(reason). The phase then returns to Ready, and the outcome stays visible.
  - In Ready, the first `ACTION_DOWN` (`repeatCount == 0`) starts recording. Repeats and any press while recording are ignored, so only one throw is ever active.
  - `ACTION_UP` ends recording, and its event time is the release, with two exceptions that cancel instead: `FLAG_CANCELED`, or a key-up more than 5 s after the press ("held too long"). A ViewModel timer also cancels at 5 s if no key-up arrives.
  - Other cancels: the Activity pauses (switching apps, screen off), or the window loses focus (notification shade, system dialogs). The reason shown is whichever signal arrives first. Home and screen-off usually show "key press cancelled by the system", because Android sends a canceled key-up before `onPause`.
  - Volume-down is consumed only in Ready and Recording, in `MainActivity.onKeyDown`/`onKeyUp`. Don't override `dispatchKeyEvent`: androidx's `ComponentActivity` marks it library-internal, and lint fails with `RestrictedApi`.
  - Disarming is ignored while recording, so a palm on the screen can't end a throw.
  - While capture is armed, the screen stays on and the orientation is locked (`SCREEN_ORIENTATION_LOCKED`). A screen timeout or a rotation (which recreates the Activity) would otherwise cancel the throw.
  - Key times are `KeyEvent.getEventTime()` values: whole milliseconds in the `SystemClock.uptimeMillis()` base, stored as-is until #4 converts them.
- **Grip:** any comfortable, secure grip. The screen doesn't need to match the disc plane. A captured **neutral grip offset** maps phone orientation to disc orientation.
- **Aim:** a camera preview with a crosshair maps the direction of a stationary physical landmark, such as a door or tree, onto the chosen virtual aim direction.
  - No AR tracking and no stored image.
  - The landmark's distance is irrelevant.
  - Calibration only defines a coordinate frame. It must never snap the flight toward the landmark or the basket.
- Track relative orientation from the aiming pose to the throwing grip. Recalibrate when the sensor session restarts or the reference is lost. The game rotation vector drifts, so don't assume a calibration can be reused indefinitely.

### Motion processing (backend, pure functions)

Build this against exported recordings before wiring it into gameplay.

- Android captures timestamped accelerometer, gyroscope and fused-orientation samples, plus the release time and calibration metadata. Record each sensor's source, units, sampling interval and coordinate conventions, and handle missing sensors explicitly.
- Normalize key and sensor timestamps to one documented time base. `KeyEvent.getEventTime()` uses `SystemClock.uptimeMillis()`, while `SensorEvent.timestamp` is in nanoseconds on the `elapsedRealtimeNanos()` base. The two drift apart after deep sleep.
- Pipeline:
  1. Validate and align the streams, then filter noise.
  2. Rotate the samples into the calibrated game frame.
  3. Remove gravity **exactly once**.
  4. Estimate release velocity over the short gesture, with an explicit initial-velocity assumption and bias/drift mitigation.
  5. Take direction and launch angle from the motion, and bank from orientation plus the grip offset.
  6. Bound the inputs, and reject invalid or too-small gestures.
- Start with a fixed release height. Nose angle comes later.
- Don't double-integrate position. Spin comes from the technique and disc profile, because a phone held in the hand can't measure a released disc's spin.
- Samples before or after the release may help filtering, but they never move the release event.
- **Stage 2 feasibility gate:** a labeled dataset from one phone and one technique must show:
  - repeatable power and direction, with the spread reported,
  - gentle and strong throws that can be told apart,
  - correct responses to deliberate left/right gestures and to opposite bank angles.
- Fix the tolerances after a pilot run and before evaluating a separate capture set.
- **Fallback if velocity is unstable:** a documented, bounded mapping from motion features to power, keeping motion-derived direction.

### Flight physics

- One engine, with versioned models and configurable disc profiles.
- Test known launch inputs first, so sensing errors and physics errors stay separate.
- Increments, in order:
  1. **Ballistic.** Check it against the analytical solution. It's enough for the first playable and stays as the regression baseline.
  2. **Bank-responsive.** Simplified lift and drag, plus orientation-dependent curvature.
  3. **Disc behavior.** Driver, midrange and putter profiles, simplified spin and stability, turn and fade, and wind.
  4. **Obstacles** with simple deflection.
- Choose coefficients, the integrator and the timestep within the increment that needs them.
- Never build advanced rotational physics to compensate for weak sensor input.

### Course, coordinates and rules

- The first course is bundled locally: ID, tee, target cylinder, par, flat ground and rectangular bounds. Courses are single-hole only.
- Work in meters in a local Cartesian frame. Define axis handedness, the forward and up axes and the quaternion conventions before writing transforms; Y-up is the candidate. GPS is separate metadata.
- The hole is complete when the trajectory intersects the target volume. Check for crossings *between* samples, so a fast disc can't tunnel through the target.
- Initially, ground contact stops the disc.
- Score is throws plus penalties, shown relative to par.
- A landing out of bounds costs one penalty throw and a rethrow from the previous lie. This is a documented simplification, not the official rules. Keep landing position and next lie as separate values.
- One pure client-side rules function updates score and lie once per accepted throw. Persist the accepted result together with its score change, and never compute scores separately per tier.

## Delivery stages (brainstorm-v2 §6)

Work one stage at a time, or a smaller slice of one. Keep the app buildable and record what was actually verified. Stages 2 and 3 can proceed independently using recordings and synthetic inputs, but real-gesture gameplay needs both.

| Stage | Deliverable | Acceptance |
|---|---|---|
| **0 — Skeleton** (done; emulator only) | One screen, a ViewModel and observable state on the existing project; documented build and run commands | Build succeeds and the screen launches. Report separately if the launch couldn't be tested on a device |
| **1 — Capture** (in progress) | Lifecycle-aware sensor recording, volume-key release and cancel, diagnostics, local export | A real gesture is exported and inspected; repeated keys, interruptions and release timing are checked on the phone |
| 2 — Controller feasibility | Aim reference, then grip offset, then recordings analyzed by pure backend functions | Passes the feasibility gate above |
| 3 — Simulation integration | PC API, ballistic engine, synthetic and recorded inputs, timed top-down playback, IDs, timing logs, replay script | Known inputs match the expected physics, and the phone shows the returned trajectory |
| 4 — First playable | Real gestures sent to the PC; one course, target/ground/OB rules, score, next lie, manual playback | A hole is finished on the device, and cancel, error or retry can't corrupt the active throw |
| 5a–5d | Cloud computation with forced tiers, then durable protected saves, then automatic routing with fallback, then the academic baseline | Identical replays run on both routes, saves survive restarts without duplication, and measured comparisons are documented |
| 6 — Refinements | Bank-responsive flight first, then one backlog feature at a time | Each feature gets its own check first |

The completion levels are:

1. **First playable:** Stage 4.
2. **Academic baseline:** Stage 5d. This is the complete stopping point.
3. Virtual refinements.
4. Later extensions such as AR, ML and coaching.

A demo presented as convincing disc golf should also include tested bank-responsive flight.

## Evaluation (measure from the first backend call)

- Replay identical recordings through both edge and cloud. Record the hardware, network, versions, trial order, cold or warm state and retries.
- Measure these separately:
  - the phone's request-to-result round trip,
  - release-to-result time,
  - backend time for motion analysis, physics and the total, using local monotonic clocks.
- Never subtract unsynchronized device and server timestamps.
- Also record payload sizes, the tier used, fallback attempts and the success rate. Report median and tail latency with sample counts, and keep persistence timing separate.
- Offloading may bring little benefit for this workload. Report that honestly, and don't inflate the physics to make the computation look expensive.
- Tests should protect deterministic behavior:
  - transforms and calibration,
  - release-time handling,
  - analytical ballistics,
  - target crossings,
  - scoring and OB,
  - duplicate and stale results,
  - persistence retries.
- Label mock fixtures as mocks. The final demo uses real capture and offloaded computation.

## Scope guardrails

- **Baseline:**
  - MEMS sensors, 2D graphics, camera calibration, async state, REST, signal and numerical processing, local files, MVVM and unit tests
  - a cloud API and database, edge–cloud communication, authentication, container deployment, logging and durable pending saves
- **Later or optional:**
  - putting practice, both fixed and progressive distance, using only the putter
  - backhand, forehand and putting selection, plus a persistent handedness setting
  - a toggleable trail, replay, playback that starts at a stable viewing orientation, and a first-person/3D view
  - wind: none first, then custom, then GPS-based weather, frozen per session
  - obstacles
  - haptics, notifications, object storage, Terraform and autoscaling
  - AR with manually anchored targets
  - ML Kit, OpenCV and an on-device technique classifier
  - an asynchronous cloud LLM "throw coach", parked as a nice-to-have that never affects physics or scoring
- **Out of scope:** multiplayer, public leaderboards, account recovery, a course editor or generated courses, multi-hole rounds, automatic environment reconstruction, offline on-device simulation and microservices.
- The professor's provisional 28-entry checklist is a priority map, not a requirement list. The user wants as many points as possible without diverging from the concept or complicating the implementation. Never add a feature just to tick a box. Whether overlapping entries earn separate credit needs the professor's confirmation. The entries, by group:
  - **Mobile:** GPS, MEMS sensors, 2D graphics, CameraX, notifications, Coroutines/Flow, REST, other.
  - **Processing:** OpenCV or image processing, ML Kit, on-device LLM/NN, other libraries, other.
  - **Storage:** local DB or files, external cloud storage, other.
  - **Architecture and quality:** MVVM, unit tests, other.
  - **Cloud:** Firebase, authentication, VM/container, serverless, autoscaling, load balancer, database, Terraform, other.

## Decision history

These come from the design conversation. Don't re-propose a rejected option without new evidence.

- The idea started as an AR throwing simulator, with paper planes and stones considered. It became disc golf because a turn-based sport fits the throw-then-observe loop.
- Throwing and viewing are separate phases, because the phone can't be the controller and the viewfinder at the same time.
- Aiming uses a physical landmark, because the player can't look at a virtual target while throwing. This applies in virtual mode too.
- **Release is a physical button (volume-down), as the user proposed.** This replaced backend automatic release detection, which was rejected as ambiguous and harder to build.
- A flat, disc-like grip was rejected because a phone can't be held like a disc. Instead the player uses a comfortable grip and the app calibrates the neutral offset.
- Spin is approximated: from the profile first, with a wrist-gyroscope contribution possibly added later. Hyzer/anhyzer, direction and power matter more.
- The user wanted physics that grows incrementally toward a simplified advanced model; the stage list above sequences it. The user also leaned toward playback triggered by the viewing orientation, but brainstorm-v2 starts with manual playback and adds the orientation trigger as a refinement.
- Rejected for throw mechanics: GPS (it's only for weather and hole metadata), magnetometer or compass headings, and continuous ARCore tracking during the throw.
- Device identity uses an app-scoped identifier with real anonymous auth, not a hardware fingerprint. Raw sensor recording is a developer setting, off by default.
- The MVP is virtual-only, and AR is a later extension; the intro slides reportedly don't require AR. Courses are single-hole only.
- The project was first called Discscape, then renamed to **DiscLab**.
- MVVM was chosen over MVC, MVP, MVI and full Clean Architecture, and the backend is a layered modular monolith. Use the Strategy pattern only where implementations really differ; identical edge and cloud APIs just need a configurable endpoint.

## Working rules for agents

1. Inspect the actual repository first and preserve the existing configuration.
2. Implement only the requested increment. Keep UI, device capture, network orchestration and pure computation separable, without speculative frameworks.
3. Record new choices, units, conventions, limitations and run commands next to the code. Update this file when they change.
4. Report what changed, what was actually verified (build, tests, device) and the smallest next step. Never claim a device test, deployment or measurement that wasn't performed.
5. The repo is **public**. Keep secrets, credentials and private sensor captures out of git. Camera calibration is transient, and raw recording or export only happens when a developer explicitly triggers it.
6. Get approval before provisioning billable cloud infrastructure.
7. Mark versions with git tags once an iteration delivers useful, working functionality, such as a finished stage or a completion level. Stage 0 is deliberately untagged.
   - The naming scheme isn't agreed yet. Before the first tag, propose one (for example an annotated `v0.1.0`) and agree on it with the user, including whether `versionName`/`versionCode` in `app/build.gradle.kts` follow the tags.
   - Confirm the tag name and target commit before pushing a tag.
8. Work on one branch per stage, created from `main` and named `feature/stage<N>-<milestone>`: Stage 1 is `feature/stage1-capture`, and Stage 0 used `setup/stage0-wiring`. Implement the stage's issues on that branch one at a time.
   - Always work towards the remote repo. After each commit, push the stage branch to `origin` and check that it arrived.
   - Then update the issue. Tick only the "Done when" items that were actually verified, and comment with the commit, the changes, any deviations from the plan, what was verified (and where), and what's still open.
