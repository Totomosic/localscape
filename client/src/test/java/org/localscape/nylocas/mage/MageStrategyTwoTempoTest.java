package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
import static org.localscape.nylocas.mage.MageStrategy.tile;

public class MageStrategyTwoTempoTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private int serial;
	private Crab crab(int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(++serial, serial, born, wave, lane, slot, big, style, tile(x, y));
		room.crabs.add(c); c.pendingDamage = 0; return c;
	}
	private Crab six()
	{
		room.configureStrategy(2); room.wave = 6; room.spawns[6] = 100;
		room.player = tile(3291, 4249);
		return crab(6, 100, Lane.EAST, 'S', true, Style.MELEE, 3304, 4248);
	}
	@Test public void leavesWestChildWhenItsReturnWouldMissEastSixEntry()
	{
		six();
		Crab split = crab(6, 103, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3290, 4249);
		Decision d = mage.decide(room, 105);
		assertNotSame(split, d.target); assertEquals(Weapon.SCYTHE, d.weapon);
	}
	@Test public void keepsAnEarlierWestChildCastWhenTheEntryBudgetFits()
	{
		six(); room.player = tile(3295, 4249);
		Crab split = crab(6, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4249);
		assertSame(split, mage.decide(room, 101).target);
	}
	@Test public void reservedEntrySwingsOnTheFirstContactTick()
	{
		Crab east = six(); room.player = tile(3301, 4248); east.point = tile(3302, 4248);
		Decision d = mage.decide(room, 107);
		assertSame(east, d.target); assertEquals(Weapon.SCYTHE, d.weapon);
	}
	@Test public void stillReturnsForTheFirstWaveEightCastAfterEntry()
	{
		Crab east = six(); room.player = tile(3301, 4248); east.point = tile(3302, 4248);
		Decision d = mage.decide(room, 107); mage.attacked(d, 108, 107);
		east.hp = 0; east.dying = true;
		room.wave = 7; room.spawns[7] = 104; room.ready = 112;
		Decision back = mage.decide(room, 108);
		assertEquals(Weapon.EYE, back.weapon); assertEquals(tile(3290, 4249), back.tile);
		room.wave = 8; room.spawns[8] = 116; room.player = tile(3290, 4249);
		Crab west = crab(8, 116, Lane.WEST, 'S', true, Style.MAGIC, 3281, 4248);
		assertSame(west, mage.decide(room, 116).target);
	}
	private Crab woundedWestEight(int pending)
	{
		room.configureStrategy(2); room.wave = 9; room.spawns[8] = 52; room.spawns[9] = 56;
		room.player = tile(3293, 4253); room.specialEnergy = 1000;
		room.specialRegenAt = 60; room.specialRegenInterval = 25;
		Crab west = crab(8, 52, Lane.WEST, 'S', true, Style.MAGIC, 3287, 4248);
		west.hp = 6; west.pendingDamage = pending;
		for (int x = 3292; x <= 3294; x++) crab(9, 50, Lane.SPLIT, (char) 0, false, Style.MELEE, x, 4254);
		return west;
	}
	@Test public void sparesWestEightWhoseNonlethalFlightWillLeaveThreeHp()
	{
		Crab west = woundedWestEight(3);
		Decision d = mage.decide(room, 58);
		assertEquals(Weapon.EYE, d.weapon); assertSame(west, d.target); assertFalse(d.special);
	}
	@Test public void knownLethalCoverageReleasesTheSparePrecapSweep()
	{
		woundedWestEight(6);
		Decision d = mage.decide(room, 58);
		assertEquals(Weapon.DINH, d.weapon); assertTrue(d.special);
	}
	@Test public void originalVersionKeepsItsExistingSweepDecision()
	{
		woundedWestEight(3); room.configureStrategy(1);
		assertEquals(Weapon.DINH, mage.decide(room, 58).weapon);
	}
}
