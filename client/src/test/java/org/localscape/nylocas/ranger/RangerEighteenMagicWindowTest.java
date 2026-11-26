package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;

import static org.junit.Assert.*;

public class RangerEighteenMagicWindowTest
{
	private final RangerStrategy strategy = new RangerStrategy();
	private Room room()
	{
		Room room = new Room();
		room.wave = 18;
		room.spawns[18] = 140;
		room.player = RangerStrategy.tile(3294, 4247);
		return room;
	}

	private Crab parent(Room room, Lane lane)
	{
		Crab crab = new Crab(85, 91, 140, 18, lane, lane == Lane.SOUTH ? 'W' : 'S', true,
			Style.MAGIC, RangerStrategy.tile(3286, 4248));
		crab.pendingDamage = 0;
		room.crabs.add(crab);
		return crab;
	}

	@Test
	public void readyWestParentUsesItsTemporaryMagicWindowInsteadOfWaitingTwoTicks()
	{
		Room room = room();
		Crab parent = parent(room, Lane.WEST);
		Decision decision = strategy.decide(room, 145);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void currentFailedNineteenWitnessUsesTheVettedWestEyeCorridor()
	{
		// Current29-control seed249433535008344 tick161: native idle, West18 HP16.
		Room room = room();
		room.spawns[18] = 156;
		room.player = RangerStrategy.tile(3291, 4248);
		Crab parent = new Crab(87, 93, 156, 18, Lane.WEST, 'S', true, Style.MAGIC,
			RangerStrategy.tile(3286, 4248));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		Decision decision = strategy.decide(room, 161);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void knownNonlethalOwnPipeDoesNotHideTheMagicWindow()
	{
		Room room = room();
		Crab parent = parent(room, Lane.WEST);
		strategy.attacked(new Decision(parent, Weapon.PIPE, room.player, false, null, "Earlier pipe"), 142);
		parent.hp = 5;
		parent.pendingDamage = 2;
		parent.ownHitUntil = 174;
		room.ready = 144;
		Decision decision = strategy.decide(room, 145);
		assertSame(decision.reason, parent, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void blockedCurrentRayDoesNotInventAnEyeLaunch()
	{
		Room room = room();
		Crab parent = parent(room, Lane.WEST);
		room.obstructed.add(parent.key);
		Decision decision = strategy.decide(room, 145);
		assertFalse(decision.reason, decision.target == parent && decision.weapon == Weapon.EYE);
	}

	@Test
	public void earlyEyeAndFollowupPipeKeepTheEarliestWestNineteenPacket()
	{
		Room room = room();
		Crab parent = parent(room, Lane.WEST);
		Decision eye = strategy.decide(room, 145);
		assertSame(parent, eye.target);
		assertEquals(Weapon.EYE, eye.weapon);
		strategy.attacked(eye, 145);
		room.ready = 148;
		room.wave = 19;
		room.spawns[19] = 148;
		room.player = RangerStrategy.tile(3293, 4248);
		parent.point = RangerStrategy.tile(3290, 4248);
		parent.style = Style.RANGE;
		parent.hp = 8;
		parent.pendingDamage = 0;
		Crab west19 = new Crab(96, 102, 148, 19, Lane.WEST, 'S', true, Style.MAGIC,
			RangerStrategy.tile(3281, 4248));
		west19.pendingDamage = 0;
		room.crabs.add(west19);
		Decision followup = strategy.decide(room, 148);
		assertSame(followup.reason, parent, followup.target);
		assertEquals(Weapon.PIPE, followup.weapon);
		strategy.attacked(followup, 148);
		room.ready = 150;
		west19.point = RangerStrategy.tile(3284, 4248);
		Decision packet = strategy.decide(room, 151);
		assertSame(packet.reason, west19, packet.target);
		assertEquals(Weapon.EYE, packet.weapon);
		assertEquals("West magic big 19", packet.rule);
	}

	@Test
	public void recentMagicFillerCanKeepItsReadyAttack()
	{
		Room room = room();
		parent(room, Lane.WEST);
		Crab recent = new Crab(59, 65, 139, 17, Lane.SPLIT, 'S', false, Style.MAGIC,
			RangerStrategy.tile(3296, 4247));
		room.crabs.add(recent);
		Decision decision = strategy.decide(room, 145);
		assertSame(decision.reason, recent, decision.target);
		assertEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void closingMagicColorCannotQueueEyeIntoRanged()
	{
		Room room = room();
		Crab parent = parent(room, Lane.WEST);
		Decision decision = strategy.decide(room, 146);
		assertNotEquals(Weapon.EYE, decision.weapon);
	}

	@Test
	public void southAndEastStillWaitForTheirAssignedEntryTiming()
	{
		for (Lane lane : new Lane[]{Lane.SOUTH, Lane.EAST})
		{
			strategy.reset();
			Room room = room();
			Crab parent = parent(room, lane);
			Decision decision = strategy.decide(room, 145);
			assertNotSame(lane + ": " + decision.reason, parent, decision.target);
			assertNotSame(lane + ": " + decision.reason, parent, decision.approach);
		}
	}

	@Test
	public void nullAndKnownLethalParentsDoNotReceiveTheEye()
	{
		for (int guard = 0; guard < 4; guard++)
		{
			strategy.reset();
			Room room = room();
			Crab parent = parent(room, Lane.WEST);
			if (guard == 0) parent.nulled = true;
			else if (guard == 1) parent.pendingDamage = parent.hp;
			else if (guard == 2) { parent.pendingDamage = -1; parent.claimedUntil = 150; }
			else { parent.pendingDamage = -1; parent.ownHitUntil = 150; parent.ownHitHp = parent.hp; }
			Decision decision = strategy.decide(room, 145);
			if (guard < 2)
			{
				assertNotSame(parent, decision.target);
				assertNotSame(parent, decision.approach);
			}
			else assertFalse(decision.reason, decision.target == parent && decision.weapon == Weapon.EYE);
		}
	}

	@Test
	public void anExistingRangedSmallKeepsItsFasterNativeAttack()
	{
		Room room = room();
		parent(room, Lane.WEST);
		Crab own = new Crab(90, 96, 144, 18, Lane.SPLIT, 'S', false, Style.RANGE,
			RangerStrategy.tile(3296, 4247));
		room.crabs.add(own);
		Decision decision = strategy.decide(room, 145);
		assertSame(decision.reason, own, decision.target);
		assertEquals(Weapon.PIPE, decision.weapon);
	}
}
