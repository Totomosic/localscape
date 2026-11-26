package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;

public class MagePrecapFreshnessTest
{
	@Test
	public void prefersAOneTickSplitForecastToAnUnhitTwoWaveOldMelee()
	{
		forecastBeforeYoungerOldMelee(2, 1);
	}

	@Test
	public void prefersATwoTickSplitForecastToAnUnhitThreeWaveOldMelee()
	{
		forecastBeforeYoungerOldMelee(3, 2);
	}

	@Test
	public void followsThroughOnAnObservedNewbornAfterOneManualApproachTick()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 116;
		room.player = MageStrategy.tile(3290, 4248);
		Crab old = new Crab(1, 1, 100, 14, Lane.SPLIT, (char) 0, false, Style.MELEE,
			MageStrategy.tile(3291, 4248));
		Crab fresh = new Crab(2, 2, 120, 16, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3301, 4254));
		old.pendingDamage = fresh.pendingDamage = 0;
		room.crabs.add(old);
		room.crabs.add(fresh);
		Decision move = strategy.decide(room, 120);
		assertNull(move.target);
		assertSame(fresh, move.approach);
		room.player = MageStrategy.tile(3292, 4248);
		Decision cast = strategy.decide(room, 121);
		assertSame(fresh, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void preservesAConfirmedSurvivorAtTheNewTwoWaveThreshold()
	{
		for (boolean opened : new boolean[] {false, true})
		{
			MageStrategy strategy = new MageStrategy();
			Room room = new Room();
			room.wave = 16;
			room.spawns[16] = 116;
			room.player = MageStrategy.tile(3298, 4251);
			Crab old = new Crab(1, 1, 100, 14, Lane.SPLIT, (char) 0, false, Style.MELEE,
				room.player.dy(1));
			old.pendingDamage = 0;
			if (opened) strategy.attacked(new Decision(old, Weapon.SULPHUR, room.player, false, null, "previous attack"), 117, 116);
			else old.hp = 4;
			room.crabs.add(old);
			Crab parent = new Crab(2, 2, 108, 15, Lane.EAST, 'S', true, Style.MELEE,
				MageStrategy.tile(3298, 4247));
			parent.hp = 0;
			parent.dying = true;
			parent.observeSplitAt(122);
			room.crabs.add(parent);
			assertSame(old, strategy.decide(room, 120).target);
		}
	}

	@Test
	public void waitsForAnAssignedLaneParentThenCastsOnItsBirthTick()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 18;
		room.spawns[18] = 136;
		room.player = MageStrategy.tile(3291, 4248);
		Crab old = new Crab(1, 1, 120, 16, Lane.SPLIT, (char) 0, false, Style.MELEE,
			MageStrategy.tile(3292, 4248));
		old.pendingDamage = 0;
		room.crabs.add(old);
		for (int tick = 142; tick <= 143; tick++)
		{
			Decision wait = strategy.decide(room, tick);
			assertNull(wait.target);
			assertTrue(wait.reason, wait.reason.startsWith("Wait for fresh crab"));
		}
		room.wave = 19;
		room.spawns[19] = 144;
		Crab parent = new Crab(2, 2, 144, 19, Lane.WEST, 'S', true, Style.MAGIC,
			MageStrategy.tile(3281, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		Decision cast = strategy.decide(room, 144);
		assertSame(parent, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
		strategy.attacked(cast, 145, 144);
	}

	private void forecastBeforeYoungerOldMelee(int waveGap, int waitTicks)
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 116;
		room.player = MageStrategy.tile(3298, 4251);
		Crab parent = new Crab(69, 69, 108, 15, Lane.EAST, 'S', true, Style.MELEE,
			MageStrategy.tile(3298, 4247));
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(120 + waitTicks);
		room.crabs.add(parent);
		Crab old = new Crab(56, 56, 100, 16 - waveGap, Lane.SPLIT, (char) 0, false, Style.MELEE,
			room.player.dy(1));
		old.pendingDamage = 0;
		room.crabs.add(old);
		for (int tick = 120; tick < 120 + waitTicks; tick++)
		{
			Decision wait = strategy.decide(room, tick);
			assertNull("must wait rather than hit the old melee: " + wait.reason, wait.target);
			assertTrue(wait.reason, wait.reason.startsWith("Wait for fresh crab"));
		}
		room.crabs.remove(parent);
		Crab child = new Crab(70, 70, 120 + waitTicks, 16, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3298, 4247));
		child.pendingDamage = 0;
		room.crabs.add(child);
		Decision cast = strategy.decide(room, 120 + waitTicks);
		assertSame(child, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
		strategy.attacked(cast, 121 + waitTicks, 120 + waitTicks);
	}

	@Test
	public void waitsTwoReadyTicksForTheForecastThenAttacksTheNewborn()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 116;
		room.player = MageStrategy.tile(3298, 4251);
		Crab parent = new Crab(69, 69, 100, 14, Lane.EAST, 'S', true, Style.RANGE,
			MageStrategy.tile(3298, 4247));
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(122);
		room.crabs.add(parent);
		Crab old = new Crab(56, 56, 86, 12, Lane.SPLIT, (char) 0, false, Style.MELEE,
			room.player.dy(1));
		old.pendingDamage = 0;
		room.crabs.add(old);

		for (int tick = 120; tick <= 121; tick++)
		{
			Decision wait = strategy.decide(room, tick);
			assertNull(wait.target);
			assertTrue(wait.reason, wait.reason.startsWith("Wait for fresh crab"));
		}
		room.crabs.remove(parent);
		Crab child = new Crab(70, 70, 122, 16, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3298, 4247));
		child.pendingDamage = 0;
		room.crabs.add(child);
		assertSame(child, strategy.decide(room, 122).target);
	}

	@Test
	public void changingTheForecastCannotExtendTheTwoReadyTickWait()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 116;
		room.player = MageStrategy.tile(3298, 4251);
		Crab parent = new Crab(69, 69, 100, 14, Lane.EAST, 'S', true, Style.RANGE,
			MageStrategy.tile(3298, 4247));
		parent.hp = 0;
		parent.dying = true;
		room.crabs.add(parent);
		Crab old = new Crab(56, 56, 86, 12, Lane.SPLIT, (char) 0, false, Style.MELEE,
			room.player.dy(1));
		old.pendingDamage = 0;
		room.crabs.add(old);

		for (int tick = 120; tick <= 121; tick++)
		{
			parent.observeSplitAt(tick + 2);
			assertNull(strategy.decide(room, tick).target);
		}
		parent.observeSplitAt(124);
		assertSame(old, strategy.decide(room, 122).target);
	}

	@Test
	public void waitOneTickWhenTheVisibleDeathAnimationEndsBeforeAnOldMeleeFollowup()
	{
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 116;
		room.player = MageStrategy.tile(3298, 4251);
		Crab parent = new Crab(69, 69, 100, 14, Lane.EAST, 'S', true, Style.RANGE,
			MageStrategy.tile(3298, 4247));
		parent.hp = 0;
		parent.dying = true;
		room.crabs.add(parent);
		strategy.decide(room, 116);

		Crab old = new Crab(56, 56, 86, 12, Lane.SPLIT, (char) 0, false, Style.MELEE,
			room.player.dy(1));
		old.pendingDamage = 0;
		room.crabs.add(old);
		parent.observeSplitAt(121);
		Decision wait = strategy.decide(room, 120);
		assertNull(wait.target);
		assertTrue(wait.reason, wait.reason.startsWith("Wait for fresh crab"));
	}

	@Test
	public void attackANewMagicSplitBeforeAFourWaveOldMelee()
	{
		Room room = new Room();
		room.wave = 9;
		room.ready = 56;
		room.player = MageStrategy.tile(3295, 4248);
		Crab old = new Crab(51, 51, 28, 5, Lane.SPLIT, (char) 0, false, Style.MELEE,
			MageStrategy.tile(3296, 4248));
		Crab fresh = new Crab(90, 90, 56, 9, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3297, 4251));
		old.pendingDamage = fresh.pendingDamage = 0;
		room.crabs.add(old);
		room.crabs.add(fresh);

		Decision attack = new MageStrategy().decide(room, 56);
		assertSame(fresh, attack.target);
		assertEquals(Weapon.EYE, attack.weapon);
	}

	@Test
	public void castOnAReachableNewSplitWhileTheOlderMeleeNeedsDiagonalMovement()
	{
		Room room = new Room();
		room.wave = 10;
		room.spawns[10] = 68;
		room.ready = 68;
		room.player = MageStrategy.tile(3296, 4246);
		Crab melee = new Crab(27, 27, 52, 8, Lane.SPLIT, (char) 0, false, Style.MELEE,
			MageStrategy.tile(3299, 4243));
		Crab magic = new Crab(36, 36, 63, 9, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			MageStrategy.tile(3290, 4248));
		melee.pendingDamage = magic.pendingDamage = 0;
		room.crabs.add(melee);
		room.crabs.add(magic);

		Decision cast = new MageStrategy().decide(room, 68);
		assertSame(magic, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void stageForWestSixWhileTheOriginalMeleeIsStillAlive()
	{
		// Native seed 68409655670094. West 6 enters one tick after this decision.
		MageStrategy strategy = new MageStrategy();
		Room room = new Room();
		room.wave = 5;
		room.spawns[5] = 20;
		room.ready = 34;
		room.player = MageStrategy.tile(3294, 4251);
		Crab old = new Crab(3, 3, 4, 1, Lane.EAST, 'N', false, Style.MELEE,
			MageStrategy.tile(3292, 4254));
		old.pendingDamage = 0;
		room.crabs.add(old);

		Decision move = strategy.decide(room, 35);
		assertNull(move.target);
		assertNotSame(old, move.approach);
		assertEquals(MageStrategy.tile(3290, 4249), move.tile);

		room.wave = 6;
		room.spawns[6] = 36;
		room.player = MageStrategy.tile(3292, 4250);
		Crab fresh = new Crab(20, 20, 36, 6, Lane.WEST, 'N', false, Style.MAGIC,
			MageStrategy.tile(3281, 4249));
		fresh.pendingDamage = 0;
		room.crabs.add(fresh);
		Decision request = strategy.decide(room, 36);
		assertSame(fresh, request.target);
		assertEquals(Weapon.EYE, request.weapon);
	}
}
