package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class RangerWestTenStageTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();

	public RangerWestTenStageTest()
	{
		room.wave = 9;
		room.spawns[9] = 56;
		room.player = RangerStrategy.tile(3295, 4247);
		room.ready = 68;
	}

	@Test
	public void theLastCooldownStepEndsOnTheFiringRow()
	{
		Decision stage = ranger.decide(room, 67);
		assertEquals(RangerStrategy.tile(3293, 4248), stage.tile);
		assertEquals(Weapon.CHIN, stage.weapon);
		assertNull(stage.target);
	}

	@Test
	public void theAlignmentStillLeavesThePreviouslyPlannedPipe()
	{
		room.ready = 66;
		Crab parent = new Crab(13, 13, 56, 9, Lane.WEST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3289, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		Decision stage = ranger.decide(room, 65);
		assertEquals(RangerStrategy.tile(3293, 4248), stage.tile);
		assertEquals(Weapon.PIPE, stage.weapon);
		room.player = stage.tile;
		assertSame(parent, ranger.decide(room, 66).target);
	}
}
