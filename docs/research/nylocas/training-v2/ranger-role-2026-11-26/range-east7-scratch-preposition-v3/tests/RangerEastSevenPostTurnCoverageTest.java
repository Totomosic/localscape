package org.localscape.nylocas.ranger;
import org.junit.Test;import org.localscape.nylocas.ranger.RangerStrategy.*;import static org.junit.Assert.*;
public class RangerEastSevenPostTurnCoverageTest {
 private Room room(){Room r=new Room();r.wave=7;r.spawns[7]=40;r.ready=50;r.player=RangerStrategy.tile(3301,4248);Crab c=new Crab(27,27,40,7,Lane.EAST,'N',false,Style.MELEE,RangerStrategy.tile(3301,4249));c.pendingDamage=0;r.crabs.add(c);return r;}
 @Test public void futureAgeTenDoesNotAdvertiseAnUnresolvedNativeTurn(){Room r=room();RangerStrategy s=new RangerStrategy();assertEquals(-1,s.east7ScratchDue(r,48));assertNotEquals("Prepare East7 melee scratch",s.decide(r,48).reason);assertEquals(-1,s.east7ScratchDue(r,49));}
 @Test public void actualAgeTenLocalCoverageImmediatelyEnablesScratch(){Room r=room();RangerStrategy s=new RangerStrategy();assertEquals(50,s.east7ScratchDue(r,50));Decision d=s.decide(r,50);assertSame(r.crabs.get(0),d.target);assertEquals(Weapon.CLAWS,d.weapon);}
 @Test public void resolvedActualLocalMovementIsMeasuredRatherThanProjected(){Room r=room();r.crabs.get(0).point=RangerStrategy.tile(3300,4250);RangerStrategy s=new RangerStrategy();assertEquals(-1,s.east7ScratchDue(r,50));r.player=RangerStrategy.tile(3301,4249);assertEquals(50,s.east7ScratchDue(r,50));r.obstructed.add(27);assertEquals(-1,s.east7ScratchDue(r,50));r.obstructed.clear();assertEquals(50,s.east7ScratchDue(r,50));}
 @Test public void futureAgeNineKeepsTheObservedStraightLaneOpening(){Room r=room();r.ready=49;assertEquals(49,new RangerStrategy().east7ScratchDue(r,48));}
}