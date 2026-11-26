package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MageWest8SplitClearanceTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private int key;

	private Crab small(int wave, int born, Lane lane, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, 'S', false, style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private void waveNine()
	{
		room.wave = 9;
		room.spawns[9] = 56;
		room.ready = 66;
		room.player = MageStrategy.tile(3296, 4248);
	}

	@Test
	public void castsOnARecentMagicSplitBeforeChasingWestNineMelee()
	{
		waveNine();
		small(9, 56, Lane.WEST, Style.MELEE, 3294, 4248);
		Crab magic = small(9, 63, Lane.SPLIT, Style.MAGIC, 3291, 4248);

		assertSame(magic, strategy.decide(room, 66).target);
	}

	@Test
	public void clearsTheAdjacentAssignedMeleeFirst()
	{
		waveNine();
		Crab melee = small(9, 56, Lane.WEST, Style.MELEE, 3295, 4248);
		small(9, 63, Lane.SPLIT, Style.MAGIC, 3291, 4248);

		assertSame(melee, strategy.decide(room, 66).target);
	}

	@Test
	public void finishesThePreviouslyDamagedMeleeFirst()
	{
		waveNine();
		Crab melee = small(9, 56, Lane.WEST, Style.MELEE, 3294, 4248);
		melee.hp = 2;
		small(9, 63, Lane.SPLIT, Style.MAGIC, 3291, 4248);

		assertSame(melee, strategy.decide(room, 66).target);
	}

	@Test
	public void preservesTheWestEightReturnAfterAFarDiagonalMelee()
	{
		// Mage at tick 48 in a packed run that was a tick late to the west lane.
		room.wave = 7;
		room.spawns[7] = 40;
		room.ready = 48;
		room.player = MageStrategy.tile(3298, 4244);
		small(6, 46, Lane.SPLIT, Style.MELEE, 3299, 4243);

		MageStrategy.Decision move = strategy.decide(room, 48);
		assertNull(move.target);
		assertEquals(Weapon.EYE, move.weapon);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);
	}
}
