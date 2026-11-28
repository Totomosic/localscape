package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.Tile;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;
import static org.localscape.nylocas.ranger.RangerStrategy.tile;

public class RangerSouthTwelveArrivalTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab southTwelve, westEighteen;

	public RangerSouthTwelveArrivalTest()
	{
		// Packed room 85126825783412 just before the original south 12 expires.
		room.wave = 18;
		room.spawns[17] = 120;
		room.spawns[18] = 132;
		room.player = tile(3297, 4249);
		room.ready = 135;
		room.observePartner(tile(3297, 4247), 139);
		southTwelve = add(57, 84, 12, Lane.SOUTH, true, Style.MELEE, 3292, 4243);
		add(72, 108, 15, Lane.SOUTH, true, Style.MAGIC, 3292, 4243);
		add(78, 120, 17, Lane.WEST, true, Style.MAGIC, 3290, 4245);
		add(79, 120, 17, Lane.SOUTH, true, Style.MAGIC, 3298, 4243);
		add(80, 120, 17, Lane.EAST, true, Style.MAGIC, 3300, 4251);
		westEighteen = add(85, 132, 18, Lane.WEST, true, Style.RANGE, 3284, 4248);
		add(86, 132, 18, Lane.SOUTH, true, Style.RANGE, 3295, 4236);
		add(87, 132, 18, Lane.EAST, true, Style.RANGE, 3306, 4248);
	}

	@Test public void runsToTheSouthBirthAndClawsOnArrival()
	{
		Tile staging = tile(3292, 4244);
		for (int tick = 135; tick <= 138; tick++)
		{
			Decision move = ranger.decide(room, tick);
			assertEquals(staging, move.tile);
			assertNull(move.target);
			room.player = tick == 135 ? tile(3295, 4247) : tick == 136 ? tile(3293, 4245) : staging;
		}
		room.crabs.remove(southTwelve);
		westEighteen.point = tile(3288, 4248);
		add(88, 139, 18, Lane.SPLIT, false, Style.MELEE, 3292, 4243);
		add(89, 139, 18, Lane.SPLIT, false, Style.MELEE, 3293, 4244);
		Decision hit = ranger.decide(room, 139);
		assertNotNull(hit.target);
		assertEquals(139, hit.target.born);
		assertEquals(Weapon.SULPHUR, hit.weapon);
	}

	@Test public void takesAReadySmallBeforeTheNaturalSplit()
	{
		Crab green = add(99, 134, 18, Lane.SPLIT, false, Style.RANGE, 3298, 4249);
		assertSame(green, ranger.decide(room, 135).target);
	}

	@Test public void finishesAnAlreadyWoundedWestParent()
	{
		ranger.attacked(new Decision(westEighteen, Weapon.PIPE, room.player, false, null, "West 18"), 134);
		westEighteen.hp = 8;
		assertSame(westEighteen, ranger.decide(room, 135).target);
	}

	private Crab add(int key, int born, int wave, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key + 6, born, wave, lane, lane == Lane.SOUTH ? 'W' : 'S', big, style, tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
