package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;

public class MageLateBarrageTest
{
	private final Room room = new Room();
	private final MageStrategy mage = new MageStrategy();

	public MageLateBarrageTest()
	{
		room.wave = 31;
		room.spawns[29] = 240;
		room.spawns[31] = 256;
		room.player = MageStrategy.tile(3295, 4248);
	}

	private Crab split(int born, Style style, int x, int y)
	{
		int key = room.crabs.size() + 1;
		Crab crab = new Crab(key, key, born, born <= 240 ? 29 : 31, Lane.SPLIT, (char) 0, false,
			style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void actualAncientBookAllowsTwoFreshMagicSmallsToShareACast()
	{
		split(256, Style.MAGIC, 3297, 4248);
		split(256, Style.MAGIC, 3298, 4248);
		assertEquals(Weapon.EYE, mage.decide(room, 260).weapon);
		room.observeBarrageBook(1, false);
		assertEquals(Weapon.BARRAGE, mage.decide(room, 260).weapon);
		room.observeBarrageBook(2, true);
		assertNotEquals(Weapon.BARRAGE, mage.decide(room, 260).weapon);
	}

	@Test
	public void aDimmedOrCoveredSiblingCannotJustifyTheSecondHit()
	{
		split(240, Style.MAGIC, 3297, 4248);
		split(256, Style.MAGIC, 3297, 4248);
		split(256, Style.MAGIC, 3298, 4248).pendingDamage = 8;
		room.observeBarrageBook(1, false);
		assertNotEquals(Weapon.BARRAGE, mage.decide(room, 260).weapon);
	}

	@Test
	public void aPartnerNulledMeleeInTheSplashKeepsTheOrdinaryEye()
	{
		split(256, Style.MAGIC, 3297, 4248);
		split(256, Style.MAGIC, 3298, 4248);
		split(256, Style.MELEE, 3297, 4249).partnerNulled = true;
		room.observeBarrageBook(1, false);
		assertNotEquals(Weapon.BARRAGE, mage.decide(room, 260).weapon);
	}

	@Test
	public void theSwapNeedsTheNativeCooldownAndAnObservablePair()
	{
		room.ready = 264;
		room.observeBarrageBook(2, true);
		assertFalse(mage.decide(room, 260).prepareOptionalBarrage());
		split(256, Style.MAGIC, 3296, 4248);
		split(256, Style.MAGIC, 3297, 4248);
		assertTrue(mage.decide(room, 260).prepareOptionalBarrage());
		room.observeBarrageBook(2, false);
		assertFalse(mage.decide(room, 260).prepareOptionalBarrage());
	}

	@Test
	public void anImminentWaveTwentyEightSpecialKeepsItsOwnCooldown()
	{
		room.wave = 28;
		room.spawns[28] = 256;
		room.ready = 264;
		room.specialEnergy = 500;
		room.observeBarrageBook(2, true);
		split(256, Style.MAGIC, 3296, 4248);
		split(256, Style.MAGIC, 3297, 4248);
		assertFalse(mage.decide(room, 260).prepareOptionalBarrage());
	}
}
