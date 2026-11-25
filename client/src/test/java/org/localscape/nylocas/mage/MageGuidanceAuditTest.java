package org.localscape.nylocas.mage;

import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Jordan's guidance at the actual policy observation seam, including native frame witnesses. */
public class MageGuidanceAuditTest
{
	private final MageStrategy strategy = new MageStrategy();
	private Room room(int wave, int born, int x, int y)
	{
		Room r = new Room(); r.wave = wave; r.spawns[wave] = born; r.player = MageStrategy.tile(x, y); return r;
	}
	private Crab crab(Room r, int key, int wave, int born, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab c = new Crab(key, key, born, wave, lane, slot, big, style, MageStrategy.tile(x, y)); r.crabs.add(c); return c;
	}
	private Crab selected(Decision d) { return d.target != null ? d.target : d.approach; }

	@Test public void leavesWave3LaneMeleeWhilePreparing4And5()
	{
		Room r = room(3, 100, 3295, 4248);
		crab(r, 1, 3, 100, Lane.WEST, 'N', false, Style.MELEE, 3294, 4248);
		assertNull(selected(strategy.decide(r, 108)));
	}
	@Test public void doesNotConfuseFreshMeleeSplitWithTheDeferred3LaneSmall()
	{
		Room r = room(3, 100, 3295, 4248);
		Crab split = crab(r, 1, 3, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3294, 4248);
		assertSame(split, selected(strategy.decide(r, 108)));
	}
	@Test public void west26SurvivorPrecedesTheNewerWest27()
	{
		Room r = room(27, 108, 3291, 4248);
		Crab old = crab(r, 1, 26, 100, Lane.WEST, 'S', true, Style.MAGIC, 3288, 4248);
		crab(r, 2, 27, 108, Lane.WEST, 'S', true, Style.MAGIC, 3288, 4248);
		assertSame(old, selected(strategy.decide(r, 111)));
	}
	@Test public void west26NullAllowsAnEarlyWest27Opening()
	{
		Room r = room(27, 108, 3291, 4248);
		Crab old = crab(r, 1, 26, 100, Lane.WEST, 'S', true, Style.MAGIC, 3288, 4248); old.nulled = true;
		Crab current = crab(r, 2, 27, 108, Lane.WEST, 'S', true, Style.MAGIC, 3288, 4248);
		assertSame(current, selected(strategy.decide(r, 111)));
	}
	@Test public void doesNotOpenWest27AfterItsEntryWindow()
	{
		Room r = room(28, 116, 3291, 4248);
		crab(r, 1, 27, 108, Lane.WEST, 'S', true, Style.MAGIC, 3290, 4248);
		assertNull(selected(strategy.decide(r, 118)));
	}
	@Test public void backsUpSouth23EvenWhenTheRangerRolledZero()
	{
		Room r = room(23, 100, 3295, 4244);
		Crab big = crab(r, 1, 23, 100, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4243);
		assertSame(big, selected(strategy.decide(r, 111)));
	}
	@Test public void south23BackupWaitsForTheNativePartnerFlight()
	{
		Room r = room(23, 100, 3295, 4244);
		Crab big = crab(r, 1, 23, 100, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4243); big.claimedUntil = 113;
		assertNull(selected(strategy.decide(r, 111)));
	}
	@Test public void recoversWest24AfterTheProtectedRangerOpening()
	{
		Room r = room(25, 108, 3291, 4248);
		Crab big = crab(r, 1, 24, 100, Lane.WEST, 'S', true, Style.MELEE, 3289, 4248); big.hp = 5;
		assertSame(big, selected(strategy.decide(r, 110)));
	}
	@Test public void south4SurvivorGetsAnotherAyakBeforeTheNewMagicSmall()
	{
		Room r = room(4, 100, 3295, 4243);
		Crab big = crab(r, 1, 4, 100, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4240);
		strategy.attacked(strategy.decide(r, 101), 102, 101);
		r.ready = 104; r.wave = 5; r.spawns[5] = 104; big.hp = 5;
		crab(r, 2, 5, 104, Lane.EAST, 'N', false, Style.MAGIC, 3300, 4249);
		assertSame(big, selected(strategy.decide(r, 104)));
	}
	@Test public void nativeSouth11FrameShootsSouthOnceBeforeOpeningWestBig()
	{
		Room r = room(11, 76, 3294, 4247);
		crab(r, 44, 11, 76, Lane.WEST, 'S', true, Style.MAGIC, 3286, 4248);
		Crab south = crab(r, 45, 11, 76, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4238);
		assertSame(south, selected(strategy.decide(r, 81)));
	}
	@Test public void nativeEast19FrameDoesNotLetLowerSouthKeyReverseTheOrder()
	{
		Room r = room(19, 148, 3296, 4243);
		crab(r, 91, 19, 148, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4240);
		Crab east = crab(r, 92, 19, 148, Lane.EAST, 'S', true, Style.MAGIC, 3302, 4248);
		assertSame(east, selected(strategy.decide(r, 155)));
	}
	@Test public void east19NullYieldsToTheSouthParent()
	{
		Room r = room(19, 148, 3296, 4243);
		Crab south = crab(r, 91, 19, 148, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4240);
		Crab east = crab(r, 92, 19, 148, Lane.EAST, 'S', true, Style.MAGIC, 3302, 4248); east.nulled = true;
		assertSame(south, selected(strategy.decide(r, 155)));
	}
	@Test public void doesNotAbandonUnopenedRequiredBigAfterItsRotationWindow()
	{
		Room r = room(27, 124, 3299, 4248);
		Crab east = crab(r, 1, 24, 100, Lane.EAST, 'S', true, Style.MELEE, 3300, 4248);
		assertSame(east, selected(strategy.decide(r, 124)));
	}

