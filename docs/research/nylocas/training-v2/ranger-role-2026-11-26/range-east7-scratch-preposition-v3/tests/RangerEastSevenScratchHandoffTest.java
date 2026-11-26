package org.localscape.nylocas.ranger;
import org.junit.Test;
import org.localscape.nylocas.ranger.RangerStrategy.*;
import static org.junit.Assert.*;
public class RangerEastSevenScratchHandoffTest {
 private Crab east;
 private Room room(){Room r=new Room();r.wave=7;r.spawns[7]=40;r.ready=49;r.specialEnergy=1000;r.player=RangerStrategy.tile(3300,4248);east=new Crab(27,27,40,7,Lane.EAST,'N',false,Style.MELEE,RangerStrategy.tile(3301,4249));east.pendingDamage=0;r.crabs.add(east);return r;}
 private Crab range(Room r,int key,boolean big){Crab c=new Crab(key,key,40,7,big?Lane.SOUTH:Lane.SPLIT,'W',big,Style.RANGE,r.player);c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void thePureProofAndSelectedNormalScratchAgree(){Room r=room();RangerStrategy s=new RangerStrategy();assertEquals(49,s.east7ScratchDue(r,49));Decision d=s.decide(r,49);assertSame(east,d.target);assertEquals(Weapon.CLAWS,d.weapon);assertFalse(d.special);}
 @Test public void localCooldownCanPrepositionWithinTwoTicks(){Room r=room();r.ready=49;RangerStrategy s=new RangerStrategy();assertEquals(49,s.east7ScratchDue(r,47));Decision d=s.decide(r,47);assertNull(d.target);assertEquals(Weapon.CLAWS,d.weapon);assertTrue(org.localscape.nylocas.AttackTiming.distance(d.tile,east.point,false)<=1);}
 @Test public void aDistantOrBlockedScratchIsNotPromised(){Room r=room();r.player=RangerStrategy.tile(3295,4248);assertEquals(-1,new RangerStrategy().east7ScratchDue(r,49));r.player=RangerStrategy.tile(3300,4248);r.obstructed.add(east.key);assertEquals(-1,new RangerStrategy().east7ScratchDue(r,49));}
 @Test public void anActualRangedSurvivorKeepsItsFollowup(){Room r=room();Crab survivor=range(r,30,false);RangerStrategy s=new RangerStrategy();s.attacked(new Decision(survivor,Weapon.PIPE,r.player,false,null,"actualpriorhit"),47);survivor.hp=3;assertEquals(-1,s.east7ScratchDue(r,49));assertSame(survivor,s.decide(r,49).target);}
 @Test public void anUncoveredAssignedParentKeepsPriorityButLethalCoverageReleases(){Room r=room();Crab big=range(r,30,true);RangerStrategy s=new RangerStrategy();assertEquals(-1,s.east7ScratchDue(r,49));big.pendingDamage=16;assertEquals(49,s.east7ScratchDue(r,49));}
 @Test public void nullAndLethalAndUnknownClaimedTargetsCannotBePromised(){for(int guard=0;guard<3;guard++){Room r=room();if(guard==0)east.nulled=true;if(guard==1)east.pendingDamage=8;if(guard==2){east.pendingDamage=-1;east.claimedUntil=60;}assertEquals("guard"+guard,-1,new RangerStrategy().east7ScratchDue(r,49));}}
 @Test public void theOriginalIdentityAndRequestWindowAreBounded(){Room r=room();r.ready=53;assertEquals(-1,new RangerStrategy().east7ScratchDue(r,53));r=room();r.crabs.clear();r.crabs.add(new Crab(27,27,40,7,Lane.SPLIT,(char)0,false,Style.MELEE,r.player));assertEquals(-1,new RangerStrategy().east7ScratchDue(r,49));}
 @Test public void proofIsSideEffectFreeAndMageNullDoesNotDisableNormalRescue(){Room r=room();RangerStrategy s=new RangerStrategy();east.partnerNulled=true;assertEquals(49,s.east7ScratchDue(r,49));assertEquals(49,s.east7ScratchDue(r,49));assertSame(east,s.decide(r,49).target);}
}
