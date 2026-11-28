package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerSpareSpecialTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();

	private Crab add(int wave, int born, Lane lane, boolean big, Style style, int x, int y)
	{
		int key = room.crabs.size() + 1;
		Crab crab = new Crab(key, key, born, wave, lane, 'S', big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test public void westFiveCanSpendOnlyTheEnergyAboveTheEastSixClaw()
	{
		room.wave = 5;
		room.spawns[5] = 20;
		room.player = RangerStrategy.tile(3290, 4248);
		Crab west = add(5, 20, Lane.WEST, true, Style.RANGE, 3285, 4248);
		room.specialEnergy = 900;
		Decision ordinary = ranger.decide(room, 22);
		assertSame(west, ordinary.target);
		assertEquals(Weapon.PIPE, ordinary.weapon);
		assertFalse(ordinary.special);

		room.specialEnergy = 1000;
		assertTrue(ranger.decide(room, 22).special);
	}

	@Test public void aFreshCleanupMeleeCanTakeTheLastClawSpecial()
	{
		room.wave = 31;
		room.spawns[29] = 244;
		room.spawns[31] = 260;
		room.player = RangerStrategy.tile(3294, 4248);
		room.cleanup = true;
		Crab split = add(31, 272, Lane.SPLIT, false, Style.MELEE, 3295, 4248);
		room.specialEnergy = 500;
		Decision claw = ranger.decide(room, 276);
		assertSame(split, claw.target);
		assertEquals(Weapon.CLAWS, claw.weapon);
		assertTrue(claw.special);

		split.hp = 3;
		Decision ordinary = ranger.decide(room, 276);
		assertEquals(Weapon.SULPHUR, ordinary.weapon);
		assertFalse(ordinary.special);
	}
}
