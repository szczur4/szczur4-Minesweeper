package szczur4.client.ui;
import szczur4.client.ClientCore;
import szczur4.client.Game;
import java.awt.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
public class ServerList extends Container{
	final Game parent;
	final HashMap<InetAddress,ServerButton>servers=new HashMap<>();
	public ServerList(Game parent){this.parent=parent;}
	public void addPeers(Set<InetAddress>peers){
		for(InetAddress peer:peers)if(!servers.containsKey(peer))servers.put(peer,new ServerButton(peer));
		var comps=Arrays.asList(getComponents());
		for(var server:servers.keySet()){
			Component entry=servers.get(server);
			if(!peers.contains(server)&&comps.contains(entry))remove(entry);
			else if(peers.contains(server)&&!comps.contains(entry)&&!ClientCore.host.equals(server))add(entry);
		}
		updateBounds();
	}
	public void disableAll(){for(ServerButton button:servers.values())button.disable();}
	public void enableAll(){for(ServerButton button:servers.values())button.enable();}
	public void updateBounds(){
		int i=0;
		for(var comp:getComponents())comp.setLocation(0,(i++)*20);
		setSize(180,20*getComponentCount());
		setLocation(parent.getWidth()-185,parent.getHeight()-getHeight()-45);
		parent.repaint();
	}
	public void paint(Graphics g){
		g.setClip(null);
		g.setColor(Game.shadow);
		g.fillRect(0,0,getWidth(),getHeight());
		Game.drawStringWithShadow((Graphics2D)g,"LAN",0,-20,180,20,Color.LIGHT_GRAY,Game.shadow);
		paintComponents(g);
	}
}