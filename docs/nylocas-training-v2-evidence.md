# Vetted duos

Written before policy changes for the `nylo-training.md` training pass. Vetting is based on the complete returned pair history, including slower recordings, before filtering individual study recordings.

| Pair | Recordings with accurate boss split | Sum ticks | Mean boss spawn | Eligible existing raw rooms |
|---|---:|---:|---:|---:|
| Drummie + cutely meows (formerly FishxBubble) | 88 | 28,712 | 326.272727 ticks = **3:15.764** | 36 distinct; mage corpus 19, ranger corpus 36 |
| cutely meows + Lacking IQ | 6 | 1,940 | 323.333333 ticks = **3:14.000** | 6 ranger |
| Drummie + Eres Un Amor | 82 | 26,612 | 324.536585 ticks = **3:14.722** | 7 mage |
| Drummie + Lacking S19 | 9 | 2,896 | 321.777778 ticks = **3:13.067** | 7 mage |
| Drummie + lmperatrice | 106 | 34,588 | 326.301887 ticks = **3:15.781** | 13 mage |

The first pair has more than five recordings and an average ≤196 seconds. The [primary API query](https://blert.io/api/v1/challenges?type=1&mode=11&scale=2&party=Drummie%2Ccutely+meows&stage=ge12&split%3A42=ge0&sort=-startTime&extraFields=splits%3A42&limit=100) returns 88 rows, below its 100-row limit. It applies **no upper boss-time cutoff**. The exact initial response is preserved as [gzip JSON](research/nylocas/vetting/initial-drummie-cutely-meows.json.gz), with its source URL, SHA-256, count, and sum in the [initial manifest](research/nylocas/vetting/initial-drummie-cutely-meows-manifest.json). Counts refer to public recorded boss spawns; raids that never reached a recorded boss spawn have no usable boss time and are outside the denominator. This does not establish an average over unrecorded play.

Each individual strategy recording still must satisfy boss spawn ≤3:16: because room ticks are integral and last 0.6 seconds, that is **≤326 ticks**. The pair-average threshold is **326⅔ ticks**, not 326; averages need not be integers. Historical aliases are joined using the API's `currentUsername`.

The first pair was written before role agents began edits; the remaining four were added after whole-history vetting completed. All five are released for this training pass. The [full vetting manifest](research/nylocas/vetting/manifest.json) pins every API page, source URL and SHA-256; the [summary](research/nylocas/vetting/summary.json) includes every denominator UUID/tick and the exact eligible existing recording UUIDs. The older mage/ranger corpus queries both prefiltered to ≤326 ticks, so their means alone cannot vet any pair. Of 136 distinct recordings in the combined old corpora, **69** are now eligible: 46 in the mage corpus and 42 in the ranger corpus, with 19 shared recordings.

## Selection and audit

The snapshot was collected on 2026-11-24 at 13:39 UTC. Each exact pair is queried with `type=1`, `mode=11`, `scale=2`, `stage=ge12`, `split:42=ge0`, an as-of start-time bound, `extraFields=splits:42`, and `limit=100`. There is no status filter: later wipes and resets remain relevant if their Nylocas boss spawn was recorded accurately. Missing boss times cannot contribute to a boss-time mean. `currentUsername` joins spelling/case and historical aliases. Every returned row is checked against its exact two-person roster, normal mode and scale.

The [primary route implementation](https://github.com/blert-io/blert/blob/master/web/app/api/v1/challenges/route.ts) sets a 100-row maximum and defaults to accurate splits. The [primary query parser](https://github.com/blert-io/blert/blob/master/web/app/api/v1/challenges/query.ts) accepts comma-separated party members and an `after` cursor under the requested sort. Retrieved source bytes are retained and checksum-pinned beside the responses; the live `master` links may subsequently change. The 101-record Exhailing pair and 106-record lmperatrice pair each required a second page. All other pairs terminate below 100. Pagination is by descending start time with an epoch-millisecond cursor; the collector refuses an observed boundary timestamp tie and deduplicates UUIDs. This is complete coverage of the returned public search for these 11 candidate pairs, not a census of every duo on Blert or unrecorded/private raids.

| Checked but excluded pair | Whole-pair denominator | Mean seconds | Reason |
|---|---:|---:|---|
| A rsh + Drummie | 9 | 198.400 | Average >196 |
| B asher + Drummie | 18 | 198.000 | Average >196 |
| cutely meows + Exhailing | 101 | 196.253 | Average >196; do not round down |
| cutely meows + intrr | 6 | 196.800 | Average >196 |
| Drummie + Selssul | 20 | 203.520 | Average >196 |
| Drummie + Unluckers | 25 | 198.432 | Average >196 |

Reproduce offline with `python tools/research/nylocas_duo_vetting.py`. It verifies all 310 raid/event copies in the old mage/ranger manifests, independently checks each distinct room's event-123 boss marker against split 42, requires zero recorded lost ticks, verifies all new search-page checksums, checks old recordings are present in the corresponding unfiltered history, recomputes the unrounded means and writes the eligible UUID list. Use `--collect` only to intentionally replace the dynamic vetting snapshot with network access. It does not refresh or change the older role corpora.

The [overview URL named in the training brief](https://blert.io/raids/tob/97bfe92d-7206-4444-92c6-3c315e8f9b61/overview) was opened through the browsing tool, which could not render it. That replay is not silently treated as a qualifying witness. The public first-party JSON APIs and existing checksum-pinned raw replay responses provide the auditable evidence used here.

## Human rotation anchors

Use only the eligible UUIDs in the vetting summary when making comparisons. Earlier [mage](nylocas-duo-mage-strategy.md) and [ranger](nylocas-duo-ranger-strategy.md) whole-corpus frequencies include excluded pairs; their 62/93 denominators must not be relabelled as vetted samples. The [independent 19-room mage cross-check](research/nylocas/mage-independent-check.md) consists entirely of the now-vetted Drummie/cutely pair and remains an eligible comparison subset.

The [0c25e1 replay](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), boss tick 308 (3:04.8), demonstrates complementary role actions: ranger west bow at tick 5 and east bow at 11; mage south Shadow at 6, west Ayak at 12, east Ayak at 16 and wave-4 south magic big at 19. During subsequent waves the mage also handles melee work. These observed actions support avoiding duplicate targets and matching the named big ownership in the user's role specifications; weapon-frequency totals do not establish optimal priority whenever two targets were simultaneously available.

For the mage's wave-12 packet, all 19 eligible cross-check rooms record east scythe then west Dinh's. Eighteen scythes are at target age eight; the other is nine. Eighteen specials follow five ticks later; the other follows ten ticks later. In the witness replay these are ticks 92 and 97, although wave 13 can already be current in other rooms. Reservations must follow the target packet across wave changes. The observed scythe primary is the northern east-slot small, so a southern primary refinement is a requested implementation choice rather than an observed invariant.

For wave 21, 13/19 cross-check rooms explicitly recognize first-tick west barrage at `s21+1`, and 13/19 have northern-small sulphur at `s21+6`. Missing recognized animation does not prove an omitted attack. Wave-22 Dinh's targets the south-lane west-slot small in 19/19, at age 10 or 11, after it changes from initial magic to current melee. The alternative fighting east big is a fallback without a primary-target witness in that subset. For the late reservation, the named wave-26 south big receives Dinh's in 16/19; offsets range `s28+7` to `s28+11`. In `0c25e1`, the special is tick 237; an old west small is removed at 240 while the big splits later at 245. Attack launch, HP zero, removal, split birth and cap release must remain separate timings.

The user's [range specification](../range-strategy.md) supplies the required wave-10 west→east→south chin packet and transition big ownership; [mage specification](../mage-strategy.md) supplies wave-11 east barrage and wave-12/21/28 reservations. Evaluate the resulting agent traces against eligible raw replays and report timing deviations separately from damage variance. These recordings support human compatibility, not a causal guarantee that copying a single replay produces its boss time.

## Ambiguities to preserve in implementation reviews

The brief says to ignore birth-wave ≤29 nylos after the wave-28 stall plan, while the range specification explicitly requires wave-29 west attacks. A literal cutoff beginning at wave 29 contradicts those instructions and eligible human actions: all 19 mage cross-check rooms contain at least one attack on a birth-wave ≤29 target after wave 29 has spawned. The user explicitly confirmed that the agent cutoff starts at actual wave 31; this is an explicit task decision, not a human invariant inferred from those clips. Newly born splits keep their own birth wave, so an old parent does not make a later-born child automatically ineligible.

“First tick” means first actionable attack opportunity for the specified target, not uniformly `laneSpawn+1`. Lane spawn, player pathing and arena entry are distinct; east scythe can reach a target before its anchor crosses a chosen interior rectangle. The word “stall” in the brief names the last successfully spawned wave: wave-28 stalls are retries of wave 29. The brief's “0 stalls from wave 21 to 28” must therefore be read with its explicit exception permitting two or three wave-28 stalls. Event-derived timing and real agent server ordering must be compared under the same convention.
