package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerCleanupRedFiringStageTest {
 private final Room room=new Room();private final RangerStrategy ranger=new RangerStrategy();private final Crab green,red;
 public RangerCleanupRedFiringStageTest() {
  room.wave=31;room.spawns[29]=260;room.spawns[31]=280;room.cleanup=true;room.ready=300;room.player=RangerStrategy.tile(3298,4247);room.magePosition=RangerStrategy.tile(3300,4247);room.mageReady=299;
  green=new Crab(1,1,290,31,Lane.SPLIT,'\0',false,Style.RANGE,RangerStrategy.tile(3299,4246));green.pendingDamage=0;
  red=new Crab(2,2,290,31,Lane.SPLIT,'\0',false,Style.MELEE,RangerStrategy.tile(3292,4254));red.pendingDamage=0;
  room.crabs.add(green);room.crabs.add(red);
 }
 @Test public void spendPipeCooldownGettingNearerTheOtherFreshRedWithoutChangingTheShot() {
  Decision d=ranger.decide(room,298);assertTrue(d.reason,d.reason.startsWith("Stage beside cleanup red"));assertTrue(d.tile.distanceTo(red.point)+2<=room.player.distanceTo(red.point));assertNull(d.approach);room.player=d.tile;assertSame(green,ranger.decide(room,300).target);
 }
 @Test public void theMageKeepsTheRedItAlreadyCovers() {
  room.magePosition=RangerStrategy.tile(3293,4253);assertFalse(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));
 }
 @Test public void aNearbyMageCannotCoverItsOwnNullRed() {
  room.magePosition=RangerStrategy.tile(3293,4253);red.partnerNulled=true;room.crabs.remove(green);room.spawns[31]=288;
  Crab finalGreen=new Crab(3,3,288,31,Lane.EAST,'S',false,Style.RANGE,RangerStrategy.tile(3299,4246));finalGreen.pendingDamage=0;room.crabs.add(finalGreen);
  assertTrue(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));
 }
 @Test public void aKnownLethalFlightDoesNotAttractTheMovement() {
  red.pendingDamage=8;assertFalse(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));
 }
 @Test public void aMovingRedIsReevaluatedInsteadOfAssumingAStationaryContact() {
  red.observePosition(RangerStrategy.tile(3293,4254));assertFalse(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));
 }
 @Test public void readyUsefulAttacksStillFireImmediately() {assertSame(green,ranger.decide(room,300).target);}
 @Test public void ownNullAndDimmedRedsDoNotAttractTheMovement() {
  red.nulled=true;assertFalse(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));red.nulled=false;room.spawns[29]=290;assertFalse(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));
 }
 @Test public void theShortEyeFlightStillWinsWhenItCannotShareThePreparedContact() {
  green.style=Style.MAGIC;green.point=RangerStrategy.tile(3301,4246);Decision d=ranger.decide(room,298);assertTrue(d.reason,d.reason.startsWith("Close during cleanup cooldown"));assertTrue(d.tile.distanceTo(green.point)<=2);
 }
 @Test public void thePreparedRedCanBeHitOnTheNextReadyTickInsteadOfStartingAnotherRun() {
  Decision move=ranger.decide(room,298);green.pendingDamage=green.hp;room.ready=302;
  room.player=move.tile;Decision approach=ranger.decide(room,301);assertSame(red,approach.approach);
  room.player=RangerStrategy.tile(3292,4253);Decision hit=ranger.decide(room,302);assertSame(red,hit.target);assertEquals(Weapon.SULPHUR,hit.weapon);
  RangerStrategy control=new RangerStrategy();room.player=RangerStrategy.tile(3298,4247);Decision firstRun=control.decide(room,301);assertTrue(firstRun.reason,firstRun.reason.contains("@303"));
  room.player=RangerStrategy.tile(3296,4249);Decision secondRun=control.decide(room,302);assertNull(secondRun.target);assertSame(red,secondRun.approach);assertTrue(secondRun.reason,secondRun.reason.contains("@303"));
  room.player=RangerStrategy.tile(3294,4251);assertSame(red,control.decide(room,303).target);
 }

 @Test public void noPreparationIsNeededWhenTheFollowingCooldownAlreadyReachesTheRed() {
  room.player=RangerStrategy.tile(3296,4249);assertFalse(ranger.decide(room,298).reason.startsWith("Stage beside cleanup red"));
 }

}
