package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerEastTenRangeClearanceTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab parent;
	private final Crab small;

	public RangerEastTenRangeClearanceTest()
	{
		room.wave = 11;
		room.spawns[10] = 68;
		room.spawns[11] = 76;
		room.player = RangerStrategy.tile(3297, 4248);
		parent = new Crab(43, 43, 68, 10, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3300, 4248));
		parent.hp = 12;
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		small = new Crab(40, 40, 68, 10, Lane.SOUTH, 'W', false, Style.RANGE,
			RangerStrategy.tile(3295, 4243));
		small.pendingDamage = 0;
		room.crabs.add(small);
		ranger.attacked(new Decision(small, Weapon.CHIN, room.player, false, "South chin 10", "Previous packet"), 75);
	}

	@Test
	public void aUsefulRangeSmallIsClearedBeforeTheChinWoundedEast10Parent()
	{
		assertSame(small, ranger.decide(room, 78).target);
	}

	@Test
	public void actualParentSurvivorFeedbackRetainsItsFollowup()
	{
		ranger.attacked(new Decision(parent, Weapon.PIPE, room.player, false, null, "Previous hit"), 76);
		parent.hp = 3;
		parent.pendingDamage = 0;
		assertSame(parent, ranger.decide(room, 78).target);
	}

	@Test
	public void lethalCoverageOnTheRangeSmallsReleasesTheParent()
	{
		small.pendingDamage = small.hp;
		assertSame(parent, ranger.decide(room, 78).target);
	}

	@Test
	public void boundedParentServiceWinsDespiteAnUncoveredSmall()
	{
		room.wave = 12;
		room.spawns[12] = 84;
		assertSame(parent, ranger.decide(room, 90).target);
	}

	@Test
	public void anUnreachableSmallDoesNotCauseAnIdleParentHold()
	{
		room.obstructed.add(small.key);
		assertSame(parent, ranger.decide(room, 78).target);
	}
}
