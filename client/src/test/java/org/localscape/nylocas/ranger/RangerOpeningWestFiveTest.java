package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class RangerOpeningWestFiveTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();

	public RangerOpeningWestFiveTest()
	{
		room.wave = 4;
		room.spawns[4] = 16;
		room.player = RangerStrategy.tile(3296, 4243);
		room.ready = 21;
		Crab scratch = new Crab(5, 5, 8, 2, Lane.SOUTH, 'W', false, Style.MELEE, room.player);
		ranger.attacked(new Decision(scratch, Weapon.CLAWS, room.player, false, "Opening scratch", "scratch"), 18);
	}

	@Test
	public void scratchCooldownMovesDirectlyToTheWestFiringTile()
	{
		Crab small = new Crab(10, 10, 16, 4, Lane.WEST, 'N', false, Style.RANGE,
			RangerStrategy.tile(3283, 4249));
		room.crabs.add(small);
		Decision stage = ranger.decide(room, 18);
		assertNull(stage.approach);
		assertEquals(Weapon.PIPE, stage.weapon);
		assertEquals(RangerStrategy.tile(3290, 4248), stage.tile);
	}

	@Test
	public void blockedParentGetsDirectMovementAndReceivesTheFirstClearPipe()
	{
		room.wave = 5;
		room.spawns[5] = 20;
		room.player = RangerStrategy.tile(3290, 4245);
		Crab parent = new Crab(13, 13, 20, 5, Lane.WEST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3283, 4248));
		room.crabs.add(parent);
		room.obstructed.add(parent.key);
		Decision blocked = ranger.decide(room, 22);
		assertNull(blocked.target);
		assertNull(blocked.approach);
		assertEquals(RangerStrategy.tile(3290, 4248), blocked.tile);
		room.player = blocked.tile;
		room.obstructed.clear();
		parent.point = RangerStrategy.tile(3284, 4248);
		Decision attack = ranger.decide(room, 23);
		assertSame(parent, attack.target);
		assertEquals("West big 5", attack.rule);
	}
}
