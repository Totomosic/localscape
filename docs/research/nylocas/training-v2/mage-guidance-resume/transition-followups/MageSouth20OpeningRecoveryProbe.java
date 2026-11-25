package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageSouth20OpeningRecoveryProbe {
 private Crab parent;
 private Room room(){Room r=new Room();r.wave=20;r.spawns[20]=156;r.player=MageStrategy.tile(3295,4244);r.ready=164;r.specialEnergy=900;
 parent=new Crab(96,96,156,20,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4242));r.crabs.add(parent);return r;}
 @Test public void missedMagicOpenerDoesNotAbandonTheAssignedSouth20MeleeParent(){Room r=room();Decision d=new MageStrategy().decide(r,164);assertSame(parent,d.target);assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void partnerFlightStillProtectsTheUnopenedParent(){Room r=room();parent.claimedUntil=166;assertNull(new MageStrategy().decide(r,164).target);}
 @Test public void mageNullStillBlocksRecovery(){Room r=room();parent.nulled=true;assertNull(new MageStrategy().decide(r,164).target);}
 @Test public void unassignedEast20MeleeParentIsNotOpenedByThisException(){Room r=room();r.crabs.clear();r.crabs.add(new Crab(1,1,156,20,Lane.EAST,'S',true,Style.MELEE,MageStrategy.tile(3296,4244)));assertNull(new MageStrategy().decide(r,164).target);}
}
