package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MagePostTwelveApertureTest {
 private final MageStrategy s=new MageStrategy();private final Room r=new Room();
 private void setup(){r.wave=13;r.spawns[13]=96;r.ready=105;r.player=MageStrategy.tile(3293,4250);Crab d=new Crab(54,60,88,12,Lane.WEST,'S',false,Style.MELEE,r.player);s.attacked(new Decision(d,Weapon.DINH,r.player,true,"12 west Dinh","12 west Dinh"),105,100);Crab c=new Crab(60,66,96,13,Lane.SOUTH,'W',false,Style.MELEE,MageStrategy.tile(3295,4238));c.pendingDamage=0;r.crabs.add(c);}
 @Test public void observedPostDinhPositionUsesItsFirstStepToReachTheAperture(){setup();r.crabs.clear();Decision d=s.decide(r,101);assertNull(d.target);assertEquals(MageStrategy.tile(3292,4249),d.tile);assertTrue(d.reason,d.reason.startsWith("Reach west aperture"));}
 @Test public void proceedsToTheNormalFiringTileOnceInTheLane(){setup();r.crabs.clear();r.player=MageStrategy.tile(3292,4249);Decision d=s.decide(r,102);assertEquals(MageStrategy.tile(3290,4249),d.tile);}
 @Test public void preservesTheNorthernSetupUntilActualDinh(){setup();s.reset();Decision d=s.decide(r,101);assertFalse(d.reason,d.reason.startsWith("Reach west aperture"));}
 @Test public void keepsAReadyBlueAttack(){setup();r.wave=14;r.spawns[14]=104;r.player=MageStrategy.tile(3291,4249);Crab c=new Crab(65,71,104,14,Lane.WEST,'N',false,Style.MAGIC,MageStrategy.tile(3283,4249));c.pendingDamage=0;r.crabs.add(c);assertSame(c,s.decide(r,106).target);}
}
