package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
/** Companion to Mage's bounded22Dinh age11..13 nativefood recovery. */
public class RangerMage22RecoveryProtectionTest {
 private Crab target;
 private Room room(char slot){Room r=new Room();r.wave=22;r.spawns[22]=180;r.ready=192;r.player=RangerStrategy.tile(3295,4244);target=new Crab(127,127,180,22,Lane.SOUTH,slot,false,Style.MELEE,r.player);target.pendingDamage=0;r.crabs.add(target);return r;}
 @Test public void aTwentyThreeAdmissionRetryCannotReleaseTheRecoverableMagePrimaryAtTwelve(){Room r=room('W');Decision d=new RangerStrategy().decide(r,192);assertNotSame(d.reason,target,d.target);}
 @Test public void mageNullReleasesThePrimaryButOwnNullStillWins(){Room r=room('W');target.partnerNulled=true;assertSame(target,new RangerStrategy().decide(r,192).target);target.nulled=true;assertNotSame(target,new RangerStrategy().decide(r,192).target);}
 @Test public void theOtherSouthSmallRemainsNormalLocalFallback(){Room r=room('E');assertSame(target,new RangerStrategy().decide(r,192).target);}
 @Test public void afterTheRecoveryWindowTheOldMeleeSmallCanBeHelped(){Room r=room('W');r.wave=23;r.spawns[23]=192;r.ready=194;assertSame(target,new RangerStrategy().decide(r,194).target);target.pendingDamage=target.hp;assertNotSame(target,new RangerStrategy().decide(r,194).target);}
}
