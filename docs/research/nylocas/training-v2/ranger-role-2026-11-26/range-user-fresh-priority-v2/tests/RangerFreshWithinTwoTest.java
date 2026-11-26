package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerFreshWithinTwoTest {
 private Crab old,fresh;
 private Room nativeWestNine(){Room r=new Room();r.wave=9;r.spawns[9]=56;r.ready=61;r.player=RangerStrategy.tile(3299,4247);
  old=new Crab(17,23,32,5,Lane.SPLIT,(char)0,false,Style.MAGIC,RangerStrategy.tile(3299,4243));old.hp=4;old.pendingDamage=0;r.crabs.add(old);
  fresh=new Crab(34,40,56,9,Lane.WEST,'S',true,Style.RANGE,RangerStrategy.tile(3285,4248));fresh.pendingDamage=0;r.crabs.add(fresh);return r;}
 @Test public void nativeCooldownPreparesWestNineInsteadOfTheFourWavesOldEye(){Room r=nativeWestNine();Decision d=new RangerStrategy().decide(r,60);assertTrue(d.reason,d.target==fresh||d.approach==fresh);assertEquals(Weapon.PIPE,d.weapon);}
 @Test public void nativeReadyEyeFiveYieldsOneTickToTheFreshWestNinePipe(){Room r=nativeWestNine();r.player=RangerStrategy.tile(3297,4247);fresh.point=RangerStrategy.tile(3286,4248);Decision d=new RangerStrategy().decide(r,61);assertTrue(d.reason,d.target==fresh||d.approach==fresh);assertEquals(Weapon.PIPE,d.weapon);}
 @Test public void aRealNewSplitCanUseTheSameShortApproachMargin(){Room r=nativeWestNine();r.player=RangerStrategy.tile(3295,4247);old.point=RangerStrategy.tile(3295,4243);r.crabs.remove(fresh);fresh=new Crab(40,40,60,9,Lane.SPLIT,(char)0,false,Style.RANGE,RangerStrategy.tile(3306,4247));fresh.pendingDamage=0;r.crabs.add(fresh);Decision d=new RangerStrategy().decide(r,61);assertTrue(d.reason,d.target==fresh||d.approach==fresh);}
 @Test public void aLongerFreshApproachDoesNotHoldAnOrdinaryReadyAttack(){Room r=nativeWestNine();r.player=RangerStrategy.tile(3290,4247);old.point=RangerStrategy.tile(3291,4247);r.crabs.remove(fresh);fresh=new Crab(40,40,60,9,Lane.SPLIT,(char)0,false,Style.RANGE,RangerStrategy.tile(3303,4253));fresh.pendingDamage=0;r.crabs.add(fresh);Decision d=new RangerStrategy().decide(r,61);assertSame(d.reason,old,d.target);assertEquals(Weapon.EYE,d.weapon);}
 @Test public void threeWavesOldIsStillAllowedToSpendItsReadyAttack(){Room r=nativeWestNine();r.crabs.remove(old);old=new Crab(17,23,36,6,Lane.SPLIT,(char)0,false,Style.MAGIC,RangerStrategy.tile(3299,4243));old.pendingDamage=0;r.crabs.add(old);assertSame(old,new RangerStrategy().decide(r,61).target);}
 @Test public void nullLethalAndRemovedFreshWorkReleaseTheOlderShotImmediately(){for(int guard=0;guard<3;guard++){Room r=nativeWestNine();if(guard==0)fresh.nulled=true;if(guard==1)fresh.pendingDamage=16;if(guard==2)r.crabs.remove(fresh);assertSame("guard"+guard,old,new RangerStrategy().decide(r,61).target);}}
}
