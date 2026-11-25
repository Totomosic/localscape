# Native shared in-flight claims — implemented and verified

Resumed on **25 November 2026**. The coordination defect is fixed in `NylocasBotController.kt`; the training speed goals remain unmet. See [the main handoff](nylocas-training-resume.md) for current experiments.

## Observed defect

The preserved [30-room opening evidence](research/nylocas/training-v2-shadow-shared-flight.json) has 26/30 ranger Eyes launched at the mage’s Shadow target before its first observed HP change. Shadow launches at local5 and the first HP change is12; the old partner claim expired at10 while the own-flight lease remained37. This proves overlapping flights, not that every overlap wasted damage.

Reproduce the historical audit with `python3 tools/research/nylocas_shadow_flight_audit.py` (default input remains the immutable old30 run).

## Fix and event semantics

- A confirmed primary gives the source an own-flight lease through `tick + 32` and its partner the same interval. Partner `claimedUntil` is exclusive, so its stored endpoint is `tick + 33`.
- Non-thrall `NpcHitEvents.Applied`, including splash/zero, records a release candidate for that NPC and source role. After the native cycle, release both corresponding leases once no queued non-thrall hit from that source remains.
- This preserves a still-airborne Shadow when an unrelated secondary lands first and waits for trailing multi-hit splats. Pending thralls neither release nor extend primary leases. Opposite-source hits cannot release another role’s lease.
- Inspect queues only outside `Applied`: `NpcQueueList.iterator()` is shared with the active native queue processor. The callback runs through the same `afterCycle` input interface in live service and simulation.
- Cancellation without any application recovers at the finite bound. Despawn pruning removes candidate state. No NPC NULL rule, damage roll, supply, cooldown, or encounter rule was changed.
- `overlapping_primary_flights` counts confirmed primaries launched while the other role’s own-flight lease is active. It is distinct from same-tick double-ups and shared lifetime targets; it does not establish wasted damage. Missing legacy counters remain unknown.

The source/NPC pending-hit check is conservative: a later same-source area hit can extend protection while it remains queued. It does not require nullable report-only `Hit.attackId` or expand engine APIs.

## Verification

Native command, from `upstream/rsmod` with `tools/dev.py`’s JDK21 environment:

```bash
./gradlew --offline :content:other:nylocas-bots:integration
```

**32/32 passed**, including real Shadow with both controlled roles, real ranger decisions, zero/splash release, a positive surviving hit, six additional source/secondary/splat/thrall/cancellation cases, and existing service/supply/utility checks. Formatting applied to bot and combat-script modules. Python report suite: **35 tests, four skipped**.

The initial test draft had a Kotlin receiver-shadowing error counting the mage as ranger; that draft’s failure is not evidence. The corrected regression captures the ranger explicitly and checks event ordering, including launches earlier in the same native cycle as impact. Restoring only the former five-tick claim timing made both corrected Shadow cases fail on `rangerBeforeImpact == 0`; restoring the fix made the full suite green. Logs: `.state/simulations/training-v2-flight-corrected-red.log` and `training-v2-flight-green.log`. The exercised short-timing controller is saved separately as `training-v2-flight-old-timing-controller.kt`.

Full-encounter live-profile smoke22: completed352 ticks, no death, both wave12 packets, zero old-birth cleanup primaries and zero overlaps. Prior smoke was348: functional correctness does not demonstrate a speed win.

Fresh seeds131–230: `.state/simulations/training-v2-flight-fix-fresh-100`, **100/100 completed**, **336.36 ticks / 3:21.816 mean**, pre-cap4.47, zero overlap counter in every room, **0/100 opening partner launches before first observed HP change**, and all100 west12 Dinh packets. This also supplies the previously missing representative saved12-guard batch. These new seeds do not constitute a paired speed comparison with the earlier31–130 runs. Immutable source and adapter snapshots, conditions, CSV, summary, training audit and opening audit are in the run directory.

## Damage audit findings preserved so far

Native duo HP is correctly8/16. The locally decoded enriched cache has small defence1/magic1 and big defence20/magic20, with ordinary defensive bonuses default0; cached HP11/22 is deliberately replaced by duo scaling. No native NPC-stat change was made.

Preliminary nonflickering lane observations (birth≤15, initially full HP, first primary Eye, removed within12 ticks with no second recorded primary) show mage Eye small273/339 humans versus149/183 bots, and big81/138 versus53/90. These are approximately80.5%/81.4% and58.7%/58.9%; they give no obvious mage damage deficit, but are **not** clean single-hit attribution because secondary and human thrall damage are incompletely observed. The formal damage audit was stopped before stratification and portable full outcome extraction were completed.

Gear is a material confound, and the user's requested bot gear must remain fixed:

- Vetted human ranger records contain **no observed Anguish19547 attacks** in either corpus. Common pipe/Eye gear uses Necklace of rupture33639, with a few Rancour(s)29804 observations. Ranger ammunition is unreported in the equipment slot, so ordinary-arrow agreement cannot be established.
- Human mage commonly switches Ultor28307 and Lightbearer25975. In mage46, Eye has671 Lightbearer launches; Ultor should not directly change Eye damage, but melee must be ring-stratified. Mage ammunition is generally Seeking dragon arrow33595, occasionally21944, never observed ordinary11212.
- Cosmetic locked Void and twisted Ancestral variants should not be treated as different damage gear.

Equipment was decoded using first-party [Blert item-delta source](https://github.com/blert-io/blert/blob/main/common/item-delta.ts), with item names checked against the primary [RuneLite name table](https://static.runelite.net/cache/item/names.json). Those new-item stat magnitudes were not established; do not invent an advantage or change the loadout from names alone. Current native blowpipe code explicitly uses dragon darts for its projectile, but its full damage/ammunition formula audit is unfinished.
