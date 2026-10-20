# Nylocas lane split placement verification

Verified 2026-10-20 using all 13 checksum-pinned normal-mode solo/duo Blert
recordings. The reported symptom is supported: **horizontal-lane splits first
appear closer to the arena than the simulator's fixed SW/NE pair.** This is a
spatial discrepancy even when the parent's final anchor is held constant.

`NylocasBehaviour.split` creates children at `parent.coords` and
`parent.coords.translate(1, 1)`. Their AI starts on a later tick; `tick` also
rejects processing on their birth cycle. The existing splitting tests explicitly
expect this unshifted pair when the parent disappears. The comparison below
uses that strategy, rather than replaying player combat inputs through the
simulator.

Across 559 bigs, 553 have two recorder-associated children. Of those pairs,
527 match the fixed pair and 26 differ. The differences appear in ten recordings;
22 occur on ticks with only one big removal anywhere in the room. The other six
parents have incomplete recorder associations and are excluded from pair counts.

| Parent's final position | Recorded child offsets from final SW anchor | Pairs | Fixed pair matches? |
| --- | --- | ---: | --- |
| West lane, x3285–3288 | (1,0), (2,1) | 12 | No: both children are one tile farther east |
| West entrance, x3289 | (1,0), (1,1) | 6 | No: southwest child is one tile farther east |
| East lane, x3302–3303 | (-1,0), (0,1) | 3 | No: both children are one tile farther west |
| East entrance, x3301 | (0,0), (0,1) | 5 | No: northeast child is one tile farther west |
| South lane, y4238–4242 | (0,0), (1,1) | 20 | Yes |
| Other arena pairs | (0,0), (1,1) | 507 | Yes |

“Lane” in the totals means the final anchor is outside x3290..3301,
y4243..4254. Thus there are **41 lane deaths: 18 west, 3 east, 20 south**.
The five east-entrance cases are counted as arena deaths. All 26 mismatches
have y4248 as the parent's final row.

The horizontal observations are consistent with each child taking one step
toward its lane entry on the birth tick, stopping at x3290 from the west or
x3301 from the east. This is a description of the observed positions, **not proof
of the internal spawn/movement mechanism**. South-lane observations do not show
the analogous northward shift. A universal extra parent step or a universal
one-tile child offset would therefore be an incorrect conclusion.

## Exact replay checks

