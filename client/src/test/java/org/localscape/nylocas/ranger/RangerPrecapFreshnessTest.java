package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerPrecapFreshnessTest
{
	@Test
	public void anObservedSixTickDeathLeavesTimeForOneUsefulPipeCycleBeforeTheSplit()
	{
		RangerStrategy ranger = new RangerStrategy();
		Room room = new Room();
		room.wave = 17;
		room.spawns[17] = 132;
		room.player = RangerStrategy.tile(3295, 4245);
		Crab body = new Crab(64, 64, 100, 13, Lane.EAST, 'S', true, Style.MELEE,
			room.player.dx(1));
		body.hp = 0;
		body.dying = true;
		room.crabs.add(body);
		ranger.decide(room, 132);

		Crab old = new Crab(61, 61, 102, 13, Lane.SPLIT, (char) 0, false, Style.RANGE, room.player);
		old.pendingDamage = 0;
		room.crabs.add(old);
		body.observeSplitAt(138);
		assertSame(old, ranger.decide(room, 136).target);
	}

	@Test
	public void aPredictedSplitCanSpendTwoReadyTicks()
	{
		RangerStrategy ranger = new RangerStrategy();
		Room room = new Room();
		room.wave = 7;
		room.ready = 40;
		room.spawns[7] = 40;
		room.player = RangerStrategy.tile(3295, 4248);
		Crab parent = new Crab(2, 2, 16, 4, Lane.SOUTH, 'W', true, Style.MAGIC,
			RangerStrategy.tile(3292, 4244));
		parent.hp = 0;
		parent.dying = true;
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		ranger.decide(room, 40);

		Crab old = new Crab(1, 1, 12, 3, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			RangerStrategy.tile(3295, 4247));
		old.pendingDamage = 0;
		room.crabs.add(old);
		assertNull(ranger.decide(room, 45).target);
		assertNull(ranger.decide(room, 46).target);
		assertSame(old, ranger.decide(room, 47).target);
	}

	@Test
	public void aNearbySplitCanBeClawedWhileTheOriginalMeleeIsLeftAlone()
	{
		Room room = new Room();
		room.wave = 6;
		room.spawns[6] = 36;
		room.ready = 40;
		room.player = RangerStrategy.tile(3295, 4244);
		Crab old = new Crab(1, 1, 4, 1, Lane.EAST, 'N', false, Style.MELEE, room.player);
		old.hp = 1;
		old.pendingDamage = 0;
		room.crabs.add(old);

		Decision idle = new RangerStrategy().decide(room, 40);
		assertNotSame(old, idle.target);
		assertNotSame(old, idle.approach);

		Crab split = new Crab(2, 2, 34, 5, Lane.SPLIT, (char) 0, false, Style.MELEE, room.player);
		split.pendingDamage = 0;
		room.crabs.add(split);
		Decision attack = new RangerStrategy().decide(room, 40);
		assertSame(split, attack.target);
		assertEquals(Weapon.SULPHUR, attack.weapon);
	}
}
