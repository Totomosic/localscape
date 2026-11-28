package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;

import static org.junit.Assert.*;

public class MageEast11ReturnTest
{
	@Test
	public void leavesTheNorthwestFillerOneTickEarlierForTheEastBarrageRay()
	{
		// Packed holdout seed 11276202: a Sulphur at 71 leaves only four
		// return cycles before actual11=76. The fifth move loses the opening ray.
		Room room = room(68, 3292, 4253);
		Crab old = melee(room, 3291, 4253);
		Decision move = new MageStrategy().decide(room, 71);
		assertNotSame(old, move.target);
		assertNull(move.target);
		assertEquals(MageStrategy.tile(3301, 4249), move.tile);
		assertEquals(Weapon.BARRAGE, move.weapon);
	}

	@Test
	public void stillTakesTheFillerWhenCooldownAndReturnBothFit()
	{
		Room room = room(68, 3292, 4253);
		Crab old = melee(room, 3291, 4253);
		assertSame(old, new MageStrategy().decide(room, 70).target);
	}

	@Test
	public void doesNotAddAReturnMoveWhenAlreadyOnTheFiringTile()
	{
		Room room = room(68, 3301, 4249);
		Crab old = melee(room, 3300, 4249);
		assertSame(old, new MageStrategy().decide(room, 72).target);
	}

	private static Room room(int born, int x, int y)
	{
		Room room = new Room();
		room.wave = 10;
		room.spawns[10] = born;
		room.player = MageStrategy.tile(x, y);
		return room;
	}

	private static Crab melee(Room room, int x, int y)
	{
		Crab c = new Crab(25, 31, 40, 7, Lane.SPLIT, (char) 0, false, Style.MELEE, MageStrategy.tile(x, y));
		c.pendingDamage = 0;
		room.crabs.add(c);
		return c;
	}
}
