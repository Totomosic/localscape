# Nylocas training — manual guidance

This file is for Jordan to edit. A continuing agent should read it before making changes and preserve these entries when updating the training handoff. Blank fields mean no additional guidance has been supplied.

Training is stopped until Jordan explicitly asks to resume. Adding observations here does not restart training. On resumption, read the ranger, mage and shared observations below before choosing changes, turn concrete mistakes into reproducible checks where possible, and report which observations each change addresses.

## Common mistakes noticed

Record what happened and what should happen instead. A wave, role, recording, seed or timestamp helps turn an observation into a reproducible check.

Add rows freely; short notes are enough. Useful details include the lane, big/small or split, birth wave, weapon, remaining HP, and whether the partner was a human or the other bot. Expected behavior and evidence can be left blank when uncertain. Use priority values such as high / medium / low.

### Ranger observations

These mistakes are mainly taken from my observation as a mage running with a ranger bot. Guidance is mostly intended for the ranger bot.

| Wave / phase / role | Mistake observed | Expected behavior | Example / seed / recording | Priority |
|---|---|---|---|---|
| 1 ranger | not instantly tbowing the east 2 ranger, should tbow west 1 then bow east 2 | the early rotation IS ALWAYS bow west, bow east, chin south, claw scratch south, blowpipe big west then small west | | |
| 6 ranger | tbowed the small 6 south, never do this, either chin it or blowpipe it. Tbowing the wave 7 big behind it is acceptable | | | |
| 11 ranger | if the 11 mage big is alive when wave 12 is entering the room, the ranger should back it up and hit it while the mager is scything the melee nylos | | | |
| 5 | did not kill wave 5 west big | always kill this ASAP | | |
| 30 | did not chinchompa the south 30s as they flickered | you will blowpipe the 30 west big as it flicks range, manually path towards the middle of the room to gain line of sight and chin the south 30 smalls as they flicker range | | |
| 24 | incorrect 24 west big procedure | you should blowpipe it when in range and then eye of ayak it when it flickers mage only if required and not dead to the blowpipe already | | |
| 31 | Did not focus 31 rangers | 31 rangers turn melee, they must die before this happens | | |
| | | | | |
| | | | | |

### Mage observations

| Wave / phase | Mistake observed | Expected behavior | Example / seed / recording | Priority |
|---|---|---|---|---|
| 4 | did not ayak the wave 4 big again after it lived the first hit | must always kill this big ASAP | | |
| 19 | hit the south big 19 before the east | focus the east big first since it is a fighting big | | |
| 26 | kill the 26 west big before the 27 west big, only kill the 27 west big if you are ahead and it is still in the lane or just entered the room | | | |
| 11 | hit the 11 big before the south 11 smalls | hit the south 11 smalls once first then hit the big 11 so that it splits after wave 13 spawns | | |
| 23 | did not backup the south 23 big when the ranger didn't 1 shot it | the 23 south big must die, it is the mager's job if the ranger doesn't kill it | | |
| 24-26 | not killing bigs | the assigned bigs: 24 south, 24 east, 24 west, 25 west, 26 west must all die | | |
| 3 | hit the wave 3 melee small | leave this and hit the 4 instead followed by the 5 south | | |
| | | | | |

### Shared / coordination observations

Use this for doubling up, missed partner assistance, target ownership, movement conflicts or problems involving both roles.

| Wave / phase | Role(s) / partner human or bot | Mistake observed | Expected behavior | Example / seed / recording | Priority |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |
| | | | | | |

### Quick notes while watching

Use these for observations that are easier to describe outside a table.

