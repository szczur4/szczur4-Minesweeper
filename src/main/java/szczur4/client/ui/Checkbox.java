package szczur4.client.ui;
import szczur4.client.ClientCore;
import szczur4.client.Game;

import java.awt.*;
public class Checkbox extends Component{
	private boolean value;
	public Checkbox(boolean value){this.value=value;}
	public void setValue(boolean value){this.value=value;}
	public boolean getValue(){return value;}
	public void paint(Graphics gr){
		Graphics2D g=(Graphics2D)gr;
		g.setColor(Game.shadow);
		g.fillRect(0,0,getWidth(),getHeight());
		g.fillRect(3,3,getWidth()-6,getHeight()-6);
		if(value)g.drawImage(ClientCore.ss16img.get(10),2,2,getWidth()-4,getHeight()-4,null);
		g.dispose();
	}
}