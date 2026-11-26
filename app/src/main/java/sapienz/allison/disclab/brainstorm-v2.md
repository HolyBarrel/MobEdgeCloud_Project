# DiscLab — Incremental project brief

**Subtitle:** A Motion-Based Disc Golf Training Simulator  
**Course:** Mobile, Edge and Cloud Computing — Sapienza University of Rome  
**Version:** 2, 26 November 2026  
**Status:** Implementation plan; no functionality or infrastructure is claimed as implemented.  
**Next implementation task:** Stage 0 only, after locating or creating the DiscLab Android project.

This revision replaces the working plan while preserving `brainstorm.md` as the original record. It separates the first playable application, the academic deliverable and later refinements. Stages describe outcomes, not deadlines; implement and verify one small increment at a time.

## 1. Concept and scope

DiscLab uses an Android phone as a motion controller for a virtual disc. The player performs a controlled gesture and releases a physical button to indicate virtual release, while keeping hold of the phone. An edge PC or cloud backend analyzes the recorded motion, simulates flight and returns a trajectory for the app to display.

The priorities are consistent power, motion-derived direction and meaningful hyzer/anhyzer control. The phone is a controller, so estimated game inputs need not claim the accuracy of real-disc measurements. Start with a playable virtual hole and measured distributed processing.

| Completion level | Required outcome |
|---|---|
| First playable | One bundled hole, one disc profile, one selected technique/handedness configuration, camera-based physical aim calibration, real gestures, PC processing, top-down playback, target/ground detection and scoring. Ballistic flight is sufficient for this integration milestone. |
| Academic baseline | First playable plus the same computation in cloud, protected persistence/retrieval, actual edge–cloud communication, forced execution modes and automatic fallback, durable pending saves, meaningful tests and reproducible edge/cloud measurements. |
| Virtual refinements | Bank-responsive flight first; then additional discs/techniques, putting, obstacles, weather and presentation improvements, each independently deliverable. |
| Later extensions | AR disc golf/putting and optional image, ML, coaching or infrastructure experiments. |

The academic baseline is a complete stopping point for implementation scope, subject to confirming the course requirements. A demo presented as convincing disc-golf gameplay should additionally include a tested bank-responsive flight model. AR is not a dependency of either milestone.

## 2. Gameplay and controller

### Initial game loop

1. Inspect the virtual target and choose an aim direction.
2. Establish a physical aim reference and neutral grip.
3. Hold volume-down, make a controlled gesture, then release the button.
4. Wait for the backend result and start playback manually when ready to look at the screen.
5. Show the landing point, throw distance, remaining distance and score. Move the virtual player to the next lie and repeat until the target is hit.

The player stays in one physical location. Use simple top-down graphics and timestamped trajectory interpolation; the client renders returned physics results. Allow only one active throw. Add gameplay phases as needed: calibration, ready, recording, processing, awaiting playback, playback, result and completed, with explicit cancellation/retry handling.

### Input and calibration

- Use a comfortable, secure grip; the phone screen need not coincide with the virtual disc plane. Never release the phone.
- Volume-down press begins capture; button-up defines release. Ignore repeated key-down events and cancel interrupted gestures. Consume the button only during the relevant gameplay interaction. Test ergonomics on the actual device; change the button or provide an alternative only if needed.
- Camera calibration maps the direction toward a stationary physical landmark to the chosen virtual aim direction. A preview/crosshair plus an orientation reference is sufficient; no AR tracking or stored image is needed.
- Capture a separate neutral grip offset so phone orientation maps to disc orientation. Track relative orientation while moving from camera aiming to the throwing grip.
- Calibration establishes a coordinate frame; it must not snap flight toward the landmark or basket. Horizontal direction and launch angle come from estimated motion. Landmark distance has no effect on virtual distance.
- Initially recalibrate when the sensor session restarts or its reference is lost. Test drift and decide how long reuse is acceptable. The same physical landmark may be remapped to a new virtual aim direction between throws.

