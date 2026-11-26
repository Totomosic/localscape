package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MageWest8OpeningTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();

	private Crab add(Crab crab)
	{
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private void beforeEight(int ready)
	{
		room.wave = 7;
		room.spawns[7] = 40;
		room.ready = ready;
		room.player = MageStrategy.tile(3297, 4245);
	}

	private Crab atEight()
	{
		room.wave = 8;
		room.spawns[8] = 52;
		room.ready = 52;
		room.player = MageStrategy.tile(3290, 4249);
		return add(new Crab(28, 28, 52, 8, Lane.WEST, 'S', true, Style.MAGIC, MageStrategy.tile(3281, 4248)));
	}

	private Crab olderMelee(int x, int y)
	{
		return add(new Crab(25, 25, 40, 7, Lane.EAST, 'N', false, Style.MELEE, MageStrategy.tile(x, y)));
	}

	private Crab southMagic(int y)
	{
		return add(new Crab(24, 24, 40, 7, Lane.SOUTH, 'E', false, Style.MAGIC, MageStrategy.tile(3296, y)));
	}

	private Crab rangeSplit()
	{
		return add(new Crab(31, 31, 49, 7, Lane.SPLIT, (char) 0, false, Style.RANGE, MageStrategy.tile(3291, 4249)));
	}

	@Test
	public void reservesTheOpeningBeforeChasingOlderMelee()
	{
		beforeEight(50);
		olderMelee(3300, 4250);

		Decision stage = strategy.decide(room, 50);
		assertNull(stage.target);
		assertNull(stage.approach);
		assertEquals(Weapon.EYE, stage.weapon);
		assertEquals(MageStrategy.tile(3290, 4249), stage.tile);
	}

	@Test
	public void firesOnTheBirthTickBeforeAnAdjacentOlderMelee()
	{
		Crab parent = atEight();
		olderMelee(3291, 4249);

		Decision attack = strategy.decide(room, 52);
		assertSame(parent, attack.target);
		assertEquals(Weapon.EYE, attack.weapon);
	}

	@Test
	public void earlySouthMagicFitsTheCooldownAndReturn()
	{
		beforeEight(47);
		room.player = MageStrategy.tile(3294, 4243);
		Crab magic = southMagic(4240);

		assertSame(magic, strategy.decide(room, 47).target);
	}

	@Test
	public void lateSouthMagicWouldSpendTheOpeningCooldown()
	{
		beforeEight(50);
		southMagic(4243);

		assertNull(strategy.decide(room, 50).target);
	}

	@Test
	public void reservesThePackedReturnAfterTheSouthSevenFollowup()
	{
		// Seed87034019481679/754: three direct-distance ticks are four on the packed route.
		beforeEight(49);
		room.player = MageStrategy.tile(3296, 4243);
		southMagic(4242);
		Decision move = strategy.decide(room, 49);
		assertNull(move.target);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);
	}

	@Test
	public void reservesThePackedReturnEvenWhenTheLateCastAimsWest()
	{
		// Seed87034019481745: the West6 target does not make a southern casting tile safe.
		beforeEight(49);
		room.player = MageStrategy.tile(3296, 4244);
		add(new Crab(20, 20, 36, 6, Lane.WEST, 'N', false, Style.MAGIC, MageStrategy.tile(3294, 4253)));
		Decision move = strategy.decide(room, 49);
		assertNull(move.target);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);
	}

	@Test
	public void aLateSplitForecastMustFitItsAttackCooldownBeforeWestEight()
	{
		beforeEight(49);
		room.player = MageStrategy.tile(3290, 4249);
		Crab old = add(new Crab(1, 1, 24, 5, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3291, 4250)));
		Crab dying = add(new Crab(2, 2, 36, 6, Lane.WEST, 'S', true, Style.MAGIC,
			MageStrategy.tile(3294, 4249)));
		dying.hp = 0;
		dying.dying = true;
		dying.observeSplitAt(50);
		// Eye49 fits52. Waiting for a split50 would release neither an Eye
		// nor a melee cooldown in time for the required parent.
		Decision cast = strategy.decide(room, 49);
		assertSame(old, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void leavesFreeReturnCyclesAfterANorthernMeleeRequest()
	{
		// Seeds264763169323407/435: contact can finish one tile beyond the estimate.
		for (int[] row : new int[][] {{3298, 4251, 3299, 4253}, {3297, 4251, 3299, 4254}})
		{
			strategy.reset();
			room.crabs.clear();
			beforeEight(47);
			room.player = MageStrategy.tile(row[0], row[1]);
			Crab melee = add(new Crab(19, 19, 34, 5, Lane.SPLIT, (char) 0, false, Style.MELEE,
				MageStrategy.tile(row[2], row[3])));
			melee.fighting = true;
			Decision move = strategy.decide(room, 47);
			assertNull(move.target);
			assertEquals(MageStrategy.tile(3290, 4249), move.tile);
		}
	}

	@Test
	public void aReturnCannotUseEitherTheFillerRequestOrTheWestEightBirthCycle()
	{
		// Seed264763169323550: the casting x is west of the old coordinate-specific margin.
		beforeEight(49);
		room.player = MageStrategy.tile(3293, 4243);
		Crab magic = add(new Crab(19, 19, 34, 5, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3291, 4245)));
		magic.hp = 6;
		Decision move = strategy.decide(room, 49);
		assertNull(move.target);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);
	}

	@Test
	public void movesWestDuringCooldownWhenThePrepCastStillReachesFromTheOpeningTile()
	{
		beforeEight(49);
		room.player = MageStrategy.tile(3293, 4245);
		Crab magic = add(new Crab(19, 19, 34, 5, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3291, 4245)));
		magic.hp = 6;
		Decision move = strategy.decide(room, 47);
		assertNull(move.target);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);
		room.player = move.tile;
		Decision prep = strategy.decide(room, 49);
		assertSame(magic, prep.target);
		assertEquals(Weapon.EYE, prep.weapon);
		strategy.attacked(prep, 50, 49);
		Crab parent = atEight();
		assertSame(parent, strategy.decide(room, 52).target);
	}

	@Test
	public void anEarlierSparePipeFits()
	{
		beforeEight(49);
		room.player = MageStrategy.tile(3290, 4249);
		Crab range = rangeSplit();

		assertSame(range, strategy.decide(room, 49).target);
	}

	@Test
	public void aLatePipeWouldSpendTheOpeningCooldown()
	{
		beforeEight(51);
		room.player = MageStrategy.tile(3290, 4249);
		rangeSplit();

		assertNull(strategy.decide(room, 51).target);
	}

	@Test
	public void ownNullReleasesTheReservation()
	{
		Crab parent = atEight();
		parent.nulled = true;
		Crab melee = olderMelee(3291, 4249);

		assertSame(melee, strategy.decide(room, 52).target);
	}

	@Test
	public void queuedLethalDamageReleasesTheReservation()
	{
		Crab parent = atEight();
		parent.pendingDamage = parent.hp;
		Crab melee = olderMelee(3291, 4249);

		assertSame(melee, strategy.decide(room, 52).target);
	}

	@Test
	public void theDeathAnimationReleasesTheReservation()
	{
		Crab parent = atEight();
		parent.dying = true;
		Crab melee = olderMelee(3291, 4249);

		assertSame(melee, strategy.decide(room, 52).target);
	}

	@Test
	public void theAssignmentStillWorksAfterTheOpeningWindow()
	{
		Crab parent = atEight();
		parent.point = MageStrategy.tile(3284, 4248);

		assertSame(parent, strategy.decide(room, 55).target);
	}

	@Test
	public void cooldownPreventsAnImmediateAttack()
	{
		atEight();
		room.ready = 55;

		assertNull(strategy.decide(room, 52).target);
	}

	@Test
	public void reservesTheNextAdmissionWhenTheParentHasNotSpawned()
	{
		beforeEight(50);

		Decision stage = strategy.decide(room, 54);
		assertNull(stage.target);
		assertTrue(stage.reason, stage.reason.contains("Reserve West big 8"));
	}

	@Test
	public void aConfirmedOpeningDoesNotReserveTwice()
	{
		atEight();
		strategy.attacked(strategy.decide(room, 52), 53, 52);
		room.ready = 55;

		Decision stage = strategy.decide(room, 53);
		assertFalse(stage.reason, stage.reason.startsWith("Reserve West big 8"));
	}

	@Test
	public void retriesAnUncoveredSurvivorAsSoonAsTheEyeIsReady()
	{
		Crab parent = atEight();
		strategy.attacked(strategy.decide(room, 52), 53, 52);
		room.ready = 55;
		parent.hp = 7;
		parent.point = MageStrategy.tile(3284, 4248);
		olderMelee(3291, 4249);

		Decision retry = strategy.decide(room, 55);
		assertSame(parent, retry.target);
		assertEquals(Weapon.EYE, retry.weapon);
	}
}
