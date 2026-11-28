package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageLateTwentyTwoRelativeSweepTest {
 private final MageStrategy s=new MageStrategy();private final Room r=new Room();private Crab small,big;private int key;
 private Crab add(int wave,int born,Lane lane,boolean large,Style style,int x,int y){Crab c=new Crab(++key,key,born,wave,lane,'W',large,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 private void delayed(){r.wave=22;r.spawns[21]=168;r.spawns[22]=176;r.ready=185;r.specialEnergy=500;r.player=MageStrategy.tile(3295,4244);small=add(22,176,Lane.SOUTH,false,Style.MELEE,3295,4243);big=add(22,176,Lane.WEST,true,Style.MELEE,3292,4245);Crab magic=add(21,174,Lane.SPLIT,false,Style.MAGIC,3296,4246);Decision d=s.decide(r,185);assertSame(magic,d.target);s.attacked(d,188,185);magic.pendingDamage=8;r.ready=188;r.player=MageStrategy.tile(3294,4244);}
 @Test public void sharedPartnerNulledExposureDoesNotEraseTheBigComparison(){delayed();add(21,174,Lane.SPLIT,false,Style.RANGE,3295,4247).partnerNulled=true;Decision d=s.decide(r,188);assertSame(big,d.target);assertEquals("22 south Dinh",d.rule);}
 @Test public void rejectsANewlyExposedPartnerNulledTarget(){delayed();add(21,174,Lane.SPLIT,false,Style.RANGE,3294,4250).partnerNulled=true;assertSame(small,s.decide(r,188).target);}
 @Test public void rejectsANewlyExposedRequiredParent(){delayed();add(20,160,Lane.SOUTH,true,Style.RANGE,3294,4250);assertSame(small,s.decide(r,188).target);}
 @Test public void keepsFallbackWhenItsExpectedUsefulVictimsAreBetter(){delayed();add(21,174,Lane.SPLIT,false,Style.MELEE,3294,4239);assertSame(small,s.decide(r,188).target);}
 @Test public void keepsFallbackWhenBigContactCannotBeReachedInTheRequestCycle(){delayed();big.point=MageStrategy.tile(3290,4248);assertSame(small,s.decide(r,188).target);}
 @Test public void prefersTheDoubleSplatOnTheReachableFightingEastBig(){delayed();big.hp=0;big.dying=true;Crab east=add(22,176,Lane.EAST,true,Style.MELEE,3295,4245);east.fighting=true;assertSame(east,s.decide(r,188).target);}
 @Test public void ignoresAnOwnNulledBigAsAnAnchor(){delayed();big.nulled=true;assertSame(small,s.decide(r,188).target);}

 @Test public void cooldownPreparationUsesTheFightingEastBigsWestFace(){delayed();big.hp=0;big.dying=true;Crab east=add(22,176,Lane.EAST,true,Style.MELEE,3295,4245);east.fighting=true;Decision d=s.decide(r,187);assertNull(d.target);assertSame(east,d.approach);assertEquals(MageStrategy.tile(3294,4245),d.tile);}

 @Test public void doesNotMistakeNulledAndLethalFlightBodiesForEmptyHuntSlots(){delayed();for(int i=0;i<10;i++){Crab c=add(21,174,Lane.SPLIT,false,Style.RANGE,3290+i,4250);if(i<5)c.nulled=true;else c.pendingDamage=8;}assertSame(small,s.decide(r,188).target);}
}
