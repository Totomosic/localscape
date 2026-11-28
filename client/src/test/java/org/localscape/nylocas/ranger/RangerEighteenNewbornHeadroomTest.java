package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerEighteenNewbornHeadroomTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab west;
	private final Crab child;

	public RangerEighteenNewbornHeadroomTest()
	{
		room.wave = 18;
		room.spawns[18] = 140;
		room.ready = 147;
		room.player = RangerStrategy.tile(3293, 4248);
		west = add(1, 140, 18, Lane.WEST, Style.RANGE, 3288, 4248, true);
		add(2, 140, 18, Lane.SOUTH, Style.RANGE, 3295, 4240, true);
		add(3, 140, 18, Lane.EAST, Style.RANGE, 3302, 4248, true);
		add(4, 116, 15, Lane.SOUTH, Style.MAGIC, 3292, 4244, true);
		add(5, 128, 17, Lane.WEST, Style.MAGIC, 3290, 4245, true);
		add(6, 128, 17, Lane.SOUTH, Style.MAGIC, 3298, 4243, true);
		add(7, 128, 17, Lane.EAST, Style.MAGIC, 3300, 4251, true);
		child = add(8, 147, 18, Lane.SPLIT, Style.RANGE, 3301, 4252, false);
	}

	@Test
	public void aNewChildDuringEighteenStillCountsAgainstTheNextAdmission()
	{
		Decision decision = ranger.decide(room, 147);
		assertFalse(decision.target == west || decision.approach == west);
		assertTrue(decision.target == child || decision.approach == child);
	}

	@Test
	public void knownLethalNewbornCoverageLeavesTheUsualWestPrefire()
	{
		child.pendingDamage = child.hp;
		assertSame(west, ranger.decide(room, 147).target);
	}

	@Test
	public void unknownActivePartnerClaimLeavesTheUsualWestPrefire()
	{
		child.pendingDamage = -1;
		child.claimedUntil = 150;
		assertSame(west, ranger.decide(room, 147).target);
	}

	@Test
	public void knownNonlethalCoverageStillRequiresTheNewbornSlot()
	{
		child.pendingDamage = 2;
		child.claimedUntil = 150;
		Decision decision = ranger.decide(room, 147);
		assertTrue(decision.target == child || decision.approach == child);
	}

	@Test
	public void anObservedOlderDeathClockCountsBeforeTheChildrenBecomeVisible()
	{
		room.crabs.remove(child);
		Crab dying = add(9, 108, 14, Lane.EAST, Style.RANGE, 3299, 4251, true);
		dying.hp = 0;
		dying.dying = true;
		dying.observeSplitAt(148);
		Decision decision = ranger.decide(room, 147);
		assertNotSame(west, decision.target);
		assertTrue(decision.reason.contains("@154"));
	}

	@Test
	public void knownLethalIncomingClockAlsoCountsBeforeTheParentDies()
	{
		room.crabs.remove(child);
		Crab parent = add(9, 108, 14, Lane.EAST, Style.RANGE, 3299, 4251, true);
		parent.pendingDamage = parent.hp;
		parent.observeIncoming(new int[] {0, parent.hp});
		Decision decision = ranger.decide(room, 147);
		assertNotSame(west, decision.target);
	}

	@Test
	public void anOpenedWestParentKeepsItsActualRecovery()
	{
		ranger.attacked(new Decision(west, Weapon.PIPE, room.player, false, null, "first shot"), 148);
		west.hp = 8;
		west.point = RangerStrategy.tile(3290, 4248);
		room.wave = 19;
		room.spawns[19] = 148;
		room.ready = 149;
		assertSame(west, ranger.decide(room, 149).target);
	}

	@Test
	public void delayedNineteenCannotConsumeTheRemainingSixShotParentBudget()
	{
		room.wave = 19;
		room.spawns[19] = 152;
		room.ready = 155;
		west.point = RangerStrategy.tile(3291, 4247);
		assertSame(west, ranger.decide(room, 155).target);
	}

	@Test
	public void aRoomWithoutUncoveredOrUpcomingSmallsKeepsTheWestPrefire()
	{
		room.crabs.remove(child);
		assertSame(west, ranger.decide(room, 147).target);
	}

	private Crab add(int key, int born, int wave, Lane lane, Style style, int x, int y, boolean big)
	{
		Crab c = new Crab(key, key, born, wave, lane, 'S', big, style, RangerStrategy.tile(x, y));
		c.pendingDamage = 0;
		room.crabs.add(c);
		return c;
	}
}