First test aim reference, grip offset and transitions separately. A temporary sensor-only reference may unblock diagnostics; camera calibration is required for the first playable milestone. Android's game rotation vector can drift, so indefinite reuse is not assumed ([Android sensor reference](https://developer.android.com/reference/kotlin/android/hardware/SensorEvent)).

## 3. Motion processing and the feasibility gate

Capture timestamped accelerometer, gyroscope and fused orientation samples, plus release time and calibration metadata. Prefer an available Android fused orientation source initially; custom sensor fusion is deferred. Record the actual sensor sources, units, sampling intervals and coordinate conventions. Handle missing sensors explicitly.

Normalize hardware-key and sensor timestamps to a documented common time basis. Use a stationary preparation period where practical. A short pre-roll or post-release window may help filtering, but must not change the authoritative release event or treat follow-through as pre-release motion.

Keep acquisition on Android and motion analysis in pure backend functions that can first run over exported files:

1. Validate samples and release timing; align streams and filter noise.
2. Transform acceleration into the calibrated game frame and remove gravity exactly once.
3. Estimate release velocity over the short gesture with bias/drift mitigation and an explicit initial-velocity assumption.
4. Derive direction/launch angle from motion and bank from phone orientation plus the grip offset. Nose angle is distinct from launch angle and can follow later.
5. Use the current lie and a fixed release height initially. Bound game inputs and reject invalid or too-small gestures.

Do not double-integrate position unless later evidence justifies it. Spin is initially simulated from a technique/disc profile: a phone retained in the hand does not measure the released disc's actual spin. Define technique/handedness signs consistently when those options are added.

### Required early evidence

Local recording/export is part of the capture increment. Build a small labeled dataset on one phone with one technique, including repeated neutral gestures and deliberate changes in strength, left/right direction and bank. Keep it reusable for tuning, regression checks and tier comparisons.

Before integrating real gestures into gameplay, demonstrate:

- Similar gestures have repeatable power/direction estimates; report their spread.
- Gentle and stronger groups are distinguishable.
- Deliberate left/right gestures and opposing bank angles produce the expected response.
- Release, cancellation and calibration remain usable with the chosen grip.

Set practical tolerances after an initial pilot and record them before evaluating a separate set of captures. Measure response consistency, not physical accuracy without ground truth.

**Fallback:** if velocity integration is unstable, use a documented bounded mapping from motion features to game power while retaining motion-derived directional control. If direction itself is unreliable, continue the controller experiment before claiming a playable gesture interface. Synthetic-input backend/UI work may proceed independently.

## 4. Flight, course and rules

Use one simulation engine with versioned models and configurable disc profiles. Keep sensing errors separate from physics errors by testing known launch inputs first.

| Physics increment | Scope and check |
|---|---|
| Ballistic baseline | Initial velocity, gravity, flat ground and target intersection. Compare flight and landing with an analytical solution. Supports the first playable integration. |
| Bank-responsive flight | Add simplified lift/drag and orientation-dependent curvature. Controlled opposite bank inputs should produce understandable, repeatable changes. This is the first gameplay refinement. |
| Disc behavior | Add driver/midrange/putter profiles, simplified spin/stability, turn/fade and wind. Test each behavior independently. |
| Further interactions | Add geometric obstacles and simple deflection. Advanced torque, skipping, sliding and rolling remain optional. |

Choose coefficients, numerical integration and timestep during the relevant increment. Keep the ballistic model as a regression baseline. Do not build advanced rotational physics to compensate for unreliable sensor input.

The first course is bundled locally and contains an ID, tee, target cylinder, par, flat ground and rectangular bounds. Use meters in a local Cartesian frame; define axis handedness, forward/up axes and quaternion conventions before transformations. Y-up is a candidate. GPS is separate metadata.

A target-volume intersection completes the hole. Detect crossings between trajectory samples so a fast disc cannot jump through a target. Ground contact stops the disc initially. Score is throws plus penalties, displayed relative to par. For initial landing-based out-of-bounds handling, use one penalty and a rethrow from the previous lie; document this simplified rule rather than claiming official rules. Keep landing position and next lie distinct.

