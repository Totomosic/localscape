package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;

import static org.junit.Assert.*;

public class MageManualApproachTest
{
	@Test
	public void movesTowardAFarLaneBeforeSendingAnAttackInput()
	{
		Room room = new Room();
		room.wave = 3;
		room.spawns[3] = 12;
		room.player = MageStrategy.tile(3290, 4248);
		Crab magic = new Crab(1, 1, 12, 3, Lane.EAST, 'N', false, Style.MAGIC,
			MageStrategy.tile(3310, 4249));
		magic.pendingDamage = 0;
		room.crabs.add(magic);

		Decision move = new MageStrategy().decide(room, 12);
		assertNull(move.target);
		assertSame(magic, move.approach);
		assertFalse(move.pathToTarget);
		assertTrue(move.tile.getX() > room.player.getX());
	}

	@Test
	public void retainsASameCycleWestEightCastWhenTheParentWalksIntoRange()
	{
		Room room = new Room();
		room.wave = 8;
		room.spawns[8] = 52;
		room.player = MageStrategy.tile(3290, 4249);
		Crab parent = new Crab(1, 1, 52, 8, Lane.WEST, 'S', true, Style.MAGIC,
			MageStrategy.tile(3281, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);

		Decision cast = new MageStrategy().decide(room, 52);
		assertSame(parent, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void usesTheCooldownToMoveAroundAnObservedBlockedRay()
	{
		Room room = new Room();
		room.wave = 3;
		room.spawns[3] = 12;
		room.ready = 16;
		room.player = MageStrategy.tile(3296, 4247);
		Crab magic = new Crab(1, 1, 12, 3, Lane.EAST, 'N', false, Style.MAGIC,
			MageStrategy.tile(3305, 4249));
		magic.pendingDamage = 0;
		room.crabs.add(magic);
		room.obstructed.add(magic.key);

		Decision move = new MageStrategy().decide(room, 13);
		assertNull(move.target);
		assertSame(magic, move.approach);
		assertFalse(move.pathToTarget);
	}
}
