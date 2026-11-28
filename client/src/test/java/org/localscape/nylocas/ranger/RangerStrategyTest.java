package org.localscape.nylocas.ranger;

import org.localscape.nylocas.ranger.RangerFlickers;
import org.localscape.nylocas.Tile;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import org.junit.Test;

import static org.junit.Assert.*;

public class RangerStrategyTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room(int wave, int spawn)
	{
		Room room = new Room();
		room.wave = wave;
		room.spawns[wave] = spawn;
		if (wave == 31) { room.spawns[28] = spawn - 24; room.spawns[29] = spawn - 8; }
		room.player = RangerStrategy.tile(3295, 4248);
		room.specialEnergy = 1000;
		return room;
	}

	private Crab crab(Room room, int wave, int born, Lane lane, char slot, boolean big, Style style)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(3295, 4246));
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void prefersFreshSmallToOldSmall()
	{
		Room room = room(12, 90);
		crab(room, 8, 65, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Crab fresh = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(fresh, strategy.decide(room, 101).target);
	}

	@Test
	public void freshSplitOutranksAFourWaveOldSmallTheRangerHasNeverHit()
	{
		Room room = room(12, 100);
		Crab damaged = crab(room, 8, 75, Lane.SPLIT, (char) 0, false, Style.RANGE);
		damaged.hp = 2;
		damaged.point = room.player.dx(-3);
		Crab fresh = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.point = room.player.dx(3);
		assertSame(fresh, strategy.decide(room, 101).target);
		assertEquals(Weapon.PIPE, strategy.decide(room, 101).weapon);
	}

	@Test
	public void observedSurvivorIsFinishedInsteadOfOpeningAnotherSmall()
	{
		Room room = room(12, 100);
		Crab survivor = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		survivor.point = room.player.dx(-3);
		Decision first = strategy.decide(room, 100);
		strategy.attacked(first, 101);
		survivor.hp = 3;
		Crab fresh = crab(room, 12, 102, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.point = room.player.dx(3);
		room.ready = 102;
		assertSame(survivor, strategy.decide(room, 102).target);
	}

	@Test
	public void softOpeningAndSouthBowDoNotReserveIdleTicks()
	{
		Room room = room(6, 100);
		Crab split = crab(room, 6, 102, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		assertSame(split, strategy.decide(room, 103).target);
		assertEquals(Weapon.EYE, strategy.decide(room, 103).weapon);
	}

	@Test
	public void softLaneTargetCanBeKilledBeforeItsRecordedOffset()
	{
		Room room = room(13, 100);
		Crab small = crab(room, 13, 100, Lane.WEST, 'N', false, Style.RANGE);
		assertSame(small, strategy.decide(room, 100).target);
	}

	@Test
	public void usefulNearbyAttackWinsOverTravellingToRangedTarget()
	{
		Room room = room(12, 100);
		Crab distant = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		distant.point = RangerStrategy.tile(3310, 4248);
		Crab nearby = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		assertSame(nearby, strategy.decide(room, 100).target);
	}

	@Test
	public void ordinaryReachableBigUsesPipeInsteadOfFiveTickBow()
	{
		Room room = room(11, 124);
		Crab big = crab(room, 9, 100, Lane.WEST, 'S', true, Style.RANGE);
		strategy.attacked(new Decision(big, Weapon.PIPE, room.player, false, null, "Previous hit"), 124);
		big.hp = 10;
		big.point = RangerStrategy.tile(3293, 4248);
		crab(room, 11, 128, Lane.SPLIT, (char) 0, false, Style.RANGE).point = RangerStrategy.tile(3291, 4243);
		crab(room, 11, 128, Lane.SPLIT, (char) 0, false, Style.RANGE).point = RangerStrategy.tile(3298, 4243);
		Decision decision = strategy.decide(room, 128);
		assertSame(big, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void distantSmallCanReceiveAnEarlierChinWithoutUsingTheBow()
	{
		Room room = room(12, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab distant = crab(room, 12, 105, Lane.SPLIT, (char) 0, false, Style.RANGE);
		distant.point = RangerStrategy.tile(3300, 4248);
		Decision decision = strategy.decide(room, 108);
		assertSame(distant, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
		assertFalse(decision.pathToTarget);
	}

	@Test
	public void nativeSameCycleRunningMakesPipeAvailableAtDistanceSixAndSeven()
	{
		for (int distance : new int[]{6, 7})
		{
			strategy.reset();
			Room room = room(12, 100);
			Crab ranged = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
			ranged.point = room.player.dx(distance);
			Decision decision = strategy.decide(room, 100);
			assertSame(ranged, decision.target);
			assertEquals(Weapon.PIPE, decision.weapon);
		}
	}

	@Test
	public void nativeSameCycleReachableFinishCompetesWithNearbyFullHpTarget()
	{
		for (int wave : new int[]{12, 22})
		{
			strategy.reset();
			Room room = room(wave, 100);
			Crab survivor = crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
			survivor.point = room.player.dx(7);
			survivor.hp = 1;
			crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.RANGE).point = room.player.dx(-1);
			assertSame(survivor, strategy.decide(room, 100).target);
		}
	}

	@Test
	public void threeStepsDoNotFitIntoOneNativeRunningCycle()
	{
		Room room = room(12, 100);
		Crab distant = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		distant.point = room.player.dx(8);
		Decision decision = strategy.decide(room, 100);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertNull(decision.target);
		assertSame(distant, decision.approach);
		assertFalse(decision.pathToTarget);
	}

	@Test
	public void movingToShootDoesNotBorrowTheFollowingCycleForReservationReturn()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab north = crab(room, 22, 109, Lane.SPLIT, (char) 0, false, Style.RANGE);
		north.point = RangerStrategy.tile(3290, 4254);
		// Shooting after one northward step now would leave three return cycles before s23+0.
		Decision decision = strategy.decide(room, 110);
		assertNull(decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
		assertEquals(RangerStrategy.tile(3295, 4244), decision.tile);
	}

	@Test
	public void namedSouthBowRemainsEligibleWhenBothWeaponsCanReach()
	{
		Room room = room(7, 100);
		Crab south = crab(room, 7, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		south.hp = 10;
		south.point = RangerStrategy.tile(3295, 4243);
		Decision decision = strategy.decide(room, 101);
		assertSame(south, decision.target);
		assertEquals("South bow 7", decision.rule);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void southThreeOpeningUsesChinWithOnlyOneMatchingRangedVictim()
	{
		Room room = room(3, 100);
		room.player = RangerStrategy.tile(3296, 4244);
		Crab ranged = crab(room, 3, 100, Lane.SOUTH, 'E', false, Style.RANGE);
		ranged.point = RangerStrategy.tile(3296, 4235);
		Crab magic = crab(room, 3, 100, Lane.SOUTH, 'W', false, Style.MAGIC);
		magic.point = RangerStrategy.tile(3295, 4235);
		Decision decision = strategy.decide(room, 102);
		assertSame(ranged, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
		assertEquals("South chin 3", decision.rule);
	}

	@Test
	public void cooldownCanApproachTheSingleSouthThreeChin()
	{
		Room room = room(3, 100);
		room.player = RangerStrategy.tile(3296, 4248);
		room.ready = 102;
		Crab ranged = crab(room, 3, 100, Lane.SOUTH, 'E', false, Style.RANGE);
		ranged.point = RangerStrategy.tile(3296, 4233);
		Decision plan = strategy.decide(room, 100);
		assertSame(ranged, plan.approach);
		assertEquals(Weapon.CHIN, plan.weapon);
		room.player = plan.tile;
		ranged.point = ranged.point.dy(2);
		assertSame(ranged, strategy.decide(room, 102).target);
		assertEquals(Weapon.CHIN, strategy.decide(room, 102).weapon);
	}

	@Test
	public void otherOptionalSingleChinsStillNeedTwoMatchingVictims()
	{
		Room room = room(8, 100);
		Crab ranged = crab(room, 8, 100, Lane.EAST, 'S', false, Style.RANGE);
		ranged.point = RangerStrategy.tile(3303, 4248);
		Decision decision = strategy.decide(room, 101);
		assertSame(ranged, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void cooldownPlansAnApproachToNextTarget()
	{
		Room room = room(12, 100);
		room.ready = 104;
		Crab distant = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		distant.point = RangerStrategy.tile(3303, 4248);
		Decision decision = strategy.decide(room, 100);
		assertNull(decision.target);
		assertSame(distant, decision.approach);
		assertNotEquals(room.player, decision.tile);
		assertTrue(decision.reason.contains("@104"));
	}

	@Test
	public void newerSplitReleasesAnUnhitApproachWithoutDelayingTheReadyShot()
	{
		Room room = room(12, 100); room.ready = 105;
		Crab original = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		original.point = RangerStrategy.tile(3303, 4248);
		assertSame(original, strategy.decide(room, 100).approach);
		room.player = RangerStrategy.tile(3297, 4248);
		Crab fresh = crab(room, 12, 101, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.point = RangerStrategy.tile(3290, 4248); fresh.hp = 2;
		assertSame(fresh, strategy.decide(room, 101).approach);
		room.player = RangerStrategy.tile(3295, 4248);
		assertSame(fresh, strategy.decide(room, 105).target);
	}

	@Test
	public void postCapCooldownAlsoKeepsAnApproachAcrossSpawns()
	{
		Room room = room(22, 100); room.ready = 105;
		Crab original = crab(room, 22, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		original.point = RangerStrategy.tile(3303, 4248);
		assertSame(original, strategy.decide(room, 100).approach);
		room.player = RangerStrategy.tile(3297, 4248);
		Crab fresh = crab(room, 22, 101, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.point = RangerStrategy.tile(3290, 4248);
		assertSame(original, strategy.decide(room, 101).approach);
		room.player = RangerStrategy.tile(3299, 4248);
		assertSame(original, strategy.decide(room, 105).target);
	}

	@Test
	public void claimsDeathAndEarlierAttacksReleaseTheRangerApproach()
	{
		for (int wave : new int[]{12, 22})
		{
			for (int invalidation = 0; invalidation < 3; invalidation++)
			{
				strategy.reset();
				Room room = room(wave, 100); room.ready = 105;
				Crab original = crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
				original.point = RangerStrategy.tile(3303, 4248);
				assertSame(original, strategy.decide(room, 100).approach);
				Crab local = crab(room, wave, 101, Lane.SPLIT, (char) 0, false, Style.RANGE);
				local.point = RangerStrategy.tile(3291, 4248);
				if (invalidation == 0) original.dying = true;
				if (invalidation == 1) original.claimedUntil = 120;
				if (invalidation == 2) room.ready = 101;
				Decision decision = strategy.decide(room, 101);
				if (decision.target == null && decision.approach == null)
				{
					// A cooldown-only close move carries a tile; the next ready attack must still use the replacement.
					assertTrue(decision.tile.distanceTo(local.point) < room.player.distanceTo(local.point));
					room.player = decision.tile;
					assertSame(local, strategy.decide(room, room.ready).target);
				}
				else assertSame(local, decision.target != null ? decision.target : decision.approach);
			}
		}
	}

	@Test
	public void newHardPacketCanInterruptTheCommittedFirstShot()
	{
		Room room = room(9, 102); room.ready = 109;
		Crab original = crab(room, 9, 107, Lane.SPLIT, (char) 0, false, Style.RANGE);
		original.point = RangerStrategy.tile(3303, 4248);
		assertSame(original, strategy.decide(room, 107).approach);
		room.wave = 10; room.spawns[10] = 114; room.ready = 114;
		room.player = RangerStrategy.tile(3291, 4248);
		Crab west = crab(room, 10, 114, Lane.WEST, 'S', false, Style.RANGE);
		west.point = RangerStrategy.tile(3281, 4248);
		Decision packet = strategy.decide(room, 114);
		assertSame(west, packet.target);
		assertEquals("West chin 10", packet.rule);
	}

	@Test
	public void cooldownPlansForRangedFlickerRatherThanCurrentMelee()
	{
		Room room = room(16, 100);
		room.ready = 107;
		Crab flicker = crab(room, 16, 100, Lane.EAST, 'S', false, Style.MAGIC);
		Decision decision = strategy.decide(room, 104);
		assertNull(decision.target);
		assertSame(flicker, decision.approach);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void multiAttackPlanUsesClosingColorWindowBeforePermanentRangedTarget()
	{
		Room room = room(16, 100);
		Crab permanent = crab(room, 16, 102, Lane.SPLIT, (char) 0, false, Style.RANGE);
		permanent.point = room.player.dx(3);
		Crab closing = crab(room, 16, 100, Lane.WEST, 'N', false, Style.RANGE);
		closing.point = room.player.dx(-3);
		Decision decision = strategy.decide(room, 102);
		assertSame(closing, decision.target);
		strategy.attacked(decision, 103);
		closing.pendingDamage = closing.hp;
		room.ready = 104;
		assertSame(permanent, strategy.decide(room, 104).target);
	}

	@Test
	public void emptyCooldownStagesForUpcomingLaneWithoutReservingAttackTime()
	{
		Room room = room(1, 100);
		room.ready = 105;
		Decision decision = strategy.decide(room, 102);
		assertNull(decision.target);
		assertEquals(RangerStrategy.tile(3298, 4249), decision.tile);
	}

	@Test
	public void waveTenChinsStillReserveCooldown()
	{
		Room room = room(9, 100);
		room.player = RangerStrategy.tile(3291, 4248);
		crab(room, 9, 110, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		Decision decision = strategy.decide(room, 110);
		assertNull(decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void waveTenChinOutranksDamagedSplitAndCannotBeReplacedWithPipe()
	{
		Room room = room(10, 100);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab west = crab(room, 10, 100, Lane.WEST, 'S', false, Style.RANGE);
		Crab damaged = crab(room, 10, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		damaged.hp = 1;
		damaged.point = room.player.dx(3);
		Decision decision = strategy.decide(room, 100);
		assertSame(west, decision.target);
		assertEquals("West chin 10", decision.rule);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void plansDoNotSpendRealHpOrCompleteUnobservedAttacks()
	{
		Room room = room(12, 100);
		Crab small = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Decision first = strategy.decide(room, 100);
		assertSame(small, first.target);
		assertSame(small, strategy.decide(room, 100).target);
		assertEquals(8, small.hp);
		assertEquals(0, small.ownHitUntil);
	}

	@Test
	public void reusesChinOnTheRemainingPairAfterObservingTheFirstPairDie()
	{
		Room room = room(12, 100);
		for (int x : new int[]{3292, 3293, 3298, 3299})
			crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE).point = RangerStrategy.tile(x, 4248);
		Decision decision = strategy.decide(room, 100);
		assertEquals(Weapon.CHIN, decision.weapon);
		strategy.attacked(decision, 101);
		for (Crab crab : room.crabs)
			if (crab.point.distanceTo(decision.target.point) <= 1) { crab.dying = true; crab.hp = 0; }
		room.ready = 103;
		Decision second = strategy.decide(room, 103);
		assertEquals(Weapon.CHIN, second.weapon);
		assertNotNull(second.target);
		assertTrue(second.target.point.distanceTo(decision.target.point) > 1);
	}

	@Test
	public void obstructedTargetDoesNotDisplaceAnAvailableAttack()
	{
		Room room = room(12, 100);
		Crab blocked = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		blocked.hp = 1;
		blocked.point = room.player.dx(-3);
		room.obstructed.add(blocked.key);
		Crab available = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		available.point = room.player.dx(3);
		assertSame(available, strategy.decide(room, 100).target);
	}

	@Test
	public void blockedLineOfSightUsesARepositionInBothPhases()
	{
		for (int wave : new int[]{12, 22})
		{
			strategy.reset(); Room room = room(wave, 100);
			Crab blocked = crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
			room.obstructed.add(blocked.key);
			Decision decision = strategy.decide(room, 100);
			assertNull(decision.target); assertSame(blocked, decision.approach); assertFalse(decision.pathToTarget);
		}
	}

	@Test
	public void earlyWestAvailabilityDoesNotDisplaceImmediateSmallClearing()
	{
		Room room = room(18, 100);
		Crab big = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		big.hp = 1;
		for (int i = 0; i < 9; i++)
			crab(room, 18, 100, Lane.SPLIT, (char) 0, false, Style.MELEE).dying = true;
		Crab small = crab(room, 18, 105, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(small, strategy.decide(room, 107).target);
	}

	@Test
	public void west9GetsTheFirstLegalPipeBeforeOrdinarySmalls()
	{
		Room room = room(9, 100);
		room.player = RangerStrategy.tile(3292, 4248);
		Crab parent = crab(room, 9, 100, Lane.WEST, 'S', true, Style.RANGE);
		parent.point = RangerStrategy.tile(3286, 4248);
		Crab small = crab(room, 9, 103, Lane.SPLIT, (char) 0, false, Style.RANGE);
		small.hp = 1;
		small.point = room.player;
		Decision attack = strategy.decide(room, 105);
		assertSame(parent, attack.target);
		assertEquals(Weapon.PIPE, attack.weapon);
	}

	@Test
	public void west9DoesNotReplaceAUsefulSmallWithATooEarlyBow()
	{
		Room room = room(9, 100);
		room.player = RangerStrategy.tile(3298, 4248);
		Crab parent = crab(room, 9, 100, Lane.WEST, 'S', true, Style.RANGE);
		parent.point = RangerStrategy.tile(3286, 4248);
		Crab small = crab(room, 9, 103, Lane.SPLIT, (char) 0, false, Style.RANGE);
		small.point = room.player;
		assertSame(small, strategy.decide(room, 105).target);
	}

	@Test
	public void everyAssignedPreCapBigGetsServiceBeforeItsDeadlineDespiteFreshSmalls()
	{
		int[][] assignments = {
			{5, Lane.WEST.ordinal(), 2, Style.RANGE.ordinal()},
			{6, Lane.EAST.ordinal(), 9, Style.MELEE.ordinal()},
			{7, Lane.SOUTH.ordinal(), 0, Style.RANGE.ordinal()},
			{9, Lane.WEST.ordinal(), 7, Style.RANGE.ordinal()},
			{10, Lane.EAST.ordinal(), 9, Style.RANGE.ordinal()}
		};
		for (int[] assignment : assignments)
		{
			Room room = room(assignment[0], 100);
			int tick = 100 + assignment[2];
			Crab big = crab(room, assignment[0], 100, Lane.values()[assignment[1]], 'S', true, Style.values()[assignment[3]]);
			big.point = room.player.dx(1);
			for (int i = 0; i < 10; i++)
				crab(room, room.wave, tick, Lane.SPLIT, (char) 0, false, Style.RANGE);
			assertSame("Wave " + big.wave + " " + big.lane, big, strategy.decide(room, tick).target);
		}
	}

	@Test
	public void wave14BigCanBeStartedDuringWave15EastWork()
	{
		Room room = room(15, 108);
		Crab big = crab(room, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		assertSame(big, strategy.decide(room, 108).target);
	}

	@Test
	public void wave14BigIsStillDeferredBeforeEastSideWork()
	{
		Room room = room(14, 100);
		crab(room, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		assertNull(strategy.decide(room, 104).target);
	}

	@Test
	public void wave14BigKeepsFinishPriorityThroughMissesAndNonlethalHits()
	{
		Room room = room(16, 116);
		Crab big = crab(room, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		Decision first = strategy.decide(room, 116);
		assertSame(big, first.target);
		assertEquals(Weapon.PIPE, first.weapon);
		strategy.attacked(first, 117);
		assertEquals("Deferred east big 14", first.rule);
		for (int i = 0; i < 10; i++)
			crab(room, 16, 118, Lane.SPLIT, (char) 0, false, Style.RANGE);
		room.ready = 118;
		// Full HP before projectile impact does not establish a miss. Spend this tick elsewhere.
		assertNotSame(big, strategy.decide(room, 118).target);
		// Once the projectile window passes, a miss still keeps finish priority.
		assertSame(big, strategy.decide(room, 120).target);
		big.hp = 4;
		assertSame(big, strategy.decide(room, 120).target);
		big.dying = true;
		assertNotSame(big, strategy.decide(room, 121).target);
	}

	@Test
	public void cooldownApproachesDueBigInsteadOfStayingWithFreshSmalls()
	{
		Room room = room(16, 116);
		room.player = RangerStrategy.tile(3290, 4248);
		room.ready = 118;
		Crab big = crab(room, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		big.point = RangerStrategy.tile(3301, 4248);
		for (int i = 0; i < 6; i++)
			crab(room, 16, 116, Lane.SPLIT, (char) 0, false, Style.RANGE).point = room.player;
		Decision decision = strategy.decide(room, 116);
		assertNull(decision.target);
		assertSame(big, decision.approach);
		assertNotEquals(room.player, decision.tile);
	}

	@Test
	public void dueBigDoesNotCauseIdleWhenUsefulAttackIsAvailableNow()
	{
		Room room = room(16, 116);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab big = crab(room, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		big.point = RangerStrategy.tile(3305, 4248);
		Crab small = crab(room, 16, 116, Lane.SPLIT, (char) 0, false, Style.RANGE);
		small.point = room.player;
		assertSame(small, strategy.decide(room, 116).target);
	}

	@Test
	public void wave20LeavesEveryPreCapBigAliveEvenAfterOpeningIt()
	{
		Room room = room(20, 130);
		for (int wave : new int[]{5, 6, 7, 9, 14, 18, 19})
			for (Lane lane : new Lane[]{Lane.WEST, Lane.SOUTH, Lane.EAST})
			{
				Crab big = crab(room, wave, 100, lane, 'S', true, wave == 19 ? Style.MAGIC : Style.RANGE);
				big.hp = 1;
				strategy.attacked(new Decision(big, Weapon.PIPE, room.player, false, null, "Previous hit"), 128);
			}
		Crab small = crab(room, 20, 130, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(small, strategy.decide(room, 130).target);
		room.crabs.remove(small);
		assertNull(strategy.decide(room, 130).target);
	}

	@Test
	public void wave18WaitsForWave19MeleeFlickerEvenIfAlreadyOpened()
	{
		for (Lane lane : new Lane[]{Lane.SOUTH, Lane.EAST})
		{
			Room room = room(19, 108);
			Crab big = crab(room, 18, 100, lane, 'S', true, Style.RANGE);
			strategy.attacked(new Decision(big, Weapon.PIPE, room.player, false, null, "Earlier hit"), 110);
			Crab small = crab(room, 19, 108, Lane.SPLIT, (char) 0, false, Style.RANGE);
			small.claimedUntil = 200;
			assertNull("Hold " + lane, strategy.decide(room, 112).target);
			assertSame("Release " + lane, big, strategy.decide(room, 113).target);
		}
	}

	@Test
	public void wave20FinishesRangedWave18BigBeforeStartingOrdinarySplits()
	{
		Room room = room(20, 120);
		room.spawns[19] = 108;
		Crab survivor = crab(room, 18, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		survivor.hp = 13;
		Crab fresh = crab(room, 20, 120, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.point = room.player.dx(3);
		Decision decision = strategy.decide(room, 120);
		assertSame(survivor, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void wave18ContinuationStillYieldsToWave20WestBow()
	{
		Room room = room(20, 120);
		room.spawns[19] = 110;
		crab(room, 18, 102, Lane.SOUTH, 'W', true, Style.RANGE).hp = 1;
		Crab flicker = crab(room, 20, 120, Lane.WEST, 'S', true, Style.RANGE);
		Decision decision = strategy.decide(room, 125);
		assertSame(flicker, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void wave20SwitchesToEyeForEighteensPermanentMagicFlicker()
	{
		Room room = room(20, 120);
		room.spawns[19] = 108;
		Crab survivor = crab(room, 18, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		assertSame(survivor, strategy.decide(room, 124).target);
		assertNull(strategy.decide(room, 125).target);
		survivor.style = Style.MAGIC;
		assertSame(survivor, strategy.decide(room, 126).target);
		assertEquals(Weapon.EYE, strategy.decide(room, 126).weapon);
	}

	@Test
	public void wave18AfterGuideTakesPriorityOverSmallsAndEarlierOverdueBigs()
	{
		for (Lane lane : new Lane[]{Lane.WEST, Lane.SOUTH, Lane.EAST})
		{
			Room room = room(19, 108);
			crab(room, 14, 80, Lane.EAST, 'S', true, Style.RANGE);
			Crab big = crab(room, 18, 100, lane, 'S', true, Style.RANGE);
			for (int i = 0; i < 8; i++) crab(room, 19, 113, Lane.SPLIT, (char) 0, false, Style.RANGE);
			assertSame(lane.toString(), big, strategy.decide(room, 113).target);
		}
	}

	@Test
	public void equallyEarlyTransitionBigsFollowWestSouthEastRatherThanBirthKey()
	{
		for (int wave : new int[]{19, 20})
		{
			strategy.reset();
			Room room = room(wave, wave == 19 ? 108 : 120);
			room.spawns[19] = 108;
			Crab east = crab(room, 18, 100, Lane.EAST, 'S', true, Style.RANGE);
			east.hp = 1;
			Crab south = crab(room, 18, 100, Lane.SOUTH, 'W', true, Style.RANGE);
			south.hp = 5;
			Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
			int tick = wave == 19 ? 113 : 120;
			assertSame(west, strategy.decide(room, tick).target);
			west.dying = true;
			assertSame(south, strategy.decide(room, tick).target);
			south.dying = true;
			assertSame(east, strategy.decide(room, tick).target);
		}
	}

	@Test
	public void blockedPreferredTransitionBigNeverMakesTheRangerWait()
	{
		Room room = room(19, 108);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		Crab south = crab(room, 18, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		Crab east = crab(room, 18, 100, Lane.EAST, 'S', true, Style.RANGE);
		room.obstructed.add(west.key);
		assertSame(south, strategy.decide(room, 113).target);
		room.obstructed.add(south.key);
		assertSame(east, strategy.decide(room, 113).target);
	}

	@Test
	public void onlyWest18CanBeKilledBeforeTheWave19Guide()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		crab(room, 18, 100, Lane.SOUTH, 'S', true, Style.RANGE);
		crab(room, 18, 100, Lane.EAST, 'S', true, Style.RANGE);
		assertSame(west, strategy.decide(room, 110).target);
		west.dying = true;
		assertNull(strategy.decide(room, 110).target);
	}

	@Test
	public void dyingSmallsDoNotBlockEarlyWest()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		Crab small = crab(room, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		small.dying = true;
		assertSame(west, strategy.decide(room, 110).target);
		room.crabs.remove(small);
		assertSame(west, strategy.decide(room, 110).target);
	}

	@Test
	public void wave18GuideUsesActualWave19SpawnAfterStalls()
	{
		Room room = room(18, 100);
		Crab south = crab(room, 18, 100, Lane.SOUTH, 'S', true, Style.RANGE);
		assertNull(strategy.decide(room, 118).target);
		room.wave = 19;
		room.spawns[19] = 116;
		assertNull(strategy.decide(room, 120).target);
		assertSame(south, strategy.decide(room, 121).target);
	}

	@Test
	public void wave18CannotBeAttackedUntilObservedInsideRoom()
	{
		Lane[] lanes = {Lane.SOUTH, Lane.EAST};
		Tile[] outside = {RangerStrategy.tile(3295, 4242), RangerStrategy.tile(3302, 4248)};
		Tile[] inside = {RangerStrategy.tile(3295, 4243), RangerStrategy.tile(3301, 4248)};
		for (int i = 0; i < lanes.length; i++)
		{
			Room room = room(19, 108);
			Crab big = crab(room, 18, 100, lanes[i], 'S', true, Style.RANGE);
			big.point = outside[i];
			assertNull("Still in " + lanes[i] + " lane", strategy.decide(room, 113).target);
			big.point = inside[i];
			assertSame("Entered from " + lanes[i], big, strategy.decide(room, 113).target);
		}
	}

	@Test
	public void earlyWestCanBePrefiredBeforeObservedRoomEntry()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		west.point = RangerStrategy.tile(3289, 4248);
		assertSame(west, strategy.decide(room, 110).target);
		west.point = RangerStrategy.tile(3290, 4248);
		assertSame(west, strategy.decide(room, 110).target);
	}

	@Test
	public void west18CanBePrefiredBefore19DespiteUnrelatedLivingSmall()
	{
		Room room = room(18, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		west.point = RangerStrategy.tile(3281, 4248);
		crab(room, 17, 95, Lane.SPLIT, (char) 0, false, Style.MAGIC).claimedUntil = 200;
		crab(room, 18, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		crab(room, 18, 100, Lane.EAST, 'S', true, Style.RANGE);
		Decision decision = strategy.decide(room, 100);
		assertSame(west, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void openedWest18KeepsFinishPriorityBeforeWave19Guide()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		west.point = RangerStrategy.tile(3292, 4248);
		strategy.attacked(new Decision(west, Weapon.PIPE, room.player, false, null, "Earlier hit"), 103);
		west.hp = 5;
		Crab small = crab(room, 18, 107, Lane.SPLIT, (char) 0, false, Style.RANGE);
		small.point = RangerStrategy.tile(3299, 4247);
		assertSame(west, strategy.decide(room, 107).target);
	}

	@Test
	public void unopenedWest18BecomesUrgentNearItsFinalFlicker()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		west.point = RangerStrategy.tile(3292, 4248);
		crab(room, 18, 121, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(west, strategy.decide(room, 121).target);
	}

	@Test
	public void wave18StopsBeforeAge26MageAndDoesNotEyeTheSurvivor()
	{
		Room room = room(19, 108);
		Crab big = crab(room, 18, 100, Lane.SOUTH, 'S', true, Style.RANGE);
		assertSame(big, strategy.decide(room, 124).target);
		assertNull(strategy.decide(room, 125).target); // Request would launch into age-26 mage.
		big.style = Style.MAGIC;
		assertNull(strategy.decide(room, 126).target);
	}

	@Test
	public void wave19WestAfterItsPacketStillCompetesWithUsefulCleanup()
	{
		Room room = room(19, 100);
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		Decision first = strategy.decide(room, 110);
		assertSame(west, first.target);
		assertEquals(Weapon.EYE, first.weapon);
		strategy.attacked(first, 111);
		west.hp = 1;
		Crab small = crab(room, 19, 110, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		assertSame(small, strategy.decide(room, 110).target);
		room.crabs.remove(small);
		Crab melee = crab(room, 19, 110, Lane.SPLIT, (char) 0, false, Style.MELEE);
		melee.point = room.player.dx(1);
		assertSame(west, strategy.decide(room, 110).target);
	}

	@Test
	public void west19EyePrefiresInTheLaneAtExactlyRequestAgeThree()
	{
		Room room = room(19, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west.point = RangerStrategy.tile(3283, 4248);
		Decision before = strategy.decide(room, 102);
		assertNull(before.target);
		assertSame(west, before.approach);
		west.point = west.point.dx(1);
		Decision launch = strategy.decide(room, 103);
		assertSame(west, launch.target);
		assertEquals(Weapon.EYE, launch.weapon);
		assertEquals("West magic big 19", launch.rule);
	}

	@Test
	public void west19PacketTakesItsTickAheadOfAnOpenedWave18Big()
	{
		Room room = room(19, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		Crab older = crab(room, 18, 92, Lane.WEST, 'S', true, Style.RANGE);
		strategy.attacked(new Decision(older, Weapon.PIPE, room.player, false, null, "Previous hit"), 99);
		older.hp = 1;
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west.point = RangerStrategy.tile(3284, 4248);
		assertSame(west, strategy.decide(room, 103).target);
	}

	@Test
	public void west19ReservationAllowsAnEarlierPipeOnWest18()
	{
		Room room = room(19, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		Crab older = crab(room, 18, 92, Lane.WEST, 'S', true, Style.RANGE);
		older.point = RangerStrategy.tile(3292, 4248);
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west.point = RangerStrategy.tile(3282, 4248);
		Decision early = strategy.decide(room, 101);
		assertSame(older, early.target);
		assertEquals(Weapon.PIPE, early.weapon);
	}

	@Test
	public void west19PacketDoesNotHoldForNullOrMissedWindow()
	{
		Room room = room(19, 100);
		Crab older = crab(room, 18, 92, Lane.WEST, 'S', true, Style.RANGE);
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west.nulled = true;
		assertSame(older, strategy.decide(room, 103).target);
		west.nulled = false;
		room.ready = 104;
		assertSame(older, strategy.decide(room, 104).target);
	}

	@Test
	public void west19PacketNeverInventsItsWaveThroughAStall()
	{
		Room room = room(18, 100);
		Crab split = crab(room, 18, 110, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(split, strategy.decide(room, 110).target);
		assertEquals(-1, room.spawns[19]);
	}

	@Test
	public void observedWestNineteenPacketNeverRepeatsAnAcknowledgedAttack()
	{
		Room room = room(19, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west.point = RangerStrategy.tile(3284, 4248);
		Decision launch = strategy.decide(room, 103);
		assertSame(west, launch.target);
		assertEquals("West magic big 19", launch.rule);
		strategy.attacked(launch, 104);
		assertNotEquals(launch.rule, strategy.decide(room, 104).rule);
	}

	@Test
	public void obstructedOrClaimedWestNineteenPacketRequiresANewObservationBeforeLaunch()
	{
		Room room = room(19, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		Crab west = crab(room, 19, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west.point = RangerStrategy.tile(3284, 4248);
		room.ready = 103;
		west.claimedUntil = 104;
		assertNotSame(west, strategy.decide(room, 103).target);
		west.claimedUntil = 0;
		room.obstructed.add(west.key);
		assertNotSame(west, strategy.decide(room, 103).target);
		room.obstructed.clear();
		assertSame(west, strategy.decide(room, 103).target);
	}

	@Test
	public void aWoundedMageAssignmentGetsBackupWhileTheMageIsOnCooldown()
	{
		Room room = room(28, 108);
		Crab west26 = crab(room, 26, 100, Lane.WEST, 'S', true, Style.MAGIC);
		west26.hp = 6;
		west26.pendingDamage = 0;
		room.observePartner(room.player, 110);
		assertNull(strategy.decide(room, 110).target);
		room.observePartner(room.player, 114);
		assertSame(west26, strategy.decide(room, 110).target);
		west26.pendingDamage = 6;
		assertNull(strategy.decide(room, 110).target);
	}

	@Test
	public void partnerClaimOnDueBigLeavesOtherUsefulWorkAvailable()
	{
		Room room = room(16, 116);
		Crab big = crab(room, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		big.claimedUntil = 120;
		Crab small = crab(room, 16, 116, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(small, strategy.decide(room, 116).target);
		assertSame(big, strategy.decide(room, 120).target);
	}

	@Test
	public void overdueBigCannotDisplaceWaveTenChinOrWave20Flicker()
	{
		Room ten = room(10, 116);
		crab(ten, 9, 100, Lane.WEST, 'S', true, Style.RANGE);
		Crab chin = crab(ten, 10, 116, Lane.WEST, 'S', false, Style.RANGE);
		assertSame(chin, strategy.decide(ten, 116).target);
		assertEquals(Weapon.CHIN, strategy.decide(ten, 116).weapon);
		Room twenty = room(20, 136);
		crab(twenty, 14, 100, Lane.EAST, 'S', true, Style.RANGE);
		Crab flicker = crab(twenty, 20, 136, Lane.WEST, 'S', true, Style.RANGE);
		assertSame(flicker, strategy.decide(twenty, 141).target);
		assertEquals(Weapon.BOW, strategy.decide(twenty, 141).weapon);
	}

	@Test
	public void doesNotDoubleUpDuringPartnerProjectileWindow()
	{
		Room room = room(12, 90);
		Crab claimed = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		claimed.claimedUntil = 105;
		assertNull(strategy.decide(room, 101).target);
		assertSame(claimed, strategy.decide(room, 105).target);
	}

	@Test
	public void nulledCrabDoesNotAttractAnotherPrimaryAttackInEitherPhase()
	{
		for (int wave : new int[]{12, 22})
		{
			strategy.reset();
			Room room = room(wave, 100);
			Crab blocked = crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
			blocked.nulled = true;
			Crab available = crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC);
			assertSame(available, strategy.decide(room, 101).target);
			room.crabs.remove(available);
			assertNull(strategy.decide(room, 101).target);
		}
	}

	@Test
	public void nulledHardPacketDoesNotReserveCooldownAgainstUsefulWork()
	{
		Room room = room(20, 100);
		Crab west = crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		west.nulled = true;
		Crab fresh = crab(room, 20, 105, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		assertSame(fresh, strategy.decide(room, 105).target);
	}

	@Test
	public void partnerNullSmallStillGetsEyeAndOwnNullAlwaysWins()
	{
		Room room = room(22, 100);
		Crab magic = crab(room, 22, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		magic.partnerNulled = true;
		assertSame(magic, strategy.decide(room, 101).target);
		assertEquals(Weapon.EYE, strategy.decide(room, 101).weapon);
		magic.partnerNulled = false;
		assertSame(magic, strategy.decide(room, 101).target);
		magic.partnerNulled = true;
		magic.nulled = true;
		assertNull(strategy.decide(room, 101).target);
	}

	@Test
	public void recentMageBigOnlyBecomesFallbackWhenPartnerIsNulled()
	{
		Room room = room(28, 108);
		Crab big = crab(room, 26, 100, Lane.WEST, 'S', true, Style.MAGIC);
		assertNull(strategy.decide(room, 110).target);
		big.partnerNulled = true;
		assertSame(big, strategy.decide(room, 110).target);
		assertEquals(Weapon.EYE, strategy.decide(room, 110).weapon);
		big.nulled = true;
		assertNull(strategy.decide(room, 110).target);
	}

	@Test
	public void partnerNullEast19BigGetsEyeWithoutChangingNormalOwnership()
	{
		Room room = room(19, 100);
		Crab east = crab(room, 19, 100, Lane.EAST, 'S', true, Style.MAGIC);
		east.point = RangerStrategy.tile(3299, 4248);
		assertNull(strategy.decide(room, 107).target);
		east.partnerNulled = true;
		assertSame(east, strategy.decide(room, 107).target);
		assertEquals(Weapon.EYE, strategy.decide(room, 107).weapon);
		east.nulled = true;
		assertNull(strategy.decide(room, 107).target);
	}

	@Test
	public void partnerNullDoesNotReleaseSavedSouth26OrStaleParents()
	{
		Room room = room(27, 104);
		Crab saved = crab(room, 26, 100, Lane.SOUTH, 'W', true, Style.MAGIC);
		saved.partnerNulled = true;
		assertNull(strategy.decide(room, 105).target);
		room = room(30, 128);
		Crab old = crab(room, 27, 100, Lane.WEST, 'S', true, Style.MAGIC);
		old.partnerNulled = true;
		assertNull(strategy.decide(room, 130).target);
		room = room(31, 120);
		Crab excluded = crab(room, 27, 110, Lane.WEST, 'S', true, Style.MAGIC);
		excluded.partnerNulled = true;
		assertNull(strategy.decide(room, 122).target);
	}

	@Test
	public void savedWest12SouthCannotBeClawedBeforeTheMageDinhDeadline()
	{
		Room room = room(13, 108);
		Crab south = crab(room, 12, 100, Lane.WEST, 'S', false, Style.MELEE);
		south.point = room.player.dx(1);
		assertNull(strategy.decide(room, 112).target);
		assertNull(strategy.decide(room, 115).target);
		room.wave = 14;
		room.spawns[14] = 116;
		assertNull(strategy.decide(room, 116).target);
		assertNull(strategy.decide(room, 117).target);
		assertSame(south, strategy.decide(room, 118).target);
		assertEquals(Weapon.SULPHUR, strategy.decide(room, 118).weapon);
	}

	@Test
	public void savedWest12SouthReleasesWhenMageIsNulledButOwnNullStillWins()
	{
		Room room = room(13, 108);
		Crab south = crab(room, 12, 100, Lane.WEST, 'S', false, Style.MELEE);
		south.point = room.player.dx(1);
		south.partnerNulled = true;
		assertSame(south, strategy.decide(room, 112).target);
		south.nulled = true;
		assertNull(strategy.decide(room, 112).target);
		south.nulled = false;
		room.wave = 14;
		room.spawns[14] = 116;
		assertSame(south, strategy.decide(room, 117).target);
		south.nulled = true;
		assertNull(strategy.decide(room, 117).target);
	}

	@Test
	public void west12ProtectionLeavesTheNorthSiblingAndOtherUsefulWorkAvailable()
	{
		Room room = room(13, 108);
		Crab south = crab(room, 12, 100, Lane.WEST, 'S', false, Style.MELEE);
		south.point = room.player.dx(1);
		Crab north = crab(room, 12, 100, Lane.WEST, 'N', false, Style.MELEE);
		north.point = room.player.dx(1).dy(1);
		assertSame(north, strategy.decide(room, 112).target);
		room.crabs.remove(north);
		Crab ranged = crab(room, 13, 112, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(ranged, strategy.decide(room, 112).target);
		assertEquals(Weapon.PIPE, strategy.decide(room, 112).weapon);
	}

	@Test
	public void doesNotOpenUnassignedMagicBigBeforeCap()
	{
		Room room = room(17, 100);
		crab(room, 17, 100, Lane.WEST, 'S', true, Style.MAGIC);
		assertNull(strategy.decide(room, 100).target);
	}

	@Test
	public void waitsInsteadOfEyeThatWouldBlockWave23Prefire()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3295, 4244);
		crab(room, 22, 109, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		Decision decision = strategy.decide(room, 110);
		assertNull(decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
		assertEquals(RangerStrategy.tile(3295, 4244), decision.tile);
	}

	@Test
	public void permitsTwoTickFillerThatFitsButNotOneTickLater()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3295, 4244);
		Crab fresh = crab(room, 22, 109, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(fresh, strategy.decide(room, 110).target);
		assertNull(strategy.decide(room, 111).target);
	}

	@Test
	public void southPrefireOutranksFreshSplit()
	{
		Room room = room(23, 192);
		Crab south = crab(room, 23, 192, Lane.SOUTH, 'W', true, Style.RANGE);
		crab(room, 23, 192, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Decision decision = strategy.decide(room, 192);
		assertSame(south, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void wave20UsesWestBigWhileRangedNotItsInitialMeleeStyle()
	{
		Room room = room(20, 100);
		Crab west = crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		Decision decision = strategy.decide(room, 105);
		assertSame(west, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
		west.style = Style.MELEE;
		assertNull(strategy.decide(room, 105).target);
	}

	@Test
	public void willNotReleaseAttackIntoNextTicksWrongStyle()
	{
		Room room = room(20, 100);
		crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		assertNull(strategy.decide(room, 106).target); // Melee at age seven, before the request executes.
	}

	@Test
	public void cooldownCannotBeBypassedByPrefire()
	{
		Room room = room(20, 100);
		crab(room, 20, 100, Lane.WEST, 'S', true, Style.RANGE);
		room.ready = 106;
		Decision decision = strategy.decide(room, 105);
		assertNull(decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void carriesWave27BigIntoWave28()
	{
		Room room = room(28, 108);
		Crab big = crab(room, 27, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		assertSame(big, strategy.decide(room, 108).target);
	}

	@Test
	public void stallsDoNotAdvanceWaveOrResetTargetAge()
	{
		Room room = room(28, 100);
		Crab split = crab(room, 28, 112, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(split, strategy.decide(room, 113).target);
		assertEquals(28, room.wave);
		assertEquals(100, room.spawns[28]);
	}

	@Test
	public void preservesWave29LanePacketAfter30Spawns()
	{
		Room room = room(30, 104);
		Crab north = crab(room, 29, 100, Lane.WEST, 'N', false, Style.RANGE);
		assertSame(north, strategy.decide(room, 105).target);
		assertFalse(room.cleanup);
	}

	@Test
	public void undimmedFinalLanePacketRemainsActiveBeforeCleanup()
	{
		Room room = room(31, 100);
		Crab north = crab(room, 31, 100, Lane.WEST, 'N', false, Style.RANGE);
		assertSame(north, strategy.decide(room, 105).target);
		assertFalse(room.cleanup);
	}

	@Test
	public void finalCleanupDimsByBirthTimeAndKeepsLateTwentyNineChildren()
	{
		Room room = room(31, 100);
		crab(room, 29, 90, Lane.WEST, 'S', false, Style.RANGE);
		crab(room, 28, 90, Lane.SPLIT, (char) 0, false, Style.RANGE);
		crab(room, 25, 90, Lane.SOUTH, 'W', true, Style.RANGE);
		// No lineage filter: this split's own birth wave is 31 even if its parent was 25.
		Crab child = crab(room, 31, 119, Lane.SPLIT, (char) 0, false, Style.RANGE);
		Crab late29 = crab(room, 29, 95, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(child, strategy.decide(room, 121).target);
		assertTrue(room.cleanup);
		room.crabs.remove(child);
		assertSame(late29, strategy.decide(room, 122).target);
		room.crabs.remove(late29);
		assertNull(strategy.decide(room, 123).target);
	}

	@Test
	public void actualWave31CancelsOldApproachBeforeFinalPacketsFinish()
	{
		Room room = room(30, 100);
		room.spawns[28] = 80;
		room.spawns[29] = 96;
		room.ready = 112;
		Crab old = crab(room, 28, 90, Lane.SPLIT, (char) 0, false, Style.RANGE);
		old.point = RangerStrategy.tile(3298, 4248);
		assertSame(old, strategy.decide(room, 110).approach);
		room.wave = 31;
		room.spawns[31] = 111;
		Crab south = crab(room, 31, 111, Lane.SOUTH, 'W', false, Style.MELEE);
		south.point = RangerStrategy.tile(3295, 4233);
		Decision decision = strategy.decide(room, 111);
		assertNotSame(old, decision.target);
		assertNotSame(old, decision.approach);
		assertFalse(room.cleanup);
	}

	@Test
	public void nearbyScratchCannotDisplaceUsefulRangedWork()
	{
		Room room = room(12, 90);
		Crab melee = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.MELEE);
		melee.point = room.player.dx(1);
		Crab range = crab(room, 12, 99, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(range, strategy.decide(room, 101).target);
	}

	@Test
	public void clawBurstRequiresEnergyButStillFallsBackToScratch()
	{
		Room room = room(7, 104);
		Crab big = crab(room, 6, 100, Lane.EAST, 'S', true, Style.MELEE);
		Decision decision = strategy.decide(room, 110);
		assertSame(big, decision.target);
		assertTrue(decision.special);
		room.specialEnergy = 400;
		assertFalse(strategy.decide(room, 110).special);
	}

	@Test
	public void requestsAreNotCompletedUntilAttackIsAcknowledged()
	{
		Room room = room(23, 100);
		Crab target = crab(room, 23, 100, Lane.SOUTH, 'W', true, Style.RANGE);
		Decision first = strategy.decide(room, 100);
		assertEquals("South bow 23", strategy.decide(room, 101).rule);
		strategy.attacked(first, 101);
		assertNotEquals("South bow 23", strategy.decide(room, 102).rule);
		target.dying = true;
		assertNull(strategy.decide(room, 102).target);
	}

	@Test
	public void flickerFixtureIncludesLateChangesAndFixedSplits()
	{
		Room room = room(31, 100);
		Crab north = crab(room, 31, 100, Lane.WEST, 'N', false, Style.MELEE);
		assertEquals(Style.MELEE, RangerFlickers.at(north, 0));
		assertEquals(Style.RANGE, RangerFlickers.at(north, 5));
		assertEquals(Style.MAGIC, RangerFlickers.at(north, 7));
		assertEquals(Style.RANGE, RangerFlickers.at(north, 26));
		Crab split = crab(room, 31, 100, Lane.SPLIT, (char) 0, false, Style.MELEE);
		assertEquals(Style.MELEE, RangerFlickers.at(split, 26));
	}

}
