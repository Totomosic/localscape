package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

/** Jordan's latest explicit rotation, follow-up, and cleanup corrections. */
public class RangerLatestGuidanceTest
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

	private Crab crab(Room room, int wave, int born, Lane lane, char slot,
		boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void neverBowAnySmallAfterOpeningEvenWithRangeAdvantage()
	{
		for (int wave : new int[]{3, 4, 6, 8, 12, 15, 16, 19})
		{
			strategy.reset();
			Room room = room(wave, 100);
			room.player = RangerStrategy.tile(3290, 4248);
			crab(room, wave, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3300, 4248);
			Decision decision = strategy.decide(room, 101);
			assertNotEquals("wave " + wave + ": " + decision.reason, Weapon.BOW, decision.weapon);
		}
	}

	@Test
	public void westFiveBigTakesFirstFeasiblePipeOverNearbyOldSmall()
	{
		Room room = room(5, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab big = crab(room, 5, 100, Lane.WEST, 'S', true, Style.RANGE, 3283, 4248);
		crab(room, 1, 84, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision decision = strategy.decide(room, 102);
		assertSame(decision.reason, big, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void westTwentyFourPipesBeforeMageFallbackAndWaitsForImpact()
	{
		Room room = room(24, 100);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab big = crab(room, 24, 100, Lane.WEST, 'S', true, Style.RANGE, 3284, 4248);
		Decision decision = strategy.decide(room, 103);
		assertSame(decision.reason, big, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
		strategy.attacked(decision, 103);
		big.style = Style.MAGIC;
		assertNotSame(big, strategy.decide(room, 105).target);
		big.ownHitUntil = 0;
		big.hp = 3;
		room.ready = 105;
		decision = strategy.decide(room, 105);
		assertSame(decision.reason, big, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void southThirtyChinStartsPathDuringPipeCooldown()
	{
		Room room = room(30, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		room.ready = 105;
		crab(room, 30, 100, Lane.SOUTH, 'E', false, Style.MELEE, 3296, 4236);
		Decision decision = strategy.decide(room, 103);
		assertEquals(Weapon.CHIN, decision.weapon);
		assertTrue(decision.reason, decision.tile.getX() > 3290 || decision.tile.getY() < 4248);
	}

	@Test
	public void waveThirtyOneFreshRangerBeforeMagicCleanup()
	{
		Room room = room(31, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab fresh = crab(room, 31, 100, Lane.WEST, 'S', false, Style.RANGE, 3284, 4248);
		crab(room, 30, 96, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3295, 4248);
		Decision decision = strategy.decide(room, 103);
		assertSame(decision.reason, fresh, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void openingScratchDoesNotYieldOneTickToOrdinaryFiller()
	{
		// Native seed133: the old mage small tempted Eye while the assigned scratch was two steps away.
		Room room = room(4, 116);
		room.spawns[2] = 108;
		room.spawns[3] = 112;
		room.player = RangerStrategy.tile(3292, 4243);
		Crab scratch = crab(room, 2, 108, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4242);
		crab(room, 2, 108, Lane.WEST, 'N', false, Style.MAGIC, 3290, 4249);
		crab(room, 4, 116, Lane.WEST, 'N', false, Style.RANGE, 3282, 4249);
		Decision decision = strategy.decide(room, 117);
		assertTrue(decision.reason, decision.target == scratch || decision.approach == scratch);
		assertEquals(Weapon.CLAWS, decision.weapon);
	}

	@Test
	public void openingSouthChinComesBeforeSurvivingOpeningBowTarget()
	{
		Room room = room(3, 112);
		room.spawns[1] = 104;
		room.player = RangerStrategy.tile(3296, 4244);
		Crab survivor = crab(room, 1, 104, Lane.WEST, 'S', false, Style.RANGE, 3290, 4248);
		survivor.hp = 2;
		Crab south = crab(room, 3, 112, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4235);
		Decision decision = strategy.decide(room, 114);
		assertSame(decision.reason, south, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void missedWestThirtyPipeCannotBlockSouthThirtyChinWindow()
	{
		Room room = room(30, 100);
		room.player = RangerStrategy.tile(3293, 4248);
		crab(room, 30, 100, Lane.WEST, 'S', true, Style.MELEE, 3286, 4248);
		Crab south = crab(room, 30, 100, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4238);
		crab(room, 30, 100, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4238);
		Decision decision = strategy.decide(room, 105);
		assertSame(decision.reason, south, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void survivingThirtyOneRangerTakesPriorityBeforePermanentMelee()
	{
		Room room = room(31, 100);
		room.player = RangerStrategy.tile(3295, 4248);
		room.cleanup = true;
		Crab fresh = crab(room, 31, 100, Lane.EAST, 'S', false, Style.RANGE, 3300, 4248);
		fresh.hp = 3;
		crab(room, 30, 96, Lane.EAST, 'S', true, Style.RANGE, 3292, 4248);
		Decision decision = strategy.decide(room, 123);
		assertSame(decision.reason, fresh, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void westFiveConfirmedSurvivorFinishesBeforeWestFourSmall()
	{
		Room room = room(5, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab big = crab(room, 5, 100, Lane.WEST, 'S', true, Style.RANGE, 3283, 4248);
		Decision first = strategy.decide(room, 102);
		assertSame(big, first.target);
		strategy.attacked(first, 102);
		big.ownHitUntil = 0;
		big.hp = 3;
		crab(room, 4, 96, Lane.WEST, 'N', false, Style.RANGE, 3288, 4249);
		room.ready = 104;
		Decision decision = strategy.decide(room, 104);
		assertSame(decision.reason, big, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void deadOpeningScratchDoesNotBlockWestFiveService()
	{
		Room room = room(5, 100);
		room.player = RangerStrategy.tile(3290, 4248);
		Crab scratch = crab(room, 2, 88, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4243);
		scratch.dying = true;
		scratch.hp = 0;
		Crab big = crab(room, 5, 100, Lane.WEST, 'S', true, Style.RANGE, 3283, 4248);
		Decision decision = strategy.decide(room, 102);
		assertSame(decision.reason, big, decision.target);
	}

	@Test
	public void southElevenWestSmallIsSavedForMageOpeningShot()
	{
		Room room = room(11, 100);
		Crab saved = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		assertNotSame(saved, strategy.decide(room, 105).target);
		assertNotSame(saved, strategy.decide(room, 108).target);
		assertSame(saved, strategy.decide(room, 109).target);
	}

	@Test
	public void southElevenMagePrimaryReleasesIfMageIsNulled()
	{
		Room room = room(11, 100);
		Crab saved = crab(room, 11, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4243);
		saved.partnerNulled = true;
		assertSame(saved, strategy.decide(room, 105).target);
	}

	@Test
	public void eastThirtyOneApproachesDuringCooldownBeforeNearbyCleanupSurvivor()
	{
		Room room = room(31, 100);
		room.player = RangerStrategy.tile(3291, 4248);
		room.ready = 112;
		Crab east = crab(room, 31, 100, Lane.EAST, 'S', false, Style.RANGE, 3301, 4247);
		Crab south = crab(room, 31, 100, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4244);
		strategy.attacked(new Decision(south, Weapon.CHIN, room.player, false, "South chin 31", "Previous shot"), 109);
		south.ownHitUntil = 0;
		Decision decision = strategy.decide(room, 110);
		assertTrue(decision.reason, decision.approach == east || decision.target == east);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertTrue(decision.reason, decision.tile.getX() > room.player.getX());
	}

	@Test
	public void newlyConfirmedSurvivorReplacesApproachChosenWhileProjectileWasAirborne()
	{
		Room room = room(8, 100);
		room.player = RangerStrategy.tile(3295, 4246);
		room.ready = 103;
		Crab survivor = crab(room, 8, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3293, 4245);
		Crab fresh = crab(room, 8, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3290, 4252);
		strategy.attacked(new Decision(survivor, Weapon.PIPE, room.player, false, null, "Previous shot"), 100);
		// Native leases last until the real impact; HP is still unchanged at this observation.
		survivor.ownHitUntil = 132;
		Decision approach = strategy.decide(room, 101);
		assertTrue(approach.reason, approach.approach == fresh || approach.target == fresh);
		// A real nonlethal impact resolves before the next ready decision.
		survivor.ownHitUntil = 0;
		survivor.hp = 2;
		Decision decision = strategy.decide(room, 103);
		assertSame(decision.reason, survivor, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void southTwentyTwoWestPrimaryIsSavedForMageDinh()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3296, 4244);
		Crab saved = crab(room, 22, 100, Lane.SOUTH, 'W', false, Style.MAGIC, 3295, 4236);
		assertNotSame(saved, strategy.decide(room, 103).target);
		saved.partnerNulled = true;
		assertSame(saved, strategy.decide(room, 103).target);
	}

	@Test
	public void genericChinDoesNotSplashSavedTwentyTwoMagePrimary()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3295, 4243);
		Crab saved = crab(room, 22, 100, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4238);
		crab(room, 22, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4238);
		crab(room, 22, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3297, 4238);
		Decision decision = strategy.decide(room, 105);
		assertFalse(decision.reason, decision.weapon == Weapon.CHIN
			&& decision.target != null && decision.target.point.distanceTo(saved.point) <= 1);
	}

	@Test
	public void savedTwentyTwoPrimaryDoesNotBlockChinWhenMageIsNulled()
	{
		Room room = room(22, 100);
		room.player = RangerStrategy.tile(3295, 4243);
		Crab saved = crab(room, 22, 100, Lane.SOUTH, 'W', false, Style.RANGE, 3295, 4238);
		saved.partnerNulled = true;
		crab(room, 22, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3296, 4238);
		crab(room, 22, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3297, 4238);
		Decision decision = strategy.decide(room, 105);
		assertEquals(decision.reason, Weapon.CHIN, decision.weapon);
	}

	@Test
	public void legalEarlyScratchCountsBeforeWestFiveRatherThanRepeatingOldSouthTwo()
	{
		Room room = room(4, 116);
		room.spawns[2] = 108;
		room.player = RangerStrategy.tile(3296, 4243);
		Crab scratch = crab(room, 2, 108, Lane.SOUTH, 'W', false, Style.MELEE, 3295, 4242);
		Decision first = strategy.decide(room, 117);
		assertSame(scratch, first.target);
		assertEquals("Opening scratch", first.rule);
		strategy.attacked(first, 117);
		scratch.ownHitUntil = 0;
		scratch.hp = 3;
		scratch.point = RangerStrategy.tile(3295, 4243);
		room.wave = 5;
		room.spawns[5] = 120;
		room.ready = 121;
		Crab big = crab(room, 5, 120, Lane.WEST, 'S', true, Style.RANGE, 3282, 4248);
		Decision stage = strategy.decide(room, 121);
		assertNull(stage.target);
		assertEquals(RangerStrategy.tile(3290, 4248), stage.tile);
		assertEquals(Weapon.PIPE, stage.weapon);
		room.player = stage.tile;
		big.point = RangerStrategy.tile(3283, 4248);
		Decision shot = strategy.decide(room, 122);
		assertSame(big, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
	}

	@Test
	public void requiredWestTwentySixMageNullBackupSurvivesItsAgeTwentySixBoundary()
	{
		Room room = room(29, 240);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab big = crab(room, 26, 220, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4246);
		big.partnerNulled = true;
		crab(room, 29, 244, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4247);
		Decision decision = strategy.decide(room, 247);
		assertSame(decision.reason, big, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void requiredMageNullBackupYieldsToFreshOwnRangedSmall()
	{
		Room room = room(29, 240);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab big = crab(room, 26, 220, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4246);
		big.partnerNulled = true;
		Crab own = crab(room, 29, 244, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4247);
		assertSame(own, strategy.decide(room, 247).target);
	}

	@Test
	public void requiredBackupDoesNotStealAvailableMageParent()
	{
		Room room = room(29, 240);
		room.player = RangerStrategy.tile(3291, 4248);
		crab(room, 26, 220, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4246);
		Crab fallback = crab(room, 29, 244, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4247);
		assertSame(fallback, strategy.decide(room, 247).target);
	}

	@Test
	public void requiredMageNullBackupStillRespectsFinalBirthCutoff()
	{
		Room room = room(31, 240);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab big = crab(room, 26, 220, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4246);
		big.partnerNulled = true;
		assertNotSame(big, strategy.decide(room, 247).target);
	}

	@Test
	public void requiredSouthTwentyFourMageNullBackupDoesNotEndFourWavesLater()
	{
		Room room = room(29, 240);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab big = crab(room, 24, 205, Lane.SOUTH, 'W', true, Style.MAGIC, 3291, 4246);
		big.partnerNulled = true;
		crab(room, 29, 244, Lane.SPLIT, (char) 0, false, Style.MAGIC, 3294, 4247);
		Decision decision = strategy.decide(room, 247);
		assertSame(decision.reason, big, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void requiredMageNullBackupYieldsToWestThirtyOpeningPipe()
	{
		Room room = room(30, 244);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab old = crab(room, 26, 220, Lane.WEST, 'S', true, Style.MAGIC, 3291, 4246);
		old.partnerNulled = true;
		Crab packet = crab(room, 30, 244, Lane.WEST, 'S', true, Style.RANGE, 3284, 4248);
		Decision decision = strategy.decide(room, 247);
		assertSame(decision.reason, packet, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	private void checkForecastFollowup(int damage, boolean finish)
	{
		Room room = room(8, 100);
		room.player = RangerStrategy.tile(3295, 4246);
		room.ready = 102;
		Crab survivor = crab(room, 8, 100, Lane.SPLIT, (char) 0, false, Style.RANGE, 3293, 4245);
		crab(room, 8, 102, Lane.SPLIT, (char) 0, false, Style.RANGE, 3290, 4252);
		crab(room, 8, 102, Lane.SPLIT, (char) 0, false, Style.RANGE, 3291, 4252);
		strategy.attacked(new Decision(survivor, Weapon.PIPE, room.player, false, null, "Previous shot"), 100);
		survivor.ownHitUntil = 132;
		survivor.pendingDamage = damage;
		Decision decision = strategy.decide(room, 102);
		if (finish)
		{
			assertSame(decision.reason, survivor, decision.target);
			assertEquals(Weapon.PIPE, decision.weapon);
		}
		else assertNotSame(decision.reason, survivor, decision.target);
	}

	@Test
	public void knownNonlethalFlightEarnsPrecapFollowupPriority()
	{
		checkForecastFollowup(2, true);
	}

	@Test
	public void knownLethalFlightDoesNotEarnAnotherPrecapShot()
	{
		checkForecastFollowup(8, false);
	}

	@Test
	public void unknownFlightRetainsConservativePrecapProtection()
	{
		checkForecastFollowup(-1, false);
	}

	private void checkWestTwentyFourForecast(int damage, boolean expectEye)
	{
		Room room = room(24, 100);
		room.player = RangerStrategy.tile(3291, 4248);
		Crab big = crab(room, 24, 100, Lane.WEST, 'S', true, Style.RANGE, 3284, 4248);
		Decision pipe = strategy.decide(room, 103);
		assertSame(big, pipe.target);
		strategy.attacked(pipe, 103);
		big.ownHitUntil = 135;
		big.pendingDamage = damage;
		room.ready = 105;
		big.point = RangerStrategy.tile(3285, 4248);
		Crab filler = crab(room, 24, 104, Lane.SPLIT, (char) 0, false, Style.RANGE, 3293, 4248);
		Decision approach = strategy.decide(room, 104);
		assertTrue(approach.reason, approach.target == filler || approach.approach == filler);
		big.style = Style.MAGIC;
		big.point = RangerStrategy.tile(3286, 4248);
		Decision decision = strategy.decide(room, 105);
		if (expectEye)
		{
			assertSame(decision.reason, big, decision.target);
			assertEquals(Weapon.EYE, decision.weapon);
		}
		else assertNotSame(big, decision.target);
	}

	@Test
	public void westTwentyFourNonlethalForecastReplacesInterimFillerApproach()
	{
		checkWestTwentyFourForecast(1, true);
	}

	@Test
	public void westTwentyFourLethalForecastSkipsEye()
	{
		checkWestTwentyFourForecast(16, false);
	}

	@Test
	public void westElevenBackupContinuesWhileMageDinhPacketCrossesIntoThirteen()
	{
		Room room = room(13, 92);
		room.spawns[12] = 84;
		room.player = RangerStrategy.tile(3291, 4248);
		room.ready = 98;
		Crab big = crab(room, 11, 76, Lane.WEST, 'S', true, Style.MAGIC, 3295, 4247);
		big.hp = 1;
		big.pendingDamage = 0;
		crab(room, 13, 94, Lane.SPLIT, (char) 0, false, Style.RANGE, 3294, 4248);
		Decision decision = strategy.decide(room, 97);
		assertTrue(decision.reason, decision.target == big || decision.approach == big);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void westElevenContinuationStillSkipsAlreadyLethalFlight()
	{
		Room room = room(13, 92);
		room.spawns[12] = 84;
		Crab big = crab(room, 11, 76, Lane.WEST, 'S', true, Style.MAGIC, 3295, 4247);
		big.hp = 1;
		big.pendingDamage = 1;
		Decision decision = strategy.decide(room, 97);
		assertNotSame(big, decision.target);
		assertNotSame(big, decision.approach);
	}

	@Test
	public void westElevenContinuationRespectsRangerNull()
	{
		Room room = room(13, 92);
		room.spawns[12] = 84;
		Crab big = crab(room, 11, 76, Lane.WEST, 'S', true, Style.MAGIC, 3295, 4247);
		big.nulled = true;
		Decision decision = strategy.decide(room, 97);
		assertNotSame(big, decision.target);
		assertNotSame(big, decision.approach);
	}

	@Test
	public void westElevenContinuationEndsAfterPacketAgeFifteen()
	{
		Room room = room(13, 92);
		room.spawns[12] = 84;
		Crab big = crab(room, 11, 76, Lane.WEST, 'S', true, Style.MAGIC, 3295, 4247);
		Decision decision = strategy.decide(room, 100);
		assertNotSame(big, decision.target);
		assertNotSame(big, decision.approach);
	}
}
