# Foundry performance audit and repeatable capture procedure

## Current finding and limits

The initial audit had no Minecraft JVM or representative running world available. A subsequent supplied 90-second Minecraft JFR recording now establishes CPU/allocation candidates and supports the targeted changes documented below. GPU timings and equivalent-scene before/after FPS results remain unavailable. Native lighting, fullbright materials, hatch transparency, molten rendering, physics, audio and multiplayer behavior are preserved.

The reported 100 FPS before bloom, 50 with bloom and 80 after removal correspond to approximately 10, 20 and 12.5 ms/frame respectively. The remaining 2.5 ms difference is an observation to reproduce, not evidence of a particular subsystem's cost. Different scene state, camera, shader/mod set, resolution, settings, temperature, chunk building, GC or warmup could affect the comparison. FPS averages alone do not attribute the difference to this mod.

Source/resource searches found no custom bloom classes, configuration registration, shader registration, capture wrappers, framebuffer/depth transfers, timer queries, blur/composite passes or Iris compatibility path. The built JAR was also checked for bloom class/resource entries. Old client TOML settings have no registered consumer. Existing comments calling hotspots "heat blooms" describe the preserved surface effects, not framebuffer bloom. Shader reload listeners for metal palettes/textures and the integrated rim/casting shaders remain intentionally: these are functioning non-bloom systems.

## Capture conditions

Use copies of a disposable benchmark world, not the only copy of an existing world. Build all test chunks and warm the JVM/GPU for at least two minutes before recording. Do not construct/break the test structure during a steady-state recording. Run a separate edit/reconnection trial for mutation spikes. Never change render/simulation distance, window resolution, graphics mode, FPS limit, VSync, shaders, resource packs, sounds or particles silently. Record their exact values and the entire mod/version set. Keep time/weather, entities, camera position/yaw/pitch and background applications consistent. Do not pause an integrated server during capture.

Use the same terrain/camera with and without Foundry blocks (world copies), restoring original inventory/active state before each run. Compare each scenario to its no-Foundry copy, not to a different camera location. Keep other nearby machines/entities identical. Use Fast, Fancy and Fabulous as separate controlled test series, never mix their results. Record attached and detached setups with identical metals/quantities. For a true historical comparison, use a separately built pre-bloom checkpoint and identical world/options/mod set; do not reset or overwrite this working tree.

For each pair, alternate baseline/scenario order across at least three repetitions. Collect 90 seconds of steady-state data after warmup; also collect runs with profiling off to quantify profiler overhead. Record median and p95/p99 **frame times**, not just average FPS. A frame-time capture tool already available on the test machine can supply those metrics; preserve its version/settings. Minecraft's frame-time graph is useful for correlation but a visual graph or FPS counter is not a GPU stage timer. Record server MSPT/TPS independently where the existing profiler supports it. A dedicated server needs its own JFR recording; client JFR contains the integrated server thread only in singleplayer.

## Scenario matrix

Use valid structures built with existing placement rules. Suggested sizes are repeatable starting points, not changes to capacity or simulation. One Tank holds 108 units (the current `FoundryTankNetwork.TANK_CAPACITY`); the maximum normal component is 4 x 4 x 4. Write exact positions, molten units per metal, hatch states, active recipes and block counts in SceneNotes. Remove unrelated ground items to keep hatch intake/entity workload consistent.

| ID | Setup | What to compare/check |
| --- | --- | --- |
| empty_world | Same loaded terrain, no Foundry blocks | JVM/render/chunk/audio-discovery baseline |
| empty_structure | Controller + valid 2 x 2 x 2 Tanks, no molten metal | Empty BER early exits, physical filtering, static glass/frame cost |
| partial | Same eight Tanks, 256 iron units | Mesh generation, top/side shading, native lights after settling |
| large | Valid 4 x 4 x 4, 2048 iron units | Cell/layer reconstruction, overdraw, draw calls and allocations |
| layers | Same large structure and total amount split among four metals, exact amounts recorded | Layer/interface/wave costs at equal total fill |
| pouring | Six independently active Faucets into Tables/Basins, exact orientations/targets recorded | Stream/particle rendering, transfer/sync, audio loops |
| cooling | Batch of Basins/Tables at matching filling/full/cooling/finished states | Separate hot and cooled costs; same cooling progress/time windows |
| detached | Same partial and layered structures after Controller removal | Detached cache/visibility scans and identical molten appearance |
| out_of_view | Same loaded structures, camera rotated away; paired no-Foundry run uses same rotation | BER/frustum early exits versus continuing simulation/audio |
| unloaded | Travel beyond both render and simulation ranges, no force-loaded chunks/spawn tickets | Confirm chunks really unload and sounds/replicas expire; pair with same distant terrain |

