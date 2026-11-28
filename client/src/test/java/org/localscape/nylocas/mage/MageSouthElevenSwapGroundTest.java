package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;

public class MageSouthElevenSwapGroundTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private final Crab west;

	public MageSouthElevenSwapGroundTest()
	{
		room.wave = 11;
		room.spawns[11] = 76;
		room.ready = 81;
		room.player = MageStrategy.tile(3301, 4249);
		Crab east = new Crab(100, 100, 76, 11, Lane.EAST, 'N', false, Style.MAGIC, MageStrategy.tile(3310, 4249));
		mage.attacked(new Decision(east, Weapon.BARRAGE, room.player, false, "East barrage 11", "East barrage 11"), 77, 76);
		west = south(1, 'W', 3295);
		room.crabs.add(west);
		room.crabs.add(south(2, 'E', 3296));
		room.observeSouthElevenSwap(true);
	}

	private Crab south(int key, char slot, int x)
	{
		Crab crab = new Crab(key, key, 76, 11, Lane.SOUTH, slot, false, Style.MAGIC, MageStrategy.tile(x, 4234));
		crab.pendingDamage = 0;
		return crab;
	}

	@Test
	public void preparesSouthOnTheFirstCooldownTickAfterTheEastCast()
	{
		Decision move = mage.decide(room, 77);
		assertNull(move.target);
		assertNull(move.approach);
		assertEquals(Weapon.BARRAGE, move.weapon);
		assertEquals(MageStrategy.tile(3297, 4247), move.tile);
		assertTrue(move.moveBeforeSouthElevenSwap());
		assertFalse(mage.decide(room, 78).moveBeforeSouthElevenSwap());
	}

	@Test
	public void checksTheObservedCastTheNativeSwapAndBothSouthSmalls()
	{
		room.observeSouthElevenSwap(false);
		assertFalse(mage.decide(room, 77).moveBeforeSouthElevenSwap());
		room.observeSouthElevenSwap(true);
		west.pendingDamage = west.hp;
		assertFalse(mage.decide(room, 77).moveBeforeSouthElevenSwap());
		west.pendingDamage = 0;
		mage.reset();
		assertFalse(mage.decide(room, 77).moveBeforeSouthElevenSwap());
	}

	@Test
	public void aConfirmedSouthBarrageReturnsNorthDuringCooldown()
	{
		assertTrue(mage.decide(room, 77).moveBeforeSouthElevenSwap());
		mage.attacked(new Decision(west, Weapon.BARRAGE, room.player, false, "South magic 11", "South magic 11"), 83, 82);
		for (Crab crab : room.crabs) crab.pendingDamage = crab.hp;
		room.ready = 87;
		room.player = MageStrategy.tile(3297, 4247);
		Decision move = mage.decide(room, 83);
		assertNull(move.target);
		assertEquals(4249, move.tile.getY());
	}

	@Test
	public void actualTwelveClosesTheSpellPreparation()
	{
		room.wave = 12;
		room.spawns[12] = 77;
		assertFalse(mage.decide(room, 77).moveBeforeSouthElevenSwap());
	}
}