	@Test public void requiredSouth24SurvivorDoesNotLoseEveryReadyTickToNewSmalls()
	{
		Room r = room(27, 124, 3295, 4248);
		Crab south = crab(r, 1, 24, 100, Lane.SOUTH, 'W', true, Style.MAGIC, 3296, 4248); south.hp = 1;
		crab(r, 2, 27, 124, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295, 4249);
		assertSame(south, selected(strategy.decide(r, 124)));
	}

	@Test public void south4SurvivorReplacesAnEast4ApproachChosenDuringItsFlight()
	{
		Room r = room(5, 104, 3299, 4247); r.ready = 112;
		Crab south = crab(r, 11, 4, 100, Lane.SOUTH, 'W', true, Style.MAGIC, 3293, 4243);
		Crab east = crab(r, 12, 4, 100, Lane.EAST, 'S', false, Style.MELEE, 3301, 4249);
		strategy.attacked(new Decision(south, Weapon.EYE, south.point, false, "South big 4", "South big 4"), 109, 108);
		south.hp = 8; south.ownHitHp = 8; south.ownHitUntil = 111;
		assertSame(east, selected(strategy.decide(r, 110)));
		south.hp = 7; south.ownHitUntil = 0;
		assertSame(south, selected(strategy.decide(r, 112)));
	}
	@Test public void south24SurvivorReplacesSameWaveSmallApproachAfterTheOpeningWindow()
	{
		Room r = room(27, 108, 3299, 4247); r.ready = 112;
		Crab south = crab(r, 1, 24, 88, Lane.SOUTH, 'W', true, Style.MAGIC, 3293, 4243);
		Crab split = crab(r, 2, 24, 88, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4249);
		strategy.attacked(new Decision(south, Weapon.EYE, south.point, false, null, "Fresh MAGIC assigned big"), 109, 108);
		south.hp = 8; south.ownHitHp = 8; south.ownHitUntil = 111;
		assertSame(split, selected(strategy.decide(r, 110)));
		south.hp = 7; south.ownHitUntil = 0;
		assertSame(south, selected(strategy.decide(r, 112)));
	}

	@Test public void firstActionable21TickAfterFoodStillBarragesTheWestGroup()
	{
		Room r = room(21, 100, 3290, 4249); r.ready = 102;
		Crab west = crab(r, 1, 21, 100, Lane.WEST, 'N', false, Style.MAGIC, 3283, 4249);
		assertNull(strategy.decide(r, 100).target);
		Decision d = strategy.decide(r, 102);
		assertSame(west, d.target); assertEquals(Weapon.BARRAGE, d.weapon); assertEquals("21 west barrage", d.rule);
	}
	@Test public void late21RecoveryDoesNotCastAcrossTheUpcomingWrongStyleFlicker()
	{
		Room r = room(21, 100, 3290, 4249); r.ready = 104;
		Crab west = crab(r, 1, 21, 100, Lane.WEST, 'N', false, Style.MAGIC, 3285, 4249);
		Decision d = strategy.decide(r, 104);
		assertFalse(d.target == west && d.weapon == Weapon.BARRAGE);
	}

}
