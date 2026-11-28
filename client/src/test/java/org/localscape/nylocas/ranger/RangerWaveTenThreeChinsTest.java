package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerWaveTenThreeChinsTest
{
    private Crab crab(Room room, int key, int born, int wave, Lane lane, char slot, boolean big,
        int x, int y, int pending)
    {
        Crab c = new Crab(key, key + 6, born, wave, lane, slot, big, Style.RANGE, RangerStrategy.tile(x, y));
        c.pendingDamage = pending;
        room.crabs.add(c);
        return c;
    }

    private Room afterWest(RangerStrategy strategy, int version)
    {
        Room r = new Room();
        r.configureStrategy(version);
        r.wave = 10;
        r.spawns[10] = 68;
        r.player = RangerStrategy.tile(3291, 4248);
        r.ready = 71;
        r.specialEnergy = 1000;
        Crab west = crab(r, 38, 68, 10, Lane.WEST, 'N', false, 3282, 4249, 8);
        crab(r, 39, 68, 10, Lane.WEST, 'S', false, 3282, 4248, 8);
        crab(r, 40, 68, 10, Lane.SOUTH, 'W', false, 3295, 4234, 0);
        crab(r, 41, 68, 10, Lane.SOUTH, 'E', false, 3296, 4234, 0);
        crab(r, 42, 68, 10, Lane.EAST, 'N', false, 3309, 4249, 0);
        crab(r, 43, 68, 10, Lane.EAST, 'S', true, 3308, 4248, 0);
        crab(r, 36, 63, 9, Lane.SPLIT, (char) 0, false, 3290, 4250, 0);
        strategy.attacked(new Decision(west, Weapon.CHIN, r.player, false, "West chin 10", "native confirmed"), 69);
        return r;
    }

    @Test public void versionTwoStagesEastInsteadOfSpendingTheSouthWindowOnASparePipe()
    {
        RangerStrategy s = new RangerStrategy();
        Decision stage = s.decide(afterWest(s, 2), 69);
        assertEquals(Weapon.CHIN, stage.weapon);
        assertEquals("East chin 10", stage.rule);
        assertEquals(RangerStrategy.tile(3297, 4248), stage.tile);
    }

    @Test public void versionOneKeepsItsOptionalSparePipe()
    {
        RangerStrategy s = new RangerStrategy();
        Decision stage = s.decide(afterWest(s, 1), 69);
        assertEquals(Weapon.PIPE, stage.weapon);
        assertTrue(stage.reason, stage.reason.startsWith("Prepare spare Pipe"));
    }

    @Test public void eastThenSouthFitBeforeWestElevenAndEastBigStaysInTheChin()
    {
        RangerStrategy s = new RangerStrategy();
        Room r = afterWest(s, 2);
        s.decide(r, 69);
        r.player = RangerStrategy.tile(3295, 4248);
        for (Crab c : r.crabs)
        {
            if (c.lane == Lane.EAST) c.observePosition(c.point.dx(-2));
            if (c.lane == Lane.SOUTH) c.observePosition(c.point.dy(2));
        }
        Decision east = s.decide(r, 71);
        assertNotNull(east.reason, east.target);
        assertEquals(42, east.target.key);
        assertEquals("East chin 10", east.rule);
        assertEquals(Weapon.CHIN, east.weapon);
        Crab eastBig = r.crabs.stream().filter(c -> c.big).findFirst().get();
        assertTrue(eastBig.point.distanceTo(east.target.point) <= 1);
        s.attacked(east, 72);
        r.ready = 74;
        r.player = RangerStrategy.tile(3297, 4247);
        for (Crab c : r.crabs)
        {
            if (c.lane == Lane.EAST) c.pendingDamage = c.hp;
            if (c.lane == Lane.SOUTH) c.observePosition(c.point.dy(3));
        }
        Decision south = s.decide(r, 74);
        assertNotNull(south.reason, south.target);
        assertEquals("South chin 10", south.rule);
        assertEquals(Weapon.CHIN, south.weapon);
        s.attacked(south, 75);
        r.ready = 77;
        r.wave = 11;
        r.spawns[11] = 76;
        r.player = RangerStrategy.tile(3293, 4248);
        Crab west11 = new Crab(46, 52, 76, 11, Lane.WEST, 'S', true, Style.MAGIC, RangerStrategy.tile(3282, 4248));
        west11.pendingDamage = 0;
        r.crabs.add(west11);
        Decision tome = s.decide(r, 77);
        assertSame(tome.reason, west11, tome.target);
        assertEquals(Weapon.EYE, tome.weapon);
        assertTrue(tome.special);
    }
}