Finite pours/cooling must not quietly become idle halfway through a long capture. Use identical timed batches from restored world copies; record transition timestamps and compare matching active intervals in a JFR viewer. The simple summary script aggregates the whole recording, so use it only for equivalent complete sequences or steady states. Do not increase transfer speed, capacity or cooling time to sustain a benchmark. For unloaded tests, no client draw does not establish server unload; check both sides and tickets.

## JDK 21 instrumentation (opt-in, outside the mod)

`tools/performance/Record-FoundryProfile.ps1` uses standard JDK `jcmd JFR.start settings=profile`, with a bounded duration. Nothing is installed in the mod, no dependency or game option changes, no frame hooks/GPU query work when not profiling. The script records selected graphics options, a local built-JAR hash and your notes under ignored `build/performance/`. The built-JAR hash is provenance only: verify that the game actually loaded that build. Supply the real instance's options path when it differs from `run/options.txt`. Record actual framebuffer resolution manually; GUI scale is not resolution.

First list JVMs using the same JDK/user as Minecraft:

```powershell
jcmd -l
```

Choose the Minecraft JVM, not Gradle, the launcher or a server unless deliberately profiling that server. `VM.version` verifies attachment, not that the process is Minecraft. The recorder avoids saving full JVM command lines because they can contain account tokens. Notes and JFR files may still contain paths, server/world details or identifiers: review before sharing.

Example (replace PID, notes and optional JDK path):

```powershell
./tools/performance/Record-FoundryProfile.ps1 -ProcessId 12345 -Scenario partial -Seconds 90 -SceneNotes 'Trial 1; eight Tanks, 256 iron; camera XYZ/yaw/pitch; 1920x1080; Fancy; exact mods recorded separately'
```

It returns immediately; remain in the scene until the recording duration ends. It never launches Minecraft, changes the world, sleeps in a background helper or changes VSync/caps. If attachment is unavailable, run with matching JDK/user permissions; do not bypass access controls. JFR itself adds overhead. Avoid simultaneous unrelated recordings when comparing runs.

After the timed recording completes:

```powershell
jfr summary 'build/performance/<trial>/recording.jfr'
python tools/performance/summarize_foundry_jfr.py 'build/performance/<trial>/recording.jfr'
```

Pass `--jfr 'C:/path/to/jdk/bin/jfr.exe'` if needed. The analyzer exports execution/native/allocation sample events with stack depth 128 and reports inclusive mod-method sample shares separately per thread, leaf hits and estimated allocation weights. Inclusive entries overlap; never add them. Samples are statistical, not per-method wall milliseconds; native driver waits are not GPU timestamps. Empty samples do not prove zero cost. Large JSON exports are parsed in memory after disk export; shorten recordings if memory is constrained. It refuses to overwrite an existing summary. Use `jfr summary` or a JFR viewer for GC pause distributions, CPU load, thread parks/locks and file/network events rather than misinterpreting allocation estimates as GC pause time.

Inspect Render thread versus Server thread separately. Follow expensive JDK/collection frames to their mod caller; the mod may allocate through library code. For client-only multiplayer captures, no Server thread results means the remote server was not measured. Stack sampling may miss frequent short calls or JIT-inlined frames; absence from a short recording is not an optimization license.

For GPU attribution, use an already available Minecraft-compatible GPU profiler/frame capture tool with driver support. Measure actual molten draw durations, translucent overdraw, pipeline stalls and total GPU frame time; capture before/after under identical settings. A single frame capture can perturb timing: repeat and cross-check normal frame-time traces. Do not add another mod-side framebuffer/timer pipeline just to profile this regression. This audit has no GPU measurements or verified GPU tooling installed. Vanilla debug profiling/JFR is useful for CPU work but does not provide reliable per-material GPU timings.

