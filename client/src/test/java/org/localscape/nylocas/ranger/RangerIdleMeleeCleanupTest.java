package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerIdleMeleeCleanupTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;
	private Room room()
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[28] = 236;
		room.spawns[29] = 252;
		room.spawns[31] = 260;
		room.player = RangerStrategy.tile(3295, 4248);
		room.ready = 298;
		room.cleanup = true;
		return room;
	}

	private Crab crab(Room room, int wave, int born, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, Lane.SPLIT, (char) 0, big, style,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void idleRangerHelpsMeleeWhileMageHandlesClaimedMagic()
	{
		// Native seed37176169104860 tick298: three live bodies, no ranger candidate.
		Room room = room();
		Crab melee = crab(room, 31, 260, false, Style.MELEE, 3290, 4252);
		Crab unavailable = crab(room, 30, 256, false, Style.MAGIC, 3299, 4243);
		unavailable.nulled = true;
		Crab magePrimary = crab(room, 31, 269, false, Style.MAGIC, 3290, 4252);
		magePrimary.pendingDamage = -1;
		magePrimary.claimedUntil = 299;
		Decision approach = strategy.decide(room, 298);
		assertTrue(approach.reason, approach.approach == melee || approach.target == melee && approach.pathToTarget);
		assertEquals(Weapon.SULPHUR, approach.weapon);
		assertFalse(approach.special);
		room.player = RangerStrategy.tile(3291, 4251);
		magePrimary.claimedUntil = 300;
		Decision attack = strategy.decide(room, 299);
		assertSame(attack.reason, melee, attack.target);
		assertEquals(Weapon.SULPHUR, attack.weapon);
		assertFalse(attack.special);
	}

	@Test
	public void ignoredOldBodiesRemainLivingWhileFreshMeleeReceivesHelp()
	{
		Room room = room();
		Crab melee = crab(room, 31, 280, false, Style.MELEE, 3290, 4252);
		Crab old = crab(room, 29, 252, false, Style.MAGIC, 3296, 4248);
		Decision decision = strategy.decide(room, 298);
		assertTrue(decision.reason, decision.approach == melee || decision.target == melee && decision.pathToTarget);
		assertEquals(2, room.crabs.size());
		assertEquals(8, old.hp);
		assertFalse(old.dying);
	}

	@Test
	public void normalOwnAttackOrApproachWinsBeforeMeleeHelp()
	{
		for (Style style : new Style[]{Style.RANGE, Style.MAGIC})
		{
			strategy.reset();
			Room room = room();
			crab(room, 31, 280, false, Style.MELEE, 3290, 4252);
			Crab own = crab(room, 31, 290, false, style, 3301, 4248);
			Decision decision = strategy.decide(room, 298);
			assertTrue(decision.reason, decision.target == own || decision.approach == own);
			assertNotEquals(Weapon.SULPHUR, decision.weapon);
		}
	}

	@Test
	public void claimNullKnownKillAndExpiryStillBlockTheHelper()
	{
		for (int guard = 0; guard < 4; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab melee = crab(room, 31, guard == 3 ? 249 : 280, false, Style.MELEE, 3290, 4252);
			crab(room, 29, 252, false, Style.MAGIC, 3296, 4248);
			if (guard == 0) melee.nulled = true;
			else if (guard == 1) { melee.pendingDamage = -1; melee.claimedUntil = 300; }
			else if (guard == 2) melee.pendingDamage = melee.hp;
			Decision decision = strategy.decide(room, 298);
			assertNotSame("guard " + guard, melee, decision.target);
			assertNotSame("guard " + guard, melee, decision.approach);
		}
	}

	@Test
	public void freshMeleeParentWithNonlethalQueuedDamageReceivesNormalClaws()
	{
		Room room = room();
		room.specialEnergy = 1000;
		Crab parent = crab(room, 30, 270, true, Style.MELEE, 3290, 4252);
		parent.pendingDamage = 3;
		parent.claimedUntil = 302;
		Decision decision = strategy.decide(room, 298);
		assertTrue(decision.reason, decision.target == parent || decision.approach == parent);
		assertEquals(Weapon.SULPHUR, decision.weapon);
		assertFalse(decision.special);
	}

	@Test
	public void helperWaitsForActualThirtyOneAndNeverTargetsOldBirths()
	{
		for (int guard = 0; guard < 2; guard++)
		{
			strategy.reset();
			Room room = room();
			if (guard == 0) room.wave = 30;
			Crab melee = crab(room, guard == 1 ? 29 : 30, guard == 1 ? 252 : 280, false, Style.MELEE, 3290, 4252);
			Decision decision = strategy.decide(room, 298);
			assertNotSame(melee, decision.target);
			assertNotSame(melee, decision.approach);
		}
	}

	@Test
	public void lateWaveTwentyNineMeleeChildCanReceiveIdleHelp()
	{
		Room room = room();
		Crab melee = crab(room, 29, 255, false, Style.MELEE, 3290, 4252);
		crab(room, 29, 252, false, Style.MAGIC, 3296, 4248);
		Decision decision = strategy.decide(room, 298);
		assertTrue(decision.reason, decision.approach == melee || decision.target == melee);
		assertEquals(Weapon.SULPHUR, decision.weapon);
	}

	@Test
	public void pendingNamedPacketKeepsItsStage()
	{
		Room room = room();
		room.spawns[31] = 298;
		Crab melee = crab(room, 30, 280, false, Style.MELEE, 3290, 4252);
		room.crabs.add(new Crab(++key, key, 298, 31, Lane.WEST, 'S', false,
			Style.RANGE, RangerStrategy.tile(3281, 4248)));
		Decision decision = strategy.decide(room, 298);
		assertNotSame(melee, decision.target);
		assertNotSame(melee, decision.approach);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
