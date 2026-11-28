package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageTwelveNorthRouteTest {
 private static Room room(boolean crowded) {
  Room r=new Room();r.wave=12;r.spawns[12]=100;r.player=MageStrategy.tile(3301,4249);r.specialEnergy=1000;
  add(r,12,Lane.EAST,'N',false,Style.MELEE,3303,4249);add(r,12,Lane.EAST,'S',false,Style.MELEE,3303,4248);add(r,12,Lane.WEST,'S',false,Style.MELEE,3292,4248);add(r,12,Lane.SOUTH,'W',true,Style.MELEE,3295,4243);
  if(crowded)while(r.crabs.size()<12)add(r,12,Lane.SPLIT,(char)0,false,Style.RANGE,3290,4254);return r;
 }
 private static Crab add(Room r,int wave,Lane lane,char slot,boolean big,Style style,int x,int y) {int k=r.crabs.size()+1;Crab c=new Crab(k,k,100,wave,lane,slot,big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 private static void scythe(MageStrategy s,Room r) {Decision d=s.decide(r,107);assertEquals('S',d.target.slot);assertEquals("12 east scythe",d.rule);s.attacked(d,108,107);r.ready=112;r.crabs.get(0).pendingDamage=r.crabs.get(1).pendingDamage=8;r.player=MageStrategy.tile(3301,4248);}
 @Test public void firstGroundClickMovesNorthwestDuringTheScytheCooldown() {MageStrategy s=new MageStrategy();Room r=room(true);scythe(s,r);Decision d=s.decide(r,108);assertNull(d.target);assertEquals(MageStrategy.tile(3299,4250),d.tile);}
 @Test public void retainsTheOriginalAggressiveWestPrimaryAndFiveTickTiming() {MageStrategy s=new MageStrategy();Room r=room(true);scythe(s,r);r.player=MageStrategy.tile(3293,4250);assertNull(s.decide(r,111).target);Decision d=s.decide(r,112);assertSame(r.crabs.get(2),d.target);assertEquals("12 west Dinh",d.rule);assertEquals(MageStrategy.tile(3293,4250),d.tile);}
 @Test public void theOpenRoomRetainsTheDirectReturn() {MageStrategy s=new MageStrategy();Room r=room(false);scythe(s,r);Decision d=s.decide(r,108);assertEquals(MageStrategy.tile(3293,4249),d.tile);}
}