package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;

public class RangerWestTwentyRecoveryTest
{
    private final RangerStrategy strategy = new RangerStrategy();

    private Room room(int version)
    {
        Room r = new Room();
        r.configureStrategy(version);
        r.wave = 20;
        r.spawns[20] = 160;
        r.spawns[19] = 148;
        r.player = RangerStrategy.tile(3293, 4248);
        r.ready = 170;
        r.observePartner(RangerStrategy.tile(3292, 4247), 172);
        r.observePartnerWeapon(31113);
        Crab west = new Crab(95, 101, 160, 20, Lane.WEST, 'S', true, Style.MELEE, RangerStrategy.tile(3290, 4249));
        west.hp = 10;
        west.pendingDamage = 0;
        r.crabs.add(west);
        strategy.attacked(new Decision(west, Weapon.BOW, r.player, false, "West bow flicker 20", "native confirmed"), 166);
        west.ownHitUntil = 0;
        // Populated transition from holdout seed11280114/t170. Held parents and
        // live children make the ordinary rollout choose small clearance first.
        String[] snapshot = {
            "72,15,54,MAGIC,true,16,3292,4244", "80,17,42,MELEE,true,16,3290,4245",
            "81,17,42,MELEE,true,16,3298,4243", "82,17,42,MELEE,true,16,3300,4251",
            "94,19,15,RANGE,false,8,3290,4252", "96,20,10,MELEE,true,0,3295,4242",
            "97,20,10,MELEE,true,16,3300,4250", "98,20,9,MAGIC,false,8,3291,4252",
            "100,20,8,MELEE,false,8,3293,4253", "101,20,6,RANGE,false,8,3294,4243",
            "102,20,3,RANGE,false,8,3291,4246", "103,20,3,MAGIC,false,8,3296,4247",
            "104,20,3,MELEE,false,8,3294,4245", "105,20,3,MELEE,false,8,3299,4243",
            "106,20,1,RANGE,false,8,3293,4252", "107,20,1,MAGIC,false,8,3294,4253"
        };
        for (String raw : snapshot)
        {
            String[] a = raw.split(",");
            int key = Integer.parseInt(a[0]), wave = Integer.parseInt(a[1]);
            boolean big = Boolean.parseBoolean(a[4]);
            Lane lane = !big ? Lane.SPLIT : key == 72 || key == 81 || key == 96 ? Lane.SOUTH
                : key == 82 || key == 97 ? Lane.EAST : Lane.WEST;
            Crab c = new Crab(key, key + 6, 170 - Integer.parseInt(a[2]), wave, lane, 'S', big,
                Style.valueOf(a[3]), RangerStrategy.tile(Integer.parseInt(a[6]), Integer.parseInt(a[7])));
            c.hp = Integer.parseInt(a[5]);
            c.dying = c.hp == 0;
            c.pendingDamage = 0;
            r.crabs.add(c);
        }
        return r;
    }

    @Test public void confirmedNonlethalBowGetsItsSafeLocalMeleeRecovery()
    {
        Room r = room(2);
        Decision hit = strategy.decide(r, 170);
        assertNotNull(hit.reason, hit.target);
        assertEquals(95, hit.target.key);
        assertEquals(Weapon.SULPHUR, hit.weapon);
    }

    @Test public void versionOneKeepsItsSmallClearance()
    {
        Decision hit = strategy.decide(room(1), 170);
        assertEquals(Weapon.PIPE, hit.weapon);
    }

    @Test public void coveredWestTwentyDoesNotConsumeAnotherAttack()
    {
        Room r = room(2);
        r.crabs.get(0).pendingDamage = r.crabs.get(0).hp;
        assertNotSame(r.crabs.get(0), strategy.decide(r, 170).target);
    }

    @Test public void earlyWestNineteenRecoveryStaysAheadOfWestTwenty()
    {
        Room r = room(2);
        Crab west19 = new Crab(91, 97, 148, 19, Lane.WEST, 'S', true, Style.MAGIC, RangerStrategy.tile(3290, 4250));
        west19.pendingDamage = 0;
        r.crabs.add(west19);
        Decision hit = strategy.decide(r, 170);
        assertSame(hit.reason, west19, hit.target);
        assertEquals(Weapon.EYE, hit.weapon);
    }

    @Test public void aLateMeleeReturnCannotDisplaceTheFirstEastTwentyOneChin()
    {
        Room r = room(2);
        r.ready = 172;
        r.crabs.get(0).observePosition(RangerStrategy.tile(3290, 4251));
        assertNotSame(r.crabs.get(0), strategy.decide(r, 172).target);
    }

    @Test public void ownNullStillPreventsWestTwentyRecovery()
    {
        Room r = room(2);
        r.crabs.get(0).nulled = true;
        assertNotSame(r.crabs.get(0), strategy.decide(r, 170).target);
    }

    @Test public void theMeleeCooldownAndReturnStillLeaveTheFirstEastChinReady()
    {
        Room r = room(2);
        Decision recovery = strategy.decide(r, 170);
        assertEquals(Weapon.SULPHUR, recovery.weapon);
        strategy.attacked(recovery, 171);
        recovery.target.pendingDamage = recovery.target.hp;
        r.ready = 170 + r.cooldown(recovery.weapon);
        r.player = RangerStrategy.tile(3301, 4248);
        r.wave = 21;
        r.spawns[21] = 176;
        Crab east = new Crab(118, 124, 176, 21, Lane.EAST, 'S', false, Style.RANGE, RangerStrategy.tile(3310, 4248));
        east.pendingDamage = 0;
        r.crabs.add(east);
        Decision chin = strategy.decide(r, 176);
        assertSame(chin.reason, east, chin.target);
        assertEquals(Weapon.CHIN, chin.weapon);
        assertEquals("East chin 21", chin.rule);
    }
}