Backend collision results feed one pure client-side rule implementation that updates score/lie once per accepted throw. Persist that accepted result and its score change together; avoid separate competing score calculations across tiers.

## 5. Architecture and data contracts

### Android

Use native Android with lightweight MVVM: a screen renders observable ViewModel state; device/data services handle sensors, files and networking; pure functions handle game rules and transformations where appropriate. Keep high-frequency raw samples outside normal screen state.

Start with one app module and ordinary packages. Add repositories, interfaces and dependencies only when required by a working feature. Preserve the existing Android Studio configuration; no package tree or multi-module framework is required in advance.

### Edge and cloud

Run the same motion and physics code on the PC and in cloud, using deployment configuration for service integrations:

- **Two-tier:** Android → cloud computation and persistence.
- **Three-tier:** Android → PC computation; PC → cloud persistence/retrieval services.

The PC performs meaningful computation, rather than forwarding every simulation to cloud. A small backend with HTTP handlers, pure numerical modules and storage integrations is sufficient. Fully disconnected on-device simulation is outside the planned baseline.

Use one versioned request/result schema. Define only the fields needed for the current increment, including:

| Record | Core contents |
|---|---|
| Throw request | Stable throw ID, session/course identity, sensor recording and release time, aim/grip calibration, current lie, disc/technique configuration and processing/model versions. Freeze wind and any random seed when introduced. |
| Result | Same throw ID, estimated launch parameters and validity diagnostics, timed trajectory, target/ground events, landing position, processing timings and actual execution tier. |
| Saved record | Accepted result or summary, score change, session/player ownership and configuration/version metadata sufficient to identify the experiment. |

Document units, clock basis and rotation conventions. Keep Android-specific objects out of the wire format. JSON over HTTP is the starting candidate. Include IDs and timings with the first API call, before implementing retries.

### Routing, acceptance and persistence

Start with a manually configured PC address. Add forced cloud execution before automatic routing. The final policy offers **force edge**, **force cloud** and **automatic edge preference with cloud fallback**. Forced modes never silently switch tier during experiments.

Use bounded timeouts and avoid probing an unavailable PC on every throw. Retry appropriate connection/timeout failures; invalid sensor input is not a reason to repeat computation elsewhere.

- Reuse the same throw ID and frozen input/configuration across retries and fallback.
- Accept one result for the current throw; ignore stale responses and never apply a score twice.
- Separate simulation completion from save completion. Playback can continue while a save is pending.
- Store the accepted record locally before treating it as safely queued. Pending saves survive app restart and have visible pending/saved/error status. A small file-backed list is sufficient initially.
- Retry saving the accepted record without resimulating or creating a new throw. Cloud writes must deduplicate by owner/session/throw identity and reject conflicting records rather than overwrite silently.
- In three-tier mode, demonstrate the edge forwarding accepted records to cloud and retrieving cloud data. If acknowledgement is lost, the mobile pending record can be retried safely.

Basic authentication and server-side ownership checks arrive with cloud storage. Anonymous authenticated identity is suitable; a random player ID alone is not authentication. Profile screens, recovery and cross-device accounts can wait. Without internet, a reachable edge may still simulate while saves remain pending.

### Technology choices

Native Android and MVVM are agreed. Kotlin/Compose, coroutines/StateFlow, CameraX, a conventional HTTP client, Python/FastAPI and Docker are starting candidates. Cloud Run plus Firebase Authentication/Firestore is one possible cloud stack, not a requirement. Choose compatible versions from official documentation when implementing each part; avoid unnecessary migrations or dependencies for deferred features.

## 6. Incremental delivery plan

Each row is a reviewable outcome. Split it further when needed, keep the app buildable and record actual verification. Stages 2 and 3 can be developed independently using recordings and synthetic inputs, but real-gesture gameplay requires both to work.

