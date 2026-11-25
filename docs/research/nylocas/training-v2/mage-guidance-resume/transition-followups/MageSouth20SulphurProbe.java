package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageSouth20SulphurProbe {
 private MageStrategy strategy; private Crab parent;
 private Room nativeFrame(){strategy=new MageStrategy();Room r=new Room();r.wave=20;r.spawns[20]=164;r.ready=176;r.specialEnergy=900;r.player=MageStrategy.tile(3292,4247);
 parent=new Crab(96,96,164,20,Lane.SOUTH,'W',true,Style.MAGIC,MageStrategy.tile(3295,4234));r.crabs.add(parent);
 strategy.attacked(new Decision(parent,Weapon.EYE,parent.point,false,"South opener 20","South opener 20"),166,165);
 parent.style=Style.MELEE;parent.point=MageStrategy.tile(3293,4245);parent.hp=2;parent.pendingDamage=0;parent.ownHitUntil=0;
 Crab small=new Crab(106,106,171,20,Lane.SPLIT,(char)0,false,Style.MAGIC,MageStrategy.tile(3297,4248));small.pendingDamage=0;r.crabs.add(small);return r;}
 @Test public void nativeLowHpSouth20FitsOneSulphurBeforeBarrageWhereScytheCannot(){Room r=nativeFrame();Decision d=strategy.decide(r,176);assertSame(parent,d.target);assertEquals(Weapon.SULPHUR,d.weapon);}
 @Test public void fullHpBigKeepsNormalScythe(){Room r=nativeFrame();parent.hp=16;r.ready=172;r.player=MageStrategy.tile(3294,4244);Decision d=strategy.decide(r,172);assertSame(parent,d.target);assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void sevenHpBigKeepsNormalScythe(){Room r=nativeFrame();parent.hp=7;r.ready=172;r.player=MageStrategy.tile(3294,4244);Decision d=strategy.decide(r,172);assertSame(parent,d.target);assertEquals(Weapon.SCYTHE,d.weapon);}
 @Test public void knownNonlethalFlightAllowsNeededFinisher(){Room r=nativeFrame();parent.pendingDamage=1;Decision d=strategy.decide(r,176);assertSame(parent,d.target);assertEquals(Weapon.SULPHUR,d.weapon);}
 @Test public void knownLethalFlightStillProtectsLowHpBig(){Room r=nativeFrame();parent.pendingDamage=2;assertNotSame(parent,strategy.decide(r,176).target);}
 @Test public void unknownPartnerClaimStillProtectsLowHpBig(){Room r=nativeFrame();parent.pendingDamage=-1;parent.claimedUntil=178;assertNotSame(parent,strategy.decide(r,176).target);}
 @Test public void mageNullStillBlocksLowHpBig(){Room r=nativeFrame();parent.nulled=true;assertNotSame(parent,strategy.decide(r,176).target);}
 @Test public void fourTickFinisherDoesNotCrossBarrageDeadline(){Room r=nativeFrame();r.ready=177;assertNotSame(parent,strategy.decide(r,177).target);}
}
