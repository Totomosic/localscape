package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerNineteenBackupTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room()
	{
		Room room = new Room();
		room.wave = 20;
		room.spawns[19] = 108;
		room.spawns[20] = 120;
		room.player = RangerStrategy.tile(3293, 4248);
		return room;
	}

	private Crab crab(Room room, int wave, int born, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, lane == Lane.SOUTH ? 'W' : 'S', big, style, RangerStrategy.tile(x, y));
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void knownWestNineteenSurvivorKeepsServiceBeforeTwentyBow()
	{
		Room room = room();
		Crab parent = crab(room, 19, 108, Lane.WEST, true, Style.MAGIC, 3291, 4248);
		strategy.attacked(new Decision(parent, Weapon.EYE, room.player, false, null, "Previous Eye"), 119);
		parent.hp = 9;
		parent.ownHitUntil = 0;
		parent.pendingDamage = 0;
		room.ready = 122;
		crab(room, 20, 120, Lane.WEST, true, Style.MELEE, 3283, 4248);
		crab(room, 20, 121, Lane.SPLIT, false, Style.RANGE, 3295, 4248);
		Decision decision = strategy.decide(room, 122);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void actualTwentyBackupReplacesFillerIntentPreparedDuringNineteen()
	{
		Room room = room();
		room.wave = 19;
		room.spawns[19] = 112;
		room.spawns[20] = -1;
		Crab parent = crab(room, 19, 112, Lane.EAST, true, Style.MAGIC, 3295, 4246);
		parent.hp = 3;
		parent.pendingDamage = 0;
		Crab previous = crab(room, 19, 112, Lane.WEST, true, Style.MAGIC, 3291, 4248);
		strategy.attacked(new Decision(previous, Weapon.EYE, room.player, false, "West magic big 19", "Previous Eye"), 122);
		previous.pendingDamage = previous.hp;
		room.ready = 125;
		Crab filler = crab(room, 19, 122, Lane.SPLIT, false, Style.RANGE, 3294, 4248);
		Decision planned = strategy.decide(room, 123);
		assertTrue(planned.reason, planned.target == filler || planned.approach == filler);
		room.wave = 20;
		room.spawns[20] = 124;
		crab(room, 20, 124, Lane.WEST, true, Style.MELEE, 3282, 4248);
		Decision recovery = strategy.decide(room, 124);
		assertTrue(recovery.reason, recovery.target == parent || recovery.approach == parent);
		assertEquals(Weapon.EYE, recovery.weapon);
	}

	@Test
	public void anyMagicNineteenLaneCanReceiveBackupDuringTwenty()
	{
		for (Lane lane : new Lane[]{Lane.WEST, Lane.SOUTH, Lane.EAST})
		{
			strategy.reset();
			Room room = room();
			Crab parent = crab(room, 19, 108, lane, true, Style.MAGIC, 3295, 4246);
			parent.pendingDamage = 0;
			crab(room, 20, 120, Lane.WEST, true, Style.MELEE, 3283, 4248);
			crab(room, 20, 121, Lane.SPLIT, false, Style.RANGE, 3295, 4248);
			Decision decision = strategy.decide(room, 122);
			assertSame(lane + ": " + decision.reason, parent, decision.target);
			assertEquals(Weapon.EYE, decision.weapon);
		}
	}

	@Test
	public void rangedEighteenServiceStillTakesPriority()
	{
		Room room = room();
		Crab own = crab(room, 18, 100, Lane.WEST, true, Style.RANGE, 3291, 4248);
		own.hp = 4;
		crab(room, 19, 108, Lane.EAST, true, Style.MAGIC, 3295, 4246).hp = 1;
		Decision decision = strategy.decide(room, 122);
		assertSame(own, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void twentyWestBowKeepsItsHardWindow()
	{
		Room room = room();
		crab(room, 19, 108, Lane.WEST, true, Style.MAGIC, 3291, 4248);
		Crab packet = crab(room, 20, 120, Lane.WEST, true, Style.RANGE, 3286, 4248);
		Decision decision = strategy.decide(room, 125);
		assertSame(packet, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void closingMagicColorCannotQueueEyeIntoMelee()
	{
		Room room = room();
		Crab parent = crab(room, 19, 108, Lane.WEST, true, Style.MAGIC, 3291, 4248);
		Decision decision = strategy.decide(room, 133);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
		parent.style = Style.MELEE;
		decision = strategy.decide(room, 134);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
	}

	@Test
	public void backupDoesNotSpendTheTwentyOneReturnBudget()
	{
		Room room = room();
		room.player = RangerStrategy.tile(3290, 4243);
		Crab parent = crab(room, 19, 108, Lane.SOUTH, true, Style.MAGIC, 3293, 4246);
		Decision decision = strategy.decide(room, 132);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
	}

	@Test
	public void backupRespectsNullAndAlreadyLethalQueuedHits()
	{
		for (int guard = 0; guard < 2; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab parent = crab(room, 19, 108, Lane.WEST, true, Style.MAGIC, 3291, 4248);
			if (guard == 0) parent.nulled = true;
			else parent.pendingDamage = parent.hp;
			Decision decision = strategy.decide(room, 122);
			assertNotSame(parent, decision.target);
			assertNotSame(parent, decision.approach);
		}
	}
}
