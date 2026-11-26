package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class RangerSouthPreparationTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private Crab south25;
	private int key;

	public RangerSouthPreparationTest()
	{
		room.wave = 25;
		room.spawns[25] = 200;
		room.player = RangerStrategy.tile(3295, 4246);
		south25 = add(25, 200, Lane.SOUTH, true, 3295, 4242);
	}

	@Test
	public void twoUncoveredSmallsOpenSouth25EvenWithAThirdCoveredSmall()
	{
		add(25, 204, Lane.SPLIT, false, 3293, 4247);
		add(25, 204, Lane.SPLIT, false, 3297, 4247);
		Crab covered = add(25, 204, Lane.SPLIT, false, 3298, 4247);
		covered.pendingDamage = covered.hp;
		assertSame(south25, ranger.decide(room, 207).target);
	}

	@Test
	public void aDryRangeRotationDoesNotOpenSouth25Before27()
	{
		Crab small = add(25, 204, Lane.SPLIT, false, 3295, 4247);
		assertSame(small, ranger.decide(room, 207).target);
	}

	@Test
	public void fourUncoveredSmallsHoldSouth25UntilActual27()
	{
		for (int i = 0; i < 4; i++) add(25, 206, Lane.SPLIT, false, 3293 + i, 4248);
		assertNotSame(south25, ranger.decide(room, 207).target);
		room.wave = 27;
		room.spawns[27] = 212;
		assertSame(south25, ranger.decide(room, 212).target);
	}

	@Test
	public void south25SurvivorKeepsThePipeWhenTheSmallCountChanges()
	{
		Crab first = add(25, 204, Lane.SPLIT, false, 3293, 4247);
		Crab second = add(25, 204, Lane.SPLIT, false, 3297, 4247);
		Decision opening = ranger.decide(room, 206);
		ranger.attacked(opening, 207);
		first.pendingDamage = first.hp;
		second.pendingDamage = second.hp;
		south25.hp = 3;
		assertSame(south25, ranger.decide(room, 208).target);
	}

	@Test
	public void south27FirstGreenWinsOverTheDelayedSouth25()
	{
		room.wave = 27;
		room.spawns[27] = 212;
		Crab south27 = add(27, 212, Lane.SOUTH, true, 3295, 4240);
		Decision opening = ranger.decide(room, 219);
		assertSame(south27, opening.target);
		assertEquals("South big flicker 27", opening.rule);
	}

	@Test
	public void south27CanFinishItsRangeSurvivorAfterTheOpeningWindow()
	{
		room.wave = 28;
		room.spawns[27] = 212;
		room.spawns[28] = 220;
		south25.hp = 0;
		Crab south27 = add(27, 212, Lane.SOUTH, true, 3295, 4242);
		ranger.attacked(new Decision(south27, Weapon.PIPE, room.player, false, "South big flicker 27", "first"), 220);
		south27.hp = 4;
		add(28, 220, Lane.SPLIT, false, 3297, 4247);
		Decision followup = ranger.decide(room, 228);
		assertSame(south27, followup.target);
		assertEquals(Weapon.PIPE, followup.weapon);
	}

	private Crab add(int wave, int born, Lane lane, boolean big, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, 'W', big, Style.RANGE,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
