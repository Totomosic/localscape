package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerOptionalWestTwentySevenTest
{
	@Test
	public void rangerLeavesOptionalWestTwentySevenAloneIncludingLastLivingCleanup()
	{
		for (int wave : new int[] {27, 29, 30, 31}) for (Style style : Style.values())
		{
			Room room = new Room();
			room.wave = wave;
			room.spawns[wave] = 100;
			room.player = RangerStrategy.tile(3293, 4248);
			Crab optional = new Crab(1, 1, 90, 27, Lane.WEST, (char) 0, true, style, room.player);
			optional.pendingDamage = 0;
			optional.partnerNulled = true;
			room.crabs.add(optional);
			Decision decision = new RangerStrategy().decide(room, 100);
			assertNotSame(wave + ":" + style + ":" + decision.reason, optional, decision.target);
			assertNotSame(wave + ":" + style + ":" + decision.reason, optional, decision.approach);
		}
	}

	@Test
	public void aChinClusterUsesPipeOnlyWhenItCouldDamageTheOptionalParent()
	{
		for (Style style : Style.values())
		{
			Room room = new Room();
			room.wave = 29;
			room.spawns[29] = 100;
			room.player = RangerStrategy.tile(3293, 4248);
			Crab optional = new Crab(1, 1, 90, 27, Lane.WEST, (char) 0, true, style, room.player);
			optional.pendingDamage = 0;
			room.crabs.add(optional);
			for (int i = 2; i <= 3; i++)
			{
				Crab small = new Crab(i, i, 100, 29, Lane.SPLIT, (char) 0, false, Style.RANGE, room.player.dx(i - 2));
				small.pendingDamage = 0;
				room.crabs.add(small);
			}
			Decision decision = new RangerStrategy().decide(room, 100);
			assertNotNull(decision.target);
			assertFalse(decision.target.big);
			assertEquals(style == Style.RANGE ? Weapon.PIPE : Weapon.CHIN, decision.weapon);
		}
	}
}
