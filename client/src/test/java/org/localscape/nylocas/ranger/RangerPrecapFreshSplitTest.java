package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerPrecapFreshSplitTest
{
	private final RangerStrategy strategy = new RangerStrategy();

	@Test
	public void adjacentNewSplitReleasesAnOlderApproachBeforeItsFirstAttack()
	{
		// Native seed150705631945369: Eye approach123; new17 children124; ready125.
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 120;
		room.player = RangerStrategy.tile(3301, 4245);
		room.ready = 125;
		Crab old = new Crab(75, 81, 120, 16, Lane.WEST, 'N', false, Style.RANGE,
			RangerStrategy.tile(3284, 4249));
		old.pendingDamage = 0;
		room.crabs.add(old);
		Decision planned = strategy.decide(room, 123);
		assertSame(planned.reason, old, planned.approach);
		assertEquals(Weapon.EYE, planned.weapon);
		room.wave = 17;
		room.spawns[17] = 124;
		room.player = RangerStrategy.tile(3297, 4245);
		old.point = RangerStrategy.tile(3286, 4249);
		old.style = Style.MAGIC;
		Crab fresh = new Crab(79, 85, 124, 17, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			RangerStrategy.tile(3297, 4246));
		fresh.pendingDamage = 0;
		room.crabs.add(fresh);
		Decision decision = strategy.decide(room, 125);
		assertSame(decision.reason, fresh, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void newerSplitDoesNotDelayAnAlreadyReadyCommittedAttack()
	{
		Room room = new Room();
		room.wave = 12;
		room.spawns[12] = 100;
		room.ready = 102;
		room.player = RangerStrategy.tile(3290, 4248);
		Crab old = new Crab(1, 1, 100, 12, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3291, 4248));
		old.pendingDamage = 0;
		room.crabs.add(old);
		assertSame(old, strategy.decide(room, 101).approach);
		Crab distant = new Crab(2, 2, 102, 12, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3308, 4253));
		distant.pendingDamage = 0;
		room.crabs.add(distant);
		assertSame(old, strategy.decide(room, 102).target);
	}

	@Test
	public void sameBirthAlternativeDoesNotBreakAnUnhitApproach()
	{
		Room room = new Room();
		room.wave = 12;
		room.spawns[12] = 100;
		room.ready = 105;
		room.player = RangerStrategy.tile(3295, 4248);
		Crab old = new Crab(1, 1, 100, 12, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3303, 4248));
		old.pendingDamage = 0;
		room.crabs.add(old);
		assertSame(old, strategy.decide(room, 100).approach);
		Crab alternative = new Crab(2, 2, 100, 12, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3290, 4248));
		alternative.hp = 2;
		alternative.pendingDamage = 0;
		room.crabs.add(alternative);
		assertSame(old, strategy.decide(room, 101).approach);
	}

	@Test
	public void actualSurvivorFeedbackStillWinsBeforeNewerSameStyleSplits()
	{
		Room room = new Room();
		room.wave = 8;
		room.spawns[8] = 100;
		room.player = RangerStrategy.tile(3295, 4248);
		Crab survivor = new Crab(1, 1, 100, 8, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3296, 4248));
		room.crabs.add(survivor);
		strategy.attacked(new Decision(survivor, Weapon.PIPE, room.player, false, null, "Previous hit"), 101);
		survivor.hp = 3;
		survivor.pendingDamage = 0;
		room.ready = 103;
		Crab fresh = new Crab(2, 2, 103, 8, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3294, 4248));
		fresh.pendingDamage = 0;
		room.crabs.add(fresh);
		Decision decision = strategy.decide(room, 103);
		assertSame(decision.reason, survivor, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