- Ranger: NEVER EVER TWISTED BOW A SMALL NYLOCAS (except wave 1 and 2). The 30 south chin is very important to having a fast cleanup. 
- Mage: Manually path between attacks so that you are within line of sight when off attack cooldown. Plan ahead what you will hit next and start moving toward it/into line of sight of it.
- Shared: at least in spectate mode they both summon their thralls outside the room before entering. Both roles should never spellbook swap to re-summon their thralls, only for death charge. Only spellbook swap after a 5 tick attack to not lose ticks. If there is only 1 nylo left, no matter what style it is both players should hit it. Move closer to the nylocas you want to attack if you don't need to be somewhere specific to reduce projectile delay on your ranged weapons.

ENSURE MAGE WEAPONS ARE ON LONG RANGE, RANGED WEAPONS ON RAPID ATTACK STYLES.

### Existing general observations

Additional observations:

A lot of wave 8/10 stalls which should not happen very often. Likely need to focus on killing bigs as soon as possible, killing new splits from those bigs and not doubling up.

Need to help out your partner if they are behind in their rotation due to bad damage.

VERY VERY IMPORTANT - heavy focus on newer nylos instead of older one, especially pre-cap it is fine to leave wave 1 or 2 crabs if there are new splits about to spawn. In cleanup, also prioritise newer nylos first.

## Goals and priorities

What should the next training session focus on?

### Pre-cap parent timing and P95 goal, 26 November

- The mage should attack the wave-8 west big on its spawn tick and take the next available hit if it survives.
- Clear both original wave-15 east smalls, then have the ranger kill the wave-14 east ranged big and the mage kill the wave-13 east melee big. A small with a confirmed lethal hit already in flight is covered. Each role should finish a surviving parent in combat. Waiting for natural expiry is a failure.
- Use P5 and P95 for the lower and upper reported boss-spawn times. The current upper target is **3:16.8** (328 ticks). Also count every trial that meets the deadline and report wave-19 failures separately.

- Highest priority: Pre-cap and cleanup
- Next priority: Coordination between players, even before the projectile has landed, both players should know if the nylocas is dead or not.
- Strategies or packets that must be preserved:
- Changes to avoid:
- Acceptable tradeoffs between speed, consistency, survival and human compatibility:

## Performance metrics to look for

The existing brief's targets are included as a starting point. Fill in an override or add rows as needed. One game tick is 0.6 seconds.

| Metric | Existing target | My target / clarification | Cohort size / acceptable frequency |
|---|---|---|---|
| Average boss spawn | 315 ticks / 3:09 | | |
| Common boss-time band | 3:07–3:12 | | |
| Slow rooms | Above 3:14 should be rare | | |
| Pre-cap stalls, last-spawned waves 1–19 | Usually ≤1; occasionally 2–4 | | |
| Transition | All 18/19 bigs and south/west 20 bigs dead before 21; no 21 admission stall | | |
| Stalls after last-spawned waves 21–27 | Almost always zero | | |
| Wave-28 stalls | Usually 2–3 | | |
| Deaths / completion rate | Reliable normal-99-HP duo partner | | |
| Wasted overlapping attacks / lost ticks | Minimize while preserving key packets | | |
| Making expiry | N/A | Rarely with 2 28 stalls, common with 3 28 stalls, very common with more than 3 28 stalls | varies |
| | | | |

Making expiry means that the last crabs to die are the ones that spawn on wave 29 that are avoided. This is a good sign, ideally the players kill all crabs >w29 and the last to die are the old nylocas. This is a sign that the cleanup strategy is good, there is little doubling up, etc.

## Strategy corrections or examples

- Recording links / seeds to study:
- Ranger guidance: pre-cap - focus on finishing off nylocas, if you hit a nylocas and it doesn't get 1 shot 99% of the time hit it again until it dies.
- Mage guidance:
- Cleanup guidance: So the goal is to spawn wave 29 quickly (2 or 3 wave 28 stalls) and then also spawn wave 30 quickly (since it has a big that needs to die), stalls after wave 30 are not very relevant to overall time. It is fine to hit some range nylos from wave 29 to try and push to spawn wave 30 faster.
- Guidance for partnering with a human:
- Things done well: transition seems mostly good, no wave 19 stalls, bigs popped, no 21 stalls, no stalls 21-27.

