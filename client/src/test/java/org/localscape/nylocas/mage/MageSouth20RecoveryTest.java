package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;

public class MageSouth20RecoveryTest
{
	private final MageStrategy strategy = new MageStrategy();
	private final Room room = new Room();
	private Crab south, north;

	private void afterBarrage()
	{
		room.wave = 21;
		room.spawns[21] = 168;
		room.ready = 173;
		room.specialEnergy = 500;
		room.player = MageStrategy.tile(3295, 4248);
		south = new Crab(96, 96, 152, 20, Lane.SOUTH, 'W', true, Style.MELEE, MageStrategy.tile(3294, 4246));
		north = new Crab(103, 103, 168, 21, Lane.WEST, 'N', false, Style.MELEE, MageStrategy.tile(3294, 4251));
		south.pendingDamage = north.pendingDamage = 0;
		room.crabs.add(south);
		room.crabs.add(north);
		strategy.attacked(new Decision(north, Weapon.BARRAGE, room.player, false, "21 west barrage", "barrage"), 169, 168);
	}

	@Test
	public void usesTheDocumentedSouthTwentyScytheAfterTheConfirmedBarrage()
	{
		afterBarrage();
		Decision attack = strategy.decide(room, 173);
		assertSame(south, attack.target);
		assertEquals(Weapon.SCYTHE, attack.weapon);
		assertEquals("21 South20 scythe", attack.rule);
		strategy.attacked(attack, 174, 173);
		south.hp = 0;
		south.dying = true;
		room.ready = 178;
		assertNotEquals("21 north sulphur", strategy.decide(room, 178).rule);
	}

	@Test
	public void aProposedScytheDoesNotConsumeTheNorthReservation()
	{
		afterBarrage();
		assertSame(south, strategy.decide(room, 173).target);
		south.pendingDamage = south.hp;
		Decision northShot = strategy.decide(room, 174);
		assertSame(north, northShot.target);
		assertEquals("21 north sulphur", northShot.rule);
	}

	@Test
	public void coveredNulledBlockedAndUnreachableParentsKeepTheNorthShot()
	{
		for (int mode = 0; mode < 4; mode++)
		{
			strategy.reset();
			room.crabs.clear();
			room.obstructed.clear();
			afterBarrage();
			if (mode == 0) south.pendingDamage = south.hp;
			if (mode == 1) south.nulled = true;
			if (mode == 2) room.obstructed.add(south.key);
			if (mode == 3) south.point = MageStrategy.tile(3301, 4243);
			assertSame(north, strategy.decide(room, 173).target);
		}
	}

	@Test
	public void keepsTheFourTickNorthFollowupWhenScytheWouldMissDinh()
	{
		afterBarrage();
		room.spawns[22] = 168;
		assertSame(north, strategy.decide(room, 173).target);
	}
}
