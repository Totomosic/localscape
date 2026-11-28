package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;

import static org.junit.Assert.*;

public class RangerEastFourteenRoomCountTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab parent;

	public RangerEastFourteenRoomCountTest()
	{
		room.wave = 15;
		room.spawns[14] = 100;
		room.spawns[15] = 108;
		room.player = RangerStrategy.tile(3297, 4248);
		room.ready = 112;
		parent = new Crab(1, 1, 100, 14, Lane.EAST, 'S', true, Style.RANGE, room.player.dx(3));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		for (int i = 0; i < 4; i++)
		{
			Crab small = new Crab(i + 2, i + 2, 108, 15, Lane.SPLIT, (char) 0, false, Style.RANGE,
				room.player.dx(i % 2).dy(i / 2));
			small.pendingDamage = 0;
			room.crabs.add(small);
		}
	}

	@Test
	public void crowdedRoomUsesARealSmallShotBeforeOpeningEast14()
	{
		Decision decision = ranger.decide(room, 112);
		assertNotNull(decision.target);
		assertFalse(decision.reason, decision.target.big);
	}

	@Test
	public void threeUncoveredSmallsReleaseTheParent()
	{
		room.crabs.remove(room.crabs.size() - 1);
		assertSame(parent, ranger.decide(room, 112).target);
	}

	@Test
	public void knownLethalFlightsCountAsCoveredWork()
	{
		Crab covered = room.crabs.get(room.crabs.size() - 1);
		covered.pendingDamage = covered.hp;
		assertSame(parent, ranger.decide(room, 112).target);
	}

	@Test
	public void woundedParentGetsItsRecoveryEvenWhenTheRoomIsCrowded()
	{
		parent.hp = 3;
		assertSame(parent, ranger.decide(room, 112).target);
	}

	@Test
	public void countAloneDoesNotHoldTheParentWhenEverySmallRayIsBlocked()
	{
		for (Crab crab : room.crabs) if (!crab.big) room.obstructed.add(crab.key);
		assertSame(parent, ranger.decide(room, 112).target);
	}

	@Test
	public void boundedRecoveryReleasesTheParentBeforeTransitionDespiteTheCount()
	{
		room.wave = 16;
		room.spawns[16] = 116;
		assertSame(parent, ranger.decide(room, 118).target);
	}
}
