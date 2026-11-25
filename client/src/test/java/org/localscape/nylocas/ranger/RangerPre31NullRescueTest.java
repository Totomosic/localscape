package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerPre31NullRescueTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;
	private Room room()
	{
		Room room = new Room();
		room.wave = 30;
		room.spawns[28] = 224;
		room.spawns[29] = 240;
		room.spawns[30] = 248;
		room.player = RangerStrategy.tile(3293, 4248);
		room.ready = 256;
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

	private Crab rescue(Room room)
	{
		Crab crab = crab(room, 29, 243, false, Style.MAGIC, 3299, 4244);
		crab.partnerNulled = true;
		return crab;
	}

	@Test
	public void reducedActualThirtyWitnessRescuesThePartnerBeforeOrdinaryFiller()
	{
		// Reduced from timestamp29 control seed215613141308331 tick256, delay0, HP99.
		// Key176 born243 is MAGIC/HP8/queued0 and MageNULL; key184 is ordinary RANGE.
		// The synthetic wave28 anchor keeps the selected cutoff240 for either policy.
		Room room = room();
		Crab rescue = rescue(room);
		crab(room, 30, 251, false, Style.RANGE, 3292, 4243);
		Decision decision = strategy.decide(room, 256);
		assertSame(decision.reason, rescue, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
		assertFalse(decision.special);
	}

	@Test
	public void newlyVisibleNullReleasesAStaleOrdinaryFillerIntent()
	{
		Room room = room();
		Crab rescue = rescue(room);
		rescue.partnerNulled = false;
		Crab ordinary = crab(room, 30, 251, false, Style.RANGE, 3292, 4243);
		Decision preparation = strategy.decide(room, 255);
		assertTrue(preparation.reason, preparation.target == ordinary || preparation.approach == ordinary);
		rescue.partnerNulled = true;
		Decision decision = strategy.decide(room, 256);
		assertSame(decision.reason, rescue, decision.target);
	}

	@Test
	public void allSmallAndBigCurrentStylesCanReceiveNormalRescue()
	{
		for (Style style : Style.values()) for (boolean big : new boolean[]{false, true})
		{
			strategy.reset();
			Room room = room();
			Crab rescue = crab(room, 29, 253, big, style, 3293, 4247);
			rescue.partnerNulled = true;
			crab(room, 30, 254, false, Style.RANGE, 3295, 4248);
			Decision decision = strategy.decide(room, 256);
			assertTrue(decision.reason, decision.target == rescue || decision.approach == rescue);
			assertEquals(style, decision.weapon.style);
			assertFalse(decision.special);
		}
	}

	@Test
	public void stalePartnerLeaseDoesNotSuppressRescueButOwnUnknownFlightDoes()
	{
		for (int guard = 0; guard < 2; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab rescue = rescue(room);
			rescue.pendingDamage = -1;
			rescue.claimedUntil = 270;
			if (guard == 1) { rescue.ownHitUntil = 270; rescue.ownHitHp = rescue.hp; }
			crab(room, 30, 251, false, Style.RANGE, 3292, 4243);
			Decision decision = strategy.decide(room, 256);
			if (guard == 0) assertSame(decision.reason, rescue, decision.target);
			else { assertNotSame(rescue, decision.target); assertNotSame(rescue, decision.approach); }
		}
	}

	@Test
	public void ownNullKnownLethalAndSelectedBirthCutoffStillBlockRescue()
	{
		for (int guard = 0; guard < 3; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab rescue = crab(room, 29, guard == 2 ? 240 : 243, false, Style.MAGIC, 3299, 4244);
			rescue.partnerNulled = true;
			if (guard == 0) rescue.nulled = true;
			if (guard == 1) rescue.pendingDamage = rescue.hp;
			Crab ordinary = crab(room, 30, 251, false, Style.RANGE, 3292, 4243);
			assertSame("guard " + guard, ordinary, strategy.decide(room, 256).target);
		}
	}

	@Test
	public void requiredWestThirtyPipeKeepsPriority()
	{
		Room room = room();
		room.ready = 251;
		rescue(room);
		Crab packet = new Crab(++key, key, 248, 30, Lane.WEST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3284, 4248));
		packet.pendingDamage = 0;
		room.crabs.add(packet);
		Decision decision = strategy.decide(room, 251);
		assertSame(decision.reason, packet, decision.target);
		assertEquals("West big 30", decision.rule);
	}

	@Test
	public void southThirtyChinAndItsReturnBudgetArePreserved()
	{
		for (int tick = 252; tick <= 253; tick++)
		{
			strategy.reset();
			Room room = room();
			room.ready = tick;
			Crab rescue = rescue(room);
			Crab packet = new Crab(++key, key, 248, 30, Lane.SOUTH, 'E', false,
				tick == 253 ? Style.RANGE : Style.MELEE, RangerStrategy.tile(3296, 4238));
			packet.pendingDamage = 0;
			room.crabs.add(packet);
			Decision decision = strategy.decide(room, tick);
			assertNotSame(rescue, decision.target);
			assertNotSame(rescue, decision.approach);
			assertEquals(Weapon.CHIN, decision.weapon);
		}
	}
}
