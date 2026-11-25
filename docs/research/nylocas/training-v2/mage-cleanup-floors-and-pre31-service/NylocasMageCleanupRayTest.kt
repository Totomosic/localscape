package org.rsmod.content.other.nylocas.bots

import jakarta.inject.Inject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.areas.dungeons.nylocas.NylocasRoom
import org.rsmod.map.CoordGrid

/** Exact native-map rays from timestamp29 control and combined-cutoff cleanup witnesses. */
class NylocasMageCleanupRayTest {
    class Dependencies @Inject constructor(val rays: RayCastValidator)

    @ParameterizedTest
    @CsvSource(
        "3295,4251,3292,4253", // control seed215613141308293 at270
        "3295,4251,3299,4244",
        "3296,4247,3292,4244", // combined seed191996532081100 at272
        "3295,4250,3292,4244", // combined seed191996532081086 at287
        "3295,4250,3300,4245",
    )
    fun `ready late29 magic split has native line of sight while mage chases melee`(
        sourceX: Int,
        sourceZ: Int,
        targetX: Int,
        targetZ: Int,
        state: GameTestState,
    ) =
        state.runInjectedGameTest(Dependencies::class) { deps ->
            val room = createRegion(NylocasRoom.template)
            val source = room.normal[CoordGrid(sourceX, sourceZ)]
            val target = room.normal[CoordGrid(targetX, targetZ)]
            assertEquals(true, deps.rays.hasLineOfSight(source, target, destWidth = 1, destLength = 1))
        }
}
