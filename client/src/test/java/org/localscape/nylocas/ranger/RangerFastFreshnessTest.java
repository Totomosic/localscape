package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerFastFreshnessTest
{
	private Room room(int splitAt, Style oldStyle)
	{
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 116;
		room.player = RangerStrategy.tile(3298, 4251);
		Crab parent = new Crab(69, 69, 100, 14, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3298, 4247));
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(splitAt);
		room.crabs.add(parent);
		Crab old = new Crab(56, 56, 86, 12, Lane.SPLIT, (char) 0, false, oldStyle, room.player.dy(1));
		old.pendingDamage = 0;
		room.crabs.add(old);
		return room;
	}

	@Test
	public void rangerTakesTheUsefulPipeCycleWhileMageWaitsTwoTicksForTheSameForecast()
	{
		Room range = room(122, Style.RANGE);
		Decision shot = new RangerStrategy().decide(range, 120);
		assertSame(range.crabs.get(1), shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);

		MageStrategy.Room magic = new MageStrategy.Room();
		magic.wave = 16;
		magic.spawns[16] = 116;
		magic.player = range.player;
		MageStrategy.Crab parent = new MageStrategy.Crab(69, 69, 100, 14, MageStrategy.Lane.EAST, 'S',
			true, MageStrategy.Style.RANGE, range.crabs.get(0).point);
		parent.hp = 0;
		parent.dying = true;
		parent.observeSplitAt(122);
		magic.crabs.add(parent);
		MageStrategy.Crab old = new MageStrategy.Crab(56, 56, 86, 12, MageStrategy.Lane.SPLIT, (char) 0,
			false, MageStrategy.Style.MAGIC, magic.player.dy(1));
		old.pendingDamage = 0;
		magic.crabs.add(old);
		MageStrategy mage = new MageStrategy();
		assertNull(mage.decide(magic, 120).target);
		assertNull(mage.decide(magic, 121).target);
	}

	@Test
	public void aVisibleFreshSplitTwoMovementTicksAwayCannotDelayTheReadyPipe()
	{
		Room room = room(122, Style.RANGE);
		room.crabs.remove(0);
		room.player = RangerStrategy.tile(3293, 4251);
		Crab old = room.crabs.get(0);
		old.point = room.player.dy(1);
		Crab fresh = new Crab(70, 70, 120, 16, Lane.SPLIT, (char) 0, false, Style.RANGE,
			RangerStrategy.tile(3304, 4251));
		fresh.pendingDamage = 0;
		room.crabs.add(fresh);
		Decision shot = new RangerStrategy().decide(room, 120);
		assertSame(old, shot.target);
		assertEquals(Weapon.PIPE, shot.weapon);
		assertFalse(shot.pathToTarget);
	}

	@Test
	public void oneTickImminentSplitStillJustifiesWaitingRatherThanTheOldPipe()
	{
		assertNull(new RangerStrategy().decide(room(121, Style.RANGE), 120).target);
	}

	@Test
	public void aSlowerEyeShotCanStillYieldTwoTicksForFreshWork()
	{
		Room room = room(122, Style.MAGIC);
		RangerStrategy strategy = new RangerStrategy();
		assertNull(strategy.decide(room, 120).target);
		assertNull(strategy.decide(room, 121).target);
	}

	@Test
	public void anAlmostExpiredRangeSmallDoesNotJustifyGivingUpFreshWork()
	{
		Room room = room(122, Style.RANGE);
		Crab old = room.crabs.remove(1);
		Crab expiring = new Crab(old.key, old.index, 75, 12, Lane.SPLIT, (char) 0, false, Style.RANGE, old.point);
		expiring.pendingDamage = 0;
		room.crabs.add(expiring);
		assertNull(new RangerStrategy().decide(room, 120).target);
	}

	@Test
	public void existingCooldownCanPrepareForFreshWorkWithoutSpendingReadyTicks()
	{
		Room room = room(122, Style.RANGE);
		room.ready = 122;
		Decision movement = new RangerStrategy().decide(room, 120);
		assertNull(movement.target);
		assertTrue(movement.reason, movement.reason.startsWith("Prepare finite fresh opportunity"));
	}
}