## Source-review candidates (priority to measure, not ranked measured impact)

| Priority | Path / confirmed work | Evidence needed before optimizing |
| --- | --- | --- |
| 1 | `FoundryControllerTankLiquidRenderer.render`: redistributes displayed amounts and rebuilds per-cell layer maps/counts each rendered frame; interfaces/side waves remain animated | Render-thread inclusive samples and allocation weights versus cells/metals, GPU timing for emitted materials; separate stable structure metadata from height/animation before considering caching |
| 2 | Controller `getRenderBoundingBox` and `render` each call physical-client filtering; detached renderer allocates visible sets/AABBs and reads loaded Tank states before its vessel frustum test | Out-of-view versus visible JFR profiles, distinguish necessary stale-block protection from redundant scans; retain all chunk/removal correctness if caching is later justified |
| 3 | Tank/Controller audio managers each inspect a 5 x 5 loaded-chunk square each client tick; Faucet discovery inspects 3 x 3; Tank tracker resolves cells and overlaps | Render-thread tick samples/allocations in empty versus many-Foundry scenes; preserve start/join/reload/removal latency, independent sources and attenuation. These are chunk lookup counts, not measured milliseconds |
| 4 | Storage insert/extract refreshes lighting occupancy and synchronizes; lighting recomputes occupancy but writes blockstates only when `MOLTEN_LIT` changes | Server samples, packet/serialization counts and lighting-engine samples during many concurrent transfers versus static contents; do not equate occupancy calculation with a light-engine update |
| 5 | Basin/Table active cooling and successful insertion persist/sync state; streams/particles and mixed-metal interfaces add work | Active versus cooled sample/packet/GPU comparison at identical counts; progress and multiplayer must remain correct |
| 6 | Detached change/watch payloads enumerate snapshots and chunk membership; chunk-load lighting reconciliation examines sections containing Tanks | Separate travel/edit spikes from steady-state cost; world size/vessel count/network serialization samples. They are event-driven, not a blanket per-frame SavedData scan |

Tank structure discovery is already dirty/event-driven with a cached network, rather than a full scan on every Controller tick. Intake hatches already return on seven of eight ticks before allocating/sorting positions. Detached client views are cached until payload invalidation; no second authoritative inventory exists. Loaded-chunk checks are present in sound/detached/light paths. Basin/Table remove the old BER 64-block cutoff intentionally and rely on normal visible-section/frustum selection; reducing draw range would change promised visuals and is not an accepted optimization. Controller and detached vessel bounds use a 320-block limit. Molten transparency, integrated rim/side glow and mixed-metal interface shaders can cost GPU time; no duplicate-mesh or overdraw bottleneck is established from source alone.

Audit the rest of the mod in the same recordings: ore/chunk generation, equipment event handlers, menus and recipe lookup can contribute during travel/interactions even when Foundry rendering is absent. Keep a no-Foundry control with the same whole modpack. Do not attribute every `net.minecraft` or driver sample to Foundry.

## Results and acceptance record

The supplied partial-scene recording provides inclusive CPU samples and statistical allocation attribution. Frame-time/GPU measurements and production before/after results remain unmeasured. Fill a row for every trial, including the no-Foundry control:

| Build/hash + scenario + trial | Exact scene/settings | Frame median / p95 / p99 ms | Render/Server samples and allocation estimates | Server MSPT | GPU frame/material ms | Capture paths |
| --- | --- | --- | --- | --- | --- | --- |
| Not collected | No running Minecraft scene available | Not measured | Not measured | Not measured | Not measured | None |

Only propose an optimization when matched recordings show a repeatable meaningful cost and a regression test can protect its invalidation/visibility/authority rules. For each subsequent change, record the same metrics before/after with profiling both on and off; reject gains within noise, regressions or changes to appearance/simulation. Validate hatch 50% opacity/depth writes disabled, lights 0/12, layered/gap-free liquid, detached handoff, multiple audio loops, casting and multiplayer separately in Minecraft. Build/datagen/test durations are not runtime performance evidence.

Validation commands (no clean):

```powershell
python -B -m unittest discover -s tools/performance -p 'test_*.py'
./gradlew.bat --no-configuration-cache -I verification/equipment.init.gradle runData
./gradlew.bat runData
./gradlew.bat build
```

