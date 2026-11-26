package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MageEast13CooldownContactTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private Crab parent, north, south, west;
	private int key;

	private Crab crab(int born, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab result = new Crab(++key, key, born, wave, lane, slot, big, style, MageStrategy.tile(x, y));
		result.pendingDamage = 0;
		room.crabs.add(result);
		return result;
	}

	private void whileRangerIsInRange()
	{
		// Packed seed 120739708142431 before Ranger's lethal t114 projectile.
		room.wave = 15;
		room.spawns[13] = 92;
		room.spawns[15] = 108;
		room.ready = 116;
		room.player = MageStrategy.tile(3299, 4251);
		room.rangerPosition = MageStrategy.tile(3300, 4248);
		parent = crab(92, 13, Lane.EAST, 'S', true, Style.MELEE, 3299, 4246);
		north = crab(108, 15, Lane.EAST, 'N', false, Style.MAGIC, 3305, 4249);
		north.pendingDamage = north.hp;
		south = crab(108, 15, Lane.EAST, 'S', false, Style.RANGE, 3304, 4248);
		west = crab(108, 15, Lane.WEST, 'S', false, Style.RANGE, 3287, 4248);
	}

	@Test
	public void spendsCooldownOnDirectCardinalContactButWaitsForTheLethalFlight()
	{
		whileRangerIsInRange();

		Decision stage = strategy.decide(room, 114);
		assertNull(stage.target);
		assertNull(stage.approach);
		assertEquals(Weapon.SCYTHE, stage.weapon);
		assertEquals(MageStrategy.tile(3299, 4248), stage.tile);

		room.player = stage.tile;
		south.pendingDamage = south.hp;
		Decision attack = strategy.decide(room, 116);
		assertSame(parent, attack.target);
		assertEquals(Weapon.SCYTHE, attack.weapon);
	}

	@Test
	public void reconsidersAReadyWestAttackIfTheEastPairIsStillUncovered()
	{
		whileRangerIsInRange();
		strategy.decide(room, 114);
		room.player = MageStrategy.tile(3291, 4248);

		assertSame(west, strategy.decide(room, 116).target);
	}

	@Test
	public void preservesEastNorthServiceIfTheMagicSmallNeedsAnAttack()
	{
		whileRangerIsInRange();
		north.pendingDamage = 0;

		assertNotEquals("Approach East 13 during East 15 coverage", strategy.decide(room, 114).reason);
		assertNotSame(parent, strategy.decide(room, 116).target);
	}

	@Test
	public void keepsTheWestFourteenMagicDutyDuringCooldown()
	{
		whileRangerIsInRange();
		room.crabs.remove(west);
		Crab duty = crab(100, 14, Lane.WEST, 'N', false, Style.MAGIC, 3292, 4248);

		Decision plan = strategy.decide(room, 114);
		assertSame(duty, plan.approach);
		assertEquals(Weapon.EYE, plan.weapon);
	}

	@Test
	public void needsAnObservedRangerInRangeOfTheRemainingSouthSmall()
	{
		whileRangerIsInRange();
		room.rangerPosition = MageStrategy.tile(3291, 4248);
		assertNotEquals("Approach East 13 during East 15 coverage", strategy.decide(room, 114).reason);

		room.rangerPosition = MageStrategy.tile(3300, 4248);
		south.partnerNulled = true;
		assertNotEquals("Approach East 13 during East 15 coverage", strategy.decide(room, 114).reason);
	}
}
