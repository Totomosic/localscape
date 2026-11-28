package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.Tile;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerCleanupSplitFiringStageTest
{
 private final RangerStrategy ranger = new RangerStrategy();
 private final Room room = new Room();
 private final Crab green, parent;
 public RangerCleanupSplitFiringStageTest()
 {
  room.wave=31; room.spawns[29]=260; room.spawns[31]=280; room.cleanup=true;
  room.ready=300; room.player=RangerStrategy.tile(3296,4247);
  room.magePosition=RangerStrategy.tile(3300,4247); room.mageReady=299;
  green=add(1,290,31,Lane.SPLIT,false,Style.RANGE,3299,4244);
  parent=add(2,247,27,Lane.WEST,true,Style.MAGIC,3290,4251);
 }
 @Test public void keepTheReadyPipeWhileMovingTowardTheObservableNaturalSplit()
 {
  Decision d=ranger.decide(room,298);
  assertTrue(d.reason,d.reason.startsWith("Stage for cleanup split"));
  assertEquals(Weapon.PIPE,d.weapon); assertNull(d.target); assertNull(d.approach);
  assertTrue(d.tile.distanceTo(parent.point)+2<=room.player.distanceTo(parent.point));
  assertTrue(d.tile.distanceTo(green.point)<=Weapon.PIPE.range);
  room.player=d.tile;
  assertSame(green,ranger.decide(room,300).target);
 }
 @Test public void anAlreadyReadyPipeDoesNotWaitForThePreparation()
 {
  assertSame(green,ranger.decide(room,300).target);
 }
 @Test public void aMageAlreadyCoveringTheArrivalKeepsTheOrdinaryFiringPlan()
 {
  room.magePosition=RangerStrategy.tile(3291,4250);
  assertFalse(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void theMagesObservedRecoveryCanLeaveThatArrivalForTheRanger()
 {
  room.magePosition=RangerStrategy.tile(3291,4250); room.mageReady=310;
  assertTrue(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void anEyeRetainsTheShorterProjectileInsteadOfCrossingOutOfItsTwoTileRay()
 {
  green.style=Style.MAGIC;
  Decision d=ranger.decide(room,298);
  assertFalse(d.reason.startsWith("Stage for cleanup split"));
  assertTrue(d.reason.startsWith("Close during cleanup cooldown"));
  assertTrue(d.tile.distanceTo(green.point)<=2);
 }
 @Test public void anEyeCanMoveTowardTheSplitWithinItsSameShortProjectileRay()
 {
  green.style=Style.MAGIC; green.point=RangerStrategy.tile(3294,4248);
  Decision d=ranger.decide(room,298);
  assertTrue(d.reason,d.reason.startsWith("Stage for cleanup split"));
  assertTrue(d.tile.distanceTo(green.point)<=2);
  room.player=d.tile; assertSame(green,ranger.decide(room,300).target);
 }
 @Test public void aMovingOptionalParentDoesNotInventAStaticBirthAnchor()
 {
  parent.observePosition(RangerStrategy.tile(3291,4251));
  assertFalse(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void anUnclockedDyingWestThirtyDoesNotInventASplitDate()
 {
  room.crabs.remove(parent);
  Crab dead=add(3,280,30,Lane.WEST,true,Style.RANGE,3290,4251); dead.hp=0; dead.dying=true;
  assertFalse(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void aConfirmedWestThirtyRemovalClockCanPrepareItsFreshChildren()
 {
  room.crabs.remove(parent);
  Crab dead=add(3,280,30,Lane.WEST,true,Style.RANGE,3290,4251); dead.hp=0; dead.dying=true; dead.observeRemovalAt(302);
  assertTrue(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void aDimmedShotAndAnUnknownDimClockDoNotGainCleanupPreparation()
 {
  room.spawns[29]=-1;
  assertFalse(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
  room.spawns[29]=290;
  assertFalse(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void aBlockedCurrentShotKeepsTheExistingCollisionAwareApproach()
 {
  room.obstructed.add(green.key);
  assertFalse(ranger.decide(room,298).reason.startsWith("Stage for cleanup split"));
 }
 @Test public void aNewbornMeleeHasOneFewerRunToItsCardinalContact()
 {
  Decision move=ranger.decide(room,298);
  int before=cardinalDistance(room.player,parent.point),after=cardinalDistance(move.tile,parent.point);
  assertTrue(before>=after+2);
  assertTrue((before+1)/2>(after+1)/2);
 }
 @Test public void theFinalEastGreenKeepsItsNamedLaunchFromThePreparedSquare()
 {
  room.spawns[31]=288; room.player=RangerStrategy.tile(3293,4249);
  room.magePosition=RangerStrategy.tile(3291,4247); room.crabs.clear();
  Crab east=new Crab(3,3,288,31,Lane.EAST,'S',false,Style.RANGE,RangerStrategy.tile(3300,4249));
  east.pendingDamage=0; room.crabs.add(east);
  add(4,247,26,Lane.EAST,true,Style.MAGIC,3300,4251);
  Decision move=ranger.decide(room,298);
  assertTrue(move.reason,move.reason.startsWith("Stage for cleanup split"));
  room.player=move.tile;
  Decision shot=ranger.decide(room,300);
  assertSame(east,shot.target); assertEquals(Weapon.PIPE,shot.weapon); assertEquals("East range 31",shot.rule);
 }
 @Test public void aPartialCooldownRunKeepsTheCommittedGreenReadyTick()
 {
  room.spawns[31]=288; room.player=RangerStrategy.tile(3293,4249);
  room.magePosition=RangerStrategy.tile(3291,4247); room.crabs.clear();
  Crab east=new Crab(3,3,288,31,Lane.EAST,'S',false,Style.RANGE,RangerStrategy.tile(3300,4249));
  east.pendingDamage=0; room.crabs.add(east);
  add(4,247,26,Lane.EAST,true,Style.MAGIC,3300,4251);
  Decision first=ranger.decide(room,298);
  assertTrue(first.reason.startsWith("Stage for cleanup split"));
  room.player=RangerStrategy.tile(3295,4250);
  Decision second=ranger.decide(room,299);
  assertEquals(Weapon.PIPE,second.weapon);
  assertTrue(second.reason,second.reason.contains("@300"));
  room.player=second.tile;
  assertSame(east,ranger.decide(room,300).target);
 }
 private int cardinalDistance(Tile from,Tile target)
 {
  int best=Integer.MAX_VALUE;
  for(Tile contact:new Tile[]{target.dx(-1),target.dx(1),target.dy(-1),target.dy(1)})
   best=Math.min(best,from.distanceTo(contact));
  return best;
 }
 private Crab add(int key,int born,int wave,Lane lane,boolean big,Style style,int x,int y)
 {
  Crab c=new Crab(key,key,born,wave,lane,'\0',big,style,RangerStrategy.tile(x,y));
  c.pendingDamage=0; room.crabs.add(c); return c;
 }
}
