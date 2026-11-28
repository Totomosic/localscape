package org.localscape.nylocas.mage;

import org.junit.Before;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;
import static org.localscape.nylocas.mage.MageStrategy.tile;

public class MageDelayedTwentyTwoContactTest
{
	private final MageStrategy mage = new MageStrategy();
	private final Room room = new Room();
	private Crab eastBig;
	private Crab southSmall;

	@Before
	public void clearASouthBlueWhileTheEastBigApproaches()
	{
		room.wave = 22;
		room.spawns[22] = 188;
		room.specialEnergy = 1000;
		room.ready = 199;
		room.player = tile(3292, 4247);
		Crab blue = add(111, 180, 21, Lane.SPLIT, 'S', false, Style.MAGIC, 3292, 4243);
		Crab westBig = add(126, 188, 22, Lane.WEST, 'S', true, Style.MELEE, 3290, 4246);
		southSmall = add(127, 188, 22, Lane.SOUTH, 'W', false, Style.MELEE, 3294, 4243);
		eastBig = add(129, 188, 22, Lane.EAST, 'S', true, Style.MELEE, 3298, 4247);
		eastBig.fighting = true;
		add(300, 180, 21, Lane.SPLIT, 'S', false, Style.MELEE, 3299, 4249);

		Decision clear = mage.decide(room, 199);
		assertSame(blue, clear.target);
		assertEquals(Weapon.EYE, clear.weapon);
		mage.attacked(clear, 200, 199);
		blue.pendingDamage = 35;

		room.wave = 23;
		room.spawns[23] = 200;
		room.ready = 202;
		room.player = tile(3294, 4244);
		westBig.observePosition(tile(3290, 4245));
		southSmall.observePosition(tile(3293, 4243));
		eastBig.observePosition(tile(3297, 4247));
		eastBig.observePosition(tile(3296, 4246));
	}

	@Test
	public void takesOneGroundStepThenSwingsOnTheObservedBig()
	{
		reachWestFace();
		Decision sweep = mage.decide(room, 203);
		assertSame(eastBig, sweep.target);
		assertEquals("22 south Dinh", sweep.rule);
		assertTrue(sweep.special);
	}

	@Test
	public void firesImmediatelyWhenItsTileAlreadyMeetsTheIncomingBig()
	{
		eastBig.observePosition(tile(3296, 4247));
		eastBig.observePosition(tile(3295, 4246));
		Decision sweep = mage.decide(room, 202);
		assertEquals("22 south Dinh", sweep.rule);
		assertTrue(sweep.special);
	}

	@Test
	public void sparesMagicCollateralThatOnlyTheMageCanStillKill()
	{
		reachWestFace();
		add(400, 201, 23, Lane.SPLIT, 'S', false, Style.MAGIC, 3290, 4248).partnerNulled = true;
		Decision next = mage.decide(room, 203);
		assertFalse(next.weapon == Weapon.DINH && next.special);
	}

	@Test
	public void keepsTheOriginalClosingTickEvenAfterLosingTheContact()
	{
		reachWestFace();
		assertNotEquals("22 south Dinh", mage.decide(room, 204).rule);
	}

	@Test
	public void rechecksSpecialEnergyAfterMoving()
	{
		reachWestFace();
		room.specialEnergy = 499;
		assertFalse(mage.decide(room, 203).special);
	}

	@Test
	public void hasTimeToStaffSouthAndScytheEastOnWaveTwentyFour()
	{
		reachWestFace();
		Decision sweep = mage.decide(room, 203);
		mage.attacked(sweep, 204, 203);
		room.crabs.clear();
		room.wave = 24;
		room.spawns[24] = 208;
		room.ready = 208;
		room.player = tile(3295, 4243);
		Crab south = add(500, 208, 24, Lane.SOUTH, 'W', true, Style.MAGIC, 3295, 4234);
		Crab east = add(501, 208, 24, Lane.EAST, 'S', true, Style.MELEE, 3309, 4248);
		Decision eye = mage.decide(room, 208);
		assertSame(south, eye.target);
		assertEquals(Weapon.EYE, eye.weapon);
		mage.attacked(eye, 209, 208);
		south.pendingDamage = 39;
		room.ready = 211;
		room.player = tile(3301, 4248);
		east.observePosition(tile(3302, 4248));
		Decision scythe = mage.decide(room, 214);
		assertSame(east, scythe.target);
		assertEquals(Weapon.SCYTHE, scythe.weapon);
	}

	private void reachWestFace()
	{
		Decision ground = mage.decide(room, 202);
		assertEquals(tile(3294, 4245), ground.tile);
		assertNull(ground.target);
		assertNull(ground.approach);
		assertFalse(ground.special);
		room.player = ground.tile;
		eastBig.observePosition(tile(3295, 4245));
		southSmall.observePosition(tile(3294, 4243));
	}

	private Crab add(int key, int born, int wave, Lane lane, char slot, boolean big, Style style, int x, int y)
	{
		Crab crab = new Crab(key, key, born, wave, lane, slot, big, style, tile(x, y));
		room.crabs.add(crab);
		return crab;
	}
}