The tooling tests validate sample aggregation only, not Minecraft performance. The profile-guided changes below alter four Java implementation files and regression integration; no shaders, models, textures or build configuration are changed.

## Profile-guided optimization: supplied partial scene

Evidence: `build/performance/partial-20261024-224558-6ac15ca5/recording.jfr` and its summary, 90 seconds, 1431 Render-thread execution/native samples and 823 Server-thread samples. The recording is an ignored local benchmark artifact, not a required implementation resource.

- Render: liquid renderer 555/1431 (38.78%) inclusive, layer construction 105 (7.34%), interface seams 66 (4.61%), density sorting 63 (4.40%). These overlap, are not exact method durations and must not be summed.
- Server: Faucet ticking 107/823 (13.00%), auto-pour check 94 (11.42%), lever scan 85 (10.33%), anchor collection 52 (6.32%). Again these overlap.
- Raw allocation stacks identify captured light-supplier creation at the old liquid-renderer line 235, plus BlockPos, list/map, Float and interface allocations. The first lambda allocation event has a very large 60-GB weight; do not interpret the summed weights as exact bytes allocated during this 90-second interval or as demonstrated savings. The summary's nearest-caller attribution is statistical, not a heap census.

Implemented:

1. Cache immutable heaviest/lightest density lists once. Definitions are immutable; stable catalog tie order remains unchanged. This removes repeated stream sorting from every cell/layer/smoothing lookup.
2. Keep a weak per-vessel layout cache, cleared when the world identity changes. Attached Controller and detached UUID identities remain independent. Copy and compare exact structure positions and **smoothed float amounts**, never rounded heights or only authoritative inventory totals. Stable frames reuse layer maps/lists, geometry records, side-glow metadata and junction classifications. Static horizontal bounds/counts rebuild only for structure changes. Amount/composition changes rebuild layout while smoothing continues; no animated-height quantization or delayed invalidation.
3. Remove captured per-layer light suppliers from the production rendering path. Cached junction classification selects liquid/frame routing; actual frame light is still read live where needed. Reuse the frame scratch list, avoid its two stream-predicate allocations, and pass the cached side-glow origin through interface rendering. No vertices, depth states, blend strengths or materials change.
4. Cache the bounded geometric lever search per Controller/structure revision. Probe each possible lever position once; only inspect support/faucet paths for actual powered levers. Lever power, Faucet orientation/presence and support sturdiness remain live; the existing one-result-per-Controller-per-tick behavior is retained, with no new throttling/event dependency. Candidate probes never load missing chunks. Weak world/Controller ownership prevents cross-world cache reuse.

Regression evidence (controlled native checks, not gameplay benchmarks):

- 1000 stable 64-cell frames retain the exact layer map and every cell record: 64,000 cell rebuilds are avoided after warmup, alongside corresponding distribution/list/geometry/route reconstruction. Density-list calls return the same immutable object.
- Exact direct-production layout comparisons cover changing fractional amounts, composition, block-boundary crossings, mutable input maps/sets, placement/removal, physical unload/reload, detached/reconnected/independent identities and world changes. Interface waves still change continuously with partial ticks; cached T-junction classifications match direct classification.
- 8640 native lever placement/attachment/power cases agree with the previous algorithm. In a fixed idle 64-Tank platform fixture, block-state reads decrease from **1856 to 752 per scan** (about 59.5%). This is a read-count reduction, not a measured server-time reduction. Additional production-cache tests cover revision changes, next-tick lever response and independent worlds.
- Existing emitted-mesh, layering, seam, detached persistence/sync, lighting, casting, audio and equipment regressions remain part of the complete suite.

Remaining work/cost: moving/smoothing levels necessarily rebuild exact layer layouts; no complete animated mesh is cached. Vertex emission, wavy interface calculations, surface effects, material-buffer switches and GPU overdraw remain live. Physical-client structure filtering remains live to preserve immediate stale-block/chunk removal safety. Server storage binding and discovery, audio managers and network frequency were not changed. No new production JFR, GPU capture or FPS improvement has been measured yet.

