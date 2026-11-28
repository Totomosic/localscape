package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerTwentyOneRowArrivalTest {
 private final Room room=new Room();private final RangerStrategy ranger=new RangerStrategy();
 public RangerTwentyOneRowArrivalTest(){room.wave=20;room.spawns[20]=100;room.ready=114;room.player=RangerStrategy.tile(3298,4247);}
 @Test public void oneRemainingRunFinishesOnTheEastLaneRowInsteadOfSouthOfTheRay(){
  Decision d=ranger.decide(room,115);assertEquals(4248,d.tile.getY());assertTrue(d.reason,room.player.distanceTo(d.tile)<=2);assertEquals(Weapon.CHIN,d.weapon);
 }
 @Test public void theShortWaypointMakesTheChinAtBirthWithoutAnExtraReadyTick(){
  Decision move=ranger.decide(room,115);assertTrue(room.player.distanceTo(move.tile)<=2);room.player=move.tile;room.wave=21;room.spawns[21]=116;room.ready=116;
  Crab east=add(1,116,21,Lane.EAST,'N',false,Style.RANGE,3310,4249);add(2,116,21,Lane.EAST,'S',false,Style.RANGE,3310,4248);
  Decision shot=ranger.decide(room,116);assertNotNull(shot.reason,shot.target);assertEquals("East chin 21",shot.rule);assertEquals(Weapon.CHIN,shot.weapon);
 }
 @Test public void theCurrentBlockedSouthRayStillCannotPretendToFireAtBirth(){
  room.player=RangerStrategy.tile(3300,4247);room.wave=21;room.spawns[21]=116;room.ready=116;
  Crab east=add(1,116,21,Lane.EAST,'N',false,Style.RANGE,3310,4249);room.obstructed.add(east.key);
  assertNull(ranger.decide(room,116).target);
 }
 @Test public void theQuotedWestTwentyBowKeepsItsOriginalFirstGreenTick(){
  room.player=RangerStrategy.tile(3293,4248);room.ready=105;
  Crab west=add(1,100,20,Lane.WEST,'S',true,Style.RANGE,3286,4248);
  Decision shot=ranger.decide(room,105);assertSame(west,shot.target);assertEquals(Weapon.BOW,shot.weapon);assertEquals("West bow flicker 20",shot.rule);
 }
 @Test public void anAvailableLateEighteenRecoveryStillUsesItsOwnReadyTick(){
  room.ready=111;room.player=RangerStrategy.tile(3295,4248);
  Crab parent=add(1,80,18,Lane.WEST,'S',true,Style.MAGIC,3293,4249);
  Decision shot=ranger.decide(room,111);assertSame(parent,shot.target);assertEquals(Weapon.EYE,shot.weapon);
 }
 @Test public void existingPipeCooldownCanProgressEastAndKeepThreeAvailableBlowpipeSlots(){
  room.player=RangerStrategy.tile(3294,4248);room.ready=112;
  Crab bow=new Crab(90,90,100,20,Lane.WEST,'S',true,Style.RANGE,RangerStrategy.tile(3286,4248));
  ranger.attacked(new Decision(bow,Weapon.BOW,room.player,false,"West bow flicker 20","quoted bow"),106);
  Crab first=new Crab(91,91,106,20,Lane.SPLIT,'\0',false,Style.RANGE,RangerStrategy.tile(3295,4248));
  ranger.attacked(new Decision(first,Weapon.PIPE,room.player,false,null,"first blowpipe slot"),111);
  Crab second=add(1,108,20,Lane.SPLIT,'\0',false,Style.RANGE,3294,4249);
  Decision stage=ranger.decide(room,111);assertTrue(stage.reason,stage.reason.startsWith("Stage East21 while keeping PIPE"));assertTrue(room.player.distanceTo(stage.tile)<=2);assertTrue(stage.tile.getX()>room.player.getX());
  room.player=stage.tile;Decision pipe=ranger.decide(room,112);assertSame(second,pipe.target);assertEquals(Weapon.PIPE,pipe.weapon);ranger.attacked(pipe,113);second.pendingDamage=second.hp;
  Crab third=add(2,112,20,Lane.SPLIT,'\0',false,Style.RANGE,3293,4248);room.ready=114;
  Decision move=ranger.decide(room,113);assertTrue(room.player.distanceTo(move.tile)<=2);room.player=move.tile;
  Decision last=ranger.decide(room,114);assertSame(last.reason,third,last.target);assertEquals(Weapon.PIPE,last.weapon);ranger.attacked(last,115);third.pendingDamage=third.hp;
  room.ready=116;move=ranger.decide(room,115);assertTrue(room.player.distanceTo(move.tile)<=2);room.player=move.tile;room.wave=21;room.spawns[21]=116;
  add(3,116,21,Lane.EAST,'N',false,Style.RANGE,3310,4249);add(4,116,21,Lane.EAST,'S',false,Style.RANGE,3310,4248);
  Decision chin=ranger.decide(room,116);assertNotNull(chin.target);assertEquals("East chin 21",chin.rule);
 }
 @Test public void theFirstChinCooldownReachesTheBarrierForTheOneSurvivorPipe(){
  room.wave=21;room.spawns[21]=116;room.ready=119;room.player=RangerStrategy.tile(3300,4248);
  Crab north=add(1,116,21,Lane.EAST,'N',false,Style.RANGE,3309,4249);Crab south=add(2,116,21,Lane.EAST,'S',false,Style.RANGE,3309,4248);
  ranger.attacked(new Decision(north,Weapon.CHIN,room.player,false,"East chin 21","first throw"),117);north.pendingDamage=8;south.pendingDamage=4;
  Decision stage=ranger.decide(room,117);assertTrue(stage.reason,stage.reason.startsWith("Barrier for the East21 follow-up"));assertEquals(3301,stage.tile.getX());assertTrue(room.player.distanceTo(stage.tile)<=2);
  room.player=stage.tile;north.observePosition(RangerStrategy.tile(3307,4249));south.observePosition(RangerStrategy.tile(3307,4248));
  Decision pipe=ranger.decide(room,119);assertSame(south,pipe.target);assertEquals(Weapon.PIPE,pipe.weapon);
 }
 @Test public void twoSurvivorsKeepTheShortFlightRechinOnTheSameReadyTick(){
  room.wave=21;room.spawns[21]=116;room.ready=119;room.player=RangerStrategy.tile(3300,4248);
  Crab north=add(1,116,21,Lane.EAST,'N',false,Style.RANGE,3309,4249);Crab south=add(2,116,21,Lane.EAST,'S',false,Style.RANGE,3309,4248);
  ranger.attacked(new Decision(north,Weapon.CHIN,room.player,false,"East chin 21","first throw"),117);north.pendingDamage=2;south.pendingDamage=2;
  Decision stage=ranger.decide(room,117);assertEquals(3301,stage.tile.getX());room.player=stage.tile;
  north.observePosition(RangerStrategy.tile(3307,4249));south.observePosition(RangerStrategy.tile(3307,4248));
  Decision chin=ranger.decide(room,119);assertNotNull(chin.target);assertEquals(Weapon.CHIN,chin.weapon);assertTrue(chin.target.positionAt(119,120).distanceTo(room.player)<6);
 }
 @Test public void aLateNineteenRecoveryStillUsesTheAvailableEye(){
  room.ready=111;room.player=RangerStrategy.tile(3295,4248);
  Crab parent=add(1,90,19,Lane.WEST,'S',true,Style.MAGIC,3293,4249);
  Decision shot=ranger.decide(room,111);assertSame(parent,shot.target);assertEquals(Weapon.EYE,shot.weapon);
 }
 @Test public void anUnknownPartnerFlightOnALateParentKeepsItsRecoveryPosition(){
  room.player=RangerStrategy.tile(3294,4248);room.ready=112;
  Crab parent=add(1,80,18,Lane.WEST,'S',true,Style.MAGIC,3291,4249);parent.pendingDamage=-1;parent.claimedUntil=140;
  Crab green=add(2,108,20,Lane.SPLIT,'\0',false,Style.RANGE,3295,4249);
  Decision d=ranger.decide(room,111);assertFalse(d.reason,d.reason.startsWith("Stage East21 while keeping PIPE"));assertSame(green,d.approach);
 }
 private Crab add(int key,int born,int wave,Lane lane,char slot,boolean big,Style style,int x,int y){Crab c=new Crab(key,key,born,wave,lane,slot,big,style,RangerStrategy.tile(x,y));c.pendingDamage=0;room.crabs.add(c);return c;}
}
