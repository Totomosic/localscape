package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class RangerEastFifteenStageTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();

	public RangerEastFifteenStageTest()
	{
		room.wave = 14;
		room.spawns[14] = 100;
		room.ready = 107;
		room.player = RangerStrategy.tile(3295, 4246);
		room.crabs.add(new Crab(1, 1, 100, 14, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3304, 4248)));
	}

	@Test
	public void movesEastAndKeepsTheNextPipeDuringItsCooldown()
	{
		Crab small = addRangeSplit(3295, 4244);
		Decision stage = ranger.decide(room, 106);
		assertEquals("Stage east during cooldown for East15", stage.reason);
		assertEquals(RangerStrategy.tile(3297, 4248), stage.tile);
		assertEquals(Weapon.PIPE, stage.weapon);
		assertNull(stage.target);
		room.player = stage.tile;
		assertSame(small, ranger.decide(room, 107).target);
	}

	@Test
	public void leavesThePriorPlanWhenTheMoveWouldLoseItsRange()
	{
		addRangeSplit(3291, 4246);
		assertNotEquals("Stage east during cooldown for East15", ranger.decide(room, 106).reason);
	}

	private Crab addRangeSplit(int x, int y)
	{
		Crab crab = new Crab(2, 2, 103, 14, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
