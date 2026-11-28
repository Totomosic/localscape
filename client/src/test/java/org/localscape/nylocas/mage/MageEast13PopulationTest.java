package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageEast13PopulationTest {
 private static Room room(int count) {
  Room r=new Room();r.wave=16;r.spawns[15]=116;r.spawns[16]=124;r.player=MageStrategy.tile(3296,4249);
  Crab parent=new Crab(1,1,100,13,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3297,4248));parent.pendingDamage=0;r.crabs.add(parent);
  for(int i=0;i<count;i++){Crab c=new Crab(i+2,i+2,118,15,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3295,4249+i));c.pendingDamage=0;r.crabs.add(c);}return r;
 }
 @Test public void clearsAUsefulSmallWhileMoreThanTwoRemain() {Room r=room(3);Decision d=new MageStrategy().decide(r,124);assertNotNull(d.target);assertFalse(d.target.big);}
 @Test public void opensTheParentAtTwoSmallSurvivors() {Room r=room(2);assertSame(r.crabs.get(0),new MageStrategy().decide(r,124).target);}
 @Test public void knownLethalSmallCoverageCountsTowardTheRelease() {Room r=room(3);r.crabs.get(1).pendingDamage=8;assertSame(r.crabs.get(0),new MageStrategy().decide(r,124).target);}
 @Test public void unknownFlightDoesNotCountAsConfirmedSmallCoverage() {Room r=room(3);Crab small=r.crabs.get(1);small.pendingDamage=-1;small.ownHitUntil=126;small.ownHitHp=8;Decision d=new MageStrategy().decide(r,124);assertNotNull(d.target);assertFalse(d.target.big);}
 @Test public void doesNotStandIdleWhenTheSmallPopulationCannotBeServed() {Room r=room(3);for(int i=1;i<r.crabs.size();i++)r.crabs.get(i).nulled=true;assertSame(r.crabs.get(0),new MageStrategy().decide(r,124).target);}
 @Test public void immediatelyFinishesAWoundedParentDespiteThePopulation() {Room r=room(3);r.crabs.get(0).hp=4;assertSame(r.crabs.get(0),new MageStrategy().decide(r,124).target);}
 @Test public void keepsClearingACrowdedSixteenUntilTheBoundedOpeningEdge() {Room r=room(3);assertFalse(new MageStrategy().decide(r,131).target.big);}
 @Test public void keepsTheHardLateRecoveryAtAgeThirtyFive() {Room r=room(3);assertSame(r.crabs.get(0),new MageStrategy().decide(r,135).target);}
 @Test public void opensOnActualSeventeenEvenWithUsefulSmalls() {Room r=room(3);r.wave=17;r.spawns[17]=128;assertSame(r.crabs.get(0),new MageStrategy().decide(r,128).target);}
 @Test public void clearsAReachableMeleeBeforeEarlyOpening() {Room r=room(3);for(int i=1;i<r.crabs.size();i++){r.crabs.get(i).style=Style.MELEE;r.crabs.get(i).point=MageStrategy.tile(3293,4248);}assertFalse(new MageStrategy().decide(r,124).target.big);}
}