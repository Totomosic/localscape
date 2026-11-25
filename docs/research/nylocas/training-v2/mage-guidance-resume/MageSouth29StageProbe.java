package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageSouth29StageProbe {
 @Test public void lateDinhCooldownStagesSouthRatherThanApproachingANorthSplit(){
 Room r=new Room();r.wave=29;r.spawns[29]=228;r.player=MageStrategy.tile(3294,4245);r.ready=237;
 Crab parent=new Crab(174,174,228,29,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4238));r.crabs.add(parent);
 Crab north=new Crab(180,180,232,29,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3292,4252));r.crabs.add(north);
 Decision d=new MageStrategy().decide(r,233);assertNull(d.target);assertNull(d.approach);assertEquals(MageStrategy.tile(3295,4243),d.tile);
 }
 @Test public void predicted30NeverAllowsOpeningSouth29BeforeActual30(){
 Room r=new Room();r.wave=29;r.spawns[29]=228;r.player=MageStrategy.tile(3295,4243);r.ready=233;
 Crab parent=new Crab(174,174,228,29,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4241));r.crabs.add(parent);
 assertNull(new MageStrategy().decide(r,237).target);
 }
 @Test public void actual31CutoffStillIgnoresTheOldSouth29Parent(){
 Room r=new Room();r.wave=31;r.spawns[31]=240;r.player=MageStrategy.tile(3295,4243);r.ready=240;
 Crab parent=new Crab(174,174,228,29,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4241));r.crabs.add(parent);
 assertNull(new MageStrategy().decide(r,240).target);
 }
 @Test public void normalDamageFrameDoesNotSpendTheReturnBudgetOnANorthSplit(){
 Room r=new Room();r.wave=29;r.spawns[29]=260;r.player=MageStrategy.tile(3292,4247);r.ready=271;
 Crab parent=new Crab(178,178,260,29,Lane.SOUTH,'W',true,Style.MELEE,MageStrategy.tile(3295,4242));r.crabs.add(parent);
 Crab north=new Crab(183,183,267,29,Lane.SPLIT,(char)0,false,Style.MELEE,MageStrategy.tile(3298,4253));r.crabs.add(north);
 Decision d=new MageStrategy().decide(r,269);assertNull(d.target);assertNull(d.approach);assertEquals(MageStrategy.tile(3295,4243),d.tile);
 }

}
