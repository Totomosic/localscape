package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerCap20SmallClearanceTest {
 private final RangerStrategy strategy=new RangerStrategy();
 private Crab south,east,child;
 private Room nativeFrame() {
  Room r=new Room();r.wave=19;r.spawns[18]=140;r.spawns[19]=148;r.player=RangerStrategy.tile(3291,4246);r.ready=154;
  for(int i=0;i<3;i++)add(r,70+i,124,17,Lane.WEST,true,Style.MELEE,3299,4251);
  add(r,73,116,15,Lane.SOUTH,true,Style.MAGIC,3292,4244);
  south=add(r,84,140,18,Lane.SOUTH,true,Style.RANGE,3293,4243);east=add(r,85,140,18,Lane.EAST,true,Style.RANGE,3295,4253);
  for(int i=0;i<3;i++)add(r,90+i,148,19,Lane.values()[i],true,Style.MELEE,3295,4240);
  child=add(r,88,143,18,Lane.SPLIT,false,Style.RANGE,3292,4243);child.hp=3;
  add(r,93,152,19,Lane.SPLIT,false,Style.RANGE,3288,4248);add(r,94,152,19,Lane.SPLIT,false,Style.RANGE,3289,4249);return r;
 }
 private Crab add(Room r,int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y) {
  Crab c=new Crab(key,key+6,born,wave,lane,'S',big,style,RangerStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;
 }
 private void onlyOneRangeChild(Room r) {r.crabs.get(10).nulled=true;r.crabs.get(11).nulled=true;}
 @Test public void native154ClearsARealSmallBeforeAddingAnotherParentSplit() {
  Room r=nativeFrame();Decision d=strategy.decide(r,154);assertNotNull(d.reason,d.target);assertFalse(d.reason,d.target.big);assertTrue(d.weapon==Weapon.PIPE||d.weapon==Weapon.CHIN);
 }
 @Test public void aNonlethalChildGetsTheBoundedClearanceAttempt() {
  Room r=nativeFrame();onlyOneRangeChild(r);Decision d=strategy.decide(r,154);assertSame(d.reason,child,d.target);assertEquals(Weapon.PIPE,d.weapon);
 }
 @Test public void cooldownCanPrepareTheSameClearance() {
  Room r=nativeFrame();onlyOneRangeChild(r);Decision d=strategy.decide(r,153);assertTrue(d.reason,d.approach==child||d.target==child);
 }
 @Test public void ordinaryMagicSplitCanClearItsBodyBeforeTheCapCheck() {
  Room r=nativeFrame();onlyOneRangeChild(r);child.style=Style.MAGIC;Decision d=strategy.decide(r,154);assertSame(d.reason,child,d.target);assertEquals(Weapon.EYE,d.weapon);
 }
 @Test public void nativeOldSixteenMagicStillCountsAgainstTwentyAdmission() {
  Room r=nativeFrame();r.crabs.remove(11);r.crabs.remove(10);r.crabs.remove(9);r.player=RangerStrategy.tile(3293,4246);
  Crab old=add(r,78,124,16,Lane.SOUTH,false,Style.MAGIC,3292,4243);old.hp=7;Crab body=add(r,87,140,18,Lane.WEST,true,Style.RANGE,3292,4247);body.hp=0;body.dying=true;
  Decision d=strategy.decide(r,154);assertSame(d.reason,old,d.target);assertEquals(Weapon.EYE,d.weapon);
 }
 @Test public void anOldSmallClosingItsAttackWindowBeforeTheCapDoesNotPreempt() {
  Room r=nativeFrame();onlyOneRangeChild(r);r.crabs.remove(child);child=add(r,1,111,4,Lane.SPLIT,false,Style.MAGIC,3292,4243);assertSame(south,strategy.decide(r,154).target);
 }
 @Test public void actualParentSurvivorFollowupKeepsItsPriority() {
  Room r=nativeFrame();strategy.attacked(new Decision(south,Weapon.PIPE,r.player,false,null,"actual hit"),152);south.hp=3;assertSame(south,strategy.decide(r,154).target);
 }
 @Test public void unknownNullAndLethalChildrenCannotReleaseParentService() {
  for(int guard=0;guard<3;guard++){strategy.reset();Room r=nativeFrame();onlyOneRangeChild(r);if(guard==0)child.pendingDamage=-1;if(guard==1)child.nulled=true;if(guard==2)child.pendingDamage=child.hp;assertTrue("guard"+guard,strategy.decide(r,154).target.big);}
 }
 @Test public void earlierAndLaterPhasesKeepTheOriginalParentOrder() {
  Room r=nativeFrame();r.wave=18;assertNotSame(child,strategy.decide(r,154).target);strategy.reset();r=nativeFrame();r.ready=157;assertTrue(strategy.decide(r,157).target.big);
 }
 @Test public void admissionReleasesTheSmallClearanceTier() {
  Room r=nativeFrame();r.wave=20;r.spawns[20]=154;assertSame(south,strategy.decide(r,154).target);
 }
 @Test public void lowPopulationKeepsParentService() {
  Room r=nativeFrame();r.crabs.remove(0);r.crabs.remove(0);assertSame(south,strategy.decide(r,154).target);
 }
 @Test public void lateNineteenCanUseActualTwentyMagicRecoveryBudget() {
  Room r=nativeFrame();r.spawns[19]=152;r.ready=158;for(Crab c:r.crabs)if(c.wave==19)c.style=Style.MELEE;assertFalse(strategy.decide(r,158).target.big);
 }
 @Test public void laterSmallApproachCannotDelayTheReadyParentShot() {
  Room r=nativeFrame();onlyOneRangeChild(r);child.point=RangerStrategy.tile(3310,4243);assertSame(south,strategy.decide(r,154).target);
 }
}
