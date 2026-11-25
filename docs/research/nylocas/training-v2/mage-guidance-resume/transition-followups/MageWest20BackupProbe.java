package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageWest20BackupProbe {
 private Crab west;
 private Room room(){Room r=new Room();r.wave=20;r.spawns[20]=168;r.player=MageStrategy.tile(3295,4245);r.ready=178;r.specialEnergy=900;
 west=new Crab(95,95,168,20,Lane.WEST,'S',true,Style.MELEE,MageStrategy.tile(3291,4248));west.hp=11;west.pendingDamage=0;r.crabs.add(west);return r;}
 private Crab selected(Decision d){return d.target!=null?d.target:d.approach;}
 @Test public void nativeWest20SurvivorCanReceiveMeleeBackupAfterBowWindow(){Room r=room();Decision d=new MageStrategy().decide(r,178);assertSame(west,selected(d));assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void livingSouth20StillOwnsOurTransitionBudget(){Room r=room();Crab own=new Crab(96,96,168,20,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4243));own.hp=5;r.crabs.add(own);assertNotSame(west,selected(new MageStrategy().decide(r,178)));}
 @Test public void living19ParentStillOwnsOurTransitionBudget(){Room r=room();Crab own=new Crab(91,91,156,19,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3298,4244));own.hp=2;r.crabs.add(own);assertNotSame(west,selected(new MageStrategy().decide(r,178)));}
 @Test public void alreadyRolledLethalOwn19FlightReleasesBackupBudget(){Room r=room();Crab own=new Crab(91,91,156,19,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3298,4244));own.hp=16;own.pendingDamage=30;r.crabs.add(own);assertSame(west,selected(new MageStrategy().decide(r,178)));}
 @Test public void provenLethalRangerBowProtectsTheWestParent(){Room r=room();west.pendingDamage=11;assertNull(selected(new MageStrategy().decide(r,178)));}
 @Test public void unknownPartnerClaimProtectsTheWestParent(){Room r=room();west.pendingDamage=-1;west.claimedUntil=180;assertNull(selected(new MageStrategy().decide(r,178)));}
 @Test public void nativeMageNullStillBlocksBackup(){Room r=room();west.nulled=true;assertNull(selected(new MageStrategy().decide(r,178)));}
 @Test public void rangerOpeningRangePhaseRemainsProtected(){Room r=room();r.ready=173;west.style=Style.RANGE;assertNull(selected(new MageStrategy().decide(r,173)));}
 @Test public void returnBudgetFor21BarrageCanRejectALateBackup(){Room r=room();r.ready=180;west.point=MageStrategy.tile(3294,4245);assertNull(selected(new MageStrategy().decide(r,180)));}
}
