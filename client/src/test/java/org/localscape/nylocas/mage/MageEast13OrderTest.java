package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MageEast13OrderTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private int key;

	private Crab crab(int born, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab body = new Crab(++key, key, born, wave, lane, slot, big, style, MageStrategy.tile(x, y));
		body.pendingDamage = 0;
		room.crabs.add(body);
		return body;
	}

	private Crab roomWithParent(int wave)
	{
		room.wave = wave;
		room.spawns[13] = 96;
		room.spawns[15] = 112;
		room.player = MageStrategy.tile(3294, 4249);
		return crab(96, 13, Lane.EAST, 'S', true, Style.MELEE, 3295, 4249);
	}

	private Crab eastMagic()
	{
		return crab(112, 15, Lane.EAST, 'N', false, Style.MAGIC, 3305, 4249);
	}

	private Crab eastRange()
	{
		return crab(112, 15, Lane.EAST, 'S', false, Style.RANGE, 3305, 4248);
	}

	private Crab filler()
	{
		return crab(112, 15, Lane.SPLIT, (char) 0, false, Style.MELEE, 3294, 4250);
	}

	@Test
	public void holdsTheParentWhileTheLastEastFifteenSmallCanStillSurvive()
	{
		roomWithParent(15);
		Crab magic = eastMagic();
		magic.pendingDamage = magic.hp;
		Crab range = eastRange();
		range.pendingDamage = range.hp - 1;
		Crab other = filler();

		assertSame(other, strategy.decide(room, 118).target);
	}

	@Test
	public void opensImmediatelyOnceBothEastSmallsAreCovered()
	{
		Crab parent = roomWithParent(15);
		Crab magic = eastMagic();
		magic.pendingDamage = magic.hp;
		Crab range = eastRange();
		range.pendingDamage = range.hp;
		filler();

		Decision decision = strategy.decide(room, 118);
		assertSame(parent, decision.target);
		assertEquals(Weapon.SCYTHE, decision.weapon);
	}

	@Test
	public void treatsRemovedAndDyingEastSmallsAsCleared()
	{
		Crab parent = roomWithParent(16);
		room.crabs.remove(eastMagic());
		Crab range = eastRange();
		range.hp = 0;
		range.dying = true;
		filler();

		assertSame(parent, strategy.decide(room, 122).target);
	}

	@Test
	public void releasesTheParentBeforeItsExpiryIfEastFifteenIsBehind()
	{
		Crab parent = roomWithParent(17);
		room.spawns[17] = 124;
		eastMagic();
		eastRange();
		Crab other = filler();

		assertSame(other, strategy.decide(room, 126).target);
		assertSame(parent, strategy.decide(room, 127).target);
	}

	@Test
	public void retriesTheOpenedParentEvenIfTheUncoveredEastSmallStillLives()
	{
		Crab parent = roomWithParent(16);
		Crab magic = eastMagic();
		Crab range = eastRange();
		magic.pendingDamage = magic.hp;
		range.pendingDamage = range.hp;
		Decision opening = strategy.decide(room, 122);
		assertSame(parent, opening.target);
		strategy.attacked(opening, 123, 122);
		room.ready = 127;
		parent.hp = 4;
		filler();

		assertSame(parent, strategy.decide(room, 127).target);
	}

	@Test
	public void recoversAnAdjacentParentInEarlyEighteenIfNineteenStillFits()
	{
		Crab parent = roomWithParent(18);
		room.spawns[18] = 136;
		room.ready = 139;

		assertSame(parent, strategy.decide(room, 139).target);
	}

	@Test
	public void retriesTheObservedNineHpParentAtAgeFortyThree()
	{
		// Packed-map seed 144042924228274 at tick 135, after two low Scythe hits.
		room.wave = 17;
		room.spawns[13] = 92;
		room.spawns[15] = 112;
		room.spawns[17] = 124;
		room.ready = 135;
		room.player = MageStrategy.tile(3293, 4247);
		Crab parent = crab(92, 13, Lane.EAST, 'S', true, Style.MELEE, 3295, 4246);
		parent.hp = 9;
		crab(112, 15, Lane.WEST, 'S', false, Style.RANGE, 3292, 4253);
		strategy.attacked(new Decision(parent, Weapon.SCYTHE, room.player, false,
			"Deferred east big 13", "opening"), 126, 125);

		Decision retry = strategy.decide(room, 135);
		assertSame(parent, retry.target);
		assertEquals(Weapon.SCYTHE, retry.weapon);
	}

	@Test
	public void rejectsAFollowupThatWouldSpendTheFirstWaveNineteenCooldown()
	{
		Crab parent = roomWithParent(18);
		room.spawns[18] = 136;
		room.ready = 143;
		strategy.attacked(new Decision(parent, Weapon.SCYTHE, room.player, false,
			"Deferred east big 13", "opening"), 139, 138);
		filler();

		Decision wait = strategy.decide(room, 143);
		assertNull(wait.target);
		assertTrue(wait.reason, wait.reason.startsWith("Wait for fresh crab"));
		assertEquals(Weapon.EYE, wait.weapon);
	}
}
