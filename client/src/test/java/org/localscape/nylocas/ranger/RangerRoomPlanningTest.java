package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerRoomPlanningTest
{
	private static Crab add(Room room, int key, int born, int wave, Lane lane, char slot, boolean big,
		Style style, int x, int y, int hp)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		crab.hp = hp;
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void forecastChangesTheSameReadyPipeToCapClearanceWhenTheRoomFills()
	{
		Room room = new Room();
		room.wave = 18;
		room.spawns[18] = 100;
		room.player = RangerStrategy.tile(3290, 4248);
		Crab west = add(room, 1, 100, 18, Lane.WEST, 'S', true, Style.RANGE, 3283, 4248, 3);
		Crab small = add(room, 2, 101, 18, Lane.SPLIT, (char) 0, false, Style.RANGE, 3291, 4248, 8);
		Decision open = new RangerStrategy().decide(room, 102);
		assertSame(west, open.target);
		assertEquals(Weapon.PIPE, open.weapon);

		// Both possible primaries retain exactly the same HP, position, color and timing.
		// At twelve occupied slots only a small kill makes room before the next check.
		for (int key = 3; key < 13; key++)
		{
			Crab body = add(room, key, 96, 17, Lane.SPLIT, (char) 0, false, Style.MELEE, 3295, 4251, 8);
			body.nulled = body.partnerNulled = true;
		}
		Decision clear = new RangerStrategy().decide(room, 102);
		assertSame(small, clear.target);
		assertEquals(Weapon.PIPE, clear.weapon);
	}

	@Test
	public void westElevenGivesANearbyReadyMageTheOpeningAndReleasesForTheScythe()
	{
		// Pilot seed 11275826 collided at actual12+4 on a full, uncovered west parent.
		Room room = new Room();
		room.wave = 12;
		room.spawns[11] = 80;
		room.spawns[12] = 88;
		room.player = RangerStrategy.tile(3296, 4247);
		room.observePartner(RangerStrategy.tile(3299, 4248), 92);
		Crab west = add(room, 44, 80, 11, Lane.WEST, 'S', true, Style.MAGIC, 3293, 4247, 16);
		Crab small = add(room, 45, 89, 12, Lane.SPLIT, (char) 0, false, Style.RANGE, 3295, 4248, 8);
		assertSame(small, new RangerStrategy().decide(room, 92).target);
		assertSame(west, new RangerStrategy().decide(room, 93).target);

		room.observePartner(RangerStrategy.tile(3299, 4248), 96);
		assertSame(west, new RangerStrategy().decide(room, 92).target);
		room.observePartner(RangerStrategy.tile(3299, 4248), 92);
		west.pendingDamage = 3;
		assertSame(west, new RangerStrategy().decide(room, 92).target);
		west.pendingDamage = 0;
		west.partnerNulled = true;
		assertSame(west, new RangerStrategy().decide(room, 92).target);
	}

	@Test
	public void localEarlyMagicSmallsGetATwoTickMageOpportunity()
	{
		for (int wave : new int[]{5, 6})
		{
			Room room = new Room();
			room.wave = wave;
			int tick = wave == 5 ? 30 : 39;
			room.spawns[wave] = wave == 5 ? 20 : 36;
			int x = wave == 5 ? 3300 : 3289;
			room.player = RangerStrategy.tile(3294, 4248);
			room.observePartner(RangerStrategy.tile(x, 4248), tick);
			Crab small = add(room, 1, room.spawns[wave], wave, wave == 5 ? Lane.EAST : Lane.WEST, 'N',
				false, Style.MAGIC, x, 4249, 8);
			assertNull(new RangerStrategy().decide(room, tick).target);
			assertSame(small, new RangerStrategy().decide(room, tick + 3).target);
			room.observePartner(RangerStrategy.tile(x, 4248), tick + 4);
			assertSame(small, new RangerStrategy().decide(room, tick).target);
			room.observePartner(RangerStrategy.tile(x, 4248), tick);
			small.partnerNulled = true;
			assertSame(small, new RangerStrategy().decide(room, tick).target);
		}
	}
}
