package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerEastFifteenNextShotTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab old;
	private final Crab fresh;

	public RangerEastFifteenNextShotTest()
	{
		room.wave = 15;
		room.spawns[14] = 100;
		room.spawns[15] = 108;
		room.ready = 112;
		room.player = RangerStrategy.tile(3295, 4246);
		Crab parent = new Crab(1, 1, 100, 14, Lane.EAST, 'S', true, Style.RANGE, room.player.dx(5));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		old = new Crab(2, 2, 100, 14, Lane.SOUTH, 'E', false, Style.MAGIC, room.player.dy(-1));
		old.pendingDamage = 0;
		room.crabs.add(old);
		fresh = new Crab(3, 3, 108, 15, Lane.EAST, 'N', false, Style.MAGIC,
			RangerStrategy.tile(3306, 4249));
		fresh.hp = 1;
		fresh.pendingDamage = 0;
		room.obstructed.add(fresh.key);
		room.crabs.add(fresh);
	}

	@Test
	public void theOriginalEast15SmallCanCompeteFromItsNextFiringSquare()
	{
		Decision decision = ranger.decide(room, 112);
		assertSame(decision.reason, fresh, decision.approach);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void aConfirmedOlderSurvivorKeepsItsImmediateFollowup()
	{
		ranger.attacked(new Decision(old, Weapon.EYE, room.player, false, null, "Previous hit"), 109);
		old.hp = 3;
		old.pendingDamage = 0;
		assertSame(old, ranger.decide(room, 112).target);
	}
}
