package szczur4.client.ui;
import szczur4.client.ClientCore;
import szczur4.client.Game;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
public class ServerButton extends Component{
	private final InetAddress address;
	private final MouseAdapter adapter=new MouseAdapter(){public void mouseClicked(MouseEvent e){ClientCore.connect(address);}};
	public ServerButton(InetAddress address){
		setSize(180,20);
		this.address=address;
		addMouseListener(adapter);
	}
	public void disable(){removeMouseListener(adapter);}
	public void enable(){addMouseListener(adapter);}
	public void paint(Graphics gr){
		Game.drawStringWithShadow((Graphics2D)gr,address.toString(),0,0,getWidth(),getHeight(),Color.LIGHT_GRAY,Game.shadow);
		gr.setColor(Color.LIGHT_GRAY);
		gr.drawLine(0,20,getWidth(),20);
	}
}