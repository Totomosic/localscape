package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerEastSevenPriorityWitnessTest {
 @Test public void theCoveredLocalScratchPrecedesOrdinaryUnhitRangeWork(){Room r=new Room();r.wave=7;r.spawns[7]=40;r.ready=49;r.player=RangerStrategy.tile(3300,4248);Crab east=new Crab(27,27,40,7,Lane.EAST,'N',false,Style.MELEE,RangerStrategy.tile(3301,4249));east.pendingDamage=0;r.crabs.add(east);Crab range=new Crab(30,30,48,7,Lane.SPLIT,(char)0,false,Style.RANGE,RangerStrategy.tile(3299,4248));range.pendingDamage=0;r.crabs.add(range);Decision d=new RangerStrategy().decide(r,49);assertSame(d.reason,east,d.target);assertEquals(Weapon.CLAWS,d.weapon);assertFalse(d.special);}
}
