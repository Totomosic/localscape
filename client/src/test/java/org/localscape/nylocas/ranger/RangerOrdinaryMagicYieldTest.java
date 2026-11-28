package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerOrdinaryMagicYieldTest
{
    private final RangerStrategy strategy = new RangerStrategy();
    private Room room(boolean melee)
    {
        Room r = new Room();
        r.configureStrategy(2);
        r.wave = 5;
        r.spawns[5] = 20;
        r.player = RangerStrategy.tile(3293, 4243);
        r.observePartner(RangerStrategy.tile(3301, 4251), 36);
        Crab magic = new Crab(19, 25, 32, 5, Lane.SPLIT, (char) 0, false, Style.MAGIC, RangerStrategy.tile(3289, 4249));
        magic.pendingDamage = 0;
        r.crabs.add(magic);
        if (melee) add(r, 16, Style.MELEE, 3294, 4243);
        return r;
    }

    private Crab add(Room r, int key, Style style, int x, int y)
    {
        Crab c = new Crab(key, key + 6, 32, 5, Lane.SPLIT, (char) 0, false, style, RangerStrategy.tile(x, y));
        c.pendingDamage = 0;
        r.crabs.add(c);
        return c;
    }

    private void fill(Room r, int occupied)
    {
        while (r.crabs.size() < occupied)
        {
            Crab c = add(r, 200 + r.crabs.size(), Style.MELEE, 3300, 4253);
            c.nulled = true; // Still occupies the native cap.
        }
    }

    @Test public void nativeWaveFiveWitnessUsesNearbyMeleeWhileMageApproachesMagic()
    {
        Room r = room(true);
        fill(r, 7);
        Decision hit = strategy.decide(r, 33);
        assertNotNull(hit.reason, hit.target);
        assertEquals(16, hit.target.key);
        assertEquals(Weapon.SULPHUR, hit.weapon);
    }

    @Test public void ordinaryMagicYieldsToProductiveCrossbows()
    {
        Room r = room(false);
        Crab green = add(r, 17, Style.RANGE, 3295, 4245);
        Decision hit = strategy.decide(r, 33);
        assertSame(hit.reason, green, hit.target);
        assertEquals(Weapon.PIPE, hit.weapon);
    }

    @Test public void noProductiveAlternativeStagesForKnownWaveSixInsteadOfChasingMageWork()
    {
        Room r = room(false);
        Decision stage = strategy.decide(r, 34);
        assertNull(stage.target);
        assertNull(stage.approach);
        assertEquals(Weapon.CHIN, stage.weapon);
        assertEquals(RangerStrategy.tile(3295, 4243), stage.tile);
    }

    @Test public void capPressureRetainsUsefulOrdinaryMagic()
    {
        Room r = room(false);
        fill(r, 9);
        Decision hit = strategy.decide(r, 33);
        assertEquals(19, hit.target.key);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void imminentChildrenCountAgainstTheFourSlotMargin()
    {
        Room r = room(false);
        fill(r, 7);
        Crab parent = new Crab(13, 19, 20, 5, Lane.WEST, 'S', true, Style.RANGE, RangerStrategy.tile(3290, 4248));
        parent.hp = 0;
        parent.dying = true;
        parent.observeSplitAt(35);
        r.crabs.add(parent);
        Decision hit = strategy.decide(r, 33);
        assertEquals(19, hit.target.key);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void partnerNullRescueDoesNotYield()
    {
        Room r = room(true);
        r.crabs.get(0).partnerNulled = true;
        Decision hit = strategy.decide(r, 33);
        assertEquals(19, hit.target.key);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void aStoredChargeKeepsTheUsefulMagicShot()
    {
        Room r = room(true);
        r.observeTomeCharges(1);
        Decision hit = strategy.decide(r, 33);
        assertEquals(19, hit.target.key);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void anUnavailableMageDoesNotSuppressOrdinarySupport()
    {
        Room r = room(true);
        r.mageReady = 37;
        Decision hit = strategy.decide(r, 33);
        assertEquals(19, hit.target.key);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void versionOneRetainsTheOriginalOrdinaryCast()
    {
        Room r = room(true);
        r.configureStrategy(1);
        Decision hit = strategy.decide(r, 33);
        assertEquals(19, hit.target.key);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void namedEastNineDoesNotYieldToTheMage()
    {
        Room r = new Room();
        r.configureStrategy(2);
        r.wave = 9;
        r.spawns[9] = 100;
        r.player = RangerStrategy.tile(3295, 4248);
        r.observePartner(RangerStrategy.tile(3295, 4248), 102);
        Crab magic = new Crab(33, 39, 100, 9, Lane.EAST, 'N', false, Style.MAGIC, RangerStrategy.tile(3300, 4249));
        magic.pendingDamage = 0;
        r.crabs.add(magic);
        Decision hit = strategy.decide(r, 102);
        assertSame(hit.reason, magic, hit.target);
        assertEquals("East magic 9", hit.rule);
    }

    @Test public void anEligibleSpecialKeepsItsMagicOpportunity()
    {
        Room r = room(false);
        r.wave = 14;
        r.spawns[14] = 20;
        r.specialEnergy = 1000;
        add(r, 20, Style.MAGIC, 3297, 4248);
        add(r, 21, Style.MAGIC, 3297, 4249);
        Decision hit = strategy.decide(r, 33);
        assertNotNull(hit.reason, hit.target);
        assertEquals(Weapon.EYE, hit.weapon);
        assertTrue(hit.special);
    }
}
