# Ranger cleanup rescue after the paused review

Jordan explicitly resumed training with cleanup as the primary focus. This work reads the appended [guidance](../../../nylocas-training-guidance.md) and [review questions](../../../nylocas-performance-review-questions.md), preserves both repositories' working trees, and starts from the current retained South20 source. Saved experimental trees are evidence, not the source of truth.

The retained benchmark completes 500/500. Under the new reporting rule, 470 rooms with no wave19 stall average **3:16.171915**, minimum **3:04.8**, maximum **3:31.2**; the 30 excluded rooms remain policy failures. The all-room mean is **3:16.704**, minimum **3:04.8**, maximum **3:38.4**. The new reliability goal uses all trials: **440/500 (88%)** meet 3:22, versus the required 95%. These are the [paused review baseline calculations](performance-review/cleanup-95pct-3m22-baseline.json), not results for the new candidates.

## Actual blockers and native witnesses

The canonical [RangerStrategy](../../../../upstream/rsmod/api/nylocas-agents/src/main/java/org/localscape/nylocas/ranger/RangerStrategy.java) offers ordinary melee smalls only within one tile. Its big-target weapon branch offers melee only for the wave6 assignment. Thus an otherwise idle cleanup ranger does not approach remote melee children or a surviving permanent-melee west30 parent. This is a policy restriction; normal claws and native movement are already available.

Retained South20 seed279799561480401 illustrates the parent case. West30 parent185 has16HP, becomes permanently MELEE at26, and neither role is NULL on it. At tick305 (age37), the ranger has no ordinary candidate. The parent reaches natural expiry, creates late31 children, and the boss spawns at348 ticks/3:28. The [review evidence](performance-review/slow-rooms-pre28-expiry.json) records this late fresh-parent lifetime. Ranger normal claws can help; a separate mage Scythe helper is being investigated by the mage role.

The native controller already reads each lifetime's actual partner NULL using `isNulled(npc, partner)`, separately from the bot's own NULL. Zero damage is not used to infer NULL. Ranger's ordinary priorities and limited parent whitelist do not comprehensively promote those targets. In retained seed279799561480397, Mage NULL is observed on fresh magic child186 at275. At280 it has8HP, queued damage0 and is within Eye range; ranger instead fires Eye at ordinary magic192. Ranger first selects186 at308 and attacks310. The [native rescue audit](ranger-cleanup-null-last-native-witnesses.json) records both actions, positions, readiness and predecision damage. Distance alone does not prove a clear ray; the correction must allow useful approach as well as attack.

Across the retained200/300 constituents, the audit observes20 fresh Mage-NULL lifetimes with ranger still able to damage them;17 have a ready nonlethal opportunity screen. Some never receive a later primary, which does not prove abandonment because incidental attacks or thralls can kill them. It also records66 final-target frames where ranger is ready and Mage launches at the sole living fresh NPC. Example seed257451351778437 tick307: Magic child206 is the only positive-HP non-dying NPC, ranger is ready, and Mage fires Eye. The controller's temporary unknown same-cycle claim keeps ranger idle. The new permission to cooperate on the last living target directly changes that decision.

The reusable audit is [nylocas_ranger_cleanup_rescue_audit.py](../../../../tools/research/nylocas_ranger_cleanup_rescue_audit.py). Its normal travel/range screens are candidates for review, not guarantees of line of sight or missed native input. It uses predecision `NYLO_FORECAST`; printed leases can be postcycle. The controller's temporary same-cycle claim is reconstructed from Mage's ready confirmed primary rather than guessed from final leases.

## Vetted human support

