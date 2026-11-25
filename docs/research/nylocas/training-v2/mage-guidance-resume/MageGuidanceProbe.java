package org.localscape.nylocas.mage;
import org.localscape.nylocas.mage.MageStrategy.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class MageGuidanceProbe {
 private Room room(int wave,int born,int x,int y) { Room r=new Room();r.wave=wave;r.spawns[wave]=born;r.player=MageStrategy.tile(x,y);return r; }
 private Crab c(Room r,int key,int wave,int born,Lane lane,boolean big,Style style,int x,int y) { Crab c=new Crab(key,key,born,wave,lane,'S',big,style,MageStrategy.tile(x,y));r.crabs.add(c);return c; }
 @Test public void leaveWave3Melee(){Room r=room(3,100,3295,4248);c(r,1,3,100,Lane.WEST,false,Style.MELEE,3294,4248);assertNull(new MageStrategy().decide(r,108).target);}
 @Test public void west26Before27(){Room r=room(27,108,3291,4248);Crab old=c(r,1,26,100,Lane.WEST,true,Style.MAGIC,3288,4248);c(r,2,27,108,Lane.WEST,true,Style.MAGIC,3288,4248);assertSame(old,new MageStrategy().decide(r,111).target);}
 @Test public void south23ZeroHitBackup(){Room r=room(23,100,3295,4244);Crab big=c(r,1,23,100,Lane.SOUTH,true,Style.MELEE,3295,4243);assertSame(big,new MageStrategy().decide(r,111).target);}
 @Test public void west24SurvivorBackup(){Room r=room(25,108,3291,4248);Crab big=c(r,1,24,100,Lane.WEST,true,Style.MAGIC,3289,4248);big.hp=5;assertSame(big,new MageStrategy().decide(r,110).target);}
 @Test public void east19BeforeSouth(){Room r=room(19,100,3296,4248);Crab east=c(r,1,19,100,Lane.EAST,true,Style.MAGIC,3306,4248);c(r,2,19,100,Lane.SOUTH,true,Style.MAGIC,3295,4243);assertSame(east,new MageStrategy().decide(r,108).target);}
 @Test public void south4SurvivorFollowup(){MageStrategy s=new MageStrategy();Room r=room(4,100,3295,4243);Crab big=c(r,1,4,100,Lane.SOUTH,true,Style.MAGIC,3295,4240);s.attacked(s.decide(r,101),102,101);r.ready=104;r.wave=5;r.spawns[5]=104;big.hp=5;c(r,2,5,104,Lane.EAST,false,Style.MAGIC,3300,4249);assertSame(big,s.decide(r,104).target);}
 @Test public void nativeSouth11BeforeWest11(){Room r=room(11,76,3294,4247);Crab west=c(r,1,11,76,Lane.WEST,true,Style.MAGIC,3286,4248);Crab south=c(r,2,11,76,Lane.SOUTH,false,Style.MAGIC,3295,4238);south=new Crab(2,2,76,11,Lane.SOUTH,'W',false,Style.MAGIC,MageStrategy.tile(3295,4238));r.crabs.set(1,south);assertSame(south,new MageStrategy().decide(r,81).target);}
 @Test public void nativeEast19BeforeSouth19(){Room r=room(19,148,3296,4243);Crab east=c(r,2,19,148,Lane.EAST,true,Style.MAGIC,3302,4248);c(r,1,19,148,Lane.SOUTH,true,Style.MAGIC,3295,4240);Decision d=new MageStrategy().decide(r,155);assertSame(east,d.target==null?d.approach:d.target);}
}
