package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerCooldownCloseTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;

	private Room room(int wave, int spawn)
	{
		Room room = new Room();
		room.wave = wave;
		room.spawns[wave] = spawn;
		room.player = RangerStrategy.tile(3295, 4248);
		return room;
	}

	private Crab crab(Room room, int born, Lane lane, char slot, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, room.wave, lane, slot, false, style, RangerStrategy.tile(x, y));
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void pipeAlreadyAtShortestNativeDelayKeepsItsPositionAndLaunchTick()
	{
		Room room = room(12, 100);
		room.ready = 107;
		Crab target = crab(room, 100, Lane.SPLIT, (char) 0, Style.RANGE, 3300, 4248);
		Decision close = strategy.decide(room, 105);
		assertEquals(Weapon.PIPE, close.weapon);
		// Native thrown projectiles take two server ticks at every rapid-pipe distance1–5.
		assertEquals(room.player, close.tile);
		room.player = close.tile;
		Decision shot = strategy.decide(room, 107);
		assertSame(shot.reason, target, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
	}

	@Test
	public void visibleEyeTargetGetsCloserWhenItsCooldownAndReturnBudgetFit()
	{
		Room room = room(12, 100);
		room.ready = 106;
		Crab target = crab(room, 100, Lane.SPLIT, (char) 0, Style.MAGIC, 3301, 4248);
		Decision close = strategy.decide(room, 103);
		assertEquals(Weapon.EYE, close.weapon);
		assertTrue(close.reason, close.tile.distanceTo(target.point) < room.player.distanceTo(target.point));
		assertTrue((room.player.distanceTo(close.tile) + 1) / 2 <= room.ready - 103);
	}

	@Test
	public void readyShotIsNeverDelayedToMoveCloser()
	{
		Room room = room(12, 100);
		room.ready = 105;
		Crab target = crab(room, 100, Lane.SPLIT, (char) 0, Style.MAGIC, 3300, 4248);
		Decision shot = strategy.decide(room, 105);
		assertSame(target, shot.target);
		assertEquals(room.player, shot.tile);
	}

	@Test
	public void namedLanePacketRetainsItsApproach()
	{
		Room room = room(13, 100);
		room.ready = 105;
		crab(room, 100, Lane.WEST, 'S', Style.MAGIC, 3290, 4248);
		Decision shot = strategy.decide(room, 103);
		assertFalse(shot.reason.startsWith("Close during cooldown"));
		assertEquals(room.player, shot.tile);
	}

	@Test
	public void nextSoftLaneStageStillHasItsReturnBudget()
	{
		Room room = room(12, 100);
		room.ready = 107;
		crab(room, 100, Lane.SPLIT, (char) 0, Style.MAGIC, 3301, 4253);
		Decision shot = strategy.decide(room, 105);
		assertFalse(shot.reason.startsWith("Close during cooldown"));
		assertEquals(room.player, shot.tile);
	}

	@Test
	public void blockedRayKeepsCollisionAwareApproach()
	{
		Room room = room(12, 100);
		room.ready = 106;
		Crab target = crab(room, 100, Lane.SPLIT, (char) 0, Style.MAGIC, 3301, 4248);
		room.obstructed.add(target.key);
		Decision shot = strategy.decide(room, 103);
		assertFalse(shot.reason.startsWith("Close during cooldown"));
	}
}
