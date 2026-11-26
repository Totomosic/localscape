package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class RangerWestEighteenHeadroomTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab west;

	public RangerWestEighteenHeadroomTest()
	{
		room.wave = 18;
		room.spawns[18] = 140;
		room.player = RangerStrategy.tile(3293, 4248);
		west = add(1, 140, 18, Lane.WEST, true, Style.RANGE, 3281, 4248);
		add(2, 140, 18, Lane.SOUTH, true, Style.RANGE, 3292, 4243);
		add(3, 140, 18, Lane.EAST, true, Style.RANGE, 3292, 4253);
		add(4, 116, 15, Lane.SOUTH, true, Style.MAGIC, 3292, 4243);
		add(5, 128, 17, Lane.WEST, true, Style.MELEE, 3290, 4248);
		add(6, 128, 17, Lane.SOUTH, true, Style.MELEE, 3298, 4243);
		add(7, 128, 17, Lane.EAST, true, Style.MELEE, 3300, 4251);
	}

	@Test
	public void oneUncoveredOlderSmallUsesTheSlotThatAnImmediateSplitWouldNeed()
	{
		Crab older = add(8, 136, 17, Lane.SPLIT, false, Style.MELEE, 3299, 4253);
		assertNotSame(west, ranger.decide(room, 140).target);
		older.pendingDamage = older.hp;
		assertSame(west, ranger.decide(room, 140).target);
		assertEquals(Weapon.BOW, ranger.decide(room, 140).weapon);
	}

	@Test
	public void aDueSouth12SplitCanUseTheSpareSlot()
	{
		add(8, 104, 12, Lane.SOUTH, true, Style.MELEE, 3292, 4243);
		assertNotSame(west, ranger.decide(room, 140).target);
	}

	@Test
	public void lateReleaseStillOpensWestInsideItsRangeWindow()
	{
		add(8, 136, 17, Lane.SPLIT, false, Style.MELEE, 3299, 4253);
		room.wave = 19;
		room.spawns[19] = 148;
		west.point = RangerStrategy.tile(3290, 4248);
		assertNotSame(west, ranger.decide(room, 153).target);
		assertSame(west, ranger.decide(room, 154).target);
		assertEquals(Weapon.PIPE, ranger.decide(room, 154).weapon);
	}

	private Crab add(int key, int born, int wave, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, 'S', big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
