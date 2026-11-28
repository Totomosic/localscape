package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerCleanupLocalMeleeAllocationTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab blue;
	private final Crab red;

	public RangerCleanupLocalMeleeAllocationTest()
	{
		room.wave = 31;
		room.spawns[29] = 256;
		room.spawns[31] = 272;
		room.cleanup = true;
		room.ready = 304;
		room.player = RangerStrategy.tile(3294, 4252);
		room.magePosition = RangerStrategy.tile(3296, 4245);
		room.mageReady = 304;
		room.observePartnerWeapon(31113);
		blue = new Crab(193, 199, 276, 31, Lane.SPLIT, '\0', false, Style.MAGIC, RangerStrategy.tile(3301, 4252));
		blue.hp = 7;
		blue.pendingDamage = 0;
		room.crabs.add(blue);
		red = new Crab(205, 211, 291, 31, Lane.SPLIT, '\0', false, Style.MELEE, RangerStrategy.tile(3292, 4253));
		red.pendingDamage = 0;
		room.crabs.add(red);
	}

	@Test
	public void localRedPreventsBothReadyPlayersFromTakingTheSameEye()
	{
		Decision decision = ranger.decide(room, 304);
		assertSame(red, decision.target);
		assertEquals(Weapon.SULPHUR, decision.weapon);
		assertFalse(decision.special);
	}

	@Test
	public void unknownOrMeleePartnerGearKeepsTheOrdinaryEye()
	{
		for (int itemId : new int[] {-1, 29084, 22325})
		{
			ranger.reset();
			room.observePartnerWeapon(itemId);
			assertSame(blue, ranger.decide(room, 304).target);
		}
	}

	@Test
	public void partnerCooldownKeepsTheRangerOnTheBlue()
	{
		room.mageReady = 305;
		assertSame(blue, ranger.decide(room, 304).target);
	}

	@Test
	public void nearbyPartnerMeleeIsNotTakenOver()
	{
		room.magePosition = RangerStrategy.tile(3292, 4252);
		assertSame(blue, ranger.decide(room, 304).target);
	}

	@Test
	public void partnerNullBlueStillNeedsTheRangersEye()
	{
		blue.partnerNulled = true;
		assertSame(blue, ranger.decide(room, 304).target);
	}

	@Test
	public void ownNullMeleeIsNeverPrimary()
	{
		red.nulled = true;
		assertSame(blue, ranger.decide(room, 304).target);
	}

	@Test
	public void aDiagonalThreeStepMeleeDoesNotBecomeAnImmediateHit()
	{
		red.point = RangerStrategy.tile(3291, 4249);
		assertSame(blue, ranger.decide(room, 304).target);
	}

	@Test
	public void aPillarCornerCannotBeCutToInventATwoStepHit()
	{
		room.player = RangerStrategy.tile(3290, 4252);
		red.point = RangerStrategy.tile(3292, 4254);
		Decision decision = ranger.decide(room, 304);
		assertFalse(decision.reason.startsWith("Local red"));
	}

	@Test
	public void inclusiveTwentyNineCutoffStillExcludesTheMelee()
	{
		room.crabs.remove(red);
		Crab dimmed = new Crab(206, 212, 256, 29, Lane.SPLIT, '\0', false, Style.MELEE, red.point);
		dimmed.pendingDamage = 0;
		room.crabs.add(dimmed);
		assertSame(blue, ranger.decide(room, 304).target);
	}

	@Test
	public void aLaterTwentyNineChildRemainsFresh()
	{
		room.crabs.remove(red);
		Crab fresh = new Crab(206, 212, 291, 29, Lane.SPLIT, '\0', false, Style.MELEE, red.point);
		fresh.pendingDamage = 0;
		room.crabs.add(fresh);
		assertSame(fresh, ranger.decide(room, 304).target);
	}

	@Test
	public void aRequiredWestThirtyParentKeepsItsFinish()
	{
		Crab parent = new Crab(207, 213, 280, 30, Lane.WEST, 'S', true, Style.RANGE, RangerStrategy.tile(3294, 4249));
		parent.pendingDamage = 0;
		room.crabs.add(parent);
		assertSame(parent, ranger.decide(room, 304).target);
	}

	@Test
	public void aFinalGreenKeepsItsPriorityAfterItsFirstPacket()
	{
		room.spawns[31] = 288;
		room.crabs.remove(blue);
		Crab otherBlue = new Crab(208, 214, 295, 30, Lane.SPLIT, '\0', false, Style.MAGIC, blue.point);
		otherBlue.pendingDamage = 0;
		room.crabs.add(otherBlue);
		Crab green = new Crab(209, 215, 288, 31, Lane.EAST, 'S', false, Style.RANGE, RangerStrategy.tile(3301, 4248));
		green.pendingDamage = 0;
		room.crabs.add(green);
		ranger.attacked(new Decision(green, Weapon.PIPE, room.player, false, "East range 31", "first green hit"), 301);
		Decision decision = ranger.decide(room, 304);
		assertTrue(decision.target == green || decision.approach == green);
	}
}
