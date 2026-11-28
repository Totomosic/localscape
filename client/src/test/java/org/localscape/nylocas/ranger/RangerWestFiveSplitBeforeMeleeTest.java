package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerWestFiveSplitBeforeMeleeTest
{
 private final RangerStrategy ranger = new RangerStrategy();
 private final Room room = new Room();
 private final Crab parent;
 private final Crab melee;

 public RangerWestFiveSplitBeforeMeleeTest()
 {
  room.wave = 5; room.spawns[5] = 20; room.player = RangerStrategy.tile(3295, 4244); room.ready = 30;
  parent = new Crab(1, 1, 20, 5, Lane.WEST, 'S', true, Style.RANGE, RangerStrategy.tile(3287, 4248));
  parent.hp = 0; parent.dying = true; parent.observeSplitAt(32); room.crabs.add(parent);
  melee = new Crab(2, 2, 20, 5, Lane.SOUTH, 'S', false, Style.MELEE, RangerStrategy.tile(3296, 4243));
  melee.pendingDamage = 0; room.crabs.add(melee);
 }

 @Test public void preparesTheConfirmedSplitInsteadOfCommittingFourTicksToFreshMelee()
 {
  Decision stage = ranger.decide(room, 30);
  assertNull(stage.target); assertTrue(stage.reason.contains("Prepare finite fresh opportunity @32"));
  room.player = RangerStrategy.tile(3293, 4246);
  Decision nextStage = ranger.decide(room, 31);
  assertNull(nextStage.target); assertTrue(nextStage.reason.contains("Prepare finite fresh opportunity @32"));
  room.player = nextStage.tile; room.crabs.remove(parent);
  Crab green = new Crab(3, 3, 32, 5, Lane.SPLIT, '\0', false, Style.RANGE, RangerStrategy.tile(3287, 4248));
  green.pendingDamage = 0; room.crabs.add(green);
  Decision shot = ranger.decide(room, 32);
  assertSame(green, shot.target); assertEquals(Weapon.PIPE, shot.weapon);
 }

 @Test public void keepsUsefulMeleeWithoutAConfirmedBirthWithinTwoTicks()
 {
  parent.observeSplitAt(35);
  assertSame(melee, ranger.decide(room, 30).target);
 }

 @Test public void aMeleeSurvivorKeepsItsFollowup()
 {
  ranger.attacked(new Decision(melee, Weapon.SULPHUR, room.player, false, null, "first hit"), 29);
  melee.hp = 3;
  assertSame(melee, ranger.decide(room, 30).target);
 }

 @Test public void doesNotHoldAReadyRangedSmallForTheSplit()
 {
  melee.style = Style.RANGE;
  Decision shot = ranger.decide(room, 30);
  assertSame(melee, shot.target); assertEquals(Weapon.PIPE, shot.weapon);
 }

 @Test public void liveUncoveredWestFiveStillTakesItsRequiredShot()
 {
  parent.hp = 16; parent.dying = false; parent.pendingDamage = 0; parent.observeSplitAt(-1);
  parent.point = RangerStrategy.tile(3291, 4248); room.player = RangerStrategy.tile(3293, 4248);
  assertSame(parent, ranger.decide(room, 30).target);
 }
}
