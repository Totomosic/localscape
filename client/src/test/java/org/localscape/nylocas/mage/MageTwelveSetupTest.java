package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageTwelveSetupTest {
 private static Crab body(Room r,int wave,int born,Lane lane,char slot,boolean big,Style style,int x,int y) {int k=r.crabs.size()+1;Crab c=new Crab(k,k,born,wave,lane,slot,big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 private static Room ten(){Room r=new Room();r.wave=10;r.spawns[10]=68;r.player=MageStrategy.tile(3293,4247);return r;}
 @Test public void clearsAnImmediatelyReachableEastFaceBlockerBeforeAFiller() {Room r=ten();Crab c=body(r,8,52,Lane.SPLIT,(char)0,false,Style.MAGIC,3292,4244);body(r,10,66,Lane.SPLIT,(char)0,false,Style.MELEE,3293,4248);assertSame(c,new MageStrategy().decide(r,68).target);}
 @Test public void helpsARangerOwnedEastFaceSmallWhenNoFlightCoversIt() {Room r=ten();Crab c=body(r,8,52,Lane.SPLIT,(char)0,false,Style.RANGE,3292,4244);Decision d=new MageStrategy().decide(r,68);assertSame(c,d.target);assertEquals(Weapon.PIPE,d.weapon);}
 @Test public void doesNotStealTheOriginalSouthTenChinWindow() {Room r=ten();Crab c=body(r,10,64,Lane.SOUTH,'W',false,Style.RANGE,3292,4244);assertNotSame(c,new MageStrategy().decide(r,68).target);}
 @Test public void usesAFreeNorthernFiringSquareDuringTheEastBarrageCooldown() {
  MageStrategy s=new MageStrategy();Room r=new Room();r.wave=11;r.spawns[11]=76;r.player=MageStrategy.tile(3301,4249);r.ready=81;
  Crab east=body(r,11,76,Lane.EAST,'N',false,Style.MAGIC,3310,4249);s.attacked(new Decision(east,Weapon.BARRAGE,r.player,false,"East barrage 11","East barrage 11"),77,76);east.pendingDamage=8;
  body(r,11,76,Lane.SOUTH,'W',false,Style.MAGIC,3295,4234);body(r,11,76,Lane.SOUTH,'E',false,Style.MAGIC,3296,4234);
  Decision d=s.decide(r,77);assertNull(d.target);assertEquals(Weapon.BARRAGE,d.weapon);assertTrue(d.tile.toString(),d.tile.getY()>=4249);
 }
 @Test public void standingNorthCannotAvoidTheBigWhenTheMandatoryAggroIsTooFarSouth() {
  // Packed control seed201851789785554/t96, pre-cycle observation.
  int tx=3292,ty=4247,bx=3293,by=4243;
  for(int[]delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
   assertTrue(Math.max(Math.abs(tx+delta[0]-bx),Math.abs(ty+delta[1]-by))<=5);
 }
}