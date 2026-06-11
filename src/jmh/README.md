# Musicott JMH Benchmarks

Local-only performance benchmarks for Musicott's import and startup paths, built with the
[`me.champeau.jmh`](https://github.com/melix/jmh-gradle-plugin) Gradle plugin. These are
**developer tools — they do not run in CI** and use hardcoded local dataset paths.

## Benchmarks

| Class | Scenario | Notes |
|-------|----------|-------|
| `MediaImportBenchmark` | iTunes import throughput + heap allocation | Imports into a non-FX `CoreMusicLibrary` (no toolkit). Backend-independent — measures parse + metadata read + in-memory add. |
| `BootTimeBenchmark` | Boot load time, JSON vs SQLite | Constructs the repository and measures load-to-ready. **Small tier only** — see limitation below. |

Each runs in `SingleShotTime` mode (import/boot are one-shot, stateful operations; no warmup).

## Dataset tiers

Benchmarks are parameterized over three dataset tiers with hardcoded local paths:

| Tier | Source | Path |
|------|--------|------|
| `small` | existing UAT fixture (~85 tracks) | `~/software/itunes-library-uat.xml` |
| `medium` | beets `Compilations` (~1,173 tracks) | `~/software/musicott-benchmark-medium.xml` |
| `large` | full beets library (~17,732 tracks) | `~/software/musicott-benchmark-large.xml` |

The `medium` and `large` iTunes-plist XML files are generated from a local
[beets](https://beets.io/) library:

```bash
python3 scripts/generate-benchmark-xml.py medium ~/software/musicott-benchmark-medium.xml
python3 scripts/generate-benchmark-xml.py large  ~/software/musicott-benchmark-large.xml
```

`small` uses the existing `~/software/itunes-library-uat.xml` fixture.

## Running

```bash
# All benchmarks, all tiers (writes build/reports/jmh/results.json + per-benchmark JFR dirs)
gradle jmh

# Compile only (no execution) — quick sanity check
gradle jmhClasses
```

To run a subset, set `includes` / `benchmarkParameters` in the `jmh { }` block of
`build.gradle`, e.g.:

```groovy
jmh {
    includes = ['MediaImportBenchmark']
    benchmarkParameters = ['dataset': objects.listProperty(String).value(['large'])]
}
```

The import benchmark forks with `-Xmx12g`: the full library retains a copy of each track's
embedded cover-art image, so peak heap scales with library size (see
[octaviospain/music-commons#142](https://github.com/octaviospain/music-commons/issues/142)).
A 4 GB heap OOMs on the `large` tier.

## Profilers and JFR analysis

The `gc` and `jfr` profilers are enabled by default. Each benchmark produces:

- `build/reports/jmh/results.json` — throughput + `gc.alloc.rate(.norm)` columns
- `build/reports/jmh/gc-import.log` — GC log (import benchmark)
- `<benchmark-name>-<params>/profile.jfr` — a flight recording per benchmark

Analyze a recording with the JDK's `jfr` tool (use a JDK 24 `jfr` to match the toolchain):

```bash
jfr summary <benchmark-dir>/profile.jfr
jfr print --events jdk.ObjectAllocationSample --stack-depth 12 <benchmark-dir>/profile.jfr
jfr print --events jdk.OldObjectSample <benchmark-dir>/profile.jfr   # retained objects
jfr print --events jdk.GCHeapSummary <benchmark-dir>/profile.jfr     # peak heap
```

## Whole-application GC / JFR runs

To profile the real application (not microbenchmarks) under the E2E suite:

```bash
gradle e2eTestWithGcLog   # -> build/reports/gc/e2e-gc.log
gradle e2eTestWithJfr     # -> build/reports/jfr/musicott-import.jfr
```

## Known limitation — `BootTimeBenchmark` is small-tier only

The boot benchmark seeds its on-disk artifact by importing into an `FXMusicLibrary`. At the
medium/large tiers this floods `Platform.runLater` with per-item observable-property updates
faster than the single JavaFX thread drains them, exhausting the heap during `@Setup`
(regardless of heap size or Monocle-vs-Gtk toolkit). The `dataset` param is therefore
restricted to `small`. Producing a scale boot comparison requires a seeding strategy that
does not marshal every insert through the FX thread — e.g. a bulk/event-suppressed import or
pre-generating the seed `.json`/`.db` artifacts out of band.
