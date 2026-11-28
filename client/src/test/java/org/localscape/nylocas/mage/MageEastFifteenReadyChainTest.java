package org.localscape.nylocas.mage;
import org.junit.Test;
import org.localscape.nylocas.mage.MageStrategy.*;
import static org.junit.Assert.*;
public class MageEastFifteenReadyChainTest {
 private final MageStrategy s=new MageStrategy();private final Room r=new Room();private int key;
 private Crab add(int wave,int born,Lane lane,char slot,boolean big,Style style,int x,int y){Crab c=new Crab(++key,key,born,wave,lane,slot,big,style,MageStrategy.tile(x,y));c.pendingDamage=0;r.crabs.add(c);return c;}
 private void wave15(){r.wave=15;r.spawns[15]=112;r.ready=112;r.player=MageStrategy.tile(3301,4249);r.rangerPosition=MageStrategy.tile(3294,4245);add(15,112,Lane.EAST,'N',false,Style.MAGIC,3310,4249);}
 @Test public void keepsTheOpenedEastThirteenFollowupBeforeOptionalEastFifteen(){wave15();Crab p=add(13,92,Lane.EAST,'N',true,Style.MELEE,3300,4247);p.hp=8;s.attacked(new Decision(p,Weapon.SCYTHE,r.player,false,"Deferred east big 13","hit"),112,107);assertSame(p,s.decide(r,112).target);}
 @Test public void takesTheReadySouthThirteenLaunchBeforeAnOptionalShare(){wave15();Crab c=add(13,92,Lane.SOUTH,'W',false,Style.MELEE,3300,4249);assertSame(c,s.decide(r,112).target);}
 @Test public void birthApproachCannotExtendTwoContestedReadyHolds(){r.wave=14;r.spawns[14]=112;r.ready=118;r.player=MageStrategy.tile(3296,4247);r.rangerPosition=MageStrategy.tile(3294,4245);s.decide(r,117);add(14,112,Lane.WEST,'S',false,Style.MELEE,3290,4248);assertTrue(s.decide(r,118).reason.contains("East 15 north"));assertTrue(s.decide(r,119).reason.contains("East 15 north"));r.wave=15;r.spawns[15]=120;add(15,120,Lane.EAST,'N',false,Style.MAGIC,3310,4249);Decision d=s.decide(r,120);assertFalse(d.reason,d.reason.contains("East 15 north"));}
}
