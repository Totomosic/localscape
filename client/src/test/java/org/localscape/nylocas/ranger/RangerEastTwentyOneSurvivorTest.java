package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

public class RangerEastTwentyOneSurvivorTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab north;
	private final Crab south;

	public RangerEastTwentyOneSurvivorTest()
	{
		room.wave = 21;
		room.spawns[21] = 184;
		room.ready = 187;
		room.player = RangerStrategy.tile(3301, 4249);
		north = add(1, 'N', 4249);
		south = add(2, 'S', 4248);
		ranger.attacked(new Decision(south, Weapon.CHIN, room.player, false, "East chin 21", "opening"), 185);
	}

	@Test
	public void oneUncoveredOriginalGetsTheShortPipeFromTheBarrier()
	{
		north.pendingDamage = 16;
		south.pendingDamage = 3;
		Decision finish = ranger.decide(room, 187);
		assertSame(south, finish.target);
		assertEquals(Weapon.PIPE, finish.weapon);
		assertEquals("Pipe the uncovered East21 survivor", finish.reason);
	}

	@Test
	public void twoUncoveredOriginalsKeepTheRechin()
	{
		north.pendingDamage = 5;
		south.pendingDamage = 3;
		assertEquals(Weapon.CHIN, ranger.decide(room, 187).weapon);
	}

	@Test
	public void oneMoreTileWestIsOutsideTheProjectedPipeRange()
	{
		room.player = RangerStrategy.tile(3300, 4249);
		north.pendingDamage = 16;
		south.pendingDamage = 3;
		assertNotEquals("Pipe the uncovered East21 survivor", ranger.decide(room, 187).reason);
	}

	private Crab add(int key, char slot, int y)
	{
		Crab crab = new Crab(key, key, 184, 21, Lane.EAST, slot, false, Style.RANGE,
			RangerStrategy.tile(3307, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
