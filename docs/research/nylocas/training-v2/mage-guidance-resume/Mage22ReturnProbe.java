package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class Mage22ReturnProbe {
 @Test public void northFightingSmallDoesNotSpendTheLastSouthDinhReturnTick(){
 Room r=new Room();r.wave=22;r.spawns[22]=184;r.player=MageStrategy.tile(3295,4251);r.ready=189;r.specialEnergy=900;
 Crab north=new Crab(119,119,183,21,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3297,4252));north.fighting=true;r.crabs.add(north);
 Crab primary=new Crab(125,125,184,22,Lane.SOUTH,'W',false,Style.MAGIC,MageStrategy.tile(3295,4236));r.crabs.add(primary);
 Decision d=new MageStrategy().decide(r,187);assertNull(d.target);assertNull(d.approach);assertEquals(Weapon.DINH,d.weapon);assertEquals(MageStrategy.tile(3295,4244),d.tile);
 }
 @Test public void anImmediatelyNearbyFillerStillFitsTheRealCooldownBudget(){
 Room r=new Room();r.wave=22;r.spawns[22]=184;r.player=MageStrategy.tile(3295,4244);r.ready=189;r.specialEnergy=900;
 Crab small=new Crab(119,119,183,21,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3296,4244));small.fighting=true;r.crabs.add(small);
 Decision d=new MageStrategy().decide(r,187);assertSame(small,d.target!=null?d.target:d.approach);
 }
}
