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
	@Test public void defers13BigAndDoesNotOpen15Or17MagicBigsDuring17()
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
	@Test public void ignoreAllBirthsThrough29OnlyAfterAllWavesHaveSpawned()
	{
		Room r = room(29, 240, 3295, 4243); r.spawns[28] = 228;
		Crab wave29 = crab(r, 29, 240, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4244);
		assertSame(wave29, strategy.decide(r, 242).target);
		r.wave = 30; r.spawns[30] = 244;
		assertSame(wave29, strategy.decide(r, 244).target);
		r.wave = 31; r.spawns[31] = 248;
		assertNull(strategy.decide(r, 248).target);
		Crab older = crab(r, 28, 230, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4244);
		assertNull(strategy.decide(r, 248).target);
	}
	@Test public void fresh30ChildIsEligibleDespiteOldParentAnd30BigCanFinishIn31()
	{
		Room r = room(31, 256, 3297, 4249);
		Crab big = crab(r, 30, 248, Lane.EAST, 'S', true, Style.MAGIC, 3301, 4248);
		assertSame(big, strategy.decide(r, 258).target);
		r.crabs.remove(big); Crab child = crab(r, 30, 253, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3297, 4248);
		assertSame(child, strategy.decide(r, 258).target);
	}
	@Test public void leavesUnclaimedPartnerMagicAndFlickersToTheRanger()
	{
		Room r = room(16, 120, 3295, 4248);
		Crab partnerWork = crab(r, 15, 112, Lane.EAST, 'N', false, Style.MAGIC, 3295, 4249);
		Crab flicker = crab(r, 16, 120, Lane.EAST, 'S', false, Style.MAGIC, 3295, 4248);
		assertFalse(MageStrategy.matchesNext(flicker, Style.MAGIC, 124));
		assertNull(strategy.decide(r, 124).target);
		partnerWork.claimedUntil = 130;
		assertNull(strategy.decide(r, 124).target);
	}
	@Test public void attackCooldownApproachesWithoutCompletingOrRepeatingAttack()
	{
		Room r = room(24, 200, 3295, 4243); r.ready = 205;
		Crab target = crab(r, 24, 200, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4234);
		Decision d = strategy.decide(r, 201); assertNull(d.target); assertSame(target, d.approach);
	}
	@Test public void approachCommitsAcrossNewSpawnsUntilAnAttackIsConfirmed()
	{
		Room r = room(18, 100, 3295, 4248); r.ready = 104;
		Crab melee = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		assertSame(melee, strategy.decide(r, 100).approach);
		r.player = MageStrategy.tile(3297, 4248);
		Crab fresh = crab(r, 18, 101, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3292, 4248);
		assertSame(melee, strategy.decide(r, 101).approach);
		r.player = MageStrategy.tile(3300, 4248);
		Decision hit = strategy.decide(r, 104);
		assertSame(melee, hit.target);
		strategy.attacked(hit, 105);
		assertSame(fresh, strategy.decide(r, 105).target);
	}
	@Test public void readyAttackWinsOverStartingACrossRoomMeleeChase()
	{
		Room r = room(18, 100, 3290, 4248);
		crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		Crab local = crab(r, 18, 98, Lane.SPLIT, (char) 0, false, Style.MELEE, 3291, 4248);
		assertSame(local, strategy.decide(r, 100).target);
	}
	@Test public void aUsefulEarlierAttackCanInterruptAnApproach()
	{
		Room r = room(18, 100, 3290, 4248);
		Crab distant = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		Decision approach = strategy.decide(r, 100);
		assertSame(distant, approach.target); assertTrue(approach.pathToTarget);
		r.player = MageStrategy.tile(3292, 4248);
		Crab local = crab(r, 18, 101, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3293, 4248);
		assertSame(local, strategy.decide(r, 101).target);
	}
	@Test public void invalidIntentIsReleasedOnTheNextObservation()
	{
		for (int invalidation = 0; invalidation < 5; invalidation++)
		{
			strategy.reset();
			Room r = room(18, 100, 3295, 4248); r.ready = 104;
			Crab original = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
			assertSame(original, strategy.decide(r, 100).approach);
			Crab replacement = crab(r, 18, 101, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4248);
			switch (invalidation)
			{
				case 0: original.dying = true; break;
				case 1: original.claimedUntil = 110; break;
				case 2: r.crabs.remove(original); break;
				case 3: original.style = Style.RANGE; break;
				case 4: r.obstructed.add(original.key); r.ready = 101; break;
			}
			Decision decision = strategy.decide(r, 101);
			assertSame(replacement, decision.target != null ? decision.target : decision.approach);
		}
	}
	@Test public void approachAndCooldownOverlapWithoutMakingScythePacketLate()
	{
		Room r = room(12, 108, 3295, 4248); r.ready = 112;
		Crab magic = crab(r, 12, 109, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3308, 4248);
		assertSame(magic, strategy.decide(r, 109).approach);
		r.player = MageStrategy.tile(3300, 4248);
		Decision cast = strategy.decide(r, 112);
		assertSame(magic, cast.target);
		strategy.attacked(cast, 113); r.ready = 115;
		Crab east = crab(r, 12, 108, Lane.EAST, 'S', false, Style.MELEE, 3302, 4248);
		assertNull(strategy.decide(r, 113).target);
		assertEquals(MageStrategy.tile(3301, 4249), strategy.decide(r, 113).tile);
		assertSame(east, strategy.decide(r, 115).target);
	}
	@Test public void finalWaveCutoffAndResetReleaseAnExistingApproach()
	{
		Room r = room(29, 100, 3290, 4248); r.specialEnergy = 0;
		Crab original = crab(r, 29, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		assertSame(original, strategy.decide(r, 100).target);
		r.wave = 31; r.spawns[31] = 101;
		assertNull(strategy.decide(r, 101).target);
		strategy.reset(); r.crabs.clear(); r.wave = 18; r.ready = 105;
		Crab fresh = crab(r, 18, 101, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3291, 4248);
		assertSame(fresh, strategy.decide(r, 101).approach);
	}
	@Test public void commitmentYieldsWhenRemainingTravelWouldMakeAPacketLate()
	{
		Room r = room(12, 108, 3295, 4248);
		Crab melee = crab(r, 12, 108, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		Decision approach = strategy.decide(r, 108);
		assertSame(melee, approach.target); assertTrue(approach.pathToTarget);
		// No movement progress for two ticks: even moving and attacking in the
		// same cycle can no longer fit a clearing hit before +7.
		Decision reserve = strategy.decide(r, 110);
		assertNull(reserve.target); assertNull(reserve.approach);
		assertEquals(MageStrategy.tile(3301, 4249), reserve.tile);
		r.player = MageStrategy.tile(3301, 4249);
		Crab east = crab(r, 12, 108, Lane.EAST, 'S', false, Style.MELEE, 3302, 4248);
		assertSame(east, strategy.decide(r, 115).target);
	}
	@Test public void useAnotherTargetWhileAnUnresolvedProjectileCanKillThePreviousOne()
	{
		Room r = room(18, 100, 3295, 4248);
		Crab original = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4248);
		strategy.attacked(strategy.decide(r, 100), 101);
		r.ready = 103;
		Crab melee = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295, 4249);
		assertSame(melee, strategy.decide(r, 103).target);
		// A real HP decrease resolves the uncertainty and makes a useful finish available.
		original.hp = 3;
		melee.dying = true;
		assertSame(original, strategy.decide(r, 103).target);
	}
	@Test public void blockedLineOfSightUsesARepositionRatherThanRepeatingAnAttack()
	{
		Room r = room(18, 100, 3295, 4248);
		Crab blocked = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3292, 4243);
		r.obstructed.add(blocked.key);
		Decision decision = strategy.decide(r, 100);
		assertNull(decision.target); assertSame(blocked, decision.approach); assertFalse(decision.pathToTarget);
	}
	@Test public void ordinaryLaneWorkAttacksAtItsFirstUsefulTick()
	{
		Room r = room(19, 100, 3296, 4248);
		Crab east = crab(r, 19, 100, Lane.EAST, 'S', true, Style.MAGIC, 3308, 4248);
		Decision decision = strategy.decide(r, 100);
		assertSame(east, decision.target);
		assertEquals("East big 19", decision.rule);
		assertSame(east, strategy.decide(r, 103).target);
	}
	@Test public void quietPreCapGapDoesNotOpenAnUnassignedBig()
	{
		Room r = room(18, 112, 3295, 4248);
		Crab old = crab(r, 17, 100, Lane.WEST, 'S', true, Style.MAGIC, 3296, 4248);
		assertNull(strategy.decide(r, 112).target);
		Crab body = crab(r, 17, 105, Lane.SPLIT, (char) 0, false, Style.MELEE, 3294, 4248);
		body.dying = true;
		assertNull(strategy.decide(r, 112).target);
	}
	@Test public void south29DeadlineTakesPriorityDuring30AndCancelsAt31()
	{
		Room r = room(30, 104, 3296, 4248); r.spawns[29] = 100;
		Crab lastChance = crab(r, 29, 100, Lane.SOUTH, 'W', true, Style.MELEE, 3297, 4248);
		Crab fresh = crab(r, 30, 104, Lane.EAST, 'S', true, Style.MAGIC, 3300, 4248);
		assertSame(lastChance, strategy.decide(r, 105).target);
		r.wave = 31; r.spawns[31] = 108;
		assertSame(fresh, strategy.decide(r, 108).target);
	}
	@Test public void capRetriesPreferNewWave29ChildrenWithoutInventingAnotherWave()
	{
		Room r = room(29, 100, 3295, 4248);
		Crab olderBig = crab(r, 27, 88, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4248);
		Crab child = crab(r, 29, 128, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		assertSame(child, strategy.decide(r, 130).target);
		assertEquals(29, r.wave); assertEquals(-1, r.spawns[30]);
		r.wave = 31; r.spawns[30] = 130; r.spawns[31] = 134;
		assertNull(strategy.decide(r, 134).target);
	}
	@Test public void predictedFinalSpawnTickDoesNotDiscardWorkBeforeTheRealWaveMarker()
	{
		Room r = room(30, 126, 3295, 4248);
		crab(r, 27, 88, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4248);
		Crab child = crab(r, 29, 128, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		assertSame(child, strategy.decide(r, 130).target);
		assertEquals(30, r.wave); assertEquals(-1, r.spawns[31]);
	}
	@Test public void fourWaveLimitAppliesEvenWhenAnOlderTargetIsTheOnlyWork()
	{
		Room r = room(20, 100, 3295, 4248);
		Crab old = crab(r, 15, 90, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		old.hp = 1;
		assertNull(strategy.decide(r, 100).target);
		Crab boundary = crab(r, 16, 90, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		assertSame(boundary, strategy.decide(r, 100).target);
		r.wave = 21;
		assertNull(strategy.decide(r, 101).target);
	}
	@Test public void earlierUsefulAttackWinsOverChasingANewerWave()
	{
		Room r = room(18, 100, 3290, 4248); r.ready = 102;
		Crab old = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3291, 4248);
		assertSame(old, strategy.decide(r, 100).approach);
		r.wave = 19; r.spawns[19] = 101;
		Crab fresh = crab(r, 19, 101, Lane.SPLIT, (char) 0, false, Style.MELEE, 3301, 4248);
		assertSame(old, strategy.decide(r, 101).approach);
	}
	@Test public void assignedTransitionBigOutranksAFreshSplit()
	{
		Room r = room(20, 152, 3296, 4246);
		Crab big = crab(r, 19, 140, Lane.SOUTH, 'W', true, Style.MAGIC, 3296, 4244);
		Crab fresh = crab(r, 20, 153, Lane.SPLIT, (char) 0, false, Style.MELEE, 3297, 4246);
		assertSame(big, strategy.decide(r, 154).target);
	}
	@Test public void blowpipeSupportIsLimitedToTheWest15RotationAssignment()
	{
		Room r = room(15, 100, 3291, 4248);
		crab(r, 15, 100, Lane.EAST, 'S', false, Style.RANGE, 3292, 4248);
		crab(r, 15, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3291, 4249);
		assertNull(strategy.decide(r, 108).target);
		Crab west = crab(r, 15, 100, Lane.WEST, 'S', false, Style.RANGE, 3290, 4248);
		assertSame(west, strategy.decide(r, 108).target);
		assertEquals("West ranged 15", strategy.decide(r, 108).rule);
	}
	@Test public void deliberateDeferredPacketStillWinsOverFreshOrdinaryClearing()
	{
		Room r = room(17, 124, 3295, 4249);
		Crab deferred = crab(r, 13, 96, Lane.EAST, 'S', true, Style.MELEE, 3294, 4248);
		Crab fresh = crab(r, 17, 124, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3296, 4249);
		assertSame(deferred, strategy.decide(r, 127).target);
		r.wave = 18;
		assertSame(fresh, strategy.decide(r, 128).target);
	}
	@Test public void reservesEast11BarrageDuring10AndCastsOnFirstTick()
	{
		Room r = room(10, 100, 3295, 4248);
		crab(r, 10, 104, Lane.SPLIT, (char) 0, false, Style.MELEE, 3296, 4248);
		Decision hold = strategy.decide(r, 106);
		assertNull(hold.target); assertEquals(MageStrategy.tile(3301, 4249), hold.tile);
		r.wave = 11; r.spawns[11] = 108; r.player = MageStrategy.tile(3301, 4249);
		Crab east = crab(r, 11, 108, Lane.EAST, 'N', false, Style.MAGIC, 3310, 4249);
		Decision cast = strategy.decide(r, 108);
		assertSame(east, cast.target); assertEquals(Weapon.BARRAGE, cast.weapon);
	}
	@Test public void missedScythePacketDoesNotProtectItsSurvivorsForever()
	{
		Room r = room(13, 108, 3300, 4248); r.spawns[12] = 100; r.specialEnergy = 0;
		Crab east = crab(r, 12, 100, Lane.EAST, 'S', false, Style.MELEE, 3301, 4248);
		assertSame(east, strategy.decide(r, 114).target);
	}
	@Test public void mageOwnsEveryWave19MagicBigUnlessPartnerClaimedIt()
	{
		Room r = room(19, 100, 3291, 4248);
		Crab west = crab(r, 19, 100, Lane.WEST, 'S', true, Style.MAGIC, 3287, 4248);
		assertSame(west, strategy.decide(r, 103).target);
		west.claimedUntil = 110; assertNull(strategy.decide(r, 103).target);
	}
	@Test public void matchingMeleeSmallWinsBeforeOrdinaryMagicCleanup()
	{
		Room r = room(18, 100, 3295, 4248);
		Crab melee = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3296, 4248);
		crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		assertSame(melee, strategy.decide(r, 102).target);
	}
	@Test public void magicProjectileDoesNotClaimUpcomingRangerLaneFlicker()
	{
		Room r = room(23, 100, 3290, 4249);
		Crab north = crab(r, 23, 100, Lane.WEST, 'N', false, Style.MAGIC, 3285, 4249);
		assertNull(strategy.decide(r, 104).target);
		north.style = Style.MAGIC; north.point = MageStrategy.tile(3291, 4249);
		assertSame(north, strategy.decide(r, 112).target);
	}

	@Test public void wave29SouthBigRemainsAttackableAfterItsFirstEntryTick()
	{
		Room r = room(30, 108, 3295, 4244); r.spawns[29] = 100;
		Crab south = crab(r, 29, 100, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4243);
		Decision attack = strategy.decide(r, 109);
		assertSame(south, attack.target); assertEquals(Weapon.SCYTHE, attack.weapon);
		r.wave = 31; r.spawns[31] = 110;
		assertNull(strategy.decide(r, 110).target);
	}

	@Test public void authoritativeNullStatusExcludesOrdinaryAssignedAndPacketTargets()
	{
		Room r = room(4, 100, 3295, 4243);
		Crab big = crab(r, 4, 100, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4238);
		big.nulled = true;
		assertNull(strategy.decide(r, 104).target);
		Crab useful = crab(r, 4, 103, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295, 4244);
		assertSame(useful, strategy.decide(r, 104).target);
		useful.nulled = true;
		assertNull(strategy.decide(r, 104).target);
		r = room(12, 100, 3301, 4249);
		Crab east = crab(r, 12, 100, Lane.EAST, 'S', false, Style.MELEE, 3302, 4248);
		east.nulled = true;
		assertNull(strategy.decide(r, 107).target);
	}

	@Test public void nulledWestBarragePairDoesNotReserveAwayUsefulFiller()
	{
		Room r = room(21, 100, 3290, 4249);
		crab(r, 21, 100, Lane.WEST, 'N', false, Style.MAGIC, 3281, 4249).nulled = true;
		crab(r, 21, 100, Lane.WEST, 'S', false, Style.MAGIC, 3281, 4248).nulled = true;
		Crab useful = crab(r, 20, 99, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3291, 4249);
		assertSame(useful, strategy.decide(r, 100).target);
	}

	@Test public void distantMeleeDoesNotDisplaceAReadyMagicLaneAttack()
	{
		Room r = room(5, 100, 3300, 4248);
		Crab magic = crab(r, 5, 100, Lane.EAST, 'N', false, Style.MAGIC, 3304, 4249);
		crab(r, 5, 102, Lane.SPLIT, (char) 0, false, Style.MELEE, 3290, 4248);
		assertSame(magic, strategy.decide(r, 103).target);
	}
	@Test public void urgentMagicBigRetainsPriorityOverAnEquallyReadyMeleeSmall()
	{
		Room r = room(4, 100, 3295, 4243);
		Crab big = crab(r, 4, 100, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4238);
		crab(r, 4, 102, Lane.SPLIT, (char) 0, false, Style.MELEE, 3296, 4243);
		assertSame(big, strategy.decide(r, 103).target);
	}

	@Test public void blowpipeRescuesARangedSmallOnlyWhenItsAssignedPartnerIsNulled()
	{
		Room r = room(18, 100, 3295, 4248);
		Crab ranged = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4248);
		assertNull(strategy.decide(r, 102).target);
		ranged.partnerNulled = true;
		Decision rescue = strategy.decide(r, 102);
		assertSame(ranged, rescue.target); assertEquals(Weapon.PIPE, rescue.weapon);
		ranged.nulled = true;
		assertNull(strategy.decide(r, 102).target);
	}
	@Test public void partnerNullRescueRemainsBelowReadyMagicAndMeleeWork()
	{
		Room r = room(18, 100, 3295, 4248);
		Crab ranged = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4248);
		ranged.partnerNulled = true;
		Crab magic = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		assertSame(magic, strategy.decide(r, 102).target);
		strategy.reset();
		Crab melee = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3294, 4248);
		assertSame(melee, strategy.decide(r, 102).target);
	}
	@Test public void partnerNullRescueDoesNotBypassFinalCutoffOrAgeLimit()
	{
		Room r = room(31, 108, 3295, 4248);
		Crab old = crab(r, 29, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4248);
		old.partnerNulled = true;
		assertNull(strategy.decide(r, 108).target);
		Crab fresh = crab(r, 30, 104, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4248);
		fresh.partnerNulled = true;
		assertSame(fresh, strategy.decide(r, 108).target);
		assertNull(strategy.decide(r, 154).target);
	}
	@Test public void partnerNullRescueDoesNotConsumeAPreservedRangerPacket()
	{
		Room r = room(28, 100, 3295, 4243);
		Crab south = crab(r, 28, 100, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4239);
		south.partnerNulled = true;
		assertNull(strategy.decide(r, 105).target);
	}

	@Test public void rescueCommitmentYieldsWhenPrimaryRoleWorkBecomesAvailable()
	{
		Room r = room(18, 100, 3295, 4248); r.ready = 102;
		Crab ranged = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4248);
		ranged.partnerNulled = true;
		assertSame(ranged, strategy.decide(r, 100).approach);
		Crab magic = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4249);
		assertSame(magic, strategy.decide(r, 101).approach);
	}

	@Test public void south29BigWaitsForActual30AndDoesNotInferItThroughCapRetries()
	{
		Room r = room(29, 100, 3295, 4244);
		Crab south = crab(r, 29, 100, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4243);
		assertNull(strategy.decide(r, 109).target);
		assertNull(strategy.decide(r, 124).target);
		assertEquals(-1, r.spawns[30]);
		r.wave = 30; r.spawns[30] = 124;
		assertSame(south, strategy.decide(r, 124).target);
		r.wave = 31; r.spawns[31] = 128;
		assertNull(strategy.decide(r, 128).target);
	}
	@Test public void lastPre31ScytheDeadlineRejectsALongCooldownFiller()
	{
		Room r = room(30, 100, 3295, 4248); r.spawns[29] = 92;
		crab(r, 29, 92, Lane.SOUTH, 'W', true, Style.MELEE, 3295, 4242);
		crab(r, 30, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3296, 4248);
		Decision plan = strategy.decide(r, 100);
		assertFalse(plan.target != null && plan.target.wave == 30);
		assertEquals(Weapon.SCYTHE, plan.weapon);
	}

	@Test public void west28EntryDinhSurvivesAnEarly29AdmissionUntilActual31()
	{
		Room r = room(29, 104, 3293, 4245); r.spawns[28] = 100;
		Crab big = crab(r, 26, 88, Lane.SOUTH, 'W', true, Style.MELEE, 3293, 4243);
		crab(r, 28, 100, Lane.WEST, 'N', false, Style.MELEE, 3289, 4249);
		crab(r, 28, 100, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4241);
		Decision dinh = strategy.decide(r, 108);
		assertSame(big, dinh.target); assertEquals("26 Dinh on 28", dinh.rule);
		r.wave = 31; r.spawns[31] = 108;
		assertFalse("26 Dinh on 28".equals(strategy.decide(r, 108).rule));
	}

	@Test public void twoNativeRunStepsCanReachMeleeBeforeTheSameCycleAttack()
	{
		for (int distance : new int[] {2, 3})
		{
			strategy.reset();
			Room r = room(18, 100, 3295, 4248);
			Crab melee = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295 + distance, 4248);
			crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3301, 4249);
			assertSame(melee, strategy.decide(r, 100).target);
		}
	}
	@Test public void threeRequiredRunStepsDoNotDisplaceAReadyMagicAttack()
	{
		Room r = room(18, 100, 3295, 4248);
		crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE, 3299, 4248);
		Crab magic = crab(r, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3301, 4249);
		assertSame(magic, strategy.decide(r, 100).target);
	}

	private Room fourthDinhRoom()
	{
		Room r = room(25, 200, 3291, 4248); r.extraDinh25Enabled = true;
		r.specialEnergy = 600; r.deathChargeStacks = 2; r.deathChargeExpiry = 280;
		r.specialRegenAt = 225;
		crab(r, 25, 200, Lane.WEST, (char) 0, true, Style.MELEE, 3290, 4248);
		crab(r, 25, 200, Lane.SPLIT, (char) 0, false, Style.MELEE, 3292, 4249);
		crab(r, 25, 200, Lane.SPLIT, (char) 0, false, Style.MELEE, 3293, 4249);
		return r;
	}
	@Test public void fourthDinhCanUseSixHundredEnergyWithActiveChargeAndNativeRegenBudget()
	{
		Room r = fourthDinhRoom();
		Decision d = strategy.decide(r, 206);
		assertEquals("25 west extra Dinh", d.rule); assertEquals(Weapon.DINH, d.weapon);
		assertEquals(25, d.target.wave); assertEquals(Lane.WEST, d.target.lane);
		assertTrue(d.target.big); assertTrue(d.special);
		strategy.attacked(d, 207, 206);
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 208).rule));
	}
	@Test public void fourthDinhCannotSpendTheMandatoryLatePacketWithoutObservedResources()
	{
		Room r = fourthDinhRoom(); r.deathChargeStacks = 0;
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
		r.deathChargeStacks = 2; r.deathChargeExpiry = 207;
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
		r.deathChargeExpiry = 280; r.specialRegenAt = 231;
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
		r.specialRegenAt = 225; r.specialEnergy = 500;
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
	}
	@Test public void fourthDinhUsesEarnedEnergyWithoutAssumingAnotherDeathCharge()
	{
		Room r = fourthDinhRoom(); r.specialEnergy = 900; r.deathChargeStacks = 0;
		assertEquals("25 west extra Dinh", strategy.decide(r, 206).rule);
	}
	@Test public void fourthDinhDoesNotHoldCooldownOrChaseAnOffstageBig()
	{
		Room r = fourthDinhRoom();
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 205).rule));
		r.player = MageStrategy.tile(3295, 4248);
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
		r.player = MageStrategy.tile(3291, 4248); r.ready = 207;
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
		r.ready = 0; r.crabs.get(0).nulled = true;
		assertFalse("25 west extra Dinh".equals(strategy.decide(r, 206).rule));
	}

	@Test public void west19YieldsAnEquallyEarlyEastOrSouthBigDuringRangerPacket()
	{
		Room r = room(19, 100, 3295, 4248);
		Crab west = crab(r, 19, 100, Lane.WEST, (char) 0, true, Style.MAGIC, 3290, 4248);
		Crab east = crab(r, 19, 100, Lane.EAST, (char) 0, true, Style.MAGIC, 3300, 4248);
		Crab south = crab(r, 19, 100, Lane.SOUTH, (char) 0, true, Style.MAGIC, 3295, 4242);
		assertSame(east, strategy.decide(r, 100).target);
		east.dying = true;
		assertSame(south, strategy.decide(r, 101).target);
		assertNotSame(west, strategy.decide(r, 104).target);
	}
	@Test public void west19RetainsImmediateFallbackForPartnerNullOrNoOtherReadyAttack()
	{
		Room r = room(19, 100, 3295, 4248);
		Crab west = crab(r, 19, 100, Lane.WEST, (char) 0, true, Style.MAGIC, 3290, 4248);
		assertSame(west, strategy.decide(r, 100).target);
		Crab east = crab(r, 19, 100, Lane.EAST, (char) 0, true, Style.MAGIC, 3300, 4248);
		west.partnerNulled = true;
		assertSame(west, strategy.decide(r, 101).target);
		west.partnerNulled = false;
		assertSame(east, strategy.decide(r, 102).target);
	}
	@Test public void west19FullPriorityReturnsAfterPartnerWindowButRespectsItsProjectile()
	{
		Room r = room(19, 100, 3295, 4248);
		Crab west = crab(r, 19, 100, Lane.WEST, (char) 0, true, Style.MAGIC, 3290, 4248);
		Crab east = crab(r, 19, 100, Lane.EAST, (char) 0, true, Style.MAGIC, 3300, 4248);
		assertSame(west, strategy.decide(r, 105).target);
		west.claimedUntil = 108;
		assertSame(east, strategy.decide(r, 106).target);
	}

	@Test public void feasibleRangerWest19PacketIsProtectedOnlyThroughItsActualRequest()
	{
		Room r = room(19, 100, 3295, 4248); r.rangerWest19PacketFeasible = true;
		Crab west = crab(r, 19, 100, Lane.WEST, (char) 0, true, Style.MAGIC, 3290, 4248);
		assertNull(strategy.decide(r, 100).target);
		assertNull(strategy.decide(r, 103).target);
		r.rangerWest19PacketFeasible = false;
		assertSame(west, strategy.decide(r, 103).target);
		r.rangerWest19PacketFeasible = true;
		assertSame(west, strategy.decide(r, 107).target);
	}
	@Test public void rangerWest19ProtectionDoesNotSuppressMageOwnedBiggerWorkOrNullRescue()
	{
		Room r = room(19, 100, 3295, 4248); r.rangerWest19PacketFeasible = true;
		Crab west = crab(r, 19, 100, Lane.WEST, (char) 0, true, Style.MAGIC, 3290, 4248);
		Crab east = crab(r, 19, 100, Lane.EAST, (char) 0, true, Style.MAGIC, 3300, 4248);
		assertSame(east, strategy.decide(r, 100).target);
		east.dying = true; west.partnerNulled = true;
		assertSame(west, strategy.decide(r, 101).target);
	}

}
