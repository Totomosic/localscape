package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
import static org.localscape.nylocas.mage.MageStrategy.tile;

public class MageStrategyTwoSouthNineteenTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private int serial;
	private Crab crab(int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(++serial, serial, born, wave, lane, slot, big, style, tile(x, y));
		c.pendingDamage = 0; room.crabs.add(c); return c;
	}
	private Crab survivor()
	{
		room.configureStrategy(2); room.wave = 22; room.spawns[19] = 152;
		room.spawns[20] = 164; room.spawns[21] = 180; room.spawns[22] = 188;
		room.player = tile(3291, 4251); room.ready = 194; room.specialEnergy = 1000;
		Crab south = crab(19, 152, Lane.SOUTH, 'W', true, Style.MELEE, 3298, 4244);
		south.hp = 2;
		crab(22, 191, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3292, 4250);
		return south;
	}
	@Test public void usesOrdinaryCastCooldownToApproachTheOldSouthParent()
	{
		Crab south = survivor();
		Decision d = mage.decide(room, 192);
		assertNull(d.target); assertSame(south, d.approach); assertEquals(Weapon.SCYTHE, d.weapon);
	}
	@Test public void finishesOnTheFirstReadyContact()
	{
		Crab south = survivor(); room.player = tile(3297, 4245); room.ready = 195;
		Decision d = mage.decide(room, 195);
		assertSame(south, d.target); assertEquals("South19 melee recovery", d.rule);
	}
	@Test public void doesNotReplaceAnImmediateEyeWithALongReadyChase()
	{
		Crab south = survivor(); room.ready = 192;
		Decision d = mage.decide(room, 192);
		assertNotSame(south, d.target); assertEquals(Weapon.EYE, d.weapon);
	}
	@Test public void unfinishedSouthTwentyRetainsItsRotation()
	{
		Crab south = survivor();
		Crab twenty = crab(20, 164, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4243); twenty.hp = 4;
		Decision d = mage.decide(room, 192);
		assertNotSame(south, d.target); assertNotSame(south, d.approach);
	}
	@Test public void aRecoveryThatWouldOverrunTheDelayedDinhIsRejected()
	{
		Crab south = survivor(); room.spawns[22] = 184;
		Decision d = mage.decide(room, 192);
		assertNotSame(south, d.target); assertNotSame(south, d.approach);
	}
	@Test public void theObservedEastTwentyTwoDinhStillExecutesFirst()
	{
		Crab south = survivor(); room.spawns[22] = 180; room.ready = 193; room.player = tile(3297, 4248);
		Crab east = crab(22, 180, Lane.EAST, 'S', true, Style.MELEE, 3298, 4248); east.fighting = true;
		Decision d = mage.decide(room, 193);
		assertSame(east, d.target); assertEquals(Weapon.DINH, d.weapon); assertTrue(d.special);
	}
	@Test public void lethalCoverageAndPersonalNullDoNotStartTheRecovery()
	{
		Crab south = survivor(); south.pendingDamage = 2;
		assertNotSame(south, mage.decide(room, 192).approach);
		south.pendingDamage = 0; south.nulled = true;
		assertNotSame(south, mage.decide(room, 192).approach);
	}
	@Test public void versionOneRetainsItsOriginalClearing()
	{
		Crab south = survivor(); room.configureStrategy(1);
		Decision d = mage.decide(room, 192);
		assertNotSame(south, d.target); assertNotSame(south, d.approach);
	}
}
