# Nylocas gear and native damage evidence

Audited **25 November 2026**. The exact recordings do **not** support a claim that the human ranger switches to a Void magic helm for Eye. They also do not supply the ranger's current combat levels, ring or ammunition. Human equipment is heterogeneous, so a pooled human-versus-bot kill rate is not a controlled damage comparison. The requested bot loadouts remain unchanged.

## Scope and reproducibility

The existing [whole-history vetting](research/nylocas/vetting/summary.json) admits **46 mage-corpus rooms and 42 ranger-corpus rooms**, comprising **69 unique rooms with 19 overlapping rooms**. Only those pinned eligible UUIDs are used. The two corpora are reported separately; 88 is not an independent room count. Every CSV attack is matched exactly to its raw event (player, tick, weapon, target and attack type), and every consumed raw raid/event file is checked against its original manifest's **uncompressed SHA-256** before decoding. The [audit JSON](research/nylocas/training-v2/gear-damage-audit.json) records the input checksums, full launch-profile counts, per-slot distributions and names. [Compressed launch evidence](research/nylocas/training-v2/gear-damage-launches.json.gz) retains each attack, equipment, current/base levels and update ticks.

```bash
python3 tools/research/nylocas_gear_damage_audit.py
python3 tools/research/nylocas_native_damage_audit.py
```

Equipment is replayed per player from `equipmentDeltas`, resetting the container on a complete snapshot, then sampling the latest update at or before the attack tick. All same-tick updates precede sampling. Decoded weapon equipment agrees with the recorded attack weapon on **every audited launch**. Partial quantity removals and item replacement follow Blert's implementation. Specifically, use **ID `(raw >> 32) & 0xffff` and slot `(raw >> 48) & 0x1f`**; the prose bit-layout comment above that implementation differs from its actual masks. Skill decoding uses **current `raw >> 16`, base `raw & 0xffff`**. [Blert ItemDelta source](https://github.com/blert-io/blert/blob/main/common/item-delta.ts), [skill source](https://github.com/blert-io/blert/blob/main/common/challenge.ts), [equipment-slot protocol](https://github.com/blert-io/protos/blob/main/event.proto).

