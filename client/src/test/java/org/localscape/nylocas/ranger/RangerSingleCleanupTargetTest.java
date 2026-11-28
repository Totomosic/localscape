package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerSingleCleanupTargetTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room()
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[28] = 76;
		room.spawns[29] = 92;
		room.spawns[31] = 100;
		room.player = RangerStrategy.tile(3293, 4248);
		return room;
	}

	private Crab crab(Room room, int wave, int born, boolean big, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, big ? Lane.WEST : Lane.SPLIT, big ? 'S' : (char) 0,
			big, Style.MELEE, RangerStrategy.tile(x, y));
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void soleFreshMeleeSmallUsesNormalClawsAndApproaches()
	{
		Room room = room();
		Crab target = crab(room, 31, 120, false, 3299, 4248);
		Decision decision = strategy.decide(room, 121);
		assertTrue(decision.reason, decision.target == target || decision.approach == target);
		assertEquals(Weapon.SULPHUR, decision.weapon);
		assertTrue(decision.tile.getX() > room.player.getX());
		assertFalse(decision.special);
	}

	@Test
	public void soleFreshMeleeBigAlsoReceivesNormalClaws()
	{
		Room room = room();
		Crab target = crab(room, 30, 96, true, 3291, 4245);
		target.hp = 4;
		Decision decision = strategy.decide(room, 130);
		assertSame(decision.reason, target, decision.target);
		assertEquals(Weapon.SULPHUR, decision.weapon);
		assertFalse(decision.special);
	}

	@Test
	public void ignoredOldLivingCrabStillCountsAgainstSingleton()
	{
		Room room = room();
		Crab target = crab(room, 31, 120, false, 3299, 4248);
		target.claimedUntil = 140;
		crab(room, 29, 92, false, 3290, 4253).hp = 1;
		Decision decision = strategy.decide(room, 121);
		assertNotSame(target, decision.approach);
		assertNotSame(target, decision.target);
	}

	@Test
	public void positiveHpParentStillCountsEvenAtLateAge()
	{
		Room room = room();
		Crab target = crab(room, 31, 120, false, 3299, 4248);
		target.claimedUntil = 140;
		crab(room, 27, 69, true, 3290, 4253).hp = 1;
		Decision decision = strategy.decide(room, 121);
		assertNotSame(target, decision.approach);
		assertNotSame(target, decision.target);
	}

	@Test
	public void soleOldCrabRemainsExcludedAfterActualThirtyOne()
	{
		Room room = room();
		Crab old = crab(room, 29, 90, false, 3299, 4248);
		Decision decision = strategy.decide(room, 121);
		assertNotSame(old, decision.approach);
		assertNotSame(old, decision.target);
	}

	@Test
	public void singletonDoesNotBypassOwnNullKnownKillOwnFlightOrAgeCutoff()
	{
		for (int guard = 0; guard < 4; guard++)
		{
			strategy.reset();
			Room room = room();
			if (guard == 3) { room.spawns[28] = 26; room.spawns[29] = 42; room.spawns[31] = 50; }
			Crab target = crab(room, 31, guard == 3 ? 70 : 120, false, 3299, 4248);
			if (guard == 0) target.nulled = true;
			if (guard == 1) { target.claimedUntil = 140; target.pendingDamage = target.hp; }
			if (guard == 2) { target.ownHitUntil = 140; target.ownHitHp = 8; }
			Decision decision = strategy.decide(room, 121);
			assertNotSame("guard " + guard, target, decision.target);
			assertNotSame("guard " + guard, target, decision.approach);
		}
	}

	@Test
	public void knownNonlethalPartnerFlightAllowsUsefulSingletonHelp()
	{
		Room room = room();
		Crab target = crab(room, 31, 120, false, 3299, 4248);
		target.claimedUntil = 140;
		target.pendingDamage = 2;
		Decision decision = strategy.decide(room, 121);
		assertTrue(decision.reason, decision.target == target || decision.approach == target);
		assertEquals(Weapon.SULPHUR, decision.weapon);
	}

	@Test
	public void knownLethalPartnerFlightPreventsWastedSingletonShot()
	{
		Room room = room();
		Crab target = crab(room, 31, 120, false, 3299, 4248);
		target.claimedUntil = 140;
		target.pendingDamage = 8;
		Decision decision = strategy.decide(room, 121);
		assertNotSame(target, decision.target);
		assertNotSame(target, decision.approach);
	}
}
