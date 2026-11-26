package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerWest23To24BudgetTest {
 private final RangerStrategy strategy=new RangerStrategy();
 private Crab west23,west24;
 private Room room(int tick) {
  Room r=new Room();r.wave=24;r.spawns[23]=192;r.spawns[24]=200;r.ready=tick;r.player=RangerStrategy.tile(3291,4248);
  west23=new Crab(131,137,192,23,Lane.WEST,'S',false,Style.RANGE,RangerStrategy.tile(3290,4247));west23.pendingDamage=0;r.crabs.add(west23);
  west24=new Crab(134,140,200,24,Lane.WEST,'S',true,Style.RANGE,RangerStrategy.tile(3281+tick-200,4248));west24.pendingDamage=0;r.crabs.add(west24);return r;
 }
 @Test public void native202LateWest23CannotConsumeTheOnlySafeWest24Pipe() {
  Room r=room(202);Decision d=strategy.decide(r,202);assertNull(d.reason,d.target);assertEquals(Weapon.PIPE,d.weapon);
 }
 @Test public void lateWest23StillLaunchesWhenItEndsExactlyAtWest24Due() {
  Room r=room(201);assertSame(west23,strategy.decide(r,201).target);
 }
 @Test public void west24StillReceivesItsExactAgeThreePipe() {
  Room r=room(203);Decision d=strategy.decide(r,203);assertSame(d.reason,west24,d.target);assertEquals("West big 24",d.rule);assertEquals(Weapon.PIPE,d.weapon);
 }
 @Test public void nullDeadAndCoveredWest24ReleaseTheLateWest23() {
  for(int guard=0;guard<3;guard++){strategy.reset();Room r=room(202);if(guard==0)west24.nulled=true;if(guard==1)west24.dying=true;if(guard==2)west24.pendingDamage=16;assertSame("guard"+guard,west23,strategy.decide(r,202).target);}
 }
}
