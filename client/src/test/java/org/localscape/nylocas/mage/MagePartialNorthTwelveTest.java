package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MagePartialNorthTwelveTest {
 private static Room room(){Room r=new Room();r.wave=11;r.spawns[11]=100;r.ready=106;r.player=MageStrategy.tile(3295,4243);Crab c=new Crab(1,1,100,11,Lane.SOUTH,'W',false,Style.MAGIC,MageStrategy.tile(3295,4238));c.pendingDamage=0;r.crabs.add(c);return r;}
 private static MageStrategy strategy(Room r){MageStrategy s=new MageStrategy();Crab east=new Crab(99,99,100,11,Lane.EAST,'N',false,Style.MAGIC,MageStrategy.tile(3310,4249));s.attacked(new Decision(east,Weapon.BARRAGE,r.player,false,"East barrage 11","East barrage 11"),101,100);return s;}
 @Test public void takesTheTwoFreeNorthTilesWhenTheFullRowHasNoRayInRange(){Room r=room();Decision d=strategy(r).decide(r,105);assertNull(d.target);assertEquals(Weapon.EYE,d.weapon);assertEquals(4245,d.tile.getY());}
 @Test public void keepsTheReadySouthCastInsteadOfWaitingForTheNorthRow(){Room r=room();Decision d=strategy(r).decide(r,106);assertSame(r.crabs.get(0),d.target);}
 @Test public void takesTheFullNorthRowWhenItAlreadyFits(){Room r=room();r.player=MageStrategy.tile(3295,4247);r.crabs.get(0).point=MageStrategy.tile(3295,4243);Decision d=strategy(r).decide(r,105);assertEquals(4249,d.tile.getY());}
}