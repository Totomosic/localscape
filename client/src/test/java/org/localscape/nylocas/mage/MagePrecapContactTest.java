package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class MagePrecapContactTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private int key;

	private Crab body(int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, slot, big, style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private Crab waveTen()
	{
		room.wave = 10;
		room.spawns[9] = 56;
		room.spawns[10] = 68;
		room.ready = 68;
		room.player = MageStrategy.tile(3291, 4249);
		return body(9, 56, Lane.WEST, 'S', false, Style.MELEE, 3293, 4251);
	}

	private void fill()
	{
		for (int i = 0; i < 10; i++) body(9, 63, Lane.SPLIT, (char) 0, false, Style.RANGE, 3301, 4249);
	}

	@Test
	public void takesTheAdjacentSplitBeforeADiagonalChaseInACrowdedRoom()
	{
		// Packed-map seed 226232325230655, actual10 at tick 68.
		waveTen();
		Crab local = body(9, 63, Lane.SPLIT, (char) 0, false, Style.MELEE, 3292, 4249);
		fill();

		Decision action = strategy.decide(room, 68);
		assertSame(local, action.target);
		assertEquals(Weapon.SULPHUR, action.weapon);
	}

	@Test
	public void keepsTheNamedMeleeWhenThereIsAlreadySpace()
	{
		Crab west = waveTen();
		body(9, 63, Lane.SPLIT, (char) 0, false, Style.MELEE, 3292, 4249);

		assertSame(west, strategy.decide(room, 68).target);
	}

	@Test
	public void keepsTheWoundedNamedMeleeFollowupInTheCrowd()
	{
		Crab west = waveTen();
		west.hp = 3;
		body(9, 63, Lane.SPLIT, (char) 0, false, Style.MELEE, 3292, 4249);
		fill();

		assertSame(west, strategy.decide(room, 68).target);
	}

	@Test
	public void neverTakesACapContactOverTheTimedEastElevenBarrage()
	{
		waveTen();
		room.wave = 11;
		room.spawns[11] = 76;
		room.ready = 76;
		room.player = MageStrategy.tile(3301, 4249);
		Crab east = body(11, 76, Lane.EAST, 'N', false, Style.MAGIC, 3310, 4249);
		body(10, 71, Lane.SPLIT, (char) 0, false, Style.MELEE, 3300, 4249);
		fill();

		assertSame(east, strategy.decide(room, 76).target);
	}
}
