package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

/** Event timing and room-reset safety of the private last-confirmed-pipe observation. */
public class RangerKnownPipeTimingTest
{
	@Test
	public void observationTickMeansLaunchOneTickEarlierAndResetRemovesKnowledge()
	{
		RangerStrategy strategy = new RangerStrategy();
		Crab crab = new Crab(87, 93, 140, 18, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3292, 4253));
		strategy.attacked(new Decision(crab, Weapon.PIPE, crab.point, false, null, "Confirmed"), 157);
		crab.ownHitUntil = 188;
		crab.ownHitHp = 0;
		assertFalse(strategy.knownPipeImpactDue(crab, 157));
		assertTrue(strategy.knownPipeImpactDue(crab, 158));
		Crab anotherIdentity = new Crab(88, 94, 140, 18, Lane.EAST, 'S', true, Style.RANGE, crab.point);
		assertFalse(strategy.knownPipeImpactDue(anotherIdentity, 158));
		strategy.reset();
		assertFalse(strategy.knownPipeImpactDue(crab, 158));
		assertEquals(188, crab.ownHitUntil);
	}
}