## Decisions for the next session

- Gear changes permitted, if any:
- Preferred experiment / hypothesis:
- Anything the continuing agent should ask about before proceeding:

See [the resume handoff](nylocas-training-resume.md) for the current implementation, measurements, known bugs and reproduction commands.


## Additional observation during the paused review — 25 November 2026

Jordan: “if there is a nylo alive sometimes the bot will just not hit it”.

Treat unexplained refusal to attack a living, legally serviceable nylo as a priority correctness issue, especially during cleanup. Review whether a ready bot attacks or starts moving into a legal attack position when fresh wave30/31 targets remain. Inspect partner assistance as well as own-role targets, and investigate whether ownership filters, ended assignment windows, stale commitments or movement/return estimates suppress useful work. These are hypotheses to check against native evidence.

Preserve the explicit actual31 cutoff for births29and earlier, normal NULL restrictions, native expiry, cooldowns, line of sight, finite supplies and named packets. A target covered by already-rolled lethal damage is different from an uncovered survivor. For every apparent refusal, record the target identity/HP/birth, both bots' readiness and selected actions, queued damage and the precise blocking reason; verify the reason instead of assuming that the target is unserviceable.

Reusable review question: **When a nylo remains alive, why is neither bot attacking it or moving to attack it? Is that a real native restriction, or an unnecessary policy filter?**

This observation was added during the paused review. It does not restart training.


## Partner NULL and final-target coordination — paused review, 25 November 2026

Jordan: “Each bot needs to track if the other player NULLs on nylo and focus on that. It is fine to double up if it is the last nylo alive”.

- Both roles must track the partner's actual NULL state separately for each NPC lifetime, including when partnering with a human. A zero damage roll does not establish NULL; flickering style does not clear a lifetime's NULL state.
- If the partner is NULL on a living target and this bot can still damage it, treat that target as priority rescue work. Cover bigs and smalls, and cross ordinary role ownership where needed. A stale ownership, assignment or partner-flight claim must not leave it abandoned. Check actual queued damage and legal movement/attack opportunities.
- When exactly one living nylo remains, double-ups are acceptable. Ordinary anti-overlap/partner-ownership rules must not by themselves keep the other bot idle. Count all living NPCs when determining whether it is the last, including otherwise ignored older births; zeroHP/death-queue bodies are not living targets. Preserve this bot's own NULL restriction and the existing actual31 old-birth cutoff; this clarification permits cooperative attacks without changing native combat rules.

Review both directions: Ranger NULL → Mage rescue, and Mage NULL → Ranger rescue. Record time from NULL becoming visible to the partner's first legal rescue attack or approach, and record any unexplained idle delay. Separately test the last-target exception with a partner's unknown/nonlethal flight and with a queued lethal hit, so permission to double up is not confused with requiring an unnecessary attack.

This clarification was recorded while training is paused; it does not resume training.


## Cleanup reliability target — paused review, 25 November 2026

Jordan: “I think the biggest difference is going to be cleaning up these slow boss spawns which seems like a cleanup issue. Cleanup is a vital part of the room that saves or loses a lot of time. Work towards 95% of rooms being a boss spawn of 3:22 or less”.

Make cleanup the primary focus for reducing slow rooms. Add a reliability requirement: **at least95% of rooms spawn the boss by3:22 (202seconds)** under normal randomized damage, finite resources and the specified loadouts. Keep the original3:09average and zero-wave19stall goals alongside it. Evaluate this percentage over all trials; failures/timeouts cannot pass and wave19failure rooms remain in this denominator. The earlier instruction to exclude entire wave19failure rooms applies to the reported average, not to hiding reliability failures.

Review late fresh30/31survivors, making expiry versus wave28stall count, refusal to attack or approach eligible survivors, partnerNULL rescue and permissible final-target cooperation. Use normal native evidence; the always-max-hit setting remains a separate diagnostic.

