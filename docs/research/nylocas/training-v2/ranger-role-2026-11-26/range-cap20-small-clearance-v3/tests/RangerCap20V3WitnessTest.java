package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerCap20V3WitnessTest {
 private Crab add(Room r,int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y,int hp){Crab c=new Crab(key,key,born,wave,lane,'S',big,style,RangerStrategy.tile(x,y));c.hp=hp;c.pendingDamage=0;r.crabs.add(c);return c;}
 private Room room(int count) {Room r=new Room();r.wave=19;r.spawns[18]=140;r.spawns[19]=148;r.ready=154;r.player=RangerStrategy.tile(3293,4248);for(int i=0;i<count;i++)add(r,20+i,120,17,Lane.WEST,true,Style.MELEE,3299,4251,16);return r;}
 @Test public void nativeHighPopulationTwoHpMagicCanPrecedeThreeFullUnopenedParents(){
  // seed214365853148778/154: population13, three18parentsHP16, old17split83HP2.
  Room r=room(7);add(r,85,140,18,Lane.WEST,true,Style.RANGE,3292,4248,16);add(r,86,140,18,Lane.SOUTH,true,Style.RANGE,3293,4243,16);add(r,87,140,18,Lane.EAST,true,Style.RANGE,3295,4253,16);
  Crab magic=add(r,83,139,17,Lane.SPLIT,false,Style.MAGIC,3296,4248,2);add(r,84,139,17,Lane.SPLIT,false,Style.MAGIC,3297,4249,8);add(r,88,144,18,Lane.SPLIT,false,Style.MELEE,3295,4245,8);
  Decision d=new RangerStrategy().decide(r,154);assertSame(d.reason,magic,d.target);assertEquals(Weapon.EYE,d.weapon);
 }
 @Test public void nativeTwoHpWestEighteenUsesAgeFiveInsteadOfWaitingForPipeSeven(){
  // seed214365853148787/153: BOW0 left2HP, readyMAGIC5, no smalls.
  Room r=new Room();r.wave=18;r.spawns[18]=148;r.ready=153;r.player=RangerStrategy.tile(3293,4248);Crab west=add(r,87,148,18,Lane.WEST,true,Style.MAGIC,3286,4248,2);
  Decision d=new RangerStrategy().decide(r,153);assertSame(d.reason,west,d.target);assertEquals(Weapon.EYE,d.weapon);
 }
 @Test public void nativeImminentWestChildKeepsTheFinalAgeSevenRemovalOpportunity(){
  // seed214365853148815: West18splits+7, PIPE+6wouldforcechildshot+8, too late.
  Room r=room(7);Crab body=add(r,85,140,18,Lane.WEST,true,Style.RANGE,3290,4248,0);body.dying=true;add(r,86,140,18,Lane.SOUTH,true,Style.RANGE,3293,4243,16);add(r,87,140,18,Lane.EAST,true,Style.RANGE,3295,4253,16);Crab smallBody=add(r,50,120,15,Lane.SPLIT,false,Style.MELEE,3295,4243,0);smallBody.dying=true;
  RangerStrategy strategy=new RangerStrategy();Decision hold=strategy.decide(r,154);assertNull(hold.reason,hold.target);assertEquals("Reserve imminent cap children",hold.reason);
  r.crabs.remove(body);Crab range=add(r,92,155,19,Lane.SPLIT,false,Style.RANGE,3290,4248,8);add(r,91,155,19,Lane.SPLIT,false,Style.MELEE,3291,4249,8);Decision clear=strategy.decide(r,155);assertSame(clear.reason,range,clear.target);assertEquals(Weapon.PIPE,clear.weapon);
 }
 @Test public void ordinaryAgeEightSmallPriorityCannotAssumeAHitReleasesCapByTwelve(){
  Room r=room(9);Crab south=add(r,86,140,18,Lane.SOUTH,true,Style.RANGE,3293,4243,16);add(r,87,140,18,Lane.EAST,true,Style.RANGE,3295,4253,16);add(r,92,152,19,Lane.SPLIT,false,Style.RANGE,3292,4243,3);r.ready=156;assertSame(south,new RangerStrategy().decide(r,156).target);
 }
 @Test public void theImminentHoldExpiresAfterOneTickEvenIfTheBodyPersists(){
  Room r=room(9);Crab body=add(r,85,140,18,Lane.WEST,true,Style.RANGE,3290,4248,0);body.dying=true;Crab south=add(r,86,140,18,Lane.SOUTH,true,Style.RANGE,3293,4243,16);add(r,87,140,18,Lane.EAST,true,Style.RANGE,3295,4253,16);RangerStrategy strategy=new RangerStrategy();assertNull(strategy.decide(r,154).target);r.ready=155;assertSame(south,strategy.decide(r,155).target);
 }
 @Test public void aKnownSurvivingParentCannotBeReplacedByTheHold(){
  Room r=room(9);Crab body=add(r,85,140,18,Lane.WEST,true,Style.RANGE,3290,4248,0);body.dying=true;Crab south=add(r,86,140,18,Lane.SOUTH,true,Style.RANGE,3293,4243,3);RangerStrategy strategy=new RangerStrategy();strategy.attacked(new Decision(south,Weapon.PIPE,r.player,false,null,"real previoushit"),152);assertSame(south,strategy.decide(r,154).target);
 }
}
