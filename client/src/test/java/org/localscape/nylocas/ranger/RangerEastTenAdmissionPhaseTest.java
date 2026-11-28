package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerEastTenAdmissionPhaseTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab parent;
	private final Crab magic;

	public RangerEastTenAdmissionPhaseTest()
	{
		room.wave = 11;
		room.spawns[10] = 68;
		room.spawns[11] = 76;
		room.player = RangerStrategy.tile(3297, 4248);
		parent = new Crab(43, 43, 68, 10, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3300, 4248));
		parent.hp = 7;
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		magic = new Crab(44, 44, 76, 11, Lane.SPLIT, (char) 0, false, Style.MAGIC, room.player.dx(-1).dy(1));
		magic.pendingDamage = 0;
		room.crabs.add(magic);
		for (int i = 0; i < 5; i++)
		{
			Crab older = new Crab(10 + i, 10 + i, 52, 8, Lane.SPLIT, (char) 0, false, Style.MELEE,
				RangerStrategy.tile(3290, 4252));
			older.nulled = true;
			older.pendingDamage = 0;
			room.crabs.add(older);
		}
	}

	@Test
	public void aRealSmallShotCanMoveTheParentSplitPastTheFollowingAdmission()
	{
		Decision decision = ranger.decide(room, 81);
		assertSame(decision.reason, magic, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
		// Eye81, Pipe84, impact87, children93: after the earliest wave13 check92.
		assertTrue(81 + decision.weapon.cooldown + 3 + 6 > 92);
	}

	@Test
	public void anEarlierParentSlotThatCannotCrossTheCheckKeepsItsDeadline()
	{
		assertSame(parent, ranger.decide(room, 79).target);
	}

	@Test
	public void aConfirmedParentSurvivorDoesNotYieldItsFollowup()
	{
		ranger.attacked(new Decision(parent, Weapon.PIPE, room.player, false, null, "Previous hit"), 79);
		parent.hp = 3;
		parent.pendingDamage = 0;
		assertSame(parent, ranger.decide(room, 81).target);
	}

	@Test
	public void aBlockedSmallDoesNotCreateAnEmptyParentWait()
	{
		room.obstructed.add(magic.key);
		assertSame(parent, ranger.decide(room, 81).target);
	}
}
