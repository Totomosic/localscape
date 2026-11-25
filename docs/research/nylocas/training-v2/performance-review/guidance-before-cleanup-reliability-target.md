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
