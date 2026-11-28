package org.localscape.nylocas;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy;
import org.localscape.nylocas.ranger.RangerStrategy;

import static org.junit.Assert.*;

public class CleanupMeleeTest
{
	private static final int NOW = 300;
	private static final Tile WEST = new Tile(3292, 4249, 0);
	private static final Tile EAST = new Tile(3299, 4249, 0);

	private static RoomPlanner<?> observed(boolean ranger)
	{
		RoomPlanner<?> planner;
		if (ranger)
		{
			RangerStrategy.Room r = new RangerStrategy.Room();
			r.wave = 31;
			r.spawns[29] = 270;
			r.player = WEST;
			r.observePartner(EAST, NOW);
			planner = RoomPlanner.ranger(r, NOW);
		}
		else
		{
			MageStrategy.Room m = new MageStrategy.Room();
			m.wave = 31;
			m.spawns[29] = 270;
			m.player = EAST;
			m.observePartner(WEST, NOW);
			planner = RoomPlanner.mage(m, NOW);
		}
		planner.crabs.add(child(1, 277, WEST.dy(1)));
		planner.crabs.add(child(2, 286, EAST.dx(1)));
		return planner;
	}

	private static RoomProjection.Crab child(int key, int born, Tile tile)
	{
		return new RoomProjection.Crab(key, born, 30, 3, (char) 0, false, tile.getX(), tile.getY(), 0);
	}

	@Test public void bothRolesPlanDistinctCardinalContacts()
	{
		CleanupMelee ranger = CleanupMelee.choose(observed(true));
		CleanupMelee mage = CleanupMelee.choose(observed(false));
		assertEquals(1, ranger.key);
		assertEquals(2, mage.key);
		assertEquals(WEST, ranger.contact);
		assertEquals(EAST, mage.contact);
		assertEquals(0, ranger.steps);
		assertEquals(0, mage.steps);
	}

	@Test public void ignoresAChildBornOnTheActualDimAndOneWithLethalCoverage()
	{
		RoomPlanner<?> room = observed(true);
		room.crabs.add(child(3, 270, WEST.dx(1)));
		room.crabs.get(0).pending = 8;
		assertNull(CleanupMelee.choose(room));
	}

	@Test public void leavesAOneSidedNullAndAnImminentParentSplitToTheOrdinaryPolicy()
	{
		RoomPlanner<?> room = observed(true);
		room.crabs.get(1).partnerNull = true;
		assertNull(CleanupMelee.choose(room));
		room.crabs.get(1).partnerNull = false;
		room.crabs.add(new RoomProjection.Crab(3, NOW - 51, 27, 0, (char) 0, true, 3294, 4248, 0));
		assertNull(CleanupMelee.choose(room));
	}
}
