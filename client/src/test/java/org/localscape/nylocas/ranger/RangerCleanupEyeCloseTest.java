package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerCleanupEyeCloseTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab target;

	public RangerCleanupEyeCloseTest()
	{
		room.wave = 31;
		room.spawns[29] = 240;
		room.spawns[31] = 268;
		room.cleanup = true;
		room.player = RangerStrategy.tile(3295, 4248);
		room.ready = 300;
		target = new Crab(1, 1, 290, 31, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			RangerStrategy.tile(3301, 4248));
		target.pendingDamage = 0;
		room.crabs.add(target);
	}

	@Test
	public void cooldownReachesTheShorterEyeFlightSquareAndKeepsTheReadyCast()
	{
		Decision move = ranger.decide(room, 297);
		assertTrue(move.reason, move.reason.startsWith("Close during cleanup cooldown"));
		assertTrue(move.tile.distanceTo(target.point) <= 2);
		assertNull(move.approach);
		room.player = move.tile;
		Decision cast = ranger.decide(room, 300);
		assertSame(target, cast.target);
		assertEquals(Weapon.EYE, cast.weapon);
	}

	@Test
	public void anAlreadyReadyCastDoesNotWaitForTheShorterFlight()
	{
		assertSame(target, ranger.decide(room, 300).target);
	}

	@Test
	public void insufficientCooldownKeepsTheOriginalLaunchPlan()
	{
		room.ready = 298;
		Decision move = ranger.decide(room, 297);
		assertFalse(move.reason, move.reason.startsWith("Close during cleanup cooldown"));
		assertSame(target, move.approach);
	}

	@Test
	public void aBlockedRayKeepsTheCollisionAwareApproach()
	{
		room.obstructed.add(target.key);
		assertFalse(ranger.decide(room, 297).reason.startsWith("Close during cleanup cooldown"));
	}
}
