package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerOptionalEast23PipeTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private int key;
	private Room room()
	{
		Room room = new Room();
		room.wave = 28;
		room.spawns[27] = 224;
		room.spawns[28] = 232;
		room.player = RangerStrategy.tile(3296, 4244);
		room.ready = 235;
		return room;
	}

	private Crab parent(Room room)
	{
		Crab crab = new Crab(++key, key, 196, 23, Lane.EAST, 'S', true, Style.RANGE,
			RangerStrategy.tile(3300, 4251));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	private Crab south28(Room room)
	{
		Crab crab = new Crab(++key, key, 232, 28, Lane.SOUTH, 'E', false, Style.MELEE,
			RangerStrategy.tile(3296, 4236));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void nativeReadyReserveWitnessCanSpendTwoTicksOnTheOldParent()
	{
		// Parent-lifecycle seed178734103529236 tick235; own South28 PIPE is due237.
		Room room = room();
		Crab parent = parent(room);
		south28(room);
		Decision decision = strategy.decide(room, 235);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
		assertFalse(decision.special);
	}

	@Test
	public void cooldownApproachCanFireAndReturnForTheMandatorySouthPacket()
	{
		Room room = room();
		Crab parent = parent(room);
		Crab packet = south28(room);
		room.obstructed.add(parent.key);
		Decision approach = strategy.decide(room, 234);
		assertSame(approach.reason, parent, approach.approach);
		assertEquals(Weapon.PIPE, approach.weapon);
		room.obstructed.clear();
		room.player = RangerStrategy.tile(3296, 4246);
		Decision pipe = strategy.decide(room, 235);
		assertSame(pipe.reason, parent, pipe.target);
		strategy.attacked(pipe, 235);
		room.ready = 237;
		parent.pendingDamage = 0;
		Decision returnToPacket = strategy.decide(room, 236);
		assertNotSame(parent, returnToPacket.target);
		assertEquals(RangerStrategy.tile(3296, 4244), returnToPacket.tile);
		room.player = RangerStrategy.tile(3296, 4244);
		packet.style = Style.RANGE;
		packet.point = RangerStrategy.tile(3296, 4238);
		Decision required = strategy.decide(room, 237);
		assertSame(required.reason, packet, required.target);
		assertEquals("South east flicker 28", required.rule);
	}

	@Test
	public void tooLittleSpareBudgetDoesNotDelayTheNextPacket()
	{
		Room room = room();
		room.ready = 236;
		Crab parent = parent(room);
		south28(room);
		Decision decision = strategy.decide(room, 236);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
		assertEquals(RangerStrategy.tile(3296, 4244), decision.tile);
	}

	@Test
	public void usefulNewerAttackAndApproachKeepPriority()
	{
		for (int guard = 0; guard < 3; guard++)
		{
			strategy.reset();
			Room room = room();
			parent(room);
			Crab own = new Crab(++key, key, 234, 28, Lane.SPLIT, (char) 0, false,
				guard == 0 ? Style.RANGE : Style.MAGIC, RangerStrategy.tile(3297, 4247));
			own.pendingDamage = 0;
			room.crabs.add(own);
			if (guard == 2) room.obstructed.add(own.key);
			Decision decision = strategy.decide(room, 235);
			assertTrue(decision.reason, decision.target == own || decision.approach == own);
		}
	}

	@Test
	public void healthyParentCanReceiveAnotherSparePipeAfterNonlethalDamage()
	{
		Room room = room();
		room.player = RangerStrategy.tile(3296, 4247);
		Crab parent = parent(room);
		Decision first = strategy.decide(room, 235);
		assertSame(parent, first.target);
		strategy.attacked(first, 235);
		room.ready = 237;
		parent.hp = 14;
		parent.pendingDamage = 0;
		assertSame(parent, strategy.decide(room, 237).target);
	}

	@Test
	public void ownNullUnknownClaimKnownKillAndAgeCutoffStillBlock()
	{
		for (int guard = 0; guard < 4; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab parent = parent(room);
			if (guard == 0) parent.nulled = true;
			else if (guard == 1) { parent.pendingDamage = -1; parent.claimedUntil = 250; }
			else if (guard == 2) parent.pendingDamage = parent.hp;
			else { room.spawns[27] = 234; room.spawns[28] = 242; room.ready = 245; }
			Decision decision = strategy.decide(room, guard == 3 ? 245 : 235);
			assertNotSame(parent, decision.target);
			assertNotSame(parent, decision.approach);
		}
	}

	@Test
	public void actualThirtyOneStillDimsTheOldParent()
	{
		Room room = room();
		Crab parent = parent(room);
		room.wave = 31;
		room.spawns[31] = 235;
		Decision decision = strategy.decide(room, 235);
		assertNotSame(parent, decision.target);
		assertNotSame(parent, decision.approach);
	}
}
