package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerWestThirteenStageTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();

	public RangerWestThirteenStageTest()
	{
		room.wave = 12;
		room.spawns[12] = 84;
		room.player = RangerStrategy.tile(3295, 4249);
		room.ready = 92;
	}

	@Test
	public void freeMovementPreparesTheBirthEyeThenTheThirdTickPipe()
	{
		Decision first = ranger.decide(room, 90);
		assertTrue(first.reason, first.reason.startsWith("Stage West13"));
		room.player = first.tile;
		Decision second = ranger.decide(room, 91);
		room.player = second.tile;
		assertEquals(RangerStrategy.tile(3291, 4249), room.player);
		room.wave = 13;
		room.spawns[13] = 92;
		Crab magic = add(1, 92, 13, Lane.WEST, 'S', Style.MAGIC, 3281, 4248);
		Crab ranged = add(2, 92, 13, Lane.WEST, 'N', Style.RANGE, 3281, 4249);
		Decision eye = ranger.decide(room, 92);
		assertSame(eye.reason, magic, eye.target);
		assertEquals(Weapon.EYE, eye.weapon);
		ranger.attacked(eye, 93);
		magic.pendingDamage = magic.hp;
		room.ready = 95;
		magic.observePosition(RangerStrategy.tile(3284, 4248));
		ranged.observePosition(RangerStrategy.tile(3284, 4249));
		Decision pipe = ranger.decide(room, 95);
		assertSame(pipe.reason, ranged, pipe.target);
		assertEquals(Weapon.PIPE, pipe.weapon);
	}

	@Test
	public void aCurrentShotThatNeedsTheEastRayRetainsItsCooldownPosition()
	{
		add(1, 88, 12, Lane.SPLIT, (char) 0, Style.MAGIC, 3301, 4249);
		assertFalse(ranger.decide(room, 90).reason.startsWith("Stage West13"));
	}

	@Test
	public void aReadyUsefulCastDoesNotWaitForTheUpcomingLane()
	{
		room.ready = 90;
		Crab target = add(1, 88, 12, Lane.SPLIT, (char) 0, Style.MAGIC, 3294, 4249);
		assertSame(target, ranger.decide(room, 90).target);
	}

	private Crab add(int key, int born, int wave, Lane lane, char slot, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, false, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
