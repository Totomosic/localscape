package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MageEast15EarlyShareTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private int key;

	private Crab small(int wave, int born, Lane lane, char slot, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, lane, slot, false, style, MageStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private Crab waveFourteen()
	{
		room.wave = 14;
		room.spawns[14] = 104;
		room.ready = 109;
		room.player = MageStrategy.tile(3292, 4249);
		room.rangerPosition = MageStrategy.tile(3294, 4245);
		return small(14, 104, Lane.WEST, 'N', Style.MAGIC, 3284, 4249);
	}

	private Crab waveFifteen()
	{
		room.wave = 15;
		room.spawns[15] = 112;
		room.ready = 112;
		room.player = MageStrategy.tile(3301, 4249);
		room.rangerPosition = MageStrategy.tile(3294, 4245);
		return small(15, 112, Lane.EAST, 'N', Style.MAGIC, 3310, 4249);
	}

	@Test
	public void stagesDuringCooldownWhenWestFourteenMagicIsCoveredAndRangerIsWest()
	{
		Crab west = waveFourteen();
		west.pendingDamage = west.hp;

		Decision stage = strategy.decide(room, 107);
		assertNull(stage.target);
		assertEquals("Prepare East 15 north", stage.reason);
		assertEquals(MageStrategy.tile(3301, 4249), stage.tile);
	}

	@Test
	public void keepsTheWestFourteenAssignmentWhileItLives()
	{
		Crab west = waveFourteen();
		room.ready = 107;

		assertSame(west, strategy.decide(room, 107).target);
	}

	@Test
	public void launchesAtBirthWhenMageHasTheEasternRayAndRangerIsWest()
	{
		Crab north = waveFifteen();
		small(15, 112, Lane.EAST, 'S', Style.RANGE, 3310, 4248);

		Decision attack = strategy.decide(room, 112);
		assertSame(north, attack.target);
		assertEquals(Weapon.EYE, attack.weapon);
		assertEquals("Early East 15 north share", attack.reason);
	}

	@Test
	public void leavesTheOpeningToTheRangerWhenAlreadyOnTheEastSide()
	{
		waveFifteen();
		room.rangerPosition = MageStrategy.tile(3298, 4248);

		assertNull(strategy.decide(room, 112).target);
	}

	@Test
	public void releasesTheShareWhenThePartnerAlreadyHasDamageInFlight()
	{
		Crab north = waveFifteen();
		north.pendingDamage = 2;

		assertNull(strategy.decide(room, 112).target);
	}
}
