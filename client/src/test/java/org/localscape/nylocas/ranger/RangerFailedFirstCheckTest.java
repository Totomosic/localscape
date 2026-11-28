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

public class RangerFailedFirstCheckTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab south;
	private final Crab child;

	public RangerFailedFirstCheckTest()
	{
		room.wave = 19;
		room.spawns[18] = 140;
		room.spawns[19] = 148;
		room.player = RangerStrategy.tile(3291, 4246);
		room.ready = 160;
		for (int key = 70; key < 73; key++)
		{
			add(key, 124, 17, Lane.WEST, true, Style.MELEE, 3299, 4251);
		}
		add(73, 116, 15, Lane.SOUTH, true, Style.MAGIC, 3292, 4244);
		south = add(84, 140, 18, Lane.SOUTH, true, Style.RANGE, 3293, 4243);
		add(85, 140, 18, Lane.EAST, true, Style.RANGE, 3295, 4253);
		for (Lane lane : new Lane[] {Lane.WEST, Lane.SOUTH, Lane.EAST})
		{
			add(90 + lane.ordinal(), 148, 19, lane, true, Style.MELEE, 3295, 4240);
		}
		child = add(88, 143, 18, Lane.SPLIT, false, Style.RANGE, 3292, 4243);
		child.hp = 3;
		add(93, 152, 19, Lane.SPLIT, false, Style.RANGE, 3288, 4248).nulled = true;
		add(94, 152, 19, Lane.SPLIT, false, Style.RANGE, 3289, 4249).nulled = true;
	}

	@Test
	public void alreadyFailedAdmissionCanUseTheLocalSmall()
	{
		Decision attack = ranger.decide(room, 160);
		assertSame(child, attack.target);
		assertEquals(Weapon.PIPE, attack.weapon);
	}

	@Test
	public void naturalReleaseOfOneSlotKeepsTheSouthParent()
	{
		Crab dyingSmall = room.crabs.get(room.crabs.size() - 1);
		dyingSmall.hp = 0;
		dyingSmall.dying = true;
		dyingSmall.observeRemovalAt(161);
		assertSame(south, ranger.decide(room, 160).target);
	}

	private Crab add(int key, int born, int wave, Lane lane, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key + 6, born, wave, lane, 'S', big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
