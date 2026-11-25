package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageBarrageRecoveryProbe {
 @Test public void firstActionable21TickAfterFoodStillBarragesTheWestGroup(){
 Room r=new Room();r.wave=21;r.spawns[21]=100;r.player=MageStrategy.tile(3290,4249);r.ready=102;
 Crab c=new Crab(1,1,100,21,Lane.WEST,'N',false,Style.MAGIC,MageStrategy.tile(3283,4249));r.crabs.add(c);
 MageStrategy s=new MageStrategy();assertNull(s.decide(r,100).target);
 Decision d=s.decide(r,102);assertSame(c,d.target);assertEquals(Weapon.BARRAGE,d.weapon);assertEquals("21 west barrage",d.rule);
 }
 @Test public void recoveryDoesNotCastAcrossTheUpcomingWrongStyleFlicker(){
 Room r=new Room();r.wave=21;r.spawns[21]=100;r.player=MageStrategy.tile(3290,4249);r.ready=104;
 Crab c=new Crab(1,1,100,21,Lane.WEST,'N',false,Style.MAGIC,MageStrategy.tile(3285,4249));r.crabs.add(c);
 Decision d=new MageStrategy().decide(r,104);assertFalse(d.target==c && d.weapon==Weapon.BARRAGE);
 }
}
