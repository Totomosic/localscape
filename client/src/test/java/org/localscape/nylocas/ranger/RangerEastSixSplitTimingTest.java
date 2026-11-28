package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerEastSixSplitTimingTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final RangerPreCapPlanner planner = new RangerPreCapPlanner(ranger);
	private final Room room = new Room();
	private final Decision claw;
	private final Crab green;

	public RangerEastSixSplitTimingTest()
	{
		room.wave = 7;
		room.spawns[6] = 36;
		room.spawns[7] = 40;
		room.player = RangerStrategy.tile(3298, 4246);
		Crab east = add(6, 36, Lane.EAST, Style.MELEE, 3299, 4246, true);
		green = add(7, 42, Lane.SPLIT, Style.RANGE, 3297, 4246, false);
		for (int i = 0; i < 8; i++)
			add(5, 32, Lane.SPLIT, Style.MAGIC, 3291, 4253, false);
		claw = new Decision(east, Weapon.CLAWS, room.player, true, "East big claw special", "East big claw special");
	}

	private Crab add(int wave, int born, Lane lane, Style style, int x, int y, boolean big)
	{
		int key = room.crabs.size() + 1;
		Crab crab = new Crab(key, key, born, wave, lane, (char) 0, big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void clearsOneSmallBeforeAnEarlySplitThatCanOverfillTheCheck()
	{
		Decision clear = planner.beforeEastSixSplit(room, 47, claw);
		assertSame(green, clear.target);
		assertEquals(Weapon.PIPE, clear.weapon);
		ranger.attacked(clear, 48);
		assertSame(claw, planner.beforeEastSixSplit(room, 49, claw));
	}

	@Test
	public void keepsTheClawWhenAChildWouldArriveAfterTheCheck()
	{
		assertSame(claw, planner.beforeEastSixSplit(room, 51, claw));
	}

	@Test
	public void anAlreadyRemovingSmallLeavesRoomForTheClaw()
	{
		room.crabs.get(3).observeRemovalAt(50);
		assertSame(claw, planner.beforeEastSixSplit(room, 47, claw));
	}

	@Test
	public void insufficientEnergySulphurKeepsTheExistingPreSplitClearance()
	{
		Decision fallback = new Decision(claw.target, Weapon.SULPHUR, room.player, false, null, "No special energy");
		Decision clear = planner.beforeEastSixSplit(room, 47, fallback);
		assertSame(green, clear.target);
		assertEquals(Weapon.PIPE, clear.weapon);
	}
}
