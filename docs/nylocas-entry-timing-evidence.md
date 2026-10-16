# Nylocas entry timing evidence

Research date: 2026-10-17. Scope: player-directed lane spawns in the 13 pinned normal-mode solo/duo recordings. The [manifest](research/nylocas/manifest.json) records original API URLs, uncompressed SHA-256 hashes, recorder commits and zero reported lost ticks for every room. These are client observations from a convenience sample, not an authoritative server script.

## Finding

**A stationary tick immediately after room entry is common for aggros, but the corpus does not support imposing it unconditionally.** Among 318 eligible player-directed NPCs, 285 have the same anchor on their first two consecutive positive-HP room observations and 33 move. Both sizes have clear moving counterexamples. An observed hold establishes unchanged coordinates over that interval; it does not by itself establish a compulsory entry delay or its cause.

The [Blert mechanics guide](https://blert.io/guides/tob/nylocas/mechanics#behavior), checked on the research date, identifies fixed player aggros but does not specify an entry movement delay. The primary evidence for the timing conclusion is the recorded tick/coordinate/ID sequence below, preserved in the pinned API snapshots.

## Method

Load every raid and event snapshot with [`load_checked`](../tools/research/nylocas_replays.py), which verifies the manifest's original-content hash before parsing. Identify lane spawns from recorded spawn metadata and join each NPC to [lane-spawn-targets.csv](research/nylocas/lane-spawn-targets.csv) using `(wave, lane, initial style, big, spawn x, spawn y)`. This retains the existing guide-based initial-aggro classification, checked against later native aggro transformations in the [movement study](nylocas-movement-evidence.md).

Run `python tools/research/nylocas_entry_contact.py` from the repository root to reproduce the table with the [analysis script](../tools/research/nylocas_entry_contact.py). Its [JSON output](research/nylocas/entry-contact-observations.json) retains all 33 aggro movement counterexamples, representative entry intervals and the separate north-pillar contact observations. The script also verifies the CSV's aggro flags and sizes against the checksum-pinned guide input.

Initial ordinary IDs 8342–8347 cannot identify pillar attackers: aggros use those IDs while incoming too. Native fighting IDs are 8348–8353. A later aggro transformation does not retroactively prove that every preceding observation was actively targeting a player, particularly for late waves after pillar-state changes.

For each lane spawn, find its first spawn/update observation whose southwest anchor is within the replay arena, X 3290–3301 and Y 4243–4254. Compare it with the next observation only when the tick increases by exactly one and both have positive recorded HP (`hitpoints >> 16 > 0`). Exclude NPCs that never enter or lack that pair. This deliberately uses the same anchor criterion for both sizes, rather than counting partial 2×2 overlap with the arena as entry. A hold means identical X and Y; a move means either changes.

| Initial assignment | Size | Holds after entry | Moves after entry | Eligible NPCs |
| --- | --- | ---: | ---: | ---: |
| Player aggro | Small | 126 | 22 | 148 |
| Player aggro | Big | 159 | 11 | 170 |
| Pillar | Small | 18 | 585 | 603 |
| Pillar | Big | 1 | 296 | 297 |

The eligible subset contains 1,218 of the corpus's 1,560 lane NPCs: 318 of 455 initial aggros and 900 of 1,105 pillar-directed NPCs. These counts describe recorded coordinate intervals, not independent trials or measured combat delays. Positive recorded HP reduces obvious death-animation contamination but may lag a hit; zero reported lost ticks does not resolve sub-tick client/server ordering. The pillar comparison also shows that a stationary entry interval is not exclusive to aggros.

## A clear one-tick hold

In [duo 1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas), wave 2 south small melee `roomId=60691` has HP 8 throughout this interval:

| Tick | Anchor | NPC ID |
| ---: | --- | ---: |
| 17 | `(3295,4242)` | 8342 |
| 18 | `(3295,4243)` | 8342 |
| 19 | `(3295,4243)` | 8348 |
| 20 | `(3296,4243)` | 8348 |
| 21 | `(3297,4244)` | 8348 |

It enters at tick 18, holds through the ordinary-to-fighting transition at 19, then resumes movement at 20. This supports the user's observation that entry holds occur. It does not establish that every aggro must hold.

## Counterexamples to a universal hold

In [duo 9f8f1a77](https://blert.io/raids/tob/9f8f1a77-a528-454f-bfac-8251dec9de32/nylocas), wave 1 west small ranged `roomId=47173` advances on every recorded tick from its tick-4 spawn at `(3281,4248)` through entry. There is no earlier stationary lane interval that could account for a shifted entry hold:

| Tick | Anchor | NPC ID | Recorded HP |
| ---: | --- | ---: | ---: |
| 12 | `(3289,4248)` | 8343 | 4 |
| 13 | `(3290,4248)` | 8343 | 4 |
| 14 | `(3290,4247)` | 8349 | 4 |

The ordinary-to-fighting transition coincides with a one-tile southward move in consecutive positive-HP observations. A rule requiring the anchor to remain `(3290,4248)` at tick 14 would contradict this recording.

In [duo 4ed0a471](https://blert.io/raids/tob/4ed0a471-7c3a-46d7-8f9c-e807f0275be3/nylocas), wave 30 east big magic `roomId=39609` moves west every tick from its tick-292 spawn `(3310,4248)` through tick 301, then changes direction as it transforms:

| Tick | Anchor | NPC ID | Recorded HP |
| ---: | --- | ---: | ---: |
| 300 | `(3302,4248)` | 8347 | 16 |
| 301 | `(3301,4248)` | 8347 | 16 |
| 302 | `(3300,4249)` | 8353 | 1 |
| 303 | `(3299,4249)` | 8353 | 1 |
| 304 | `(3298,4249)` | 8353 | 1 |

An additional full-HP counterexample appears in [duo 1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas): wave 28 west small aggro `roomId=64237`, initially ranged and magic by entry. Its recorded HP remains 8/8 from spawn at tick 236 through tick 255. It moves every tick from `(3281,4248)` at spawn to `(3290,4248)` at tick 245, then moves south to `(3290,4247)` at tick 246 while changing from ordinary magic ID 8344 to fighting magic ID 8350. It holds that new anchor at ticks 247–248. This confirms that the immediate post-entry move also occurs without a recorded HP reduction; it does not establish the cause of the move or the following hold.

The two earlier examples were damaged before or during the transition, but damage cannot explain every moving entry interval because the additional example retains full recorded HP. NPC target fields, regular-NPC attack events and server processing order are absent, so these records cannot settle what selects a hold versus movement or exclude a conditional activation delay.

## Implementation implication

The current [`NylocasBehaviour.tick`](../upstream/rsmod/content/areas/dungeons/nylocas/src/main/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasBehaviour.kt) can change an entered aggro to its fighting form and process player routing during that AI tick. Adding a blanket return for every first fighting tick would reproduce a common pattern but force a disagreement with the moving trajectories above. Preserve the observed holds and movers as separate timing fixtures before specifying a conditional activation rule. The corpus establishes the observed difference, but does not yet identify the exact condition that selects it.