Current retained500-room baseline:440/500(88%) meet3:22,60exceed it, P95is3:26.4. At the same500-room sample size, the95%goal requires475within target and at most25over. Of60late rooms,40have no wave19stall; cleanup remains a substantial independent review target. This note records the objective during the paused review and does not itself resume training.


## Metric correction for28+16dim — 25 November 2026

Jordan: “oh it will be very hard to get 3:02 with that 28+16 dim, don't use that metric”.

Do not use the3:02.4always-max-hit litmus as an acceptance or rejection criterion for28+16dim or comparisons involving it. Existing diagnostic runs remain historical evidence. Judge this dim using normal randomized boss-spawn averages/minimum/maximum, the95%of all rooms by3:22target, and dim-aware cleanup/expiry behavior. Intentionally dimmed targets are not policy refusal errors merely because they expire. No further max-hit diagnostic runs are required for this dim experiment.


## Timestamp dim boundary confirmed — 25 November 2026

Jordan confirmed **“Yes — later splits remain fresh”** when asked whether a split born at252after wave29spawn240remains a target after31. This supersedes the historical whole-wave29-label interpretation in earlier notes. A29dim ignores actualbirth≤actualspawn29; later29splits stay fresh. Apply both29and28+16dims using cutoff=max(actualspawn29,actualspawn28+16), inclusive, activated atactual31. Freshness applies consistently to ordinary selection, partnerNULL rescue, idle assistance, reservations and last-target cooperation. Literal-last still counts every living nylo, including dimmed bodies. Preserve ownNULL, knownlethal damage and normal combat/resource rules.

Historical whole-wave29measurements remain valid measurements of their saved policies but do not evaluate this clarified timestamp strategy. Test corrected29-only and combineddims with the same strategy/adapter/fixture and fresh random cohorts; do not use the3:02.4max-hit metric.


## Explicit wrapup and pause — 25 November 2026

Jordan: “ok lets start to wrap up, finish the current experiment and record a document for another agent to continue the training”.

The running mixed-cleave native200completed and its behavior was audited. No further training batch was started. Training is paused at Jordan’s request; resume only on explicit instruction. See [the latest continuation handoff](nylocas-training-handoff-2026-11-25.md) for retained source, normal average/minimum/maximum, pending candidates and evidence. The goals remain unmet. Preserve all earlier guidance; this entry changes training status, not the performance goals.

## Pre-cap target choice — 26 November 2026

Never hit the original wave-1 melee nylo. It is the small that spawns east in the north slot. During pre-cap, both players can spend one or two ticks waiting or moving into position for a new lane spawn or split when the alternative is a crab four waves old. This is especially important for the mage.

Jordan saw the mage run over to hit the wave-1 melee one tick before the wave-6 west magic small spawned. In that situation the mage should use the tick to get ready for the west small. Track the target of the approach as well as the eventual attack when checking this behavior.

## Boss-spawn reporting after the 26 November checkpoint

Report the mean, **P5** and **P95** boss-spawn times for completed rooms with known zero wave-19 stalls. Sort the individual room times across the cohorts being combined and use the nearest-rank 5th and 95th percentiles. One tick is 0.6 seconds.

Jordan accepted the observed **9/500 (1.8%)** wave-19 failure rate at this checkpoint. Continue to show the wave-19 count and the fraction of all trials reaching boss spawn by 3:22. The 3:09 target applies to the eligible mean. [Retained checkpoint](nylocas-training-handoff-2026-11-25.md)

## Ranger opening and south-30 chin — 26 November 2026

After the confirmed west-1 Twisted bow shot, keep the bow equipped and run east immediately. Use the five-tick bow cooldown to cross the room and bow the east-2 ranger on the first ready tick.

