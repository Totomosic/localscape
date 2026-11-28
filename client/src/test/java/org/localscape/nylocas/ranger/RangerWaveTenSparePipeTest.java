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

public class RangerWaveTenSparePipeTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab split;
	private final Crab east;
	private final Crab south;

	public RangerWaveTenSparePipeTest()
	{
		room.wave = 10;
		room.spawns[10] = 68;
		room.ready = 71;
		room.player = RangerStrategy.tile(3295, 4248);
		Crab west = add(1, Lane.WEST, 'S', Style.RANGE, 3284, 4248);
		west.pendingDamage = west.hp;
		east = add(2, Lane.EAST, 'N', Style.RANGE, 3303, 4249);
		south = add(3, Lane.SOUTH, 'W', Style.RANGE, 3295, 4236);
		split = add(4, Lane.SPLIT, (char) 0, Style.RANGE, 3291, 4249);
		for (int key = 5; key < 13; key++)
			add(key, Lane.SPLIT, (char) 0, Style.MELEE, 3290, 4245);
		ranger.attacked(new Decision(west, Weapon.CHIN, room.player, false, "West chin 10", "opening"),
			room.spawns[10] + 1);
	}

	@Test
	public void crowdedBirthTickWestAllowsOnePipeAndBothOtherChins()
	{
		Decision pipe = ranger.decide(room, 71);
		assertSame(split, pipe.target);
		assertEquals(Weapon.PIPE, pipe.weapon);
		ranger.attacked(pipe, 72);
		room.player = RangerStrategy.tile(3297, 4248);
		room.ready = 73;
		Decision eastChin = ranger.decide(room, 73);
		assertEquals("East chin 10", eastChin.rule);
		assertSame(east, eastChin.target);
		ranger.attacked(eastChin, 74);
		room.player = RangerStrategy.tile(3295, 4246);
		room.ready = 76;
		south.point = RangerStrategy.tile(3295, 4241);
		assertEquals("South chin 10", ranger.decide(room, 76).rule);
	}

	@Test
	public void obstructedCenterSmallLeavesTheNormalEastChin()
	{
		room.obstructed.add(split.key);
		assertEquals("East chin 10", ranger.decide(room, 71).rule);
	}

	@Test
	public void usefulSparePipeDoesNotRequireAnAlreadyCrowdedCheck()
	{
		room.crabs.remove(room.crabs.size() - 1);
		Decision pipe = ranger.decide(room, 71);
		assertSame(split, pipe.target);
		assertEquals(Weapon.PIPE, pipe.weapon);
	}

	@Test
	public void aClearFifthTileStillLeavesTheExactChinSequence()
	{
		split.point = RangerStrategy.tile(3290, 4249);
		crowdedBirthTickWestAllowsOnePipeAndBothOtherChins();
	}

	@Test
	public void aBlockedFifthTileLeavesTheNormalEastChin()
	{
		split.point = RangerStrategy.tile(3290, 4249);
		obstructedCenterSmallLeavesTheNormalEastChin();
	}

	@Test
	public void prefersTheAlreadyLocalSparePipeToAFifthTileAlternative()
	{
		Crab edge = room.crabs.get(room.crabs.size() - 1);
		edge.style = Style.RANGE;
		edge.point = RangerStrategy.tile(3290, 4249);
		edge.hp = 1;
		assertSame(split, ranger.decide(room, 71).target);
	}

	private Crab add(int key, Lane lane, char slot, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, 68, 10, lane, slot, false, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