| Stage | Deliverable | Acceptance check | Deferred work |
|---|---|---|---|
| 0 — Skeleton | Inspect/create the Android project; one screen, ViewModel and observable state. Document build/run commands. | Build succeeds and screen launches. Report separately if device launch could not be tested. | Sensors, full game state machine, backend and cloud. |
| 1 — Capture | Lifecycle-aware sensor recording, physical release/cancel handling, basic diagnostics and local export. | Export and inspect a real gesture; verify repeated keys, interruption and release timing on the phone. | Flight, camera UX and power estimation. |
| 2 — Controller feasibility | First establish the aim reference, then grip offset, then analyze recordings with pure backend functions. | Pass the evidence gate in section 3, including calibration reuse limits and chosen fallback if needed. | Networking and elaborate physics. |
| 3 — Simulation integration | PC API, ballistic engine, synthetic/recorded test inputs, timed top-down playback, IDs, timing/payload logs and a basic replay script. | Known inputs match expected physics; the phone displays the returned trajectory. | Complete scoring and cloud deployment. |
| 4 — First playable | Connect real gestures to the PC API; add one course, target/ground/OB rules, score, next lie and manual playback. | Finish one hole on the physical device; cancel/error/retry cannot corrupt the active throw. | Additional discs, aerodynamics, 3D and putting challenges. |
| 5a — Cloud computation | Deploy the same engine; add forced edge/cloud selection. | Replay identical inputs on both routes and record actual computation/timing results. | Automatic fallback and feature expansion. |
| 5b — Durable results | Protected cloud save/read, edge–cloud integration, local pending records and deduplicated writes. | Retrieve a saved score; interrupt a save and restart the app, then retry without loss or duplication. | Rich profiles, raw dataset uploads and advanced sync. |
| 5c — Routing resilience | Automatic edge preference, bounded fallback and stale-response handling. | Disconnect the edge and test delayed responses; one accepted throw produces one score update and saved record. | Discovery and workload-aware scheduling. |
| 5d — Academic baseline | Complete repeatable experiments, targeted tests and a documented demo. | Show both topologies, cloud retrieval and measured comparisons with limitations. | Extra checklist features. |
| 6 — Refinements | Begin with bank-responsive flight, then select one feature at a time from section 8. | Define and demonstrate a feature-specific check before expanding scope. | Everything not selected for that increment. |

The immediate implementation request remains Stage 0. Do not treat this entire table as one coding task.

## 7. Evaluation and academic scope

The original brainstorm summarizes `01_INTRODUCTION_27.pdf`, slides 2–4, as requiring mobile sensing, edge processing connected to cloud, cloud API/storage, communication between tiers and experimental comparison. This revision carries that interpretation forward; it does not independently verify the slides. The paper presentation is already complete.

The stated Type A/Type B score ranges and the provisional 28-entry checklist are separate grading concepts. Confirm requirements and overlapping credit with the professor rather than promising a score. Virtual mode supports the substantive requirements listed above without AR.

### Measurement from the first backend call

Replay identical recorded throws/configurations through edge and cloud. Record device/server hardware, network conditions, versions, trial ordering, cold/warm state and retries. Use repeated trials and report failures alongside successful timings.

- Measure phone request-to-result round-trip time and release-to-result time separately; identify capture delay and setup overhead.
- Measure motion-analysis, physics and total backend durations with local monotonic clocks. Do not subtract unsynchronized device/server timestamps.
- Record request/response sizes, execution tier, fallback attempts and success rate; summarize median and tail latency with sample counts.
- Report persistence timing separately from simulation latency.
- Compare controller repeatability separately from tier performance. Do not claim real-world accuracy without ground truth.

Offloading may offer little latency benefit for this workload. Report that honestly; do not inflate physics complexity just to make computation seem expensive. Energy, load, autoscaling and advanced-model comparisons are optional experiments.

Tests should protect meaningful deterministic behavior: transforms/calibration, release-time handling, analytical ballistics, target crossings, scoring/OB, duplicate/stale results and persistence retries. Device checks cover grip, buttons, sensors, camera and lifecycle. Use labeled fixtures for development, but real capture and offloaded computation for the final demonstration.

### Checklist priorities

