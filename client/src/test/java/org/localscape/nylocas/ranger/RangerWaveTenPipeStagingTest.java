package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerWaveTenPipeStagingTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab green;
	private final Crab east;
	private final Crab south;

	public RangerWaveTenPipeStagingTest()
	{
		room.wave = 10;
		room.spawns[10] = 68;
		room.ready = 71;
		room.player = RangerStrategy.tile(3295, 4248);
		Crab west = add(1, 68, Lane.WEST, 'S', 3284, 4248);
		west.pendingDamage = west.hp;
		east = add(2, 68, Lane.EAST, 'N', 3307, 4249);
		south = add(3, 68, Lane.SOUTH, 'W', 3295, 4236);
		green = add(4, 53, Lane.SPLIT, '\0', 3299, 4254);
		ranger.attacked(new Decision(west, Weapon.CHIN, room.player, false, "West chin 10", "opening"), 69);
	}

	@Test
	public void preparesTheSpareDuringCooldownThenKeepsBothChinPackets()
	{
		room.player = RangerStrategy.tile(3291, 4248);
		east.point = RangerStrategy.tile(3309, 4249);
		Decision first = ranger.decide(room, 69);
		assertNull(first.target);
		assertNull(first.approach);
		assertEquals(Weapon.PIPE, first.weapon);
		assertTrue(first.reason.contains("Prepare spare Pipe"));
		room.player = RangerStrategy.tile(3293, 4250);
		east.observePosition(RangerStrategy.tile(3308, 4249));
		Decision second = ranger.decide(room, 70);
		assertNull(second.target);
		assertEquals(Weapon.PIPE, second.weapon);
		room.player = second.tile;
		east.observePosition(RangerStrategy.tile(3307, 4249));
		Decision pipe = ranger.decide(room, 71);
		assertSame(green, pipe.target);
		assertEquals(Weapon.PIPE, pipe.weapon);
		ranger.attacked(pipe, 72);
		green.pendingDamage = green.hp;
		room.ready = 73;
		room.player = RangerStrategy.tile(3297, 4249);
		east.observePosition(RangerStrategy.tile(3305, 4249));
		Decision eastChin = ranger.decide(room, 73);
		assertSame(east, eastChin.target);
		assertEquals("East chin 10", eastChin.rule);
		ranger.attacked(eastChin, 74);
		room.ready = 76;
		room.player = RangerStrategy.tile(3295, 4247);
		south.observePosition(RangerStrategy.tile(3295, 4241));
		Decision southChin = ranger.decide(room, 76);
		assertSame(south, southChin.target);
		assertEquals("South chin 10", southChin.rule);
	}

	@Test
	public void aOneStepPipeIsStillARealReadyAttack()
	{
		Decision decision = ranger.decide(room, 71);
		assertSame(green, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertTrue(room.player.distanceTo(decision.tile) <= 2);
	}

	@Test
	public void coverageBeforeTheReadyTickReleasesTheUsualChin()
	{
		green.pendingDamage = green.hp;
		assertEquals("East chin 10", ranger.decide(room, 71).rule);
	}

	@Test
	public void blockedRayDoesNotSpendTheSlot()
	{
		room.obstructed.add(green.key);
		assertEquals("East chin 10", ranger.decide(room, 71).rule);
	}

	@Test
	public void aStationaryEightTileTargetCannotBorrowASecondMovementCycle()
	{
		green.point = RangerStrategy.tile(3287, 4248);
		assertEquals("East chin 10", ranger.decide(room, 71).rule);
	}

	private Crab add(int key, int born, Lane lane, char slot, int x, int y)
	{
		Crab crab = new Crab(key, key, born, born < 68 ? 8 : 10, lane, slot, false, Style.RANGE, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
