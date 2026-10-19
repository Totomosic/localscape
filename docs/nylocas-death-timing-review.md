# Independent Nylocas death-timing review

Follow-up fix: the queued-future-hit behavior described below was corrected
using the user's clarified mechanic. Overkill now requires a positive hit
clamped to zero on the player-kill tick or during the death animation. Natural
zero/miss hits and thrall-lethal attacks do not qualify. In-flight impacts
during the animation can shorten the existing deadline once without restarting
the animation. See [current behavior](nylocas-despawn-verification.md).
The follow-up validation passes 682 targeted tests, including 57 dedicated
overkill cases and an actual summoned-thrall regression. The original 204-case
review suite now contains 228 cases; explicit natural-zero hits no longer
accelerate death, and future-hit expectations assert ordinary removal timing.

The remaining review text and its test counts describe the pre-fix audit.

Reviewed 2026-10-19. Ordinary death timing and natural-expiry timing match
Blert's published matrix. The simulator's complete overkill matrix is **not
independently verified**: small acceleration has supporting replay observations,
but stationary big removal at lethal+5 has no example in this corpus. Two queue
and movement edge cases also make the earlier blanket “one tick sooner with an
extra hitsplat” description incomplete. No production behavior was changed.

## Evidence and method

Read the current [mechanics guide](https://blert.io/guides/tob/nylocas/mechanics#death-timing)
and its [complete appendix source](https://github.com/blert-io/blert/blob/e5f44caceebd3a0979c9a89742f964f65841b670/web/app/guides/tob/nylocas/mechanics/page.tsx#L421).
The current `main` source and the pinned source were byte-identical when fetched
for this review (SHA-256
`be9f2972569c87ca58e8aeb01f29ed63721f36d271d5b4f6b55b3ddbc56ed87d`).
The appendix specifies ordinary kills and expiry; it does not specify overkill.

Independently analyzed every spawn, HP update and removal in the 13
[checksum-pinned recordings](research/nylocas/manifest.json): 2,678 regular
Nylocas, including 559 bigs and 2,119 smalls. The analysis uses the shared
checksum loader but does not use the existing lifecycle-analysis logic or its
generated summaries. A fresh fetch of the
[1073635f recording](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas)
metadata and events matched both manifest hashes exactly.

Read the [recorder's hitsplat handler](https://github.com/blert-io/plugin/blob/9d11b1f7bb8f2677d39a56927cd8a255d8995e1c/src/main/java/io/blert/core/DataTracker.java#L854).
It updates estimated HP from hitsplats. The saved events include attacks and
HP snapshots, but omit individual splats and death-animation starts. An attack
with a multi-hit weapon does not identify which splat killed. Coordinate changes
do not expose the server's walking flag. Consequently, first-zero-to-removal
measurements are kept separate from server-impact-to-removal measurements.

The new [independent integration probes](../upstream/rsmod/content/areas/dungeons/nylocas/src/integration/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasDeathTimingReviewTest.kt)
observe actual hit impacts, animation starts and NPC deletion on the same live
`MapClock`, without the existing tests' end-of-tick clock adjustment. They also
check final positions, clamped damage and hit order. Production sources were
inspected alongside the probes; simulator characterization assertions are not
treated as independent evidence of the real game's overkill rules.

## Findings

### Stationary big overkill remains uncorroborated

The simulator removes a stationary big at impact+5 after a lethal-first two-hit
attack. All 393 bigs with recorded zero HP instead have first-zero-to-removal
delays of 6 or 7 ticks (108 and 285 respectively); none has a 5-tick delay.
There are 72 big observations with sulphur/scythe attacks one tick before the
first recorded zero: 52 have delay 6 and 20 have delay 7. Fifteen of these have
five consecutive unchanged anchor snapshots ending at first zero; their delays
are 6 for twelve and 7 for three.

A particularly useful follow-up fixture is room ID **64483** in
[1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas).
This pillar-directed big holds `(3298,4243)` from tick 265 onward, has 1 recorded
HP at ticks 269–273, receives a sulphur-blades attack at 273, first records zero
at 274, and despawns with its children at 280: a 6-tick delay. Other low-HP,
stationary scythe candidates are room ID 47925 in
[65c9f142](https://blert.io/raids/tob/65c9f142-f240-4284-9384-d32fa313067e/nylocas)
(2 HP, zero 65, removal 71) and room ID 35937 in
[eb3793f3](https://blert.io/raids/tob/eb3793f3-ce4a-4fcf-8ef9-e2098d39914a/nylocas)
(1 HP, zero 340, removal 346).

This is an evidence gap and a reason to investigate, **not a proven timing
defect**: the raw splats are absent, so these could be lethal-last attacks.
The guide does not establish a big-overkill exception either. A recording with
visible, tick-aligned individual splats on a stationary big is needed before
claiming that impact+5 matches the game. The relevant implementation is
`NylocasDeaths.die`, which delegates to the shared queue/death machinery.

### Fixed: a future queued hit accelerated death without a second hitsplat

Queue one lethal hit for the next tick, then another hit for ten ticks later.
Only the lethal hit impacts before deletion, yet removal occurs at +1 for a
small, +5 for a stationary big, and +6 for an already-moving big. All four
size/movement combinations reproduce this behavior.

`NpcQueueProcessor.publishExpiredQueues` revisits the queue when pending work
remains, including future work. That can run the newly added death queue in the
lethal tick. Thus the observed acceleration depends on queue contents and order;
“additional hitsplat after lethal damage” describes one trigger, not the whole
rule. Compatibility of the future-hit case with the real game is unverified by
these recordings. The verification document now makes this distinction.

### Movement transitions invalidate a universal one-tick speedup

`StandardNpcAccess.arriveDelay` reads `hasMovedPreviousCycle`. Ordinary death
and accelerated death enter that function on different ticks. The following
boundary probes are reproducible simulator behavior:

| Big's movement setup before impact | One lethal hit | Lethal then extra hit | Difference |
| --- | ---: | ---: | ---: |
| Stationary for preceding ticks | +6 | +5 | 1 tick |
| Walking continuously | +7 | +6 | 1 tick |
| First walk is queued for the impact tick | +7 | +5 | 2 ticks |
| Finished a one-tile walk on the preceding tick | +6 | +6 | 0 ticks |

In the first-step case, the accelerated death stops the pending walk before the
NPC takes its first step. In the just-arrived case, the early handler still sees
the preceding step, whereas the ordinary handler sees a stationary preceding
cycle. Smalls remain +2/+1 across these transitions. The old eight-row matrix
is valid for steady movement; it is not a complete movement-state specification.
The external evidence does not resolve these transition branches.

## Verified scenario results

Offsets below are from the actual lethal impact, not the attack click. “Extra”
means another same-tick queued hit after the lethal one, including an explicit
zero. The overkill column records simulator behavior; it is not a guide rule.

| Size and steady movement | Ordinary removal | Extra-hit removal | Ordinary animation starts |
| --- | ---: | ---: | ---: |
| Small, stationary | +2 | +1 | +1 |
| Small, walking | +2 | +1 | +1 |
| Big, stationary | +6 | +5 | +1 |
| Big, walking | +7 | +6 | +2 |

The ordinary column and animation starts agree with the guide. A single hit
four times the target's HP produces ordinary timing. Two hits where the last
one kills also produce ordinary timing. A zero before the lethal hit does not
accelerate death. A zero after it, lethal-middle-of-three, and three extra hits
all produce the accelerated timing; extra hits do not stack further speedups.
These checks cover all three combat styles and all twelve ordinary/aggro forms.
Small and killed-big animation lengths remain one and five ticks respectively.
Ordinary walking deaths retain one final step on the lethal processing tick;
the same-tick accelerated cases stop that step.

Natural-expiry tests agree with the guide after converting its inclusive
lifetime numbering to zero-based ages: small animation/removal at 51/52, big at
52/55. The recordings independently reproduce removal at age 52 for all 778
smalls without recorded zero HP and age 55 for all 166 corresponding bigs.
Existing split tests cover children appearing with parent removal; the real
wave-21 combat regression covers stationary/moving sulphur-blades kills and
their effect on wave-22 admission.

Replay distributions independently reproduced:

| Size | First zero to removal | NPCs |
| --- | ---: | ---: |
| Small | 1 | 237 |
| Small | 2 | 1,104 |
| Big | 6 | 108 |
| Big | 7 | 285 |

Small overkill has useful supporting observations: 72 of 91 sulphur-blades
attacks occurring one tick before first zero have a 1-tick zero-to-removal
delay, while the other 19 have a 2-tick delay. None of the 47 analogous small
scythe observations has a 1-tick delay; scythes hit a small once. This pattern
supports lethal-hit order as a cause of small acceleration, without proving the
individual splat order for any one observation.

## Reproduction and validation

```sh
python tools/research/nylocas_death_timing_review.py
```

This verifies every original-response checksum and regenerates
[death-timing-review.json](research/nylocas/death-timing-review.json), including
raw tick/position/HP traces and all stationary multi-hit big candidates.

From `upstream/rsmod`, with the repository's JDK and Gradle cache:

```sh
JAVA_HOME=/home/jordan/osrs/.state/tools/jdk-21 \
GRADLE_USER_HOME=/home/jordan/osrs/.state/gradle \
./gradlew :content:areas:dungeons:nylocas:integration \
  :content:areas:dungeons:nylocas:spotlessKotlinCheck \
  :api:combat:combat-scripts:integration --tests '*Nylocas*' \
  :api:game-process:integration --tests '*NpcQueueProcessorTest' \
  -PspotlessIdeHook=/home/jordan/osrs/upstream/rsmod/content/areas/dungeons/nylocas/src/integration/kotlin/org/rsmod/content/areas/dungeons/nylocas/NylocasDeathTimingReviewTest.kt \
  --offline --console=plain
```

The new suite contains 204 cases: 192 style/form/movement/hit-order combinations,
four future-hit probes, and eight movement-transition probes. All 541 room
tests, 54 Nylocas combat tests and two shared queue tests passed: **597 cases,
zero failures or skips**. Formatting checks pass for the added Kotlin file.
Unrelated formatting changes produced by the module-wide formatter were
reverted; existing source formatting is preserved.
