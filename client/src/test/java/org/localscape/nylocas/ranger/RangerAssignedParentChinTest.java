package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerAssignedParentChinTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab parent;
	private final Crab small;

	public RangerAssignedParentChinTest()
	{
		room.wave = 11;
		room.spawns[10] = 68;
		room.spawns[11] = 76;
		room.player = RangerStrategy.tile(3297, 4248);
		parent = new Crab(43, 43, 68, 10, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3300, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		small = new Crab(42, 42, 68, 10, Lane.EAST, 'N', false, Style.RANGE,
			RangerStrategy.tile(3300, 4249));
		small.pendingDamage = 0;
		room.crabs.add(small);
	}

	@Test
	public void releasedEast10CanFinishItsParentAndTheAdjacentSmallWithOneChin()
	{
		Decision decision = ranger.decide(room, 78);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void lethalCoverageOnTheSmallKeepsTheTwoTickParentPipe()
	{
		small.pendingDamage = small.hp;
		Decision decision = ranger.decide(room, 78);
		assertSame(parent, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void aHeldMageParentInTheSplashKeepsTheRangeSmallPipe()
	{
		Crab held = new Crab(46, 46, 76, 11, Lane.WEST, 'S', true, Style.MAGIC,
			RangerStrategy.tile(3300, 4247));
		held.pendingDamage = 0;
		room.crabs.add(held);
		Decision decision = ranger.decide(room, 78);
		assertSame(small, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
