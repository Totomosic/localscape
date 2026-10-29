package org.localscape.nylocas.mage;

import org.localscape.nylocas.mage.MageStrategy;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MageStrategyTest
{
	private final MageStrategy strategy = new MageStrategy();
	private int serial;
	private Room room(int wave, int born, int x, int y)
	{
		Room r = new Room(); r.wave = wave; r.spawns[wave] = born; r.player = MageStrategy.tile(x, y); r.specialEnergy = 1000; return r;
	}
	private Crab crab(Room r, int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(++serial, serial, born, wave, lane, slot, big, style, MageStrategy.tile(x, y)); r.crabs.add(c); return c;
	}

	@Test public void openingOwnsSouthAndLeavesRangedToPartner()
	{
		Room r = room(1, 4, 3296, 4244);
		crab(r, 1, 4, Lane.WEST, 'S', false, Style.RANGE, 3281, 4248);
		Crab south = crab(r, 1, 4, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4234);
		assertSame(south, strategy.decide(r, 5).target); assertEquals(Weapon.SHADOW, strategy.decide(r, 5).weapon);
		r.shadowOpening = false; assertEquals(Weapon.EYE, strategy.decide(r, 5).weapon);
	}
	@Test public void newSmallOutranksOlderComparableWork()
	{
		Room r = room(18, 100, 3295, 4248);
		crab(r, 16, 80, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4248);
		Crab fresh = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4248);
		assertSame(fresh, strategy.decide(r, 102).target);
	}
	@Test public void ignoresDyingClaimedAndExpiringTargets()
	{
		Room r = room(18, 100, 3295, 4248);
		Crab claimed = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4248); claimed.claimedUntil = 110;
		Crab dying = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4248); dying.dying = true;
		crab(r, 10, 50, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295, 4247);
		assertNull(strategy.decide(r, 102).target);
	}
	@Test public void scythe12PrefersSouthernNonFightingPrimaryAndCarriesInto13()
	{
		Room r = room(13, 108, 3301, 4249); r.spawns[12] = 100;
		crab(r, 12, 100, Lane.EAST, 'N', false, Style.MELEE, 3302, 4249).fighting = true;
		Crab south = crab(r, 12, 100, Lane.EAST, 'S', false, Style.MELEE, 3302, 4248);
		Decision d = strategy.decide(r, 108);
		assertSame(south, d.target); assertEquals("12 east scythe", d.rule);
	}
	@Test public void reservesScythe12ThenWestDinhFiveRequestTicksApart()
	{
		Room r = room(12, 100, 3301, 4249);
		Crab east = crab(r, 12, 100, Lane.EAST, 'S', false, Style.MELEE, 3303, 4248);
		crab(r, 12, 105, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3300, 4249);
		assertNull(strategy.decide(r, 106).target);
		Decision scythe = strategy.decide(r, 107); assertSame(east, scythe.target);
		strategy.attacked(scythe, 108); r.ready = 112; r.wave = 13; r.spawns[13] = 108;
		Crab west = crab(r, 12, 100, Lane.WEST, 'S', false, Style.MELEE, 3292, 4248);
		r.player = MageStrategy.tile(3293, 4249);
		assertEquals(Weapon.DINH, strategy.decide(r, 110).weapon); assertNull(strategy.decide(r, 110).target);
		Decision dinh = strategy.decide(r, 112); assertSame(west, dinh.target); assertTrue(dinh.special);
	}
	@Test public void unacknowledgedScytheCanRetryAndDoesNotFinishPacket()
	{
		Room r = room(12, 100, 3301, 4249);
		Crab east = crab(r, 12, 100, Lane.EAST, 'S', false, Style.MELEE, 3302, 4248);
		assertSame(east, strategy.decide(r, 107).target); assertSame(east, strategy.decide(r, 108).target);
	}
	@Test public void animationCallbackOnRequestTickCannotPullDinhForward()
	{
		Room r = room(12, 100, 3301, 4249);
		crab(r, 12, 100, Lane.EAST, 'S', false, Style.MELEE, 3303, 4248);
		Decision first = strategy.decide(r, 107);
		strategy.attacked(first, 107, 107);
		r.player = MageStrategy.tile(3293, 4249);
		Crab west = crab(r, 12, 100, Lane.WEST, 'S', false, Style.MELEE, 3292, 4248);
		assertNull(strategy.decide(r, 111).target);
		assertSame(west, strategy.decide(r, 112).target);
	}
	@Test public void westBarrierBarrage21ThenNorthernSplitAtFiveTicks()
	{
		Room r = room(21, 168, 3290, 4249);
		Crab west = crab(r, 21, 168, Lane.WEST, 'N', false, Style.MAGIC, 3281, 4249);
		Decision barrage = strategy.decide(r, 168); assertSame(west, barrage.target); assertEquals(Weapon.BARRAGE, barrage.weapon);
		strategy.attacked(barrage, 169); r.ready = 173; r.player = MageStrategy.tile(3291, 4251);
		Crab north = crab(r, 20, 160, Lane.SPLIT, (char) 0, false, Style.MELEE, 3291, 4252);
		crab(r, 21, 170, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3291, 4250);
		assertEquals(Weapon.SULPHUR, strategy.decide(r, 171).weapon);
		assertSame(north, strategy.decide(r, 173).target);
	}
	@Test public void wave20ReservesWestBarrageIncludingTravel()
	{
		Room r = room(20, 152, 3296, 4246);
		crab(r, 20, 164, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4246);
		Decision d = strategy.decide(r, 166); assertNull(d.target); assertEquals(MageStrategy.tile(3290, 4249), d.tile);
	}
	@Test public void southMagicPreparationOutranksOrdinaryMelee()
	{
		Room r = room(20, 152, 3295, 4246);
		Crab magic = crab(r, 20, 158, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4245);
		crab(r, 20, 160, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295, 4245);
		assertSame(magic, strategy.decide(r, 161).target);
	}
	@Test public void southWest22SpecialUsesCurrentMeleeNotBirthColor()
	{
		Room r = room(22, 180, 3295, 4244);
		Crab small = crab(r, 22, 180, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4242);
		Decision d = strategy.decide(r, 189); assertSame(small, d.target); assertTrue(d.special);
	}
	@Test public void late22FallbackIsEastFightingBigNotWestPillarBig()
	{
		Room r = room(23, 192, 3295, 4244); r.spawns[22] = 180;
		crab(r, 22, 180, Lane.WEST, 'S', true, Style.MELEE, 3294, 4244);
		Crab fighter = crab(r, 22, 180, Lane.EAST, 'S', true, Style.MELEE, 3295, 4245); fighter.fighting = true;
		assertSame(fighter, strategy.decide(r, 194).target); assertEquals(Weapon.DINH, strategy.decide(r, 194).weapon);
	}
	@Test public void missed22EntryStillAllowsMagicClearingBeforeBigArrives()
	{
		Room r = room(22, 180, 3295, 4244);
		Crab magic = crab(r, 22, 190, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4246);
		assertSame(magic, strategy.decide(r, 192).target);
	}
	@Test public void noEnergyDoesNotSendAnOrdinaryDinhAttackOrFreezeClearing()
	{
		Room r = room(22, 180, 3295, 4244); r.specialEnergy = 499;
		crab(r, 22, 180, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4242);
		Crab magic = crab(r, 22, 188, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4245);
		Decision d = strategy.decide(r, 189); assertSame(magic, d.target); assertFalse(d.special);
	}
	@Test public void defers13BigAndDoesNotOpen15Or17MagicBigs()
	{
		Room r = room(17, 124, 3295, 4249);
		Crab deferred = crab(r, 13, 96, Lane.EAST, 'S', true, Style.MELEE, 3294, 4248);
		crab(r, 17, 124, Lane.WEST, 'S', true, Style.MAGIC, 3294, 4249);
		crab(r, 15, 112, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4249);
		assertSame(deferred, strategy.decide(r, 127).target);
		r.crabs.remove(deferred); assertNull(strategy.decide(r, 127).target);
	}
	@Test public void preCapBigCannotBeOpenedBeforeDeferredWindow()
	{
		Room r = room(14, 104, 3295, 4248);
		crab(r, 13, 96, Lane.EAST, 'S', true, Style.MELEE, 3295, 4249);
		assertNull(strategy.decide(r, 110).target);
	}
	@Test public void postCapDoesNotMeanEveryMagicBigIsEligible()
	{
		Room r = room(27, 220, 3295, 4248);
		crab(r, 23, 192, Lane.EAST, 'S', true, Style.MAGIC, 3295, 4248);
		crab(r, 25, 208, Lane.EAST, 'S', true, Style.MAGIC, 3295, 4248);
		crab(r, 26, 216, Lane.EAST, 'S', true, Style.MAGIC, 3295, 4248);
		assertNull(strategy.decide(r, 224).target);
	}
	@Test public void south19ContinuationIsAllowedAfter20ButSouth20OpenerWins()
	{
		Room r = room(20, 152, 3296, 4243);
		Crab old = crab(r, 19, 140, Lane.SOUTH, 'W', true, Style.MAGIC, 3296, 4244);
		Crab current = crab(r, 20, 152, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4235);
		assertSame(current, strategy.decide(r, 154).target);
		r.crabs.remove(current); assertSame(old, strategy.decide(r, 154).target);
	}
	@Test public void west26And27AreMageAssignmentsButSavedSouth26IsProtected()
	{
		Room r = room(27, 220, 3291, 4248);
		Crab west = crab(r, 26, 216, Lane.WEST, 'S', true, Style.MAGIC, 3287, 4248);
		crab(r, 26, 216, Lane.SOUTH, 'W', true, Style.MELEE, 3291, 4246);
		assertSame(west, strategy.decide(r, 220).target);
	}
	@Test public void lateDinhRequiresDoorwayGeometryAndNeverFiresOn26()
	{
		Room r = room(26, 216, 3293, 4245);
		Crab big = crab(r, 26, 216, Lane.SOUTH, 'W', true, Style.MELEE, 3293, 4243);
		assertNull(strategy.decide(r, 218).target);
		r.wave = 28; r.spawns[28] = 228;
		crab(r, 28, 228, Lane.WEST, 'N', false, Style.MELEE, 3289, 4249);
		Crab south = crab(r, 28, 228, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4241);
		Decision d = strategy.decide(r, 236); assertSame(big, d.target); assertTrue(d.special);
		south.point = MageStrategy.tile(3296, 4243); assertNull(strategy.decide(r, 236).target);
	}
	@Test public void cutoffAt29CancelsEvenFresh29BigAndLateDinh()
	{
		Room r = room(29, 240, 3295, 4243); r.spawns[28] = 228;
		crab(r, 29, 240, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4244);
		crab(r, 26, 216, Lane.SOUTH, 'W', true, Style.MELEE, 3293, 4243);
		crab(r, 29, 240, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4244);
		assertNull(strategy.decide(r, 242).target);
	}
	@Test public void fresh30ChildIsEligibleDespiteOldParentAnd30BigCanFinishIn31()
	{
		Room r = room(31, 256, 3297, 4249);
		Crab big = crab(r, 30, 248, Lane.EAST, 'S', true, Style.MAGIC, 3301, 4248);
		assertSame(big, strategy.decide(r, 258).target);
		r.crabs.remove(big); Crab child = crab(r, 30, 253, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3297, 4248);
		assertSame(child, strategy.decide(r, 258).target);
	}
	@Test public void avoidsRangerMagicAndChangedColorRequestWindows()
	{
		Room r = room(16, 120, 3295, 4248);
		crab(r, 15, 112, Lane.EAST, 'N', false, Style.MAGIC, 3295, 4249);
		Crab flicker = crab(r, 16, 120, Lane.EAST, 'S', false, Style.MAGIC, 3295, 4248);
		assertFalse(MageStrategy.matchesNext(flicker, Style.MAGIC, 124));
		assertNull(strategy.decide(r, 124).target);
	}
	@Test public void attackCooldownApproachesWithoutCompletingOrRepeatingAttack()
	{
		Room r = room(24, 200, 3295, 4243); r.ready = 205;
		Crab target = crab(r, 24, 200, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4234);
		Decision d = strategy.decide(r, 201); assertNull(d.target); assertSame(target, d.approach);
	}
}