For the matched after capture, restart Minecraft on the new build, restore the exact original scene/camera/settings and warm up as before. Record another 90 seconds using `-Scenario partial_after` so the original is preserved. Match metals, amounts, Faucets, lever state and active transfer/cooling windows; run at least three paired repetitions. Compare Render/Server sample counts and allocations alongside actual frame-time/MSPT data; percentages alone can rise when unrelated work shrinks. Repeat large, multi-metal, actively pouring, detached, out-of-view and chunk-reload cases. Inspect continuous heights/waves/hotspots, live lever changes, solid-support edits, T-junctions and hatch/light behavior in Minecraft before accepting the optimization visually.

## Remaining-renderer allocation pass

Both recordings were inspected: the original `partial-20261024-224558-6ac15ca5` and `partial_after-20261024-232033-d17de1a7`. The latter has 1362 Render-thread execution/native samples, of which 476 (34.95%) include the liquid renderer, and 709 Server-thread samples, of which 40 (5.64%) include Faucet ticking. These differ from the original sample totals and are not directly comparable method durations or an independent FPS measurement. The reported 88–94 FPS is the user's Minecraft observation after the preceding changes, not a result measured by this pass.

The latest allocation summary attributes about 29.3 GB of statistical weight to `getShaderInterfaceTrim`. Raw events show an `InterfaceTrim` record created at that method's return, called by exterior-side emission. One early record sample alone carries about 27.1 GB of weight. Related stacks also include neighbor `BlockPos`/iterator work, temporary `ArrayList`/interval processing in `renderLayerSide`, and JOML `Vector3f` creation in vertex transformations within interface emission. The first-event weight must not be interpreted as an actual 27-GB object, exact allocation during the recording, or bytes saved by the fix.

Targeted changes:

- The existing exact-smoothed-height layout cache now owns immutable per-layer interface trims and four side-visibility descriptions. Side descriptions retain the original subtraction algorithm and epsilon decisions, including L-corner returns, overlap geometry and exposed intervals. They rebuild whenever the existing structure/amount/world/vessel keys invalidate. No mutable shared scratch arrays, timed invalidation or approximate heights were introduced.
- Shader availability remains checked live for exterior sides, so resource reload/fallback still selects trimmed or untrimmed emission correctly. Texture-frame UVs, poses, colors, fullbright light, wave time, hotspots and blending/depth states remain live and unchanged.
- Physical-client structure filtering still reads every Tank's current block state. It now returns the network's existing immutable position set when all blocks remain valid, allocating a filtered copy only when a stale/missing block actually needs removal. It does not cache world occupancy across frames.

Equivalent-workload checks call the same production methods with an ordinary layer map (the original calculation path) and the metadata-bearing cached map, using identical layer objects, geometry and poses. 680 emission comparisons across single/stacked/mixed, L, bridge/T and near-boundary fills match all vertex attribute calls bit for bit. Trim records match direct arithmetic. Existing fractional-height, world/structure/identity invalidation, detached persistence and wave-continuity checks also run.

JDK `ThreadMXBean` measured allocated bytes on the test thread, after warmup, over 50,000 calls (minimum of three repetitions):

| Workload | Original calculation path | Cached path |
| --- | ---: | ---: |
| Interface trim lookup | 1,200,000 bytes (24/call) | 0 bytes |
| Fully covered interior-side preparation | 11,600,000 bytes (232/call) | 0 bytes |

These are actual controlled allocation-counter readings, not JFR weighted estimates. They exclude cache construction/rebuilds and use the same allocation-free sink to exclude common downstream vertex transforms. They do not establish zero allocations for the whole renderer, eliminate dynamic mesh emission, measure GPU time or prove an FPS gain. Other JVMs/JIT decisions may produce different byte counts; regression assertions require a reduction, not those exact numbers. Cold layout rebuilds retain additional immutable metadata, and continuously changing heights still require rebuilding it.

Remaining candidates include interface vertex transforms/JOML temporaries, buffer submission, smoothing output maps, and rendering/visibility outside this focused path. Their output and control flow were left intact. Make the next 90-second recording under `partial_after_allocations` with the same world, camera, graphics, molten composition and active-state windows, preserving both existing recordings. Compare frame-time distributions and allocations after warmup, with matched repetitions; visually inspect animated interfaces and shader reload/fallback as well as detached Tanks. No post-change Minecraft FPS or GPU measurement was performed in this pass.
