package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageWestEightOriginalReturnTest {
 private final MageStrategy s=new MageStrategy();private final Room r=new Room();
 private Crab setup(){r.wave=7;r.spawns[7]=40;r.ready=46;r.player=MageStrategy.tile(3299,4249);Crab c=new Crab(12,18,16,4,Lane.EAST,'S',false,Style.MELEE,MageStrategy.tile(3301,4252));c.pendingDamage=0;r.crabs.add(c);return c;}
 @Test public void seed110352114203081LeavesTheOriginalNePillarMeleeForWestEight(){setup();Decision d=s.decide(r,46);assertNull(d.target);assertEquals(Weapon.EYE,d.weapon);assertEquals(MageStrategy.tile(3290,4249),d.tile);}
 @Test public void woundedOriginalAlsoMustFitThePhysicalReturn(){Crab c=setup();c.hp=3;assertNull(s.decide(r,46).target);}
 @Test public void keepsAnOrdinaryHitWhenTheWestReturnStillFits(){Crab c=setup();r.player=MageStrategy.tile(3295,4249);c.point=MageStrategy.tile(3294,4249);assertSame(c,s.decide(r,46).target);}
}
