package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerLocalMeleePreparationTest
{
	private Room room()
	{
		Room room = new Room();
		room.wave = 12;
		room.spawns[11] = 80;
		room.spawns[12] = 88;
		room.ready = 88;
		room.player = RangerStrategy.tile(3295, 4246);
		return room;
	}

	private Crab melee(Room room, int x, int y)
	{
		Crab crab = new Crab(27, 27, 52, 8, Lane.SPLIT, (char) 0, false, Style.MELEE,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void aLocalMeleeUsesTheReadyCycleInsteadOfWaitingForAHeldBackupParent()
	{
		// Native seed270302924013835: five ready ticks waiting at this square.
		Room room = room();
		Crab parent = new Crab(46, 46, 80, 11, Lane.WEST, 'S', true, Style.MAGIC,
			RangerStrategy.tile(3289, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		room.observePartner(RangerStrategy.tile(3301, 4248), 92);
		Crab melee = melee(room, 3292, 4243);
		RangerStrategy ranger = new RangerStrategy();
		ranger.decide(room, 88);
		parent.point = RangerStrategy.tile(3290, 4248);
		ranger.decide(room, 89);
		parent.point = RangerStrategy.tile(3291, 4248);
		room.observePartner(RangerStrategy.tile(3299, 4248), 92);
		Decision decision = ranger.decide(room, 90);
		assertSame(decision.reason, melee, decision.approach);
		assertEquals(Weapon.SULPHUR, decision.weapon);
		assertTrue(decision.reason, decision.reason.contains("@91"));
		assertEquals(1, Math.abs(decision.tile.getX() - melee.point.getX())
			+ Math.abs(decision.tile.getY() - melee.point.getY()));
	}

	@Test
	public void aStraightThreeTileApproachCanLaunchInTheSameCycle()
	{
		Room room = room();
		Crab melee = melee(room, 3292, 4246);
		Decision decision = new RangerStrategy().decide(room, 88);
		assertSame(melee, decision.target);
		assertEquals(Weapon.SULPHUR, decision.weapon);
	}

	@Test
	public void anAvailableRangedShotKeepsItsEarlierLaunch()
	{
		Room room = room();
		melee(room, 3292, 4243);
		Crab ranged = new Crab(51, 51, 86, 11, Lane.SPLIT, (char) 0, false, Style.RANGE,
			room.player.dx(1));
		ranged.pendingDamage = 0;
		room.crabs.add(ranged);
		assertSame(ranged, new RangerStrategy().decide(room, 88).target);
	}

	@Test
	public void localFallbackDoesNotBecomeCrossRoomMeleeChasing()
	{
		Room room = room();
		Crab melee = melee(room, 3291, 4246);
		Decision decision = new RangerStrategy().decide(room, 88);
		assertNotSame(melee, decision.target);
		assertNotSame(melee, decision.approach);
	}
}
