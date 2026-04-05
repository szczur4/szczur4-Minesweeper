package szczur4.server;
import szczur4.common.world.RegLoc;
import szczur4.common.Stats;
import szczur4.server.world.Region;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class Game extends JPanel{
	public static final int CORES=Runtime.getRuntime().availableProcessors();
	public boolean uncoverEnabled=true;
	final Map<RegLoc,Region>regions=new HashMap<>();
	ExecutorService executor=Executors.newWorkStealingPool(CORES);
	boolean resetting;
	public final Stats stats=new Stats().load();
	/// covered, flag, mines
	/// 0b 0_0_0000;
	Game()throws IOException{
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			for(Region region:regions.values())try{region.save();}catch(Exception _){}
			try{stats.save();}catch(Exception _){}
		}));
		File dir=new File("world");
		if(dir.exists())for(String name:dir.list()){try{
			String[]coords=name.split(" |\\..*");
			int X=Integer.parseInt(coords[0]),Y=Integer.parseInt(coords[1]);
			regions.put(new RegLoc(X,Y),new Region(X,Y,this).load());
		}catch(Exception _){}}
	}
	public Region getRegion(RegLoc loc){
		Region reg=regions.get(loc);
		if(reg==null)regions.put(loc,reg=new Region(loc.regX,loc.regY,this));
		return reg;
	}
	public void reset(){
		resetting=true;
		executor.shutdownNow();
		for(Region region:regions.values())if(region.exists()){
			if(!region.delete())System.err.println("Failed to delete "+region.getName()+" during reset");
			else System.out.println("Deleted "+region.getName());
		}
		regions.clear();
		stats.reset();
		executor=Executors.newWorkStealingPool(CORES);
		resetting=false;
	}
	public static boolean areLocsEqual(RegLoc loc1,RegLoc loc2){return loc1.regX==loc2.regX&&loc1.regY==loc2.regY&&loc1.chunkX==loc2.chunkX&&loc1.chunkY==loc2.chunkY&&loc1.tileX==loc2.tileX&&loc1.tileY==loc2.tileY;}
	public void fill(RegLoc loc,boolean flag){
		if(getRegion(loc).getChunk(loc).locked){
			if(getRegion(loc).getChunk(loc).lost)getRegion(loc).getChunk(loc).unlock();
			return;
		}
		ArrayList<RegLoc>locations=new ArrayList<>(),toSend=new ArrayList<>();
		locations.add(loc);
		while(!locations.isEmpty()&&!resetting){
			RegLoc tmp=switch(ServerCore.rand.nextInt(4)){
				case(0)->locations.removeFirst();
				case(1)->locations.remove(ServerCore.rand.nextInt(locations.size()));
				default->locations.removeLast();
			};
			if(resetting)break;
			for(RegLoc tmpLoc1:getRegion(tmp).getChunk(tmp).fill(tmp,flag)){
				if((getRegion(tmpLoc1).getTile(tmpLoc1)&48)==16)continue;
				boolean add=true;
				for(RegLoc tmpLoc2:locations){
					if(resetting)break;
					if(areLocsEqual(tmpLoc1,tmpLoc2)){
						add=false;
						break;
					}
				}
				if(resetting)break;
				if(add)locations.add(tmpLoc1);
				for(RegLoc tmpLoc2:toSend)if(tmpLoc1.regX==tmpLoc2.regX&&tmpLoc1.regY==tmpLoc2.regY&&tmpLoc1.chunkX==tmpLoc2.chunkX&&tmpLoc1.chunkY==tmpLoc2.chunkY){
					add=false;
					break;
				}
				if(add)toSend.add(tmpLoc1);
			}
			flag=false;
		}
		for(RegLoc tmp:toSend)ServerCore.broadcast(getRegion(tmp).getChunk(tmp));
		toSend.clear();
		System.gc();
	}
}