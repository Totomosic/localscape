package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.WaveSchedule;
import org.localscape.nylocas.mage.MageStrategy.*;

import static org.junit.Assert.*;
import static org.localscape.nylocas.mage.MageStrategy.tile;

public class MageStrategyTwoTest
{
	private final MageStrategy mage = new MageStrategy();
	private int serial;

	private Room room(int wave, int born, int x, int y)
	{
		Room room = new Room();
		room.configureStrategy(2);
		room.wave = wave; room.spawns[wave] = born; room.player = tile(x, y); room.specialEnergy = 1000;
		return room;
	}

	private Crab crab(Room room, int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++serial, serial, born, wave, lane, slot, big, style, tile(x, y));
		room.crabs.add(crab);
		return crab;
	}

	@Test public void defaultRoomRetainsVersionOne()
	{
		Room room = new Room();
		assertFalse(room.strategyTwo());
		room.configureStrategy(2); assertTrue(room.strategyTwo());
		room.configureStrategy(1); assertFalse(room.strategyTwo());
	}

	@Test(expected = IllegalArgumentException.class) public void rejectsUnknownVersion()
	{
		new Room().configureStrategy(3);
	}

	@Test public void rangerOwnsTheReassignedPrecapTargets()
	{
		Room room = room(6, 100, 3290, 4249);
		Crab west = crab(room, 6, 100, Lane.WEST, 'N', false, Style.MAGIC, 3285, 4249);
		assertNotSame(west, mage.decide(room, 102).target);
		room.configureStrategy(1); assertSame(west, mage.decide(room, 102).target);
		room = room(7, 100, 3295, 4244);
		Crab south = crab(room, 7, 100, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4239);
		assertNotSame(south, mage.decide(room, 102).target);
		room = room(11, 100, 3295, 4249);
		Crab big = crab(room, 11, 100, Lane.WEST, 'S', true, Style.MAGIC, 3290, 4248);
		assertNotSame(big, mage.decide(room, 110).target);
	}

	@Test public void observesWestFiveChildAfterTheParentDisappearsThenScythesEastSix()
	{
		Room room = room(6, 100, 3296, 4248);
		Crab dying = crab(room, 5, 84, Lane.WEST, 'S', true, Style.RANGE, 3297, 4248);
		dying.hp = 0; dying.dying = true; dying.observeSplitAt(102);
		Crab east = crab(room, 6, 100, Lane.EAST, 'S', true, Style.MELEE, 3308, 4248);
		mage.decide(room, 101);
		room.crabs.remove(dying);
		Crab child = crab(room, 6, 102, Lane.SPLIT, (char) 0, false, Style.RANGE, 3297, 4248);
		Decision split = mage.decide(room, 102);
		assertSame(child, split.target); assertEquals(Weapon.PIPE, split.weapon);
		mage.attacked(split, 103, 102);
		child.hp = 0; child.dying = true;
		room.player = tile(3301, 4248); room.ready = 104; east.observePosition(tile(3302, 4248));
		Decision scythe = mage.decide(room, 107);
		assertSame(east, scythe.target); assertEquals(Weapon.SCYTHE, scythe.weapon);
	}

	@Test public void eastSixDoesNotHoldTheWaveEightOpening()
	{
		Room room = room(7, 100, 3298, 4248);
		room.spawns[6] = 96;
		Crab east = crab(room, 6, 96, Lane.EAST, 'S', true, Style.MELEE, 3300, 4248);
		Decision decision = mage.decide(room, 111);
		assertNotSame(east, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	private void barrageEleven(Room room)
	{
		Crab east = crab(room, 11, 100, Lane.EAST, 'N', false, Style.MAGIC, 3310, 4249);
		Decision barrage = mage.decide(room, 100);
		assertSame(east, barrage.target); assertEquals(Weapon.BARRAGE, barrage.weapon);
		mage.attacked(barrage, 101, 100);
		east.hp = 0; east.dying = true;
		room.ready = 105;
	}

	@Test public void elevenClearsNewEastMeleeBeforeSouthMagic()
	{
		Room room = room(11, 100, 3301, 4249);
		barrageEleven(room);
		Crab melee = crab(room, 11, 103, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertSame(melee, mage.decide(room, 105).target);
	}

	@Test public void elevenClearsAYoungEastTenSplitBornBeforeEleven()
	{
		Room room = room(11, 100, 3301, 4249); barrageEleven(room);
		Crab melee = crab(room, 10, 99, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertSame(melee, mage.decide(room, 105).target);
	}

	@Test public void elevenDoesNotChaseADistantEastSplitAheadOfSouthMagic()
	{
		Room room = room(11, 100, 3301, 4249); barrageEleven(room);
		room.player = tile(3295, 4246);
		crab(room, 11, 104, Lane.SPLIT, (char) 0, false, Style.MELEE, 3309, 4248);
		Crab south = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertSame(south, mage.decide(room, 105).target);
	}

	@Test public void elevenDoesNotTreatAnOldEasternSplitAsNew()
	{
		Room room = room(11, 100, 3301, 4249); barrageEleven(room);
		room.player = tile(3296, 4247);
		crab(room, 10, 95, Lane.SPLIT, (char) 0, false, Style.MELEE, 3298, 4247);
		Crab south = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertSame(south, mage.decide(room, 105).target);
	}

	@Test public void elevenDoesNotPromoteAFreshMiddleRoomSplit()
	{
		Room room = room(11, 100, 3301, 4249); barrageEleven(room);
		room.player = tile(3296, 4247);
		Crab middle = crab(room, 11, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3294, 4247);
		middle.observePosition(tile(3297, 4247));
		Crab south = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertSame(south, mage.decide(room, 105).target);
	}

	@Test public void eastSplitPriorityKeepsThePairedTwelveReservation()
	{
		Room room = room(11, 100, 3301, 4249); barrageEleven(room);
		room.wave = 12; room.spawns[12] = 108; room.ready = 113;
		crab(room, 12, 111, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		Decision stage = mage.decide(room, 113);
		assertNull(stage.target); assertEquals(Weapon.SCYTHE, stage.weapon);
		Crab original = crab(room, 12, 108, Lane.EAST, 'S', false, Style.MELEE, 3302, 4248);
		Decision east = mage.decide(room, 115);
		assertSame(original, east.target); assertEquals("12 east scythe", east.rule);
	}

	@Test public void lethalEastSplitFlightReleasesSouthEleven()
	{
		Room room = room(11, 100, 3301, 4249); barrageEleven(room);
		room.player = tile(3296, 4247);
		Crab east = crab(room, 11, 103, Lane.SPLIT, (char) 0, false, Style.MELEE, 3298, 4247);
		east.pendingDamage = 8;
		Crab south = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertSame(south, mage.decide(room, 105).target);
	}

	@Test public void elevenTakesWestSouthMagicBeforeEastSouthMagic()
	{
		Room room = room(11, 100, 3301, 4249);
		barrageEleven(room);
		room.player = tile(3296, 4247);
		Crab west = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		Crab east = crab(room, 11, 100, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4243);
		assertSame(west, mage.decide(room, 105).target);
		west.pendingDamage = 20;
		assertSame(east, mage.decide(room, 105).target);
	}

	@Test public void optionalThirteenRequiresAheadRoomAndKeepsNormalDelayedRecovery()
	{
		Room room = room(14, 108, 3298, 4248); room.spawns[13] = 100;
		Crab big = crab(room, 13, 100, Lane.EAST, 'S', true, Style.MELEE, 3299, 4248);
		assertSame(big, mage.decide(room, 109).target);
		for (int i = 0; i < 6; i++) crab(room, 14, 108, Lane.SPLIT, (char) 0, false, Style.RANGE, 3293, 4250);
		assertNotSame(big, mage.decide(room, 109).target);
		room.wave = 17; room.spawns[17] = 132;
		assertSame(big, mage.decide(room, 135).target);
	}

	@Test public void seventeenAllowsOnlyOneConvenientLaneCastWithoutChasing()
	{
		Room room = room(17, 100, 3301, 4249);
		Crab east = crab(room, 17, 100, Lane.EAST, 'S', true, Style.MAGIC, 3308, 4248);
		Decision first = mage.decide(room, 101);
		assertSame(east, first.target); assertEquals(Weapon.EYE, first.weapon);
		mage.attacked(first, 102, 101); room.ready = 104;
		east.hp = 16; east.pendingDamage = 0;
		assertNotSame(east, mage.decide(room, 104).target);
		mage.reset(); room.player = tile(3292, 4249);
		assertNotSame(east, mage.decide(room, 101).target);
	}

	@Test public void seventeenLeavesUsefulOrdinaryWorkAndCrowdedRoomsAlone()
	{
		Room room = room(17, 100, 3301, 4249);
		Crab east = crab(room, 17, 100, Lane.EAST, 'S', true, Style.MAGIC, 3308, 4248);
		Crab small = crab(room, 17, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3300, 4249);
		assertSame(small, mage.decide(room, 101).target);
		room.crabs.remove(small);
		for (int i = 0; i < 6; i++) crab(room, 17, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4250);
		assertNotSame(east, mage.decide(room, 101).target);
	}

	@Test public void delayedTwentyTwoUsesEastFightingBigAfterClearingMagic()
	{
		Room room = room(22, 100, 3295, 4245);
		Crab east = crab(room, 22, 100, Lane.EAST, 'S', true, Style.MELEE, 3296, 4245); east.fighting = true;
		crab(room, 22, 100, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4244);
		Crab blue = crab(room, 21, 99, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4245);
		Decision clear = mage.decide(room, 109);
		assertSame(blue, clear.target); assertEquals(Weapon.EYE, clear.weapon);
		mage.attacked(clear, 110, 109); blue.pendingDamage = 20; room.ready = 112;
		Decision sweep = mage.decide(room, 113);
		assertSame(east, sweep.target); assertEquals(Weapon.DINH, sweep.weapon); assertTrue(sweep.special);
	}

	@Test public void unsafeTwentyTwoCollateralYieldsUsefulClearingInsteadOfStaging()
	{
		Room room = room(23, 112, 3297, 4248); room.spawns[22] = 100;
		Crab east = crab(room, 22, 100, Lane.EAST, 'S', true, Style.MELEE, 3298, 4248); east.fighting = true;
		Crab nulled = crab(room, 20, 76, Lane.EAST, 'S', true, Style.RANGE, 3299, 4251); nulled.partnerNulled = true;
		Crab blue = crab(room, 21, 99, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4247);
		Decision clear = mage.decide(room, 113);
		assertNotNull(clear.target); assertFalse(clear.special);
	}

	@Test public void instaTwentyNineSweepPreservesPartnerNulledMagicCollateral()
	{
		Room room = room(29, 104, 3295, 4243); room.spawns[28] = 100;
		crab(room, 29, 104, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4241);
		Crab blue = crab(room, 28, 103, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4244);
		blue.partnerNulled = true;
		assertFalse(mage.decide(room, 111).special);
	}

	@Test public void twentyFourHandsBothRangerParentsOverAndStartsEastMelee()
	{
		Room room = room(24, 100, 3299, 4248);
		Crab east = crab(room, 24, 100, Lane.EAST, 'S', true, Style.MELEE, 3302, 4248);
		crab(room, 24, 100, Lane.WEST, 'S', true, Style.MAGIC, 3290, 4248);
		crab(room, 24, 100, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4242);
		Decision decision = mage.decide(room, 106);
		assertSame(east, decision.target); assertEquals(Weapon.SCYTHE, decision.weapon);
	}

	@Test public void zeroActualTwentyEightRetriesChangesTheDoorwayToDinh()
	{
		Room room = room(29, 104, 3295, 4243); room.spawns[28] = 100;
		assertEquals(WaveSchedule.delayAfter(28), room.spawns[29] - room.spawns[28]);
		Crab south = crab(room, 29, 104, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4241);
		Decision sweep = mage.decide(room, 111);
		assertSame(south, sweep.target); assertEquals("29 south Dinh", sweep.rule); assertTrue(sweep.special);
		assertEquals(Weapon.DINH, sweep.weapon);
	}

	@Test public void aRealTwentyEightRetryKeepsTheNormalSouthTwentyNineScythe()
	{
		Room room = room(29, 108, 3295, 4243); room.spawns[28] = 100;
		Crab south = crab(room, 29, 108, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4241);
		Decision decision = mage.decide(room, 115);
		assertSame(south, decision.target); assertEquals(Weapon.SCYTHE, decision.weapon); assertFalse(decision.special);
	}

	@Test public void instaTwentyNineWithoutEnergyFallsBackToScythe()
	{
		Room room = room(29, 104, 3295, 4243); room.spawns[28] = 100; room.specialEnergy = 499;
		Crab south = crab(room, 29, 104, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4241);
		Decision decision = mage.decide(room, 111);
		assertSame(south, decision.target); assertEquals(Weapon.SCYTHE, decision.weapon); assertFalse(decision.special);
	}
}