The original 28 entries remain in `brainstorm.md`, section 12. Use this grouped priority map rather than turning every entry into a requirement:

| Priority | Relevant coverage |
|---|---|
| Core implementation | MEMS, 2D graphics, camera calibration, asynchronous state, REST, signal/numerical processing, local files, MVVM, tests, cloud API/database and edge–cloud communication. |
| Supports the baseline | Authentication, shared container deployment if suitable, essential logging and durable pending saves. |
| Add when useful | GPS/weather, object storage, haptics, notifications, managed scaling and Terraform. |
| Optional research | Image processing/ML Kit, a custom on-device model, AR, explicit load balancing and advanced scheduling. |

Confirm whether processing categories, architecture patterns, managed hosting/scaling and storage categories receive separate credit, and whether autoscaling requires measured demonstration. Cloud LLM feedback is not an on-device model.

## 8. Refinement backlog

These preserve the intended product direction without blocking the academic baseline:

- **Flight and controls:** bank-responsive lift/drag first, then driver/midrange/putter profiles, backhand/forehand/putting selection, persistent handedness and simplified spin/turn/fade.
- **Practice and courses:** fixed-distance and progressive putting with the putter; attempts, hits and miss distance. Add a few bundled single-hole variations and simple obstacles. No course editor or procedural generator initially.
- **Presentation:** toggleable trail, optional replay, stable-viewing-orientation playback with manual fallback, then first-person/3D as a separate increment.
- **Weather:** no wind first, then custom wind, then GPS-based regional weather. Freeze wind per session and define its mapping into the course frame. Location/weather failure must allow play with no/custom wind.
- **AR:** manually anchor a target and simple bounds in a local metric frame, reuse the simulation contract, recover tracking before playback. Regular AR play requires moving to the next lie; AR putting stays stationary. GPS/photo metadata can help rediscover holes but cannot restore precise anchors; begin with manual re-pinning.
- **Optional additions:** reference-photo processing/tagging, raw dataset cloud storage, haptics, notifications, Terraform and bounded autoscaling experiments. Add each only for a concrete use or confirmed evaluation value.
- **ML/coaching:** an optional technique classifier needs representative labeled captures and held-out evaluation. Optional asynchronous cloud coaching uses structured estimates, never changes physics/scoring and must not claim validated real-world technique analysis. Neither controls release or blocks gameplay.

Advanced AR anchors/reconstruction, detailed disc-ground dynamics, multiplayer, public leaderboards and account recovery remain outside the baseline.

## 9. Working rules and next decisions

Before each increment, resolve only decisions it needs:

- **Capture/controller:** actual phone/sensors, comfortable grip, timestamp conversion, coordinate conventions, neutral offset, measured tolerances and fallback mapping.
- **Simulation:** backend language/framework, integration method, profile coefficients and concrete request/result format.
- **Cloud:** provider, identity/storage integration, accepted-result persistence flow, deployment limits and experiment setup.
- **Refinements:** putting progression, calibration reuse UX, rendering stack and weather/AR details when selected.

For implementation agents:

1. Inspect the actual DiscLab repository and preserve existing configuration. Do not assume the currently open workspace is the Android project.
2. Implement only the requested increment. Keep UI, device capture, network orchestration and pure computations separable without speculative frameworks or empty packages.
3. Record new choices, units, limitations and run commands alongside the project. Keep mock inputs labeled and use meaningful tests for implemented behavior.
4. Report what changed, what was actually verified and the next small step. Never claim an unperformed device test, deployment or measurement.
5. Keep secrets and private captures out of version control. Camera calibration is transient by default; raw recording/export is an explicit developer action. Obtain approval before provisioning billable infrastructure.

**First implementation prompt:**

> Read `brainstorm-v2.md` and inspect the DiscLab Android project. Implement Stage 0 only: preserve the generated configuration, establish one screen with ViewModel-controlled observable state, verify the build and launch where available, and document the commands and results. Do not add the full game architecture or later-stage dependencies. Suggest the smallest next task for real-device recording and release handling.
