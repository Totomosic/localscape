package org.localscape.nylocas.ranger;

import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.Crab;
import org.localscape.nylocas.ranger.RangerStrategy.Decision;
import org.localscape.nylocas.ranger.RangerStrategy.Lane;
import org.localscape.nylocas.ranger.RangerStrategy.Room;
import org.localscape.nylocas.ranger.RangerStrategy.Style;
import org.localscape.nylocas.ranger.RangerStrategy.Weapon;
import static org.junit.Assert.*;

public class RangerWestNineIncomingStageTest
{
 @Test public void incomingBlueAllowsAFreeWestStepWithoutDelayingEye()
 {
  RangerStrategy ranger=new RangerStrategy(); Room room=new Room();
  room.wave=9; room.spawns[9]=56; room.player=RangerStrategy.tile(3301,4248); room.ready=59;
  Crab blue=new Crab(1,1,56,9,Lane.EAST,'N',false,Style.MAGIC,RangerStrategy.tile(3309,4249));
  blue.hp=5; blue.pendingDamage=0;
  Crab west=new Crab(2,2,56,9,Lane.WEST,'S',true,Style.RANGE,RangerStrategy.tile(3284,4248)); west.pendingDamage=0;
  room.crabs.add(blue); room.crabs.add(west);
  Decision step=ranger.decide(room,57);
  assertEquals(RangerStrategy.tile(3299,4248),step.tile); assertNull(step.target); assertNull(step.approach);
  room.player=step.tile; blue.observePosition(RangerStrategy.tile(3308,4249)); west.observePosition(RangerStrategy.tile(3285,4248));
  Decision hold=ranger.decide(room,58);
  assertEquals(room.player,hold.tile); assertNull(hold.target); assertNull(hold.approach);
  blue.observePosition(RangerStrategy.tile(3307,4249)); west.observePosition(RangerStrategy.tile(3286,4248));
  Decision shot=ranger.decide(room,59);
  assertSame(blue,shot.target); assertEquals(Weapon.EYE,shot.weapon);
 }
}
