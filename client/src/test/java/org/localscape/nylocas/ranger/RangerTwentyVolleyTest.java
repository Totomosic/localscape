package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RangerTwentyVolleyTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab primary;
	private final Crab neighbour;

	public RangerTwentyVolleyTest()
	{
		room.wave = 20;
		room.spawns[20] = 160;
		room.ready = 172;
		room.player = RangerStrategy.tile(3299, 4248);
		Crab parent = new Crab(1, 1, 160, 20, Lane.WEST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3290, 4248));
		ranger.attacked(new Decision(parent, Weapon.BOW, room.player, false, "West bow flicker 20", "opening"), 166);
		primary = add(2, 4249);
		neighbour = add(3, 4248);
	}

	@Test
	public void coveredNeighbourDoesNotLengthenTheWave20Cooldown()
	{
		neighbour.pendingDamage = 16;
		Decision shot = ranger.decide(room, 172);
		assertSame(primary, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
	}

	@Test
	public void actualUncoveredPairKeepsTheLongerChin()
	{
		assertEquals(Weapon.CHIN, ranger.decide(room, 172).weapon);
	}

	private Crab add(int key, int y)
	{
		Crab crab = new Crab(key, key, 166, 20, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3300, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
