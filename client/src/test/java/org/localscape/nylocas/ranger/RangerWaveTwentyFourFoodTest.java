package org.localscape.nylocas.ranger;

import org.junit.Test;
import static org.junit.Assert.*;
import org.localscape.nylocas.ranger.RangerStrategy.*;

public class RangerWaveTwentyFourFoodTest
{
    private Room room() {
        Room r=new Room(); r.configureStrategy(2); r.wave=24; r.specialEnergy=500;
        r.player=RangerStrategy.tile(3294,4247); return r;
    }
    private Decision special(Room r, Lane lane) {
        Crab c=new Crab(137,137,208,24,lane,(char)0,true,Style.MAGIC,RangerStrategy.tile(3295,4239));
        c.pendingDamage=0; r.crabs.add(c);
        return new Decision(c,Weapon.EYE,r.player,true,lane==Lane.WEST?"West tome big 24":"South tome big 24","test");
    }
    private boolean defer(Room r, Decision d) {
        return r.deferWaveTwentyFourFood(d,true,true,59,53,0,214);
    }
    @Test public void nativeFoodWitnessLaunchesSouthSpecialAt214InsteadOfEating() {
        Room r=room(); assertTrue(defer(r,special(r,Lane.SOUTH)));
    }
    @Test public void namedWestSpecialCanLaunchBeforeEatingToo() {
        Room r=room(); assertTrue(defer(r,special(r,Lane.WEST)));
    }
    @Test public void ordinaryFollowupReevaluatesFoodImmediately() {
        Room r=room(); Decision d=special(r,Lane.SOUTH);
        assertFalse(defer(r,new Decision(d.target,d.weapon,d.tile,false,d.rule,d.reason)));
    }
    @Test public void queuedDamagePreservesTheExistingFiftyFiveHpFloor() {
        Room r=room(); Decision d=special(r,Lane.SOUTH);
        assertTrue(r.deferWaveTwentyFourFood(d,true,true,59,53,4,214));
        assertFalse(r.deferWaveTwentyFourFood(d,true,true,59,53,5,214));
    }
    @Test public void lowHealthAlwaysEats() {
        Room r=room(); assertFalse(r.deferWaveTwentyFourFood(special(r,Lane.SOUTH),true,true,54,53,0,214));
    }
    @Test public void depletedPrayerAlwaysEats() {
        Room r=room(); assertFalse(r.deferWaveTwentyFourFood(special(r,Lane.SOUTH),true,true,59,0,0,214));
    }
    @Test public void attackCooldownDoesNotDeferFood() {
        Room r=room(); assertFalse(r.deferWaveTwentyFourFood(special(r,Lane.SOUTH),false,true,59,53,0,214));
    }
    @Test public void nativeActionDelayDoesNotDeferFood() {
        Room r=room(); assertFalse(r.deferWaveTwentyFourFood(special(r,Lane.SOUTH),true,false,59,53,0,214));
    }
    @Test public void originalVersionKeepsItsFoodTiming() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); r.configureStrategy(1); assertFalse(defer(r,d));
    }
    @Test public void waveTwentyFiveDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); r.wave=25; assertFalse(defer(r,d));
    }
    @Test public void anotherNamedSpecialCannotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH);
        assertFalse(defer(r,new Decision(d.target,d.weapon,d.tile,true,"West tome big 11",d.reason)));
    }
    @Test public void obstructedTargetDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); r.obstructed.add(d.target.key); assertFalse(defer(r,d));
    }
    @Test public void unreachableTargetDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); r.player=RangerStrategy.tile(3294,4254); assertFalse(defer(r,d));
    }
    @Test public void coveredTargetDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); d.target.pendingDamage=d.target.hp; assertFalse(defer(r,d));
    }
    @Test public void DyingOrNullTargetDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); d.target.dying=true; assertFalse(defer(r,d));
        d.target.dying=false; d.target.nulled=true; assertFalse(defer(r,d));
    }
    @Test public void insufficientSpecialEnergyDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH); r.specialEnergy=499; assertFalse(defer(r,d));
    }

    @Test public void unknownPartnerFlightDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH);
        d.target.pendingDamage=-1; d.target.claimedUntil=215; assertFalse(defer(r,d));
        d.target.claimedUntil=214; assertTrue(defer(r,d));
    }
    @Test public void unknownOwnFlightDoesNotDeferFood() {
        Room r=room(); Decision d=special(r,Lane.SOUTH);
        d.target.pendingDamage=-1; d.target.ownHitUntil=215; d.target.ownHitHp=d.target.hp;
        assertFalse(defer(r,d));
    }
    @Test public void knownNonlethalDamageReleasesBothFlightLeases() {
        Room r=room(); Decision d=special(r,Lane.SOUTH);
        d.target.pendingDamage=3; d.target.claimedUntil=215;
        d.target.ownHitUntil=215; d.target.ownHitHp=d.target.hp; assertTrue(defer(r,d));
    }
}
