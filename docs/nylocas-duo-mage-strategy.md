# Duo Nylocas mage: basic strategy hierarchy

The mage is the team's **timed melee/AoE and magic-clear player**, following Drummie's normal-mode duo role. Build the policy around selected lane packets, melee smalls and fresh magic splits, while the ranger clears ranged lanes. A universal “magic first” rule misses much of the mage's work and opens bigs that the recordings deliberately leave alive.

The objective is fast wave-29 admission, then a short young tail. Read the [priorities by last-spawned wave](nylocas-duo-mage-waves.md) for the route and named targets. This is a proposed basic hierarchy inferred from observations and the user's explicit timing requirements; it has not been executed as an agent or shown experimentally optimal.

Apply current-style, death-state, partner-claim and phase-cutoff filters to **every layer**, including reserved attacks. Then choose: timed packet and its staging → actionable assigned lane/big work → fresh magic clearing → fresh melee clearing → useful ranged help → staging. South-side magic preparation in waves 20–22 and high-value AoE can promote ordinary clearing; named deferred targets are explicit exceptions to youngest-first selection.

## Evidence and conventions

The [corpus](research/nylocas/mage/README.md) contains **all 62 qualifying results** from the [requested Drummie search](https://blert.io/search/challenges?party=Drummie&scale=2&mode=11&type=1&startTime=%3E%3D1788192000000&stage=ge12&split%3A42=ge0&sort=%2Bsplits%3A42%2C-startTime), narrowed to boss spawn ≤326 ticks. That is the greatest whole-tick value ≤3:16. Recordings span 2026-09-05 through 2026-10-29, eight ranger partners, and boss spawns from 3:04.8 to 3:14.4. The evidence includes 4,554 mage attacks, 6,889 partner attacks and 19,810 mage position samples. An [independent 19-room check](research/nylocas/mage-independent-check.md) verifies critical targets, tiles and removals separately.

`sN` means the **actual lane-spawn tick** of wave N, including stalls. A target's age is measured from its own birth. Lane spawn happens before room entry: the wave-12 first actionable east scythe is normally observed at `s12+8`, not `s12+1`. Wave 13 is already current for that scythe in 42/62 rooms. New wave markers must not cancel pending packets.

Blert observes attack animations and server-tile samples, not input clicks or all AoE victims. An immediate barrage is normally recorded at birth+1; that does not instruct the agent to add a tick of delay. Use current style, weapon cooldown and observed geometry. The [mechanics audit](nylocas-mechanics-source-audit.md) and [lifecycle evidence](nylocas-lifecycle-evidence.md) distinguish HP zero, removal and cap relief. Smalls generally expire at age 52; bigs remove/split later. Children have fresh ages and their own birth waves.

## Priority 1: preserve the timed packets

Reserve both movement and attack cooldown for the next hard opportunity. Ordinary clearing must not make these late. These requirements combine repeated recording anchors with explicit user instructions; the evidence column identifies where they differ.

| Packet | Required mage action and preparation | Recording evidence / limit |
| --- | --- | --- |
| Wave 12 east → west | Stage east near `(3301,4249)`. Scythe both east melee smalls at the first actionable tick, preferably selecting the **southern non-fighting** small. Move west during the five-tick cooldown and Dinh's-special the wave-12 **west-lane southern** small five ticks later, around `(3293,4249)`. Aim to remove it before the wave-14 check. | East scythe in 62/62; age eight in 54/62. The primary is **north in 61/62**, south in one at age nine. The southern preference is the user's refinement, not the dominant observed click. West special in 61/62; five-tick gap in 59/62; west age 13 in 52/62. |
| Wave 21 west barrage → north sulphur | Stand right beside the west barrier, normally `(3290,4249)`, before the lane spawn. Barrage the west mage pair on its first actionable tick. Five ticks later sulphur-blade a northern melee small if available, budgeting travel toward it during barrage cooldown. Seek an overkill before the first wave-22 check. | West barrage explicitly recognized in 44/62; 42 at `s21+1`. A sulphur attack at `s21+6` in 49/62, including 40 on a northern target with recorded y≥4250. Those targets are existing splits, not a nonexistent north lane. Damage and server ordering are not guaranteed. |
| Waves 20–22 south preparation | Outside the reserved barrage/sulphur ticks, remove south-side **magic smalls** with Ayak so the southern melee special has useful matching targets. Dinh's the **western member of the wave-22 south pair**, one tick after it enters, normally observed at `s22+10` from `(3295,4244)`. If still occupied clearing magic, use the wave-22 **east-spawned fighting big once melee**, when it reaches the mage. | South-west small special in 62/62, target ages 10 or 11, **currently melee despite initially magic**. The alternative big target is user-specified and has **0/62** observed primary specials; keep it as a fallback. See the wave guide for entry-coordinate caveats. |
| “26 Dinh's”, on wave 28 | Preserve the **wave-26 south melee big on the southwest pillar**. Stage southwest, commonly around `(3293,4245–4246)`. Special it just as the wave-28 west smalls enter useful range and before the south smalls enter the intended splash geometry. Reserve cooldown and ≥50 special energy. | Named special in 53/62: 47 with wave 28 current and six already in 29. `s28+9` is the most common observation, 32/62, rather than a universal fixed offset. Events do not enumerate all splash victims or prove southern exclusion. |

For the last packet, the ideal first two wave-29 checks fail, then the small removals admit 29 at the next retry. In [0c25e1](https://blert.io/raids/tob/0c25e13d-045e-4362-a7e3-e4cf83f229a8/nylocas), `s28=228`, Dinh's is at 237, one west small reaches estimated zero HP at 238 and removes at 240, which is `s29`. The big removes/splits at 245: **the special animation and big HP zero did not themselves free its slot**. Two or three wave-28 retries are acceptable; 40/62 qualifying rooms have that outcome. Do not spend low-value attacks trying to force zero retries.

Recover from a miss using observed HP, current color, cooldown and the next due check. A late recording is evidence of human variation, not a new desired delay. The wave-12 special target removes strictly before the **first due** wave-14 check in 52/62, on its recorded tick in eight, after it in one, with one special absent. The first due check uses `s13+8`, not a later successfully admitted wave 14. Similarly, removal on the wave-22 check tick does not establish within-tick server ordering. The desired before-check outcomes remain dependent on damage and removal timing.

## Priority 2: service the phase's assignments without opening extra bigs

These are eligibility rules for every lower priority. Avoid targets already dying, mismatched colors, imminent natural expiry and targets the ranger has a likely lethal attack in flight. Do not refresh an old target's age when it changes color. Prefer young, comparable targets over old ones; a named timed packet or useful multi-target hit can override this tie-break.

Service the named lane/big assignment in its wave-guide interval before ordinary filler when actionable. Use travel and cooldown gaps for other useful clearing. Keep an assigned big's finish eligible through a miss or nonlethal hit, but yield to the next hard packet and the cutoff; the recordings do not justify inventing a precise kill deadline for every big.

**Before the cap increase**, small kills are valuable because killing one big ultimately replaces it with two smalls. Work through the assigned early magic bigs without opening every available big. The mage's early assignments are south wave 4, west wave 8, west wave 11, then the deliberately deferred east wave-13 melee big around waves 16–17. The wave-15 south big removes at age 55 in all 62 rooms, and 185/186 wave-17 lane bigs do likewise with neither player's primary attack recorded. Leaving them is part of the baseline; “fresh big is magic” is insufficient reason to open it. These are removal/primary-target observations, not proof no incidental damage occurred.

**From actual wave 20**, use the extra room capacity for selected big kills and their fresh children. Wave 20's marker reports cap 24 in all 62 rooms; the first wave after that transition is 21. The post-cap route favors south wave 20; south wave 24; east wave-24 melee; west wave-25 melee; west wave-26 and wave-27 magic; and the saved south wave-26 Dinh's target. It still leaves many east magic bigs alive: the east wave-23, wave-25 and wave-26 bigs remove at age 55 in 58/62, 58/62 and 56/62 respectively. Increased capacity is permission for the selected rotation, not permission to kill every big.

**At wave 29, apply the user's cutoff immediately:** exclude all survivors whose own birth wave is ≤29, including the new wave-29 lane spawns. From waves 30–31, handle their new targets and children born in those waves; an old parent does not make a new child old. Do not attack an ignored old big and create a fresh tail. Continue protection and movement around ignored threats.

This cutoff is a **requested policy deviation**: all 62 recordings attack some ≤29 targets after 29 spawns, and all 62 scythe the wave-29 south big. Six named 26-Dinh's events are also late enough to fall after 29. Under the strict policy, skip those late specials and the wave-29 big rather than carrying them across the cutoff. The recordings validate the route and weapon choices, but cannot validate the performance of this stricter policy. The earlier ranger guide's exception for a fresh lane packet is not imported into this mage guide.

## Priority 3: clear fresh magic smalls and useful magic clusters

Use Ayak as the ordinary magic-small weapon; it accounts for 2,507/4,554 mage attacks. Barrage intact matching groups when its useful splash and timing outweigh the longer cooldown, especially the wave-11 east pair and reserved wave-21 west pair. Do not barrage a scattered pair merely because they share a birth wave. Shadow is the recorded opener in 62/62; Ayak is a permitted opening alternative, not a measured first-action variant in this corpus.

Plan with the pinned recorder cooldowns: Ayak three ticks, sulphur four, scythe/Dinh's/barrage/shadow five, darts two. Recheck the actual weapon/attack mode when implementing; these are the definitions used to interpret the recordings, not measured input-to-impact delays.

During waves 20–22, give south-side magic smalls extra urgency to prepare Dinh's. Otherwise choose recent unclaimed magic smalls near the current route, reconsidering fresh splits after every big removal. The role's name does not grant ownership of every magic lane: the ranger commonly handles west wave-13 magic, south wave-14 magic and east wave-15 magic while clearing the ranged member beside it.

## Priority 4: clear fresh melee smalls and service assigned melee bigs

Use sulphur blades for an isolated small or finishing hit, and scythe for a useful melee pair/cluster or the named big assignments. These account for 1,173 and 435 mage attack events respectively. Prefer a reachable, young melee target over travelling after an old survivor. Reserve the special packets even if ordinary melee work remains.

Early recurring melee work includes wave-4 east during wave 5, wave-5 south, wave-8 south during wave 9, wave-9 west during wave 10, wave-13 south, wave-14 west and wave-15 west. Later, scythe the wave-24 east big and wave-25 west big as they arrive; Dinh's is an observed alternative for the latter. Special energy spent on an optional big must leave the named 26-Dinh's funded. Do not add an optional special on the assumption that regeneration will suffice; use the live resource state and remaining time.

The magic-versus-melee ordering outside the hard windows is a baseline inference, not a measured universal preference. A multi-target scythe, damaged small that can be removed immediately, or urgent assigned big can be more valuable than an isolated full-HP magic small. Replan from the room rather than enforcing a rigid weapon alternation.

## Priority 5: help on unclaimed ranged survivors, then stage

Dragon darts are low-priority filler, normally on a nearby damaged or overlooked ranged target. They are only 82/4,554 observed mage attacks. A repeated exception is the wave-15 west ranged small beside the mage's melee assignment; the ranger is east. Check partner claims and current style before helping. Do not divert from fresh magic/melee or a timed setup to duplicate the ranger's bow, chin or blowpipe packet.

With no useful eligible attack, move toward the next staging tile during cooldown. Use actual positions and reachability; observed tiles are route anchors, not a recovered click path. After the final lane packet, follow young eligible magic/melee splits north or toward their pillars only when useful; leave old survivors to their timers.

## Pairing with the existing ranger implementation

The [ranger strategy](nylocas-duo-ranger-strategy.md) and [controller](../client/src/main/java/net/runelite/client/plugins/nyloranger/RangerStrategy.java) are comparison material, not mage evidence. The recordings support these complementary assignments:

| Workload | Mage | Ranger |
| --- | --- | --- |
| Opening | South wave 1, west wave 2, east wave 3 magic | West wave 1 and east wave 2 ranged |
| Waves 5–10 | Selected magic bigs, melee lanes and recent magic/melee splits | West ranged bigs, south ranged big, east wave-6 claw special, wave-10 chins |
| Waves 11–15 | East wave-11 barrage, south-west small, west big; wave-12 packet; west wave-14 magic and west melee | South-east wave-11 magic; west wave-13 pair; south wave-14 pair; east wave-15 pair |
| Waves 18–20 | Fresh magic/melee splits, east/south wave-19 bigs, south wave-20 big | Wave-18 ranged bigs, west wave-19 magic coverage, west wave-20 ranged flicker |
| Waves 21–28 | West barrage, south preparation and melee specials; selected south/east/west big route | East wave-21 chin; wave-23/24 west ranged packet; south wave-27/28 ranged flickers |
| Waves 30–31 | East magic big/small and fresh magic/melee children | West ranged big/smalls, south chins and east ranged target |

Two integration differences need an explicit team decision when implementing the mage: the ranger controller drops surviving pre-wave-20 bigs at 20, whereas Drummie still attacks the wave-19 south big during wave 20 in 52/62 rooms; and its documented late cleanup exception retains wave-29 ranged attacks, whereas this guide follows the user's immediate cutoff. Keep the mage's observed south-19 continuation below the wave-20 opener and wave-21 reservation, and leave shared west-19 ownership explicit. Do not assume two separately documented policies already form a tested pair. This research makes no ranger code changes.

## Validation boundary

The [wave guide](nylocas-duo-mage-waves.md#validation-against-the-recordings) links generated predicates, witnesses for every observed lane assignment, and the exact deviations above. Reproduce with `python tools/research/nylocas_mage.py`; the raw inputs are checksum-pinned. Frequencies support repeatable rotation anchors, not causality, a guaranteed kill, complete splash coverage, or an optimal policy. The preferred southern wave-12 primary, alternative wave-22 big special and strict post-29 cutoff require implementation-side validation beyond these recordings.
