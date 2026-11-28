package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerPartnerNullCleanupTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;
	private Room room(int ready)
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[28] = 244;
		room.spawns[29] = 260;
		room.spawns[31] = 276;
		room.player = RangerStrategy.tile(3294, 4248);
		room.ready = ready;
		return room;
	}

	private Crab crab(Room room, int wave, int born, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(++key, key, born, wave, Lane.SPLIT, (char) 0, big, style,
			RangerStrategy.tile(x, y));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void mageNullMagicSurvivorReplacesNewerOrdinaryMagicAndStaleIntent()
	{
		// Retained seed279799561480397: key186 MageNULL275, available280, first Ranger hit310.
		Room room = room(280);
		Crab rescue = crab(room, 30, 269, false, Style.MAGIC, 3299, 4253);
		Crab ordinary = crab(room, 31, 276, false, Style.MAGIC, 3301, 4247);
		Decision preparation = strategy.decide(room, 279);
		assertTrue(preparation.reason, preparation.target == ordinary || preparation.approach == ordinary);
		rescue.partnerNulled = true;
		Decision decision = strategy.decide(room, 280);
		assertSame(decision.reason, rescue, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void mageNullMagicParentDoesNotLoseEligibilityAtAgeTwentySix()
	{
		Room room = room(300);
		Crab rescue = new Crab(++key, key, 270, 30, Lane.EAST, 'S', true, Style.MAGIC,
			RangerStrategy.tile(3300, 4248));
		rescue.partnerNulled = true;
		rescue.pendingDamage = 0;
		room.crabs.add(rescue);
		crab(room, 31, 295, false, Style.MAGIC, 3296, 4248);
		Decision decision = strategy.decide(room, 300);
		assertSame(decision.reason, rescue, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void mageNullMeleeCanBeRescuedBeforeOrdinaryRangeFiller()
	{
		Room room = room(300);
		Crab rescue = crab(room, 31, 280, false, Style.MELEE, 3293, 4246);
		rescue.partnerNulled = true;
		crab(room, 31, 295, false, Style.RANGE, 3295, 4248);
		Decision decision = strategy.decide(room, 300);
		assertSame(decision.reason, rescue, decision.target);
		assertEquals(Weapon.SULPHUR, decision.weapon);
		assertFalse(decision.special);
	}

	@Test
	public void staleUnknownPartnerClaimDoesNotAbandonAMageNullTarget()
	{
		Room room = room(300);
		Crab rescue = crab(room, 31, 280, false, Style.MAGIC, 3296, 4248);
		rescue.partnerNulled = true;
		rescue.pendingDamage = -1;
		rescue.claimedUntil = 310;
		crab(room, 29, 260, true, Style.MELEE, 3290, 4252);
		Decision decision = strategy.decide(room, 300);
		assertSame(decision.reason, rescue, decision.target);
	}

	@Test
	public void originalFinalWaveRangerDeadlineKeepsItsUrgency()
	{
		Room room = room(300);
		room.spawns[31] = 280;
		Crab rescue = crab(room, 30, 270, false, Style.MELEE, 3293, 4246);
		rescue.partnerNulled = true;
		Crab urgent = new Crab(++key, key, 280, 31, Lane.EAST, 'S', false, Style.RANGE,
			RangerStrategy.tile(3297, 4248));
		urgent.pendingDamage = 0;
		room.crabs.add(urgent);
		Decision decision = strategy.decide(room, 300);
		assertSame(decision.reason, urgent, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void ownNullKnownKillAndOldCutoffStillBlockRescue()
	{
		for (int guard = 0; guard < 5; guard++)
		{
			strategy.reset();
			Room room = room(300);
			if (guard == 4) { room.spawns[28] = 224; room.spawns[29] = 240; }
			Crab rescue = crab(room, guard == 2 ? 29 : 30, guard == 2 ? 260 : guard == 4 ? 251 : 270, false, Style.MAGIC, 3296, 4248);
			rescue.partnerNulled = true;
			if (guard == 0) rescue.nulled = true;
			else if (guard == 1) rescue.pendingDamage = rescue.hp;
			else if (guard == 3) { rescue.pendingDamage = -1; rescue.ownHitUntil = 305; rescue.ownHitHp = rescue.hp; }
			Decision decision = strategy.decide(room, 300);
			assertNotSame(rescue, decision.target);
			assertNotSame(rescue, decision.approach);
		}
	}

	@Test
	public void lateWaveTwentyNineChildCanBeRescuedDespitePartnerLease()
	{
		Room room = room(300);
		Crab rescue = crab(room, 29, 265, false, Style.MAGIC, 3296, 4248);
		rescue.partnerNulled = true;
		rescue.pendingDamage = -1;
		rescue.claimedUntil = 310;
		crab(room, 31, 295, false, Style.RANGE, 3295, 4248);
		Decision decision = strategy.decide(room, 300);
		assertSame(decision.reason, rescue, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void pendingSouthThirtyChinKeepsItsReturnBudget()
	{
		Room room = room(300);
		room.spawns[30] = 296;
		room.spawns[31] = 300;
		Crab rescue = crab(room, 30, 297, false, Style.MELEE, 3293, 4246);
		rescue.partnerNulled = true;
		room.crabs.add(new Crab(++key, key, 296, 30, Lane.SOUTH, 'E', false,
			Style.MELEE, RangerStrategy.tile(3296, 4237)));
		Decision decision = strategy.decide(room, 300);
		assertNotSame(rescue, decision.target);
		assertNotSame(rescue, decision.approach);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void mageNullSouthThirtyPrimaryStillUsesTheRequiredChin()
	{
		Room room = room(301);
		room.spawns[30] = 296;
		room.spawns[31] = 300;
		room.player = RangerStrategy.tile(3293, 4248);
		Crab primary = new Crab(++key, key, 296, 30, Lane.SOUTH, 'E', false,
			Style.RANGE, RangerStrategy.tile(3296, 4238));
		primary.partnerNulled = true;
		primary.pendingDamage = 0;
		room.crabs.add(primary);
		Decision decision = strategy.decide(room, 301);
		assertSame(decision.reason, primary, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
		assertEquals("South chin flicker 30", decision.rule);
	}
}
