package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerPre31NullRescueCombinedTest
{
	@Test
	public void actualCombinedDimWitnessFinishesWestThirtyBeforeRescuingAnUndimmedChild()
	{
		// Combined timestamp seed191996532081150 tick266, native delay0, HP99.
		// Cutoff=max(actual29=244, actual28=228+16)=244; key190 born262 is fresh.
		RangerStrategy strategy = new RangerStrategy();
		Room room = new Room();
		room.wave = 30;
		room.spawns[28] = 228;
		room.spawns[29] = 244;
		room.spawns[30] = 256;
		room.player = RangerStrategy.tile(3293, 4248);
		room.ready = 266;
		Crab own = new Crab(185, 185, 256, 30, Lane.WEST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3290, 4247));
		own.hp = 1;
		own.pendingDamage = 0;
		room.crabs.add(own);
		strategy.attacked(new Decision(own, Weapon.PIPE, room.player, false, "West big 30", "West big 30"), 259);
		Crab rescue = new Crab(190, 190, 262, 30, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			RangerStrategy.tile(3301, 4247));
		rescue.partnerNulled = true;
		rescue.pendingDamage = 0;
		room.crabs.add(rescue);
		Decision decision = strategy.decide(room, 266);
		assertSame(decision.reason, own, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
		own.pendingDamage = own.hp;
		decision = strategy.decide(room, 266);
		assertSame(decision.reason, rescue, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void splitBornAfterWaveTwentyNineGetsEarlyRescuePriority()
	{
		RangerStrategy strategy = new RangerStrategy();
		Room room = new Room();
		room.wave = 30;
		room.spawns[28] = 236;
		room.spawns[29] = 240;
		room.spawns[30] = 248;
		room.player = RangerStrategy.tile(3293, 4248);
		room.ready = 256;
		Crab split = new Crab(176, 176, 243, 29, Lane.SPLIT, (char) 0, false,
			Style.MAGIC, RangerStrategy.tile(3299, 4244));
		split.partnerNulled = true;
		split.pendingDamage = 0;
		room.crabs.add(split);
		Crab ordinary = new Crab(184, 184, 251, 30, Lane.SPLIT, (char) 0, false,
			Style.RANGE, RangerStrategy.tile(3292, 4243));
		ordinary.pendingDamage = 0;
		room.crabs.add(ordinary);
		assertSame(split, strategy.decide(room, 256).target);
	}
}
