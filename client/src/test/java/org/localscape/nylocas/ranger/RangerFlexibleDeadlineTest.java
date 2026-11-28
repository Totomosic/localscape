package org.localscape.nylocas.ranger;

import org.junit.Test;
import static org.junit.Assert.*;
import org.localscape.nylocas.ranger.RangerStrategy.*;

public class RangerFlexibleDeadlineTest
{
    private RangerStrategy strategy = new RangerStrategy();
    private int key;
    private Room room(int wave, int spawn) {
        Room r = new Room(); r.configureStrategy(2); r.wave = wave; r.spawns[wave] = spawn;
        r.player = RangerStrategy.tile(3295,4248); r.specialEnergy=1000; return r;
    }
    private Crab magic(Room r, int wave, Lane lane, boolean big) {
        Crab c=new Crab(++key,key,100,wave,lane,(char)0,big,Style.MAGIC,RangerStrategy.tile(3295,4248));
        c.pendingDamage=0; r.crabs.add(c); return c;
    }
    private void confirmWestEleven() {
        Room r=room(11,80); Crab c=magic(r,11,Lane.WEST,true);
        Decision d=strategy.decide(r,82); assertSame(c,d.target); assertTrue(d.special);
        strategy.attacked(d,83);
    }
    @Test public void lateQuotaCanUseTheLastStoredCastInAMagicGroup() {
        confirmWestEleven(); Room r=room(18,136); r.observeTomeCharges(1);
        Crab c=magic(r,17,Lane.SPLIT,false); magic(r,18,Lane.EAST,true);
        assertTrue(strategy.tomeSpecialAvailable(r,new Decision(c,Weapon.EYE,r.player,false,null,"test"),142));
    }

    private Room deadline(int energy, int regen) {
        confirmWestEleven(); Room r=room(18,136); r.ready=142; r.specialEnergy=energy;
        r.observeSpecialRegen(regen,25); r.observeTomeCharges(1);
        Crab c=magic(r,17,Lane.SPLIT,false); c.hp=6;
        magic(r,18,Lane.EAST,true); return r;
    }
    private boolean available(Room r) {
        return strategy.tomeSpecialAvailable(r,new Decision(r.crabs.get(0),Weapon.EYE,r.player,false,null,"test"),142);
    }
    @Test public void failedNativeSeedHasEnoughThreeRealPulsesBeforeWave24() {
        Room r=deadline(700,144);
        Decision d=strategy.decide(r,142);
        assertNotNull(d.target); assertEquals(Weapon.EYE,d.weapon); assertTrue(d.special);
        assertEquals(2,r.cooldown(d.weapon));
        strategy.attacked(d,143);
        r.crabs.get(0).ownHitUntil=0;
        assertFalse(available(r));
        Room w24=room(24,200); w24.specialEnergy=500;
        Crab south=magic(w24,24,Lane.SOUTH,true);
        assertTrue(strategy.tomeSpecialAvailable(w24,new Decision(south,Weapon.EYE,w24.player,false,"South tome big 24","test"),200));
    }
    @Test public void twoPulsesCannotRecoverFromSevenHundred() {
        assertFalse(available(deadline(700,151)));
    }
    @Test public void sixHundredCannotRecoverOnTheObservedPhase() {
        assertFalse(available(deadline(600,144)));
    }
    @Test public void unknownPhaseDoesNotInventAnExtraPulse() {
        assertFalse(available(deadline(700,-1)));
    }
    @Test public void unknownPhaseCanUseOnlyGuaranteedWholePeriods() {
        assertTrue(available(deadline(800,-1)));
    }
    @Test public void invalidRegenIntervalDoesNotGrantFutureEnergy() {
        Room r=deadline(700,144); r.observeSpecialRegen(144,0); assertFalse(available(r));
    }
    @Test public void deadlineStillNeedsTwoUncoveredMagicTargets() {
        Room r=deadline(1000,144); r.crabs.get(1).pendingDamage=r.crabs.get(1).hp;
        assertFalse(available(r));
    }
    @Test public void deadlineDoesNotRepeatTheNamedWestElevenParent() {
        Room r=deadline(1000,144); r.crabs.clear();
        magic(r,11,Lane.WEST,true); magic(r,18,Lane.EAST,true);
        assertFalse(available(r));
    }
    @Test public void beforeSeventeenStoredAttacksAreStillUsedFirst() {
        Room r=deadline(1000,144); r.wave=16; r.spawns[16]=132; assertFalse(available(r));
    }
    @Test public void capPhaseDoesNotSpendTheMissingPreCapQuota() {
        Room r=deadline(1000,144); r.wave=20; r.spawns[20]=156; assertFalse(available(r));
    }
    @Test public void missingNamedWestElevenCannotSpendTheReservedSpecial() {
        Room r=room(18,136); r.ready=142; r.observeTomeCharges(1);
        magic(r,17,Lane.SPLIT,false); magic(r,18,Lane.EAST,true);
        assertFalse(available(r));
    }
    @Test public void resetClearsTheConfirmedNamedSpecial() {
        Room r=deadline(1000,144); strategy.reset(); assertFalse(available(r));
    }
}
