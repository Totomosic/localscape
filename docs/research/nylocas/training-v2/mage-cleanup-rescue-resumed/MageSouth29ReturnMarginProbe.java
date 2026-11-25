package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageSouth29ReturnMarginProbe {
 private Room frame(int spawn29,int x,int y,int born,int tx,int ty,Lane lane,char slot){Room r=new Room();r.wave=29;r.spawns[29]=spawn29;r.ready=259;r.player=MageStrategy.tile(x,y);r.specialEnergy=900;
 Crab parent=new Crab(180,180,spawn29,29,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4233+258-spawn29));parent.pendingDamage=0;r.crabs.add(parent);
 Crab filler=new Crab(164,164,born,28,lane,slot,false,Style.MELEE,MageStrategy.tile(tx,ty));filler.pendingDamage=0;filler.fighting=false;r.crabs.add(filler);return r;}
 private void assertStagesSouth(Room r){Decision d=new MageStrategy().decide(r,258);assertNull(d.target);assertNull(d.approach);assertEquals(MageStrategy.tile(3295,4243),d.tile);}
 @Test public void native87027PillarMeleeChaseIsRejectedBeforeThe259Launch(){assertStagesSouth(frame(252,3293,4248,236,3290,4252,Lane.WEST,'N'));}
 @Test public void native87061PillarMeleeChaseIsRejectedBeforeThe259Launch(){assertStagesSouth(frame(256,3291,4251,240,3291,4250,Lane.WEST,'N'));}
 @Test public void native87096LateSplitMeleeChaseIsRejectedBeforeThe259Launch(){assertStagesSouth(frame(256,3297,4249,255,3300,4252,Lane.SPLIT,(char)0));}
 @Test public void nearbyUsefulMeleeStillFitsTheNormalBudget(){Room r=frame(256,3295,4243,255,3295,4244,Lane.SPLIT,(char)0);Decision d=new MageStrategy().decide(r,258);assertSame(r.crabs.get(1),d.approach);assertEquals(Weapon.SULPHUR,d.weapon);}
 @Test public void predictionStillDoesNotOpen29BeforeActual30(){Room r=frame(256,3295,4243,255,3295,4244,Lane.SPLIT,(char)0);r.crabs.remove(1);assertNull(new MageStrategy().decide(r,258).target);}
 @Test public void actual31OldBirthCutoffIsUnchanged(){Room r=frame(256,3295,4243,255,3295,4244,Lane.SPLIT,(char)0);r.wave=31;r.spawns[31]=264;r.ready=264;assertNull(new MageStrategy().decide(r,264).target);}
}
