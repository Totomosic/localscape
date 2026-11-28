package org.localscape.nylocas;

import org.junit.Test;

import static org.junit.Assert.*;

public class PairedChildTest
{
	private final Tile first = new Tile(3292, 4243, 0);
	private final Tile second = first.dx(1).dy(1);

	@Test public void bothReadyPlayersChooseDifferentChildrenRegardlessOfObservationOrder()
	{
		Tile ranger = first.dy(-1);
		Tile mage = second.dx(1);
		assertEquals(first, AttackTiming.pairedChild(true, ranger, mage, second, first, true));
		assertEquals(second, AttackTiming.pairedChild(false, ranger, mage, first, second, true));
		assertEquals(second, AttackTiming.pairedChild(true, mage, ranger, first, second, true));
	}

	@Test public void keepsTheOriginalPlanWhenTwoMeleeApproachesCannotFit()
	{
		assertNull(AttackTiming.pairedChild(true, first.dx(6), second, first, second, true));
		assertNotNull(AttackTiming.pairedChild(true, first.dx(6), second, first, second, false));
	}
}
