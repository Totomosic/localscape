package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageDeferred13Age30Probe {
 @Test public void delaysOpening13BeyondThePrematureNativeFrame(){
 Room r=new Room();r.wave=15;r.spawns[15]=112;r.player=MageStrategy.tile(3294,4247);
 Crab big=new Crab(62,62,96,13,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3294,4244));r.crabs.add(big);
 assertNull(new MageStrategy().decide(r,123).target);
 }
 @Test public void openingIsAllowedAtTheNewAgeWithoutWaitingForAnUnspawnedWave(){
 Room r=new Room();r.wave=16;r.spawns[16]=124;r.player=MageStrategy.tile(3294,4246);
 Crab big=new Crab(62,62,96,13,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3294,4244));r.crabs.add(big);
 assertSame(big,new MageStrategy().decide(r,96+30).target);
 }
 @Test public void precedingRequestCannotBypassTheDelayedAssignmentViaGenericCleanup(){
 Room r=new Room();r.wave=16;r.spawns[16]=124;r.player=MageStrategy.tile(3294,4246);
 Crab big=new Crab(62,62,96,13,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3294,4244));r.crabs.add(big);
 assertNull(new MageStrategy().decide(r,95+30).target);
 }
}
