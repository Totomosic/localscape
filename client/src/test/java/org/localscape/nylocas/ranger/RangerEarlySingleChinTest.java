package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerEarlySingleChinTest
{
	private final Room room = new Room();
	private final RangerStrategy ranger = new RangerStrategy();
	private final Crab primary;

	public RangerEarlySingleChinTest()
	{
		room.wave = 12;
		room.spawns[12] = 100;
		room.ready = 108;
		room.player = RangerStrategy.tile(3295, 4248);
		primary = new Crab(1, 1, 100, 12, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3306, 4249));
		primary.pendingDamage = 0;
		room.crabs.add(primary);
	}

	@Test
	public void aChinCanLandBeforeAFarPipeCanLaunchAndLand()
	{
		Decision decision = ranger.decide(room, 108);
		assertSame(decision.reason, primary, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
		assertTrue(decision.reason, decision.reason.startsWith("Early chin"));
	}

	@Test
	public void onlyOneSavedLaunchTickDoesNotPayForTheExtraFlightTick()
	{
		primary.point = RangerStrategy.tile(3304, 4249);
		Decision decision = ranger.decide(room, 108);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertSame(primary, decision.approach);
	}

	@Test
	public void anOffColorSmallCanBeLeftToAnAvailableNonNullMage()
	{
		Crab secondary = new Crab(2, 2, 100, 12, Lane.SPLIT, (char) 0, false, Style.MAGIC, primary.point.dy(1));
		secondary.pendingDamage = 0;
		room.crabs.add(secondary);
		room.observePartner(RangerStrategy.tile(3301, 4249), 108);
		assertEquals(Weapon.CHIN, ranger.decide(room, 108).weapon);
	}

	@Test
	public void aMageNullSecondaryCannotBeAbandonedByAnEarlyChin()
	{
		Crab secondary = new Crab(2, 2, 100, 12, Lane.SPLIT, (char) 0, false, Style.MAGIC, primary.point.dy(1));
		secondary.pendingDamage = 0;
		secondary.partnerNulled = true;
		room.crabs.add(secondary);
		room.observePartner(RangerStrategy.tile(3301, 4249), 108);
		assertNotEquals(Weapon.CHIN, ranger.decide(room, 108).weapon);
	}

	@Test
	public void aBlockedCurrentRayStillUsesGroundMovement()
	{
		room.obstructed.add(primary.key);
		Decision decision = ranger.decide(room, 108);
		assertNull(decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