These first-party sources are [cached with Git blob IDs](research/nylocas/training-v2/gear-audit-sources/provenance.json); the decoder verifies their exact Git blobs offline. Names come only from the cached [RuneLite name table](https://static.runelite.net/cache/item/names.json), SHA-256 `6838e8f2db6954a6a637c9afc8e637aec73314a4e6211f02ee1900bcf15c8b0f`. A name establishes identity, not combat bonuses.

## Ranger equipment: the exact head slot resolves the main question

| Corpus / weapon | Launches | Void **ranger** helm (l), 24184 | Sanguine Torva full helm, 28254 | Head unreported |
|---|---:|---:|---:|---:|
| Ranger42 / Eye31113 | 882 | 450 | 432 | 0 |
| Mage46 / ranger Eye31113 | 945 | 760 | 179 | 6 |
| Ranger42 / toxic pipe12926 | 3,060 | 1,482 | 1,578 | 0 |
| Mage46 / ranger toxic pipe12926 | 1,335 | 1,335 | 0 | 0 |
| Mage46 / ranger blazing pipe28688 | 1,493 | 1,330 | 163 | 0 |

There are **no observed Void mage helms** in these ranger Eye launches. The native ranger uses Void ranger helm11664 in all styles, consistent with the requested [range setup](../range-strategy.md). Locked Void items are explicitly recognized as their corresponding set pieces by native [EquipmentChecks](../upstream/rsmod/api/player/src/main/kotlin/org/rsmod/api/player/worn/EquipmentChecks.kt). Native magic accuracy requires a **Void mage helm plus the other Void pieces** for the 1.45 effective-level modifier, and elite magic Void damage likewise requires that helm. The observed locked ranger helm does not activate those magic set checks. This is an established native rule, not a claim that the observed Torva alternatives have equal stats. [Native magic accuracy](../upstream/rsmod/api/combat/combat-formulas/src/main/kotlin/org/rsmod/api/combat/formulas/accuracy/magic/MagicAccuracyOperations.kt), [native worn bonuses](../upstream/rsmod/api/player/src/main/kotlin/org/rsmod/api/player/bonus/WornBonuses.kt).

The **full** ranger42 Eye profiles are materially different, not just different head names:

| Launches | Head / body / legs / gloves | Amulet |
|---:|---|---|
| 444 | Locked Void ranger helm / elite Void top / elite Void robe / Void gloves | Rupture33639 |
| 170 | Sanguine Torva helm / Masori body(f)27238 / Masori chaps(f)27241 / Zaryte vambraces26235 | Rupture33639 |
| 139 | Sanguine Torva helm / Masori body(f) / Oathplate legs30756 / Confliction gauntlets31106 | Rupture33639 |
| 123 | Sanguine Torva helm / Masori body(f) / Radiant Oathplate legs30781 / Confliction gauntlets | Rupture33639 |
| 6 | Locked full Void ranger profile | Rancour(s)29804 |

All five profiles report blessed Dizana's quiver(l)28957 and Avernic treads(max)31097; **ring and ammunition are unreported**. The native requested ranger uses Void range, quiver28955, Anguish19547, treads31097 and Lightbearer25975. The native implementation does not switch to the human Masori/Confliction profiles. No power magnitude is inferred merely from those item names. Exact counts and all remaining weapons are in the [audit JSON](research/nylocas/training-v2/gear-damage-audit.json).

Amulet identity alone is particularly misleading here. Rupture is observed on 876/882 ranger42 Eyes and 939/945 mage46 ranger Eyes; the remaining six in each corpus use Rancour(s). No ranger attack uses Anguish19547. However, the **existing native item editor explicitly gives Anguish the intended Rupture bonuses**: ranged attack 20, additive ranged strength 8, prayer 3. Its ordinary dragon-arrow ID is also edited to include ranged attack 20, with bow-only application handled by `WornBonuses`. Consequently “different IDs” does not establish an uncorrected native stat deficit. Those source mappings are established; independent first-party confirmation of every current live-game item stat is outside this audit. [ObjEdits](../upstream/rsmod/api/config/src/main/kotlin/org/rsmod/api/config/editors/ObjEdits.kt), [WornBonuses](../upstream/rsmod/api/player/src/main/kotlin/org/rsmod/api/player/bonus/WornBonuses.kt), [existing native regression cases](../upstream/rsmod/content/other/special-weapons/src/integration/kotlin/org/rsmod/content/other/special/weapons/SeekingDragonArrowsTest.kt).

The ranger's attack/strength/defence/ranged/magic fields are **unreported at every ranger launch in both corpora**, along with ring/ammo. A reported prayer-set value does not fill those missing fields. Do not substitute the recording mage's 112 Magic, assume Lightbearer, or interpret absent ammo as an empty quiver. These are limits of the pinned telemetry, not observations of lower levels. [Per-launch evidence](research/nylocas/training-v2/gear-damage-launches.json.gz).

## Mage equipment and current levels

In mage46 the mage's **1,866 Eye launches all report current Magic 112 and Ranged 112 on the launch tick itself**. The head slot is twisted Ancestral hat24664 on 1,728 and Sanguine Torva helm28254 on 138. Occult12002 is present on 1,821, blood fury24780 on 45. Lightbearer25975 appears on 671 and Ultor28307 on 1,195. Ammo is Seeking dragon arrow33595 on 1,850 and ruby dragon bolts(e)21944 on 16. This corrects the earlier vague reference to ammo21944: it is a **bolt**, not another arrow. [Audit JSON and cached item names](research/nylocas/training-v2/gear-damage-audit.json).

The two most common complete armor profiles are full twisted Ancestral + Occult + Confliction + infernal cape + treads, with **Ultor 1,068 launches** or **Lightbearer 634 launches**, both with Seeking arrows. Other launches include mixed melee/magic switches; they must not all be treated as the canonical mage armor. The native requested mage uses Ancestral/Occult/Confliction/Lightbearer for magic and Torva/Oathplate/blood fury/ferocious/Lightbearer for melee and pipe. [Original mage instructions](../mage-strategy.md), [native controller armor](../upstream/rsmod/content/other/nylocas-bots/src/main/kotlin/org/rsmod/content/other/nylocas/bots/NylocasBotController.kt), [full profiles](research/nylocas/training-v2/gear-damage-audit.json).

Melee must also remain ring-stratified. For example, mage46's 868 sulphur launches include a common full melee profile with Ultor 495 times versus Lightbearer 188; its 324 Sanguine Scythe launches include that full profile with Lightbearer 185 versus Ultor 125. These are equipment counts, not measured damage bonuses. In ranger42 some partner mage stats/ammo/rings are unreported; those fields remain unknown in the evidence rather than borrowed across the 19 overlapping rooms. [Full profiles](research/nylocas/training-v2/gear-damage-audit.json).

## What the new native telemetry establishes

The separate **100-room instrumented run, seeds 131–230**, is `.state/simulations/training-v2-flight-damage-instrumented-100`. It supplies launch HP/current stats/worn IDs, actual damage-roll ranges, and native applied impacts. Its [offline audit](research/nylocas/training-v2/native-damage-audit.json) records log, trials, summary and immutable adapter checksums, and independently verifies that **all 100 trial rows match the retained mage-idle-pipe baseline in every column except `wall_seconds`**: 100/100 completed, mean 336.56 ticks. The primary-attribution audit also verifies 18,905 native launches with zero target/weapon/pending mismatches. These observations establish that adding telemetry preserved this exact run's gameplay; this is not a new damage or gear policy.

| Role / weapon | Actual primaries | Damage-roll events | Observed roll range |
|---|---:|---:|---|
| Ranger Eye | 2,344 | 2,289 | 0..32, raise zero |
| Mage Eye | 3,584 | 3,518 | 0..39, raise zero |
| Ranger pipe | 6,844 | 6,702 | 0..34, raise zero |
| Mage pipe | 135 | 125 | 0..28, raise zero |

**Every one of these launches reports Magic 112/Ranged 112 and the expected native role/style gear.** Native Eye's base maximum is `floor(currentMagic/3)-6`, hence 31 at 112, before magic damage/prayer modifiers. The ranges above are observed native output, not calculated estimates of human max hits. A successful ordinary zero roll is raised to 1; an accuracy failure remains a separate path. Missing roll events must not automatically be counted as misses or as zero damage. [Eye implementation](../upstream/rsmod/content/other/special-weapons/src/main/kotlin/org/rsmod/content/other/special/weapons/magic/EyeOfAyakWeapons.kt), [native magic max-hit operations](../upstream/rsmod/api/combat/combat-formulas/src/main/kotlin/org/rsmod/api/combat/formulas/maxhit/magic/MagicMaxHitOperations.kt), [ordinary damage rolls](../upstream/rsmod/api/combat/combat-commons/src/main/kotlin/org/rsmod/api/combat/commons/DamageRolls.kt), [telemetry audit](research/nylocas/training-v2/native-damage-audit.json).

Native pipe uses dragon darts both for the projectile and worn-strength calculation. That closes the old “projectile only” uncertainty. The server's existing `Blowpipe` helper supplies unlimited scales/dragon darts; this is an existing general player implementation, not a bot refill or a change made by this audit. Human loaded dart types are not in the decoded equipment stream, so matching dart supply cannot be established. [Pipe implementation](../upstream/rsmod/content/other/special-weapons/src/main/kotlin/org/rsmod/content/other/special/weapons/ranged/ToxicBlowpipeWeapons.kt), [WornBonuses](../upstream/rsmod/api/player/src/main/kotlin/org/rsmod/api/player/bonus/WornBonuses.kt), [Blowpipe helper](../upstream/rsmod/api/player/src/main/kotlin/org/rsmod/api/player/worn/Blowpipe.kt).

## Lifetime outcomes: useful observations with explicit attribution limits

The reproducible human cohort is: lane-born, birth wave≤15, initial/current magic style, initially full 8/16 HP at its first recorded primary, first primary Eye. “Removed” means a recorded death within 12 ticks after launch with exactly one recorded primary before death. This is **not** pure one-hit accuracy: area damage, thralls, damage timing, subsequent primaries and same-tick event ordering can change the result. The new extraction is explicit and supersedes the unfinished preliminary counts; do not splice its denominators into the historical 273/339 versus 149/183 comparison. [Human cohort code](../tools/research/nylocas_gear_damage_audit.py), [all strata](research/nylocas/training-v2/gear-damage-audit.json).

| Corpus / role / small Eye armor stratum | Removed / eligible |
|---|---:|
| Mage46 / mage / all profiles | 290 / 360 |
| Mage46 / mage / Ancestral hat + Occult | 250 / 304 |
| Mage46 / ranger / Void ranger helm | 132 / 162 |
| Mage46 / ranger / Torva helm | 28 / 39 |
| Ranger42 / mage / Ancestral hat + Occult | 247 / 310 |
| Ranger42 / ranger / Void ranger helm | 78 / 97 |
| Ranger42 / ranger / Torva helm | 73 / 99 |

The head+amulet rows are deliberately **coarse named strata**; full armor/prayer/current-stat strata remain in JSON. They are not assertions that every remaining slot matches. Human mage big Eye outcomes are 81/138 in mage46 and 74/127 in ranger42. Overlapping recordings remain in their own corpus rows, not a larger pooled sample. [Audit JSON](research/nylocas/training-v2/gear-damage-audit.json).

Using analogous native first-primary/full-HP cohorts (lane origin inferred from the first observed coordinates, rather than an explicit parent ID), the instrumented 100 records ranger small Eye 417/570, mage small Eye 446/586, and mage big Eye 185/300 removed within 12 ticks with one primary. These are **observational lifetime outcomes**, not estimates of clean human-versus-native damage differences. Native impacts allow a stricter compatibility check: one unique same-launch role/target/weapon roll, a first same-source non-thrall impact matching the HP-clipped roll, a singleton target in that impact's attack-ID group, and no prior positive thrall/opposite-source assistance. That leaves ranger small Eye 549 compatible cases (411 first-impact kills), mage small Eye 577 (442 kills), and mage big Eye 291 (185 kills). Unresolved cases are retained; they are not silently deleted from the broad denominator. [Native cohort code](../tools/research/nylocas_native_damage_audit.py), [all cohort witnesses](research/nylocas/training-v2/native-damage-audit.json).

The native primary event does **not** include an attack ID, so even that check is labeled **primary-compatible**, not an exact launch-to-impact join. Applied damage is HP-clipped, and impact streams include secondary, multisplat and thrall packets. Do not sum every role impact as primary damage. Human events lack the equivalent complete attribution, while ranger levels/loaded darts are missing and armor varies. The current evidence therefore establishes equipment/profile differences and concrete native damage ranges; it does **not** establish an engine accuracy defect, quantify a gear-driven boss-time gap, or authorize loadout substitutions. [Telemetry adapter snapshot and hashes](research/nylocas/training-v2/native-damage-audit.json).

The next falsifiable benchmark is to retain the requested loadouts and the exact same policy/seeds, verify instrumented and canonical trial rows match, then add an **observational launch attack ID** if stronger attribution is needed. Predeclare ordinary lane cohorts and compare separately by role, full armor, current level, prayer and 8/16 HP; preserve zero/splash and unresolved outcomes, and cluster any uncertainty by room. A future human matched-damage claim additionally needs recordings that actually expose the ranger's levels and loaded ammunition. No damage, equipment, encounter-rule or policy change follows from the present audit.