Reach (3291, 4248) before the west-30 blowpipe shot. Use its cooldown to run to (3293, 4249), which has line of sight to the east small on the first green tick. The tile one south blocks both shots on the packed map. Throw one black chin at the original south pair as they turn ranged. Jordan explicitly wants this scheduled throw even when wave 31 has already dimmed the pair. Either green small can be the primary. If both already have lethal hits in flight, use the tick on another target. After the throw or the end of the flicker, the usual dim rules apply to survivors.

Eating a manta on the first green tick can consume the entire throwing window. The ranger waits one tick to eat when the named chin is ready, it has 60–65 HP and prayer, and damage already queued through the following tick leaves at least 55 HP. At lower health or with more incoming damage, it eats immediately.

When reviewing a room, follow the two original south crabs across the wave-counter change. Check the throw while both are green, then check the two hitsplats and remaining HP after projectile flight. [Ranger replay and counts](research/nylocas/training-v2/session-2026-11-26/ranger-opening-south30.json)

## Pre-cap movement and assigned bigs, 26–27 November

- **Mage before cap.** When a fresh spawn or split is one or two attack-ready ticks away, spend those ticks getting into position. An untouched ordinary small from two waves ago can wait. A due packet or an immediate cap-clearance duty still takes its own tick. Reset the two-tick allowance on a confirmed attack.
- **Ranger before cap.** Move for the next spawn or split during cooldown. A one-tick wait can pay off. If a healthy old target is already in clear blowpipe range and the fresh target is two ticks away, fire now. The blowpipe will be ready again for the fresh target.
- **Both roles.** Plan the next firing square before the weapon is ready. Use ground clicks around the pillars until the target is in sight and can be reached in the launch cycle. A visible incoming nylo can close the final tile during that cycle.
- **Mage after cap.** Finish the assigned originals promptly, starting with south 20 during the transition. Finish south 23 if it survives the ranger, all three 24s, west 25, west 26, south 29 and east 30. Continue on a bad roll. If a Dinh would permanently null an uncovered assigned magic big, finish it first. Late assigned parents retain their recovery across the wave-31 dim.
- **West 27 is optional.** Leave it to expire unless the team is very far ahead and every other nylo is already dead. A lethal hit still in flight on another nylo is too early. The mage may take west 27 only while it is alone and still in the west lane or just crossing the doorway. The policy closes that window after age nine even if it appears close to the door. The ranger leaves this big to the mage.
- **Ranger on wave 30.** Keep the west blowpipe and the first-green south chin. Immediately after the chin, the original west-30 big has first claim on every attack it still needs, including during wave 31. Move west during the chin cooldown. Once the big dies or a lethal hit is on the way, resume cleanup.

For the first-green chin, the current ranger can defer a manta for one tick when it has prayer and damage already queued through the following tick leaves at least 55 HP. The same floor applies if it starts the tick at 59 HP. This updates the earlier 60–65 HP condition.

## Wave-12 Dinh setup, 27 November

Both players help keep the original south-12 melee big clear of the mage's Dinh special when practical. An occasional kill is acceptable.

- Before the big settles, kill any crab occupying the east side of the southwest pillar so the big can settle one tile north.
- Line up on the southern east-lane row at (3301, 4248) to scythe the southern small and cleave its northern sibling. After the swing, move diagonally northwest. Both players should reach the northern line of the east and west lanes before Dinh's. This draws the aggressive west-12 small one tile north. Use it as the Dinh primary and keep the south big outside the sweep when practical.
- Check both player positions, the big's settled square and the aggressive small's position at launch. Count how often the Dinh clips the big, how often it kills the big, and how often the scheduled special fires. A low roll on the big still counts as a clip.

## Wave-22 Dinh timing, 27 November

When small magic nylos remain south, the mage can spend a few ticks killing them before using Dinh's. If the wave-22 melee big walks near the usual Dinh tile, it can serve as the primary for the delayed sweep. Use the normal timing when the extra shot and later sweep offer little.
