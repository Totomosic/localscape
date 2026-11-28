package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerPrecapReadyClearanceTest
{
	private final RangerStrategy ranger = new RangerStrategy();
	private final Room room = new Room();
	private final Crab blue;
	private final Crab green;

	public RangerPrecapReadyClearanceTest()
	{
		room.wave = 13; room.spawns[13] = 96; room.ready = 101; room.player = RangerStrategy.tile(3292, 4249);
		blue = add(1, 96, 13, Lane.WEST, 'S', false, Style.MAGIC, 3286, 4248);
		blue.pendingDamage = 4; blue.observeIncoming(new int[] {0, 4});
		green = add(2, 96, 13, Lane.WEST, 'N', false, Style.RANGE, 3286, 4249);
		ranger.attacked(new Decision(blue, Weapon.EYE, room.player, false, "West magic 13", "first Eye"), 99);
	}

	@Test
	public void aReadyPipeCanBeatRepeatingEyeOnAnOrdinarySmall()
	{
		Decision decision = ranger.decide(room, 101);
		assertSame(green, decision.target); assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void thePipeLeavesTheSameBlueForTheNextReadyTick()
	{
		Decision pipe = ranger.decide(room, 101); ranger.attacked(pipe, 102);
		green.pendingDamage = green.hp; room.ready = 103;
		blue.hp = 4; blue.pendingDamage = 0; blue.observeIncoming(new int[] {0});
		blue.observePosition(RangerStrategy.tile(3288, 4248));
		assertSame(blue, ranger.decide(room, 103).target);
	}

	@Test
	public void nearbyMageNullBlueCanBeatAHealthyOrdinaryFollowup()
	{
		Crab rescue = add(3, 99, 13, Lane.SPLIT, '\0', false, Style.MAGIC, 3291, 4252);
		rescue.hp = 1; rescue.partnerNulled = true;
		assertSame(rescue, ranger.decide(room, 101).target);
	}

	@Test
	public void anAlreadySelectedMageNullBlueKeepsItsService()
	{
		room.ready = 105; blue.point = RangerStrategy.tile(3290, 4248); green.point = RangerStrategy.tile(3290, 4249);
		blue.hp = 4; blue.pendingDamage = 0; blue.observeIncoming(new int[] {0}); blue.partnerNulled = true;
		ranger.attacked(new Decision(blue, Weapon.EYE, room.player, false, null, "previous hit"), 103);
		assertSame(blue, ranger.decide(room, 105).target);
	}

	@Test
	public void anOpenedParentRetainsItsExistingService()
	{
		room.wave = 15; room.spawns[14] = 104; room.spawns[15] = 112; room.ready = 118;
		room.player = RangerStrategy.tile(3296, 4248); room.crabs.clear();
		Crab parent = add(3, 104, 14, Lane.EAST, 'S', true, Style.RANGE, 3299, 4251); parent.hp = 8;
		add(4, 96, 13, Lane.SOUTH, 'E', false, Style.RANGE, 3299, 4243);
		Crab rescue = add(5, 113, 15, Lane.SPLIT, '\0', false, Style.MAGIC, 3297, 4248); rescue.partnerNulled = true;
		ranger.attacked(new Decision(parent, Weapon.PIPE, room.player, false, null, "first hit"), 116);
		assertSame(parent, ranger.decide(room, 118).target);
	}

	@Test
	public void theEastFifteenBlueDeadlineStillWinsOverASecondTickDelay()
	{
		room.wave = 16; room.spawns[15] = 112; room.spawns[16] = 120; room.ready = 121;
		room.player = RangerStrategy.tile(3300, 4248); room.crabs.clear();
		Crab eastBlue = add(3, 112, 15, Lane.EAST, 'N', false, Style.MAGIC, 3300, 4250);
		Crab eastGreen = add(4, 112, 15, Lane.EAST, 'S', false, Style.RANGE, 3301, 4248);
		add(5, 104, 14, Lane.EAST, 'S', true, Style.RANGE, 3299, 4251);
		ranger.attacked(new Decision(eastBlue, Weapon.EYE, room.player, false, "East magic 15", "first Eye"), 119);
		eastBlue.hp = 2;
		assertSame(eastBlue, ranger.decide(room, 121).target);
	}

	@Test
	public void outsideTheNewWindowTheNativeMagicFlashIsUnchanged()
	{
		room.wave = 17; room.spawns[16] = 120; room.spawns[17] = 124; room.ready = 125;
		room.player = RangerStrategy.tile(3292, 4249); room.crabs.clear();
		Crab flash = add(3, 120, 16, Lane.WEST, 'N', false, Style.MAGIC, 3286, 4249);
		add(4, 96, 13, Lane.SOUTH, 'E', false, Style.RANGE, 3299, 4243);
		ranger.attacked(new Decision(flash, Weapon.PIPE, room.player, false, null, "earlier green hit"), 123);
		flash.hp = 1;
		assertSame(flash, ranger.decide(room, 125).target);
	}

	@Test
	public void southThirteenCanUseTheTwoTickSlotBeforeTheCloseBlue()
	{
		room.ready = 104; room.player = RangerStrategy.tile(3291, 4249); room.crabs.remove(green);
		blue.point = RangerStrategy.tile(3289, 4248); blue.pendingDamage = 0; blue.observeIncoming(new int[] {0});
		ranger.attacked(new Decision(blue, Weapon.EYE, room.player, false, null, "last Eye missed"), 102);
		Crab south = add(3, 96, 13, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4241);
		Decision decision = ranger.decide(room, 104);
		assertSame(south, decision.target); assertEquals(Weapon.PIPE, decision.weapon);
	}

	@Test
	public void westFifteenCanUseTheReadyPipeSlotWithoutChangingParentRelease()
	{
		room.wave = 16; room.spawns[15] = 112; room.spawns[16] = 120; room.ready = 122;
		room.player = RangerStrategy.tile(3297, 4248); room.crabs.clear();
		Crab ordinary = add(3, 104, 14, Lane.SOUTH, 'E', false, Style.MAGIC, 3299, 4243);
		Crab west = add(4, 112, 15, Lane.WEST, 'S', false, Style.RANGE, 3292, 4248);
		ranger.attacked(new Decision(ordinary, Weapon.EYE, room.player, false, null, "last Eye missed"), 120);
		assertSame(west, ranger.decide(room, 122).target);
	}

	@Test
	public void theLastMagicTickBeforeAColorChangeIsNotSpentOnPipe()
	{
		room.wave = 16; room.spawns[16] = 120; room.ready = 125;
		room.player = RangerStrategy.tile(3292, 4249); room.crabs.clear();
		Crab flash = add(3, 120, 16, Lane.WEST, 'N', false, Style.MAGIC, 3286, 4249);
		add(4, 96, 13, Lane.SOUTH, 'E', false, Style.RANGE, 3296, 4244);
		ranger.attacked(new Decision(flash, Weapon.PIPE, room.player, false, null, "earlier range hit"), 123);
		flash.hp = 1;
		assertSame(flash, ranger.decide(room, 125).target);
	}

	@Test
	public void theFirstSouthElevenEyeKeepsItsService()
	{
		room.spawns[11] = 84; room.ready = 101; room.crabs.clear();
		Crab south = add(3, 84, 11, Lane.SOUTH, 'E', false, Style.MAGIC, 3296, 4244);
		ranger.decide(room, 100);
		add(4, 96, 13, Lane.WEST, 'N', false, Style.RANGE, 3286, 4249);
		assertSame(south, ranger.decide(room, 101).target);
	}

	private Crab add(int key, int born, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, RangerStrategy.tile(x, y));
		crab.pendingDamage = 0; room.crabs.add(crab); return crab;
	}
}
