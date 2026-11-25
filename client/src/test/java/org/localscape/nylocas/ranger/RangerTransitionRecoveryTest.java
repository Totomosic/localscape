package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerTransitionRecoveryTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room()
	{
		Room room = new Room();
		room.wave = 20;
		room.spawns[18] = 100;
		room.spawns[19] = 108;
		room.spawns[20] = 120;
		room.player = RangerStrategy.tile(3293, 4248);
		return room;
	}

	private Crab crab(Room room, int wave, int born, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, 'S', big, style, RangerStrategy.tile(x, y));
		room.crabs.add(crab);
		return crab;
	}

	private Crab lateParent(Room room)
	{
		Crab parent = crab(room, 18, 100, Lane.WEST, true, Style.MAGIC, 3291, 4248);
		parent.hp = 3;
		parent.pendingDamage = 0;
		return parent;
	}

	private void acknowledgeWestTwentyBow(Room room)
	{
		Crab bow = crab(room, 20, 120, Lane.WEST, true, Style.MELEE, 3290, 4248);
		strategy.attacked(new Decision(bow, Weapon.BOW, room.player, false, "West bow flicker 20", "Previous bow"), 125);
		bow.hp = 0;
		bow.dying = true;
		room.ready = 130;
	}

	@Test
	public void actualTwentyRecoversEighteenAfterItsPermanentMagicFlicker()
	{
		Room room = room();
		Crab parent = lateParent(room);
		acknowledgeWestTwentyBow(room);
		Decision decision = strategy.decide(room, 130);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void bowCooldownCanPrepareTheLegalMagicRecovery()
	{
		Room room = room();
		Crab parent = lateParent(room);
		acknowledgeWestTwentyBow(room);
		Decision decision = strategy.decide(room, 126);
		assertTrue(decision.reason, decision.target == parent || decision.approach == parent);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void permanentMagicFlickerReplacesFillerIntentFromThePreviousCooldownTick()
	{
		Room room = room();
		room.spawns[19] = 112;
		room.spawns[20] = 124;
		room.player = RangerStrategy.tile(3291, 4248);
		Crab parent = crab(room, 18, 100, Lane.WEST, true, Style.RANGE, 3299, 4251);
		parent.hp = 3;
		parent.pendingDamage = 0;
		Crab previous = crab(room, 19, 124, Lane.SPLIT, false, Style.RANGE, 3292, 4248);
		strategy.attacked(new Decision(previous, Weapon.PIPE, room.player, false, null, "Previous pipe"), 124);
		previous.pendingDamage = previous.hp;
		room.ready = 126;
		Crab filler = crab(room, 20, 124, Lane.SPLIT, false, Style.RANGE, 3293, 4248);
		crab(room, 20, 124, Lane.WEST, true, Style.MELEE, 3282, 4248);
		Decision planned = strategy.decide(room, 125);
		assertTrue(planned.reason, planned.target == filler || planned.approach == filler);
		parent.style = Style.MAGIC;
		Decision recovery = strategy.decide(room, 126);
		assertSame(recovery.reason, parent, recovery.target);
		assertEquals(Weapon.EYE, recovery.weapon);
	}

	@Test
	public void initialSouthEighteenMagicFlickerDoesNotReplaceItsEntryTiming()
	{
		Room room = room();
		room.wave = 18;
		Crab parent = crab(room, 18, 100, Lane.SOUTH, true, Style.MAGIC, 3295, 4236);
		Decision decision = strategy.decide(room, 105);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
	}

	@Test
	public void lateRecoveryEndsWhenTwentyOneActuallySpawns()
	{
		Room room = room();
		Crab parent = lateParent(room);
		room.wave = 21;
		room.spawns[21] = 136;
		Decision decision = strategy.decide(room, 136);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
	}

	@Test
	public void lateRecoveryDoesNotSpendTheTwentyOneChinReturnBudget()
	{
		Room room = room();
		Crab parent = lateParent(room);
		acknowledgeWestTwentyBow(room);
		Decision decision = strategy.decide(room, 134);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void lateMagicParentStillYieldsToWestTwentyBow()
	{
		Room room = room();
		room.spawns[18] = 96;
		Crab parent = crab(room, 18, 96, Lane.SOUTH, true, Style.MAGIC, 3294, 4244);
		parent.hp = 1;
		Crab bow = crab(room, 20, 120, Lane.WEST, true, Style.RANGE, 3286, 4248);
		Decision decision = strategy.decide(room, 125);
		assertSame(bow, decision.target);
		assertEquals(Weapon.BOW, decision.weapon);
	}

	@Test
	public void recoveryRespectsRangerNullAndAlreadyLethalForecast()
	{
		for (int guard = 0; guard < 2; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab parent = lateParent(room);
			acknowledgeWestTwentyBow(room);
			if (guard == 0) parent.nulled = true;
			else parent.pendingDamage = parent.hp;
			Decision decision = strategy.decide(room, 130);
			assertNotSame(parent, decision.target);
			assertNotSame(parent, decision.approach);
		}
	}
}
