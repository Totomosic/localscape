package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerCleanupFreshSplitTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;
	private Room room()
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[31] = 268;
		room.cleanup = true;
		room.ready = 284;
		room.player = RangerStrategy.tile(3297, 4248);
		return room;
	}

	private Crab split(Room room, int born, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, 31, Lane.SPLIT, (char) 0, false, style,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private Crab prepareOldEye(Room room)
	{
		Crab old = split(room, 278, Style.MAGIC, 3296, 4252);
		Decision plan = strategy.decide(room, 283);
		assertTrue(plan.reason, plan.approach == old || plan.reason.startsWith("Close during cleanup cooldown for Eye #" + old.index));
		assertEquals(Weapon.EYE, plan.weapon);
		old.point = RangerStrategy.tile(3297, 4253);
		return old;
	}

	@Test
	public void newAdjacentMagicSplitsReplaceAnOldEyeIntentAtReadiness()
	{
		// Native seed37176169104859, tick283 preparation then two children born284.
		Room room = room();
		Crab old = prepareOldEye(room);
		Crab fresh = split(room, 284, Style.MAGIC, 3298, 4248);
		split(room, 284, Style.MAGIC, 3299, 4249);
		Decision decision = strategy.decide(room, 284);
		assertSame(decision.reason, fresh, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
		assertNotSame(old, decision.target);
	}

	@Test
	public void sameBirthSiblingKeepsItsExistingApproach()
	{
		Room room = room();
		Crab old = prepareOldEye(room);
		split(room, 278, Style.MAGIC, 3298, 4248);
		assertSame(old, strategy.decide(room, 284).target);
	}

	@Test
	public void unusableNewSplitDoesNotInterruptUsefulOldWork()
	{
		for (int guard = 0; guard < 3; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab old = prepareOldEye(room);
			Crab fresh = split(room, 284, Style.MAGIC, 3298, 4248);
			if (guard == 0) fresh.nulled = true;
			else if (guard == 1) fresh.pendingDamage = fresh.hp;
			else { fresh.pendingDamage = -1; fresh.claimedUntil = 290; }
			assertSame("guard " + guard, old, strategy.decide(room, 284).target);
		}
	}

	@Test
	public void freshMagicDoesNotOverrideExistingRangedPriority()
	{
		Room room = room();
		Crab range = split(room, 278, Style.RANGE, 3296, 4248);
		assertSame(range, strategy.decide(room, 283).approach);
		split(room, 284, Style.MAGIC, 3298, 4248);
		Decision decision = strategy.decide(room, 284);
		assertSame(range, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
