package net.runelite.client.plugins.nyloranger;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Crab;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Decision;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Lane;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Room;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Style;
import net.runelite.client.plugins.nyloranger.RangerStrategy.Weapon;
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
	public void damagedRangedSmallOutranksFreshFullHpSmall()
	{
		Room room = room(12, 100);
		Crab damaged = crab(room, 8, 75, Lane.SPLIT, (char) 0, false, Style.RANGE);
		damaged.hp = 2;
		damaged.point = room.player.dx(-3);
		Crab fresh = crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE);
		fresh.point = room.player.dx(3);
		assertSame(damaged, strategy.decide(room, 101).target);
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
		assertTrue(decision.reason.contains("#" + permanent.index + " @104"));
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
	public void twoChinsClearingAllWorkAreKeptOverAThreeAttackPlan()
	{
		Room room = room(12, 100);
		for (int x : new int[]{3292, 3293, 3298, 3299})
			crab(room, 12, 100, Lane.SPLIT, (char) 0, false, Style.RANGE).point = RangerStrategy.tile(x, 4248);
		Decision decision = strategy.decide(room, 100);
		assertEquals(Weapon.CHIN, decision.weapon);
		assertEquals(2, decision.reason.split("CHIN", -1).length - 1);
		assertFalse(decision.reason.contains("PIPE"));
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
	public void dyingBodiesStillCountAgainstOpeningBigs()
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
	public void everyAssignedPreCapBigGetsServiceBeforeItsDeadlineDespiteFreshSmalls()
	{
		int[][] assignments = {
			{5, Lane.WEST.ordinal(), 2, Style.RANGE.ordinal()},
			{6, Lane.EAST.ordinal(), 9, Style.MELEE.ordinal()},
			{7, Lane.SOUTH.ordinal(), 0, Style.RANGE.ordinal()},
			{9, Lane.WEST.ordinal(), 7, Style.RANGE.ordinal()},
			{14, Lane.EAST.ordinal(), 10, Style.RANGE.ordinal()}
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
		assertTrue(first.reason.contains("#" + big.index + " @118"));
		strategy.attacked(first, 117);
		assertEquals("Deferred east big 14", first.rule);
		for (int i = 0; i < 10; i++)
			crab(room, 16, 118, Lane.SPLIT, (char) 0, false, Style.RANGE);
		room.ready = 118;
		// A miss keeps full HP; acknowledging the packet does not mean this big died.
		assertSame(big, strategy.decide(room, 118).target);
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
		big.point = RangerStrategy.tile(3302, 4248);
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
		for (Lane lane : new Lane[]{Lane.WEST, Lane.SOUTH, Lane.EAST})
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
	public void onlyWest18CanBeKilledEarlyWhenRoomHasNoSmalls()
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
	public void dyingSmallsBlockEarlyWestExceptionUntilRemoved()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		Crab small = crab(room, 18, 100, Lane.SPLIT, (char) 0, false, Style.MAGIC);
		small.dying = true;
		assertNull(strategy.decide(room, 110).target);
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
		Lane[] lanes = {Lane.WEST, Lane.SOUTH, Lane.EAST};
		WorldPoint[] outside = {RangerStrategy.tile(3289, 4248), RangerStrategy.tile(3295, 4242), RangerStrategy.tile(3302, 4248)};
		WorldPoint[] inside = {RangerStrategy.tile(3290, 4248), RangerStrategy.tile(3295, 4243), RangerStrategy.tile(3301, 4248)};
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
	public void earlyWestExceptionAlsoRequiresObservedRoomEntry()
	{
		Room room = room(18, 100);
		Crab west = crab(room, 18, 100, Lane.WEST, 'S', true, Style.RANGE);
		west.point = RangerStrategy.tile(3289, 4248);
		assertNull(strategy.decide(room, 110).target);
		west.point = RangerStrategy.tile(3290, 4248);
		assertSame(west, strategy.decide(room, 110).target);
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
	public void wave19WestMageIsFallbackEvenAfterNonlethalHit()
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
		assertSame(melee, strategy.decide(room, 110).target);
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
	public void cutoffIsOnlyAppliedAfterFinalLanePacket()
	{
		Room room = room(31, 100);
		Crab north = crab(room, 31, 100, Lane.WEST, 'N', false, Style.RANGE);
		assertSame(north, strategy.decide(room, 105).target);
		assertFalse(room.cleanup);
	}

	@Test
	public void finalCleanupIgnoresOldCrabsButKeepsFreshChildOfOldParent()
	{
		Room room = room(31, 100);
		crab(room, 29, 95, Lane.WEST, 'S', false, Style.RANGE);
		crab(room, 25, 90, Lane.SOUTH, 'W', true, Style.RANGE);
		// No lineage filter: this split's own birth wave is 31 even if its parent was 25.
		Crab child = crab(room, 31, 119, Lane.SPLIT, (char) 0, false, Style.RANGE);
		assertSame(child, strategy.decide(room, 121).target);
		assertTrue(room.cleanup);
		room.crabs.remove(child);
		assertNull(strategy.decide(room, 122).target);
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

	@Test
	public void classifiesOnlyTrueLaneAnchorsAsLaneBirths()
	{
		assertEquals(Lane.WEST, NylocasRangerPlugin.lane(new WorldPoint(3281, 4249, 0)));
		assertEquals(Lane.EAST, NylocasRangerPlugin.lane(new WorldPoint(3309, 4248, 0)));
		assertEquals(Lane.SOUTH, NylocasRangerPlugin.lane(new WorldPoint(3296, 4233, 0)));
		assertEquals(Lane.SPLIT, NylocasRangerPlugin.lane(new WorldPoint(3295, 4248, 0)));
	}
}
