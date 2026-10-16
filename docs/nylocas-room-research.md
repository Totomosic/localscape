# Nylocas room: map and RuneLite compatibility

Reviewed 2026-10-15 against the source revisions below. This is a room-access foundation, not an implementation of the waves, pillars, boss, raid party, or encounter timers. Region detection can work in an empty room; encounter overlays require the real NPC and state signals described below.

## Primary sources reviewed

The RuneLite Plugin Hub descriptors identify these upstream repositories and commits:

| Plugin | Descriptor | Reviewed implementation |
| --- | --- | --- |
| ToB QoL (`tobqol`, by damencs) | [Plugin Hub descriptor](https://github.com/runelite/plugin-hub/blob/master/plugins/tobqol) | [damencs/tob-qol@3715d6ae24547fa87dc7ed82665e37b632407ba7](https://github.com/damencs/tob-qol/tree/3715d6ae24547fa87dc7ed82665e37b632407ba7) |
| Nyloer | [Plugin Hub descriptor](https://github.com/runelite/plugin-hub/blob/master/plugins/nyloer) | [EIKOOT/nyloer@9b400ec733fb7d83c261affee4b8edf4feb3fb5f](https://github.com/EIKOOT/nyloer/tree/9b400ec733fb7d83c261affee4b8edf4feb3fb5f) |
| Nylo Death Indicators | [Plugin Hub descriptor](https://github.com/runelite/plugin-hub/blob/master/plugins/nylo-death-indicators) | [InfernoStats/Nylo-Death-Indicators@1b29a76e1799cf813299bfdc75e12251028643ae](https://github.com/InfernoStats/Nylo-Death-Indicators/tree/1b29a76e1799cf813299bfdc75e12251028643ae) |

The descriptors are moving references; the reviewed implementation links are immutable. “ToB QoL Improvements” is a different plugin and was not substituted for `tobqol`. Repository code establishes protocol expectations, not a guarantee that the latest Plugin Hub binary runs on this workspace's older RuneLite 1.11.19. End-to-end plugin behavior still needs a running client test.

## Map identity and instancing

Use source map-square **13122**, map-square coordinates **(51,66)**, origin **(3264,4224)**, plane **0**. Its 8×8 source chunks start at **(408,528)**. These coordinates come from ToB QoL's `Region.NYLOCAS`; the origin is calculated from its region ID. Its entrance timer boundary is the source rectangle **x3304, y4274–4277, plane0**. That boundary is a timing signal, not proof that it is a safe teleport tile. [Source: Region.java](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/api/game/Region.java).

| Plugin | Room detection | Static original map sufficient? |
| --- | --- | --- |
| ToB QoL | Requires `WorldView.isInstance()` and resolves the player's source region using `WorldPoint.fromLocalInstance`. | **No.** Static teleports return unknown region. |
| Nyloer | Checks whether the loaded map regions contain 13122, refreshed every five game ticks. | Yes for region detection. |
| Nylo Death Indicators | Checks whether the top-level world's loaded map regions contain 13122 after entering `LOGGED_IN`. | Yes for region detection. |

Sources: [ToB QoL region gate](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/api/game/Region.java), [Nyloer detection](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/NyloerPlugin.java), [Death Indicators detection](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NyloDeathIndicatorsPlugin.java).

**Implementation consequence:** use a dynamic instance assembled from authentic Nylocas source chunks, retaining their relative positions and rotation zero. Physical instance coordinates can differ; the client template must resolve them back to the original source region and positions. An arbitrary blank room elsewhere, or merely loading region13122 nearby, does not satisfy ToB QoL's player-location gate. Copying the room also preserves the region-local spawn positions used by both wave counters. RuneLite's `fromLocalInstance` decodes source chunk coordinates and rotation from the dynamic template. [Source: RuneLite WorldPoint.java](https://github.com/runelite/runelite/blob/3202bfde4a270c086587cfdb90d61220314e7bc4/runelite-api/src/main/java/net/runelite/api/coords/WorldPoint.java).

The teleport destination must be checked against the actual cache's terrain and collision. Plugin source alone cannot certify a walkable tile. Existing map terrain and object placement are separate from server-scripted NPCs and raid behavior.

## State contract for a future normal-mode encounter

Do not fake a started encounter merely to enable overlays in an empty room. Set and clear these fields when the eventual encounter lifecycle actually reaches their corresponding states.

| Signal | Consumer and meaning |
| --- | --- |
| Varbit6440 (`TOB_CLIENT_PARTYSTATUS`) | ToB QoL uses 0/1 as outside, 2 as inside/spectating, 3 as dead spectating. |
| Varbits6442–6446 (`TOB_CLIENT_P0`…`P4`) | ToB QoL uses zero for empty slot, one for dead, other nonzero for alive. Death Indicators counts nonempty slots and accepts party sizes1–5 before tracking spawned NPCs. |
| Varc strings330–334 (`TOB_CLIENT_NAME0`…`NAME4`) | ToB QoL reads the five raider names and pairs them with the slot varbits. |
| Varbit6447 (`TOB_CLIENT_WAVEPROGRESS_TYPE`) | ToB QoL treats one as a room-start signal for timing; zero is the between-room state. |
| Varbit6448 (`TOB_CLIENT_WAVEPROGRESS_VAL`) | Read by ToB QoL as boss health. |
| Varbit11866 | ToB QoL checks whether precise timers are enabled. |

Sources: [ToB QoL EventManager](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/EventManager.java), [InstanceService](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/InstanceService.java), [RaidConstants](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/api/game/RaidConstants.java), [local-revision VarbitID names](https://github.com/runelite/runelite/blob/3202bfde4a270c086587cfdb90d61220314e7bc4/runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java), [VarClientID names](https://github.com/runelite/runelite/blob/3202bfde4a270c086587cfdb90d61220314e7bc4/runelite-api/src/main/java/net/runelite/api/gameval/VarClientID.java).

No widget is required for the three room-detection checks. The cache contains ToB HUD interface28, but displaying it alone does not supply raid state or party names. [Source: InterfaceID.java](https://github.com/runelite/runelite/blob/3202bfde4a270c086587cfdb90d61220314e7bc4/runelite-api/src/main/java/net/runelite/api/gameval/InterfaceID.java).

### NPC identity, size, transforms, and spawn positions

Use native normal-mode IDs rather than lookalike custom NPC IDs:

| Role | Melee | Ranged | Magic |
| --- | --- | --- | --- |
| Small incoming | 8342 | 8343 | 8344 |
| Big incoming | 8345 | 8346 | 8347 |
| Small fighting | 8348 | 8349 | 8350 |
| Big fighting | 8351 | 8352 | 8353 |
| Boss combat forms | 8355 | 8357 | 8356 |

Normal pillar NPC8358 and boss arrival NPC8354 are distinct from map objects. ToB QoL also checks native names: Nylocas Ischyros, Nylocas Toxobolos, Nylocas Hagios, and Nylocas Vasilias. It infers mode from native IDs, uses transformed composition size greater than one for big nylocas, and reads health ratios and NPC change/despawn events. Nyloer maps composition size one to small and all other sizes to big, tracks ID changes, and resets on pillar spawn. [Sources: ToB QoL constants](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/commons/NylocasConstants.java), [handler](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/NylocasHandler.java), [Nyloer NPC model](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/npc/NyloerNpc.java), [Nyloer type IDs](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/npc/NyloType.java).

The wave counters recognize native source-region spawn lanes. ToB QoL accepts west x17–18/y24–25, south x31–32/y9–10, and east x46–47/y24–25, relative to origin(3264,4224). Nyloer uses specific subsets of these tiles and composition size to name lanes; unknown positions are classified as split spawns. Both infer a new wave from newly spawned lane NPCs on a new game tick. Preserve placement and spawn batching when waves are implemented; no server varbit transmits the wave number to these plugins. [Sources: ToB QoL valid spawn set](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/commons/NylocasConstants.java), [Nyloer findSpawn](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/npc/NyloerNpc.java).

Death Indicators expects normal-mode small/big HP of **8/16** for parties1–3, **9/19** for four, and **11/22** for five. Its early-death inference consumes XP drops, interacting targets, hitsplats, health and optional RuneLite-party damage messages. Server NPC indices, HP, combat XP timing and hitsplats must remain consistent. Combat interpretation also reads attack-style varp43 and defensive-autocast varbit2668. [Sources: health table](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NylocasHealth.java), [damage tracking](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NyloDeathIndicatorsPlugin.java).

### Visual and chat signals

ToB QoL uses big-nylocas animations7991/7998/8005 and7992/8000/8006 for split handling; boss attack animations are melee8004, ranged7999 and magic7989. Its object controls recognize standing pillar32862, collapsed pillar32864, spectator webs32939/32865/32937 and walls32876/32899. These are future encounter and scene fidelity requirements, not prerequisites for entering an empty map. [Sources: NylocasHandler](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/NylocasHandler.java), [NylocasConstants](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/commons/NylocasConstants.java).

Death Indicators suppresses native nylocas death spotanimations1562–1567. ToB QoL recognizes immunity graphic1558 and its own low-detail death graphics. [Sources: Death Indicators rendering](https://github.com/InfernoStats/Nylo-Death-Indicators/blob/1b29a76e1799cf813299bfdc75e12251028643ae/src/main/java/com/infernostats/NyloDeathIndicatorsPlugin.java), [ToB QoL constants](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/commons/NylocasConstants.java).

Nyloer resets on the game message `You have failed. The vampyres take pity on you and allow you to try again...`. ToB QoL separately recognizes a failure-message prefix ending `try again.` and a completion-message pattern beginning `Wave 'The Nylocas' (` and ending `) complete!`. Emit genuine lifecycle messages when implementing the encounter; an empty-room teleport should not emit completion or failure events. [Sources: Nyloer message handler](https://github.com/EIKOOT/nyloer/blob/9b400ec733fb7d83c261affee4b8edf4feb3fb5f/src/main/java/com/nyloer/NyloerPlugin.java), [ToB QoL global message handler](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/TheatreQOLPlugin.java), [completion pattern](https://github.com/damencs/tob-qol/blob/3715d6ae24547fa87dc7ed82665e37b632407ba7/src/main/java/com/tobqol/rooms/nylocas/commons/NylocasConstants.java).

## Verification boundary

For the foundation, verify that the cached native room exists, a chosen destination is walkable, the developer button routes to a server-authorized teleport, the dynamic template resolves the player back to13122/plane0, and leaving/re-entering the room remains possible. A later client smoke test should enable the three plugins and inspect region detection. Waves, boss behavior, party status, combat inference and timers require a separate implementation and test plan.
