package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageMovementProbe {
 @Test public void closesDistanceDuringCooldownWithoutWaitingForReadyTick(){
 Room r=new Room();r.wave=18;r.spawns[18]=100;r.player=MageStrategy.tile(3296,4248);r.ready=104;
 Crab c=new Crab(1,1,100,18,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3292,4244));r.crabs.add(c);
 Decision d=new MageStrategy().decide(r,102);assertNull(d.target);assertNull(d.approach);assertTrue(c.distance(d.tile)<c.distance(r.player));
 }
 @Test public void blockedRayKeepsCollisionAwareApproachDuringCooldown(){
 Room r=new Room();r.wave=18;r.spawns[18]=100;r.player=MageStrategy.tile(3296,4248);r.ready=104;
 Crab c=new Crab(1,1,100,18,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3292,4244));r.crabs.add(c);r.obstructed.add(c.key);
 Decision d=new MageStrategy().decide(r,102);assertNull(d.target);assertSame(c,d.approach);
 }
 @Test public void readyAttackIsNeverReplacedByMovingCloser(){
 Room r=new Room();r.wave=18;r.spawns[18]=100;r.player=MageStrategy.tile(3296,4248);r.ready=102;
 Crab c=new Crab(1,1,100,18,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3292,4244));r.crabs.add(c);
 assertSame(c,new MageStrategy().decide(r,102).target);
 }

}