An unambiguous west-lane example is parent **61360**, wave 8, in
[1073635f](https://blert.io/raids/tob/1073635f-5326-4bac-a147-8fdf6299f761/nylocas):

| Tick | Parent anchor / HP | Split birth anchors |
| ---: | --- | --- |
| 55 | (3284,4248), HP 16 | — |
| 56 | (3285,4248), HP 0 | — |
| 57–61 | (3285,4248), HP 0 | — |
| 62 | Removed at (3285,4248) | 61523: (3286,4248); 61524: (3287,4249) |

The fixed pair at that final parent tile would be (3285,4248)/(3286,4249).
Both recorded children are one tile closer to the arena. This parent never
takes a step after first zero HP, and it is the only big removed on tick 62.

The user's original
[Drummie/B asher recording](https://blert.io/raids/tob/c77441a7-3caf-4d9d-a482-8ffe8d7f293d/nylocas)
contains parent **41551**: first zero at tick 25, anchor (3286,4248); final step
to (3287,4248) at 26; removal at 32. Children **41694/41695** first appear at
(3288,4248)/(3289,4249) on 32. Accounting for the parent's final step still
leaves the additional one-tile child displacement.

For the opposite direction, parent **50208** in
[9f8f1a77](https://blert.io/raids/tob/9f8f1a77-a528-454f-bfac-8251dec9de32/nylocas)
first records zero at tick 197 at (3304,4248), reaches its final (3303,4248)
at 198, and is removed at 204. Children **50378/50379** appear at
(3302,4248)/(3303,4249), one tile west of the fixed pair. It is the only big
removed on that tick.

## What this resolves

1. **Split placement or movement during birth:** supported as the source of a
   discrepancy relative to a fixed final parent tile. The 26 offsets are
   directional, rather than unexplained random choices among six positions.
2. **Missing final parent movement:** insufficient to explain these pairs.
   Parent last-update, removal-event and metadata coordinates agree for every
   big; child event and metadata birth coordinates agree too. The discrepancy
   is measured after that final movement.
3. **Earlier lane movement or lethal-impact timing:** could cause additional
   differences in a particular local fight, but is unnecessary to reproduce
   this placement discrepancy. The comparison deliberately holds the final
   parent anchor constant and does not align player attack clicks with lethal
   server ticks.

All associated children in this analysis first appear on their parent's removal
tick. The mismatching parents have first-zero-to-removal delays of six ticks
(7 parents) or seven ticks (19 parents). These findings provide no reason to
change death-animation duration to address split position.

No gameplay sources were changed. Before implementing a placement correction,
distinguish a different creation tile from movement before the first client
observation, and preserve the south-lane controls and entrance stopping cases.
Blert infers parent identity by time and position; it does not expose server
creation coordinates, individual hitsplats, or processing phases. Simultaneous
big deaths remain a limitation, although the sole-removal fixtures independently
demonstrate the discrepancy.

## Follow-up: thrall range and overkill

The user identified mage/ranged thralls in the west or south lane as another
possible cause. These recordings cannot directly test that hypothesis: their
NPC events contain wave and boss IDs 8342–8357, without thrall positions or
thrall attack launches. A player's resurrection cast does not establish where
the thrall stood when it later fired, nor which impact killed or overkilled a big.

The current `Thralls.process` check is six tiles from the nearest occupied tile
of the target's full bounds, with line of sight. Thus, for a 2×2 big approaching
from the west or south, its SW anchor can be **seven** tiles away while the
nearer footprint edge is six tiles away. That distinction can make the range
look longer when measuring SW anchors. The chase/acquisition limit of 32 tiles
is separate from the six-tile attack check.

`ThrallsTest` now observes actual attack-launch checks against 2×2 targets for
both projectile styles, on all four sides. With the attack cooldown already
ready, each boundary fixture checks a six-tile edge distance and a seven-tile
edge distance before chasing can move the thrall. These tests characterize the
simulator; they do not independently measure OSRS's range rule.

Overkill does provide a mechanism for a parent to stop farther back: the
existing moving-big death probes retain a final step on an ordinary lethal
tick, while a qualifying same-tick extra hit starts death early and suppresses
that step. `NylocasOverkillTest` includes a positive thrall hit clamped to zero
after a player kill. This differs from a thrall delivering the lethal damage,
which does not qualify for the speedup. In-flight thrall projectiles can still
impact after a player kill, but `Thralls.process` rejects new attacks on a
zero-HP target. Late overkill shortens the existing animation rather than
moving the parent again.

Therefore thrall overkill can account for a missing final parent step **in the
simulator**, but its occurrence in the particular fight is unverified. Excessive
thrall range is also unverified against OSRS. Neither uncertainty removes the
separate fixed-pair discrepancy measured at the parent's final recorded tile.

## Reproduction and validation

```sh
python tools/research/nylocas_split_placement.py
```

This verifies the original-response SHA-256 checksums and regenerates
[split-placement-observations.json](research/nylocas/split-placement-observations.json).
The output includes every lane control, every mismatching pair, parent HP/tile
traces, child birth and next-tick observations, and simultaneous big-removal IDs.

The existing `NylocasSplittingTest` (11 cases) and
`NylocasDeathTimingReviewTest` (228 cases) suites also pass. They confirm the
simulator's fixed-pair and final-step behavior;
their passing assertions do not validate the shifted replay positions. Run from
`upstream/rsmod`:

```sh
JAVA_HOME=/home/jordan/osrs/.state/tools/jdk-21 \
GRADLE_USER_HOME=/home/jordan/osrs/.state/gradle \
./gradlew :content:areas:dungeons:nylocas:integration \
  --tests '*NylocasSplittingTest' --tests '*NylocasDeathTimingReviewTest' \
  --offline --console=plain
```

The follow-up passes all 47 `ThrallsTest` cases (including the 16 new launch
boundary fixtures), 57 `NylocasOverkillTest` cases and 41 `NylocasDamageTest`
cases, with no failures or skips:

```sh
JAVA_HOME=/home/jordan/osrs/.state/tools/jdk-21 \
GRADLE_USER_HOME=/home/jordan/osrs/.state/gradle \
./gradlew :content:skills:magic:utility-spells:integration --tests '*ThrallsTest' \
  :content:areas:dungeons:nylocas:integration --tests '*NylocasOverkillTest' \
  :api:combat:combat-scripts:integration --tests '*NylocasDamageTest' \
  --offline --console=plain
```
