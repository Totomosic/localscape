package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerLastLivingCooperationTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;
	private Room room()
	{
		Room room = new Room();
		room.wave = 31;
		room.spawns[28] = 236;
		room.spawns[29] = 252;
		room.spawns[31] = 260;
		room.ready = 298;
		room.player = RangerStrategy.tile(3295, 4248);
		return room;
	}

	private Crab crab(Room room, int birth, boolean big, Style style)
	{
		Crab crab = new Crab(++key, key, birth <= 29 ? 252 : 270, birth, Lane.SPLIT, (char) 0, big, style,
			RangerStrategy.tile(3290, 4252));
		room.crabs.add(crab);
		return crab;
	}

	private void assertChooses(Crab expected, Decision decision)
	{
		assertTrue(decision.reason, decision.target == expected || decision.approach == expected);
	}

	@Test
	public void literalFinalTargetCanReceiveAHitDespiteUnknownPartnerClaim()
	{
		for (Style style : Style.values())
		{
			strategy.reset();
			Room room = room();
			Crab last = crab(room, 31, false, style);
			last.claimedUntil = 305;
			last.pendingDamage = -1;
			Decision decision = strategy.decide(room, 298);
			assertChooses(last, decision);
			assertEquals(style, decision.weapon.style);
			assertFalse(decision.special);
		}
	}

	@Test
	public void nativeFinalMagicWitnessAllowsCooperationOnMagesProvisionalClaim()
	{
		// Retained seed257451351778437 tick307: Mage launches Eye at the only living key206.
		Room room = room();
		room.ready = 307;
		room.player = RangerStrategy.tile(3297, 4248);
		Crab last = new Crab(++key, key, 287, 31, Lane.SPLIT, (char) 0, false,
			Style.MAGIC, RangerStrategy.tile(3291, 4252));
		last.claimedUntil = 308;
		last.pendingDamage = -1;
		room.crabs.add(last);
		Decision decision = strategy.decide(room, 307);
		assertChooses(last, decision);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void literalFinalMageParentCrossesOrdinaryOwnershipAndAgeWindows()
	{
		Room room = room();
		Crab last = crab(room, 30, true, Style.MAGIC);
		last.claimedUntil = 305;
		last.pendingDamage = 2;
		Decision decision = strategy.decide(room, 298);
		assertChooses(last, decision);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void oldLivingBodyPreventsLastTargetClaimException()
	{
		Room room = room();
		Crab last = crab(room, 31, false, Style.MAGIC);
		last.claimedUntil = 305;
		last.pendingDamage = -1;
		crab(room, 29, true, Style.MELEE);
		Decision decision = strategy.decide(room, 298);
		assertNotSame(last, decision.target);
		assertNotSame(last, decision.approach);
	}

	@Test
	public void zeroHpAndDeathQueueBodiesDoNotPreventFinalCooperation()
	{
		Room room = room();
		Crab last = crab(room, 31, false, Style.MAGIC);
		last.claimedUntil = 305;
		last.pendingDamage = -1;
		crab(room, 29, true, Style.MELEE).hp = 0;
		crab(room, 30, false, Style.RANGE).dying = true;
		assertChooses(last, strategy.decide(room, 298));
	}

	@Test
	public void knownLethalDamageDoesNotRequireAnExtraAttack()
	{
		Room room = room();
		Crab last = crab(room, 31, false, Style.MAGIC);
		last.pendingDamage = last.hp;
		Decision decision = strategy.decide(room, 298);
		assertNotSame(last, decision.target);
		assertNotSame(last, decision.approach);
	}

	@Test
	public void ownNullAndUnknownOwnFlightRemainBlocking()
	{
		for (int guard = 0; guard < 2; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab last = crab(room, 31, false, Style.MAGIC);
			last.claimedUntil = 305;
			last.pendingDamage = -1;
			if (guard == 0) last.nulled = true;
			else { last.ownHitUntil = 303; last.ownHitHp = last.hp; }
			Decision decision = strategy.decide(room, 298);
			assertNotSame(last, decision.target);
			assertNotSame(last, decision.approach);
		}
	}

	@Test
	public void lastLivingOlderBirthStillHonorsTheActualThirtyOneCutoff()
	{
		Room room = room();
		Crab last = crab(room, 29, false, Style.MAGIC);
		Decision decision = strategy.decide(room, 298);
		assertNotSame(last, decision.target);
		assertNotSame(last, decision.approach);
	}

	@Test
	public void lateWaveTwentyNineChildRemainsFreshForFinalCooperation()
	{
		Room room = room();
		Crab last = new Crab(++key, key, 255, 29, Lane.SPLIT, (char) 0, false, Style.MAGIC,
			RangerStrategy.tile(3296, 4248));
		last.claimedUntil = 305;
		last.pendingDamage = -1;
		room.crabs.add(last);
		Decision decision = strategy.decide(room, 298);
		assertChooses(last, decision);
		assertEquals(Weapon.EYE, decision.weapon);
	}
}
