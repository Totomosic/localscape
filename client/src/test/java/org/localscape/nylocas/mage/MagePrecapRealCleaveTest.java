package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MagePrecapRealCleaveTest {
 private static Room room(){Room r=new Room();r.wave=10;r.spawns[10]=68;r.player=MageStrategy.tile(3291,4248);return r;}
 private static Crab body(Room r,int x,int y){int k=r.crabs.size()+1;Crab c=new Crab(k,k,63,9,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void aSmallUnderTheMageDoesNotCreateASecondScytheVictim(){Room r=room();body(r,3291,4249);body(r,3291,4248);assertEquals(Weapon.SULPHUR,new MageStrategy().decide(r,68).weapon);}
 @Test public void twoCardinalNeighborsAreNotAutomaticallyInTheSameScytheArc(){Room r=room();body(r,3292,4248);body(r,3291,4249);assertEquals(Weapon.SULPHUR,new MageStrategy().decide(r,68).weapon);}
 @Test public void aRealPerpendicularPairRetainsTheScytheOption(){Room r=room();body(r,3291,4249);body(r,3292,4249);assertEquals(Weapon.SCYTHE,new MageStrategy().decide(r,68).weapon);}
 @Test public void observedMovementCanRemoveTheProjectedCleave(){Room r=room();Crab north=body(r,3291,4248);north.observePosition(MageStrategy.tile(3291,4249));Crab south=body(r,3292,4250);south.observePosition(MageStrategy.tile(3292,4249));assertEquals(Weapon.SULPHUR,new MageStrategy().decide(r,68).weapon);}

}