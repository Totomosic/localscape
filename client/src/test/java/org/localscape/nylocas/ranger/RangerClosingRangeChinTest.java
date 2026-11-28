package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerClosingRangeChinTest
{
	@Test
	public void aPipeThatWouldCrossTheFlickerCannotSuppressTheLastLegalChin()
	{
		RangerStrategy ranger = new RangerStrategy();
		Room room = room(123, 3295);
		Crab west = west(room, 3284);
		Decision decision = ranger.decide(room, 123);
		assertSame(west, decision.target);
		assertEquals(Weapon.CHIN, decision.weapon);
	}

	@Test
	public void anAlreadyReachablePipeKeepsItsShorterCooldown()
	{
		RangerStrategy ranger = new RangerStrategy();
		Room room = room(123, 3290);
		Crab west = west(room, 3284);
		Decision decision = ranger.decide(room, 123);
		assertSame(west, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void aRealNextTickPipeStillSuppressesTheEqualImpactSingleChin()
	{
		RangerStrategy ranger = new RangerStrategy();
		Room room = room(122, 3294);
		Crab west = west(room, 3283);
		Decision decision = ranger.decide(room, 122);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertTrue(decision.target == west || decision.approach == west);
	}

	@Test
	public void theShortMagicWindowRemainsAnEyeOpportunity()
	{
		RangerStrategy ranger = new RangerStrategy();
		Room room = room(125, 3292);
		Crab west = west(room, 3286);
		west.style = Style.MAGIC;
		Decision decision = ranger.decide(room, 125);
		assertSame(west, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	private static Room room(int ready, int x)
	{
		Room room = new Room();
		room.wave = 16;
		room.spawns[16] = 120;
		room.ready = ready;
		room.player = RangerStrategy.tile(x, 4249);
		return room;
	}

	private static Crab west(Room room, int x)
	{
		Crab crab = new Crab(1, 1, 120, 16, Lane.WEST, 'N', false, Style.RANGE, RangerStrategy.tile(x, 4249));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}
}
