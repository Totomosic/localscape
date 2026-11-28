package org.localscape.nylocas;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OrderedIncomingTest
{
	@Test
	public void positiveSplatAfterLethalClearsTheBodyOnTheAdmissionTick()
	{
		assertEquals(0, transitionStalls(9, 1));
		assertEquals(0, transitionStalls(20, 12));
	}

	@Test
	public void lethalLastAndSingleSplatsUseTheFullDeathDelay()
	{
		assertEquals(1, transitionStalls(9, 8));
		assertEquals(1, transitionStalls(8, 0));
		assertEquals(1, transitionStalls(9, -1));
	}

	@Test
	public void damageAlreadyLandedInAnEarlierBucketCountsTowardLethal()
	{
		int[] totals = {0, 5, 6, 12};
		AttackTiming.LethalImpact earlier = AttackTiming.lethalImpact(8, totals, new int[]{0, 0, 3, 1});
		assertEquals(2, earlier.delay);
		assertTrue(earlier.anotherPositiveSplat);
		assertFalse(AttackTiming.lethalImpact(8, totals, new int[]{0, 0, 4, 1}).anotherPositiveSplat);
		assertFalse(AttackTiming.lethalImpact(8, totals, null).anotherPositiveSplat);
		assertNull(AttackTiming.lethalImpact(24, totals, null));
	}

	private int transitionStalls(int total, int last)
	{
		int now = 100;
		Room room = new Room();
		room.wave = 19;
		room.spawns[19] = now - 9;
		room.player = new Tile(3295, 4248, 0);
		room.ready = now + 30;
		for (int key = 1; key <= 12; key++)
			room.crabs.add(new Crab(key, key, now, 19, Lane.SPLIT, (char) 0, false, Style.RANGE, room.player));
		Crab target = room.crabs.get(0);
		target.pendingDamage = total;
		target.observeIncoming(new int[]{0, 0, total}, last < 0 ? null : new int[]{0, 0, last});
		return new RoomProjection(RoomPlanner.ranger(room, now), 0).run(null).transitionStalls;
	}
}
