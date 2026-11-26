package org.localscape.nylocas.mage;

import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.Crab;
import org.localscape.nylocas.mage.MageStrategy.Decision;
import org.localscape.nylocas.mage.MageStrategy.Lane;
import org.localscape.nylocas.mage.MageStrategy.Room;
import org.localscape.nylocas.mage.MageStrategy.Style;
import org.localscape.nylocas.mage.MageStrategy.Weapon;

import static org.junit.Assert.*;

public class MageCleanupSweepTest
{
    private final MageStrategy mage = new MageStrategy();
    private final Room room = new Room();
    private final int tick = 290;

    public MageCleanupSweepTest()
    {
        room.wave = 31;
        room.spawns[28] = 240;
        room.spawns[29] = 256;
        room.spawns[30] = 264;
        room.spawns[31] = 272;
        room.specialEnergy = 500;
        room.ready = tick;
        room.player = MageStrategy.tile(3296, 4249);
    }

    private Crab small(int born, Style style, int x, int y)
    {
        int key = room.crabs.size() + 1;
        Crab c = new Crab(key, key, born, 31, Lane.SPLIT, (char) 0, false, style, MageStrategy.tile(x, y));
        c.pendingDamage = 0;
        room.crabs.add(c);
        return c;
    }

    @Test
    public void sweepsTwoUncoveredFreshMeleesFromCardinalContact()
    {
        Crab primary = small(278, Style.MELEE, 3296, 4250);
        small(280, Style.MELEE, 3298, 4250);
        Decision decision = mage.decide(room, tick);
        assertSame(primary, decision.target);
        assertEquals(Weapon.DINH, decision.weapon);
        assertTrue(decision.special);
    }

    @Test
    public void lethalFlightsAndDimmedSmallsDoNotCountTowardsTheSweep()
    {
        small(279, Style.MELEE, 3296, 4250);
        Crab covered = small(280, Style.MELEE, 3298, 4250);
        covered.pendingDamage = covered.hp;
        small(256, Style.MELEE, 3297, 4248);
        assertNotEquals(Weapon.DINH, mage.decide(room, tick).weapon);
    }

    @Test
    public void preservesFiniteEnergyAndThePartnersNulledMagic()
    {
        small(279, Style.MELEE, 3296, 4250);
        small(280, Style.MELEE, 3298, 4250);
        room.specialEnergy = 499;
        assertNotEquals(Weapon.DINH, mage.decide(room, tick).weapon);
        room.specialEnergy = 500;
        small(278, Style.MAGIC, 3297, 4250).partnerNulled = true;
        assertNotEquals(Weapon.DINH, mage.decide(room, tick).weapon);
    }
}
