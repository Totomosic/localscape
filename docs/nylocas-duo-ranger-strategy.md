# Duo Nylocas ranger: basic strategy hierarchy

This is a recording-derived baseline for an agent playing **Cutely meows' ranger role in normal-mode duos**, through the wave room and boss spawn. Its objective is to get wave 29 admitted quickly, complete the fresh lane rotation, then clear only the young tail that would otherwise delay cleanup. It assumes a complementary mage, not two independent players both attacking every available crab.

Read the [wave-by-wave rotation](nylocas-duo-ranger-waves.md) alongside this hierarchy. The rotation defines the reserved attacks; this document decides what to do between them. The priorities below are inferred from repeated actions, rather than a claim to know the player's private decision process or to have proved an optimal policy.

## Evidence and timing conventions

The [research corpus](research/nylocas/ranger/README.md) preserves every qualifying result returned by the [requested search](https://blert.io/search/challenges?party=cutely+meows&scale=2&mode=11&type=1&stage=ge12&split%3A42=ge0&sort=%2Bsplits%3A42%2C-startTime), narrowed to boss-spawn split 42 ≤326 ticks. At 0.6 seconds/tick, 326 ticks is 3:15.6; 327 ticks exceeds 3:16. Former names are resolved through Blert's `currentUsername`. Both successful raids and raids that subsequently wiped are useful if their Nylocas wave room reached the qualifying boss spawn.

The analysis joins actual attack targets to NPC births, current styles and player tiles; it does not generate the rotation from the published wave table. See [event interpretation and an independent eight-room check](research/nylocas/ranger-event-interpretation.md), the [machine-readable validation](research/nylocas/ranger/validation.json), and [all attacks](research/nylocas/ranger/attacks.csv). A recorded attack is an animation observation, not proof of damage or a kill. Chinchompa secondary victims are not enumerated by its primary-target event.

Write `s23` for the actual spawn tick of wave 23, including any stalls. Offsets such as `s23+1` below are **Blert observation ticks**. Immediate opening/prefire attacks are commonly recorded one tick after NPC birth. The agent should act as soon as the correct target is actionable; it should not add an extra tick of delay merely to imitate the recorder. Flicker windows are also confirmed against the target's current style.

The room uses a four-tick cycle; normal-mode cap is 12 before the wave-20 phase and 24 afterwards. Smalls expire after roughly 52 ticks, while big removal/splitting is later. Dying NPCs continue to occupy cap until removal, and a big replaces itself with two fresh smalls. These lifecycle distinctions matter more than a nominal HP-zero counter. See the [primary-source mechanics audit](nylocas-mechanics-source-audit.md) and [measured lifecycle evidence](nylocas-lifecycle-evidence.md).

## The hierarchy

### 1. Protect the next assigned lane attack

The highest routine priority is a **short-lived assigned opportunity**, not whichever crab is nearest. Move into range ahead of the packet, reserve attack cooldown, attack the specified current style, then resume room clearing. Important anchors are:

| Packet | Ranger action | Typical recorded timing |
| --- | --- | --- |
| Opening | Twisted bow west wave-1 ranged small; twisted bow east wave-2 ranged small | `s1+1`, then `s2+3`; these are not both first-tick shots |
| Early south | Chin wave-3 south ranged small; scratch wave-2 south melee | Usually `s4+0`, then `s4+3` |
| Early bigs | Blowpipe wave-5 west big; bow wave-7 south big | About `s5+3`; south bow usually `s7+1…4` |
| East pair | Chin wave-21 east ranged lane pair | `s21+1` |
| West big flicker | Bow **wave-20 west big**, which starts melee, during its ranged flicker | `s20+6` |
| South then west | Bow wave-23 south big; blowpipe west north small when ranged; blowpipe west south small when ranged | `s23+1`, `s23+6`, `s23+8` (last is usually wave 24) |
| West big follow-up | Kill wave-24 west big in its initial ranged window | Blowpipe about `s24+4`, or bow about `s24+2`; Eye during magic if it survives |
| Late south | Blowpipe wave-27 south big after its ranged flicker; wave-28 south east then south west smalls as each becomes ranged | Commonly `s27+8`, `s28+6`, `s28+8` |
| Late west | Blowpipe wave-29 west north small on ranged flicker, and its west south companion while ranged | North at `s29+6`; south commonly `s29+4` or `+8` |
| Final lanes | Wave-30 west big, wave-30 south ranged flicker with a chin; wave-31 west pair, then south/east ranged targets | See the full wave guide; these overlap across waves |

Other early chin opportunities and the wave-13/14/15 paired assignments are described in the wave guide. They are less rigid than wave 20 or 23. Do not invent a mandatory attack on every wave.

**Cooldown rule:** before taking a filler attack, require that its cooldown and the travel needed for the next reservation fit before the reservation. Blowpipe is 2 ticks, black chin 3, Eye of Ayak 3, claw scratch/spec 4, twisted bow 5 according to the [pinned attack definitions](https://github.com/blert-io/protos/blob/3c5bc5cfc315ff4266128601829e66615f3ef83d/attack_definitions.json). A five-tick bow does not naturally stay aligned to a four-tick room cycle. Use short attacks or an intentional idle tick to recover the correct phase; never fill a gap merely because an attack is currently available. Recalculate the deadline if the wave stalls.

### 2. Clear recent ranged smalls and useful ranged clusters

Between reservations, blowpipe **fresh, unclaimed ranged smalls**: recent lane arrivals and new splits. Prefer the youngest useful target, then the one that can be finished with least travel/attacks. Chins are especially valuable on lane pairs and fresh clusters when the splash can clear multiple matching targets. An attack's recorded primary target does not prove how many were hit.

An old small close to its natural expiry usually frees its slot without help. Do not spend several attacks chasing it while fresh targets will remain for another lifetime. This is a tie-break within viable, role-appropriate work: it does not override a reserved flicker, a nearly completed assigned packet, or the need to handle a selected big. The recordings explicitly contain older-target finishes, including deferred wave-14 and wave-18 bigs; “newest first” is not an absolute global sort.

Predict **removals at the next admission check**, rather than counting attack animations as cap reductions. If the room is stalled, choose smalls that can actually be removed before a retry. Killing a big can initially increase population, so it is not interchangeable with killing one small.

### 3. Accelerate selected bigs, according to room phase

**Before cap increases:** use the named big assignments instead of indiscriminately opening every big. The core ranger route is west wave 5, south wave 7, west wave 9, the east melee big from wave 6 with a claw special, then the deferred ranged big from wave 14. The early claw special is deliberate burst on an assigned big, not the low-priority claw scratch described below.

Wave 18 is a planned exception to “avoid extra bigs”: the ranger works through its ranged bigs, often west first and south/east later while wave 19 or 20 is current. Wave-19 west magic big is a repeated Eye assignment. Budget the resulting splits together with the mage's actions. Do not interpret a simple no-bigs rule as the pre-cap strategy.

**From wave 20 onward:** the larger cap allows faster big-to-split conversion. Preserve the wave-20 west, wave-23 south, wave-24 west, wave-25 south and wave-27 south opportunities, and attack fresh ranged splits between them. Wave-25 south big is often finished in wave 26 or 27; it is not a universal first-tick prefire. The mage handles many complementary magic/melee bigs. A ranger should not chase those bigs merely because the room cap has increased.

**During wave 28:** work to admit wave 29, rather than spending cooldowns on old crabs solely to make every earlier wave stall-free. Two or three retries can be acceptable under the requested strategy. Keep the scheduled south flickers and clear young splits that are holding population up. This is a policy tradeoff, not a claim that every recording has exactly that stall count or that a stall should be deliberately created.

### 4. Cover assigned magic and fresh magic splits with the Eye

“Ranger” is an ownership role, not a ban on magic. Eye of Ayak attacks are a substantial part of the observed workload. In particular, the ranger often covers the west wave-13 magic small, south wave-14 magic small, east wave-15 magic small, and west wave-19 magic big. Handle these within the local lane packet; there is no reason to wait for the mage to cross the room to do it.

Outside those packets, use the Eye on young magic splits when there is no better ranged work and its three-tick cooldown fits the next reservation. If a flicker is about to expose the target to ranged, waiting for the correct planned shot can be better than starting an off-style cooldown. Read the current style and the known spawn identity; initial color alone is insufficient.

### 5. Scratch nearby melee only when it fits

Use ordinary claws on nearby melee smalls as filler when there is no assigned packet, recent ranged work, or useful Eye work to do, and the four-tick cooldown will not block the next reservation. Do not cross the room to scratch an old melee. The opening wave-2 south scratch and the wave-6 east-big **special** are explicit scheduled exceptions; they belong to the rotation, not this fallback.

## Prayer upkeep and protection

These upkeep rules are the requested agent policy, added to the recording-derived target rotation. Keep the offensive prayer active throughout the room: Rigour for ranged attacks, Augury for Eye attacks, and Piety/Zeal for claws. Change it when making an attack in another style, rather than when preparing a weapon during cooldown. Leave prayers on during movement, idle ticks and attack cooldowns; do not flick them off to save points.

Drink a carried prayer potion at **20 prayer points or below** by default, provided the missing points can accommodate a dose. The threshold is configurable. Finish partly used bottles first and leave at least three ticks between dose requests. Potion restoration runs alongside the rotation and does not replace a scheduled attack. If supplies run out, report that in the sidebar and continue the room.

Maintain one protection overhead against living **fighting** nylocas targeting this player and currently able to hit, using their current attack style. Prioritize the style with the most big attackers, then the most small attackers. On equal threats keep the existing overhead; keep it on between attacks and when a threat disappears. Ignore nylocas targeting the partner, pillar attackers, out-of-range or obstructed attackers, dying bodies, and this server's late melee flickers that chase without attacking. An old crab ignored for final-cleanup attacks can still require protection until it expires.

## Cooperation and movement

Treat the following as complementary defaults, subject to visible misses, changed styles and partner state. They are supported by the [independent raw replay check](research/nylocas/ranger-event-interpretation.md), not a strict never-help rule.

| Ranger | Mage complement |
| --- | --- |
| Opening west ranged, then east ranged | Opening south magic, then west magic |
| Wave-5 west ranged big | Wave-4 south magic big and wave-5 east magic small |
| Wave-7 south ranged big | South magic small and other off-style work |
| Wave-14 south pair; wave-15 east pair | Wave-14 west magic; wave-15 west-side work |
| Wave-19 west big; wave-20 west ranged flicker | Wave-19 east/south work; wave-20 south big |
| Wave-23 south big then west pair | Complementary bigs and splits |

Do not double up on a target already expected to die from the partner's in-flight attack. Re-evaluate if it survives, the partner changes task, or room state requires help. Parent links can inform split ownership but are inferred and sometimes missing; they must not be the sole way to identify a fresh split.

The movement policy is **stage, attack, reposition during cooldown**. Recorded tiles use the normalized source map, north is increasing `y`:

| Location | Observed use |
| --- | --- |
| `(3292,4249)` | Opening west bow |
| Around `(3298,4249)` | Opening east bow |
| `(3295–3296,4243–4245)` | Early south packet and south-prefire setup |
| `(3301,4248)` and adjacent tiles | Early east chins; wave-21 east pair |
| `(3293,4248)` | Especially stable base through waves 18–20 and much of 29–30 |
| `(3291,4248)` | West wave-23/24 packet and later west flickers |
| Around `(3295,4248)` | Central split clearing, especially waves 25–27 |
| Northward toward remaining young splits | Final cleanup after the lane packets |

These are observed staging positions, not immutable movement waypoints. Choose a nearby walkable tile that supplies range/line of sight, minimizes travel, and preserves the next attack window. Do not chase a dying crab into a lane. Movement intent and exact click destinations are not recorded; attack tiles and the tick-position trace are.

## Wave 29 and final cleanup

The agreed policy is to **preserve fresh lane attacks, then apply the birth-wave ≤29 cutoff in final cleanup**. Wave 29 spawning is not the moment to cancel its west flicker packet. Finish the wave-29/30/31 lane work, including early wave-30 big kills that bring their children forward, before switching to the cleanup filter.

When the final lane packet is handled, ignore surviving NPCs born in waves 1–29 and allow them to expire. Kill young NPCs born in wave 30 or later. First resolve any surviving young big assigned to you, so its children are born early; coordinate with the mage on other bigs. Then prioritize young ranged smalls/clusters, Eye young magic, and complementary melee work as needed. Recompute this set on every split birth. A child has **its own birth wave and age**: a fresh split from an old big is not old merely because its parent is old. Conversely, a child born during wave 29 remains in the ignored set even if its parent was a different wave.

The recordings support the fresh lane packets strongly but also contain occasional old-target attacks late in cleanup. The cutoff is the chosen simple agent policy, not an invariant copied from every human action. Do not chase a zero-target screen: old crabs still have to expire, big expiry can create new children, and the actual cleanup marker—not the last attack—determines when the boss-spawn wait begins.

## Minimal state needed by an agent

Track actual last-spawned wave and its tick; room-cycle phase; next lane reservation; attack-ready tick; player position; teammate target/in-flight work; every crab's stable identity, birth tick, birth wave, current style, size, lane/split origin, estimated HP and removal state; and whether the final lane packet is complete. Also track current prayer points, the last offensive prayer, selected protection style, fighting nylocas' player targets and reachability, and the next permitted potion dose tick.

On each decision: discard removed, wrong-style, already-lethally-covered and cleanup-ineligible targets; execute a due reservation; otherwise consider recent ranged smalls/clusters, phase-appropriate assigned bigs, Eye work, then local scratches. For every optional action, reject it if it would miss the next reservation after movement. A reserved target already killed by the partner should be skipped, not waited on or attacked by stale identity.

This is enough to implement the first policy without encoding an exact human tick script. The corpus validates that its rotation anchors and staging tiles were used; it does not establish counterfactual room times, exact target-value weights, or optimal choices under every random split/partner outcome.