The pinned42 ranger recordings contain15 normal cleanup claw launches at fresh30/31 melee smalls. Seven occur while multiple positive-HP NPCs remain; all seven have no fresh ranged or magic work in the reconstructed observations. For example, [0a19210e](https://blert.io/raids/tob/0a19210e-9aae-421a-9411-1e3272f86eec/nylocas) tick305 has five living NPCs, including two fresh31 melee children, and the ranger claws one. [747b0a15](https://blert.io/raids/tob/747b0a15-bdf7-4f83-aa41-6d2d4b214734/nylocas) tick290 shows normal claws on a fresh31 melee child while three NPCs remain. Raw source hashes and reconstructed positive-HP states are in [ranger-human-cleanup-claws.json](ranger-human-cleanup-claws.json). Human NULL is never inferred. Human evidence supports melee assistance; extending it to a fresh melee parent is an explicit strategy extrapolation from Jordan's help-partner requirement.

## Isolated policy candidates

All paths below are immutable policy snapshots based on current canonical South20. Their `ranger.patch`, source hashes and saved probe source make the individual deltas reviewable. None changes loadouts, resources, native damage, NULL behavior or engine rules. Root serializes all native runs.

| Candidate directory under `.state/simulations/` | Change | Focused checks |
|---|---|---:|
| `training-v4-ranger-idle-melee-cleanup` | Only when ordinary attack/approach candidates are empty and no reservation is pending, use normal claws and movement on fresh>=30 MELEE during actual31. | 7 pass |
| `training-v4-ranger-last-living-cooperation` | Count all positive-HP non-dying NPCs, including ignored old births. For the literal sole fresh target during actual31, permit unknown/nonlethal partner claims and cross ordinary ownership with PIPE/EYE/normal CLAWS. | 8 pass |
| `training-v4-ranger-partner-null-cleanup` | Promote fresh actual31 targets the Mage cannot damage, covering smalls, bigs and all matching styles. Ignore stale partner claims, retain named packets and urgent original31 rangers, and prevent stale intent from overriding rescue. | 8 pass |
| `training-v4-ranger-cleanup-coordination` | Reference combination of the three ranger deltas; excludes foundation movement/fresh-intent changes and the separate dim experiment. | 31 pass including earlier singleton checks |

Every candidate preserves own NULL, already-rolled lethal damage, unknown own-flight safety, the actual31 old-birth cutoff and existing request-age49 cutoff. Final-target cooperation permits doubling; it does not require wasting a shot on a known kill. A positive-HP old body prevents the last-target claim exception even though a separately authorized idle helper may still assist a different fresh target. The earlier strict tests now check that distinction explicitly.

The direct pre-fix loops are red: remote native-witness melee cases, old Mage-NULL magic priority/ownership, and unknown partner claims on the sole living target. They become green in the respective isolated policy tree. Named south30 chin and original31 ranged urgency have explicit rescue regressions. Combined probes accept native approach or a valid path-to-target decision rather than assuming that every useful movement action has a non-null `approach` field.

The combined reference also passes166 core/replay/guidance/cleanup checks after explicitly excluding four independently pending non-cleanup corrections: saved12 protection through17, late18 recovery, pre-cap fresh-intent release, and west11 continuation. The unfiltered170-check run fails exactly those four. This is broader regression evidence, not native performance validation.

These three snapshots retain the birth29 cleanup cutoff. Root's separate dim experiment additionally excludes births before the wave28+16 time threshold. If combined, every helper/rescue/final fallback must use the shared dim predicate; checking birth>=30 alone can otherwise revive an early30 target excluded by that experiment. Literal-last counting must continue to include dimmed living bodies.

Reproduce pure checks without Gradle:

```bash
python3 tools/research/nylocas_ranger_candidate_checks.py \
  --source .state/simulations/training-v4-ranger-cleanup-coordination/policy-source \
  --output /tmp/nylo-ranger-cleanup-checks \
  RangerIdleMeleeCleanupTest RangerLastLivingCooperationTest \
  RangerPartnerNullCleanupTest RangerSingleCleanupTargetTest
```

## Measured idle-helper result

`training-v4-ranger-idle-melee-random-200` completes200/200. Its179 zero-wave19 rooms average **3:15.86145**, minimum **3:04.8**, maximum **3:28.8**. The21 excluded rooms remain failures. Across all200 the mean is **3:16.86**, minimum **3:04.8**, maximum **3:36.0**; **175/200 (87.5%)** meet3:22 and P95 is3:26.4. The95% goal is not achieved. This fresh cohort does not isolate a causal speed estimate against a static control.

There are50 confirmed helper claw launches in34 rooms, including10 parent hits. All are MELEE, with zero own-NULL or known-lethal launches and zero old-birth primaries after31. Opening and south30 chin are200/200; west24 nonlethal Eye94/94 and lethal skip106/106; protected mage-primary theft and original31 ranged survivors are zero. The run's `ranger-idle-melee-helper-audit.json`, `ranger-guidance-audit.json` and `ranger-forecast-audit.json` preserve source hashes and individual evidence. The other coordination deltas still require native validation.
