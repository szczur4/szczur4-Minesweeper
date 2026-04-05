package szczur4.client.ui;
import szczur4.client.Game;
import java.awt.*;
import java.awt.event.MouseAdapter;
public class Button extends Component{
	private String text;
	private final MouseAdapter adapter;
	public Button(String text, MouseAdapter adapter){
		this.text=text;
		addMouseListener(this.adapter=adapter);
	}
	public void disable(){removeMouseListener(adapter);}
	public void enable(){addMouseListener(adapter);}
	public void setText(String text){this.text=text;}
	public void paint(Graphics gr){
		Graphics2D g=(Graphics2D)gr;
		g.setColor(Game.shadow);
		g.fillRect(0,0,getWidth(),getHeight());
		Game.drawStringWithShadow(g,text,0,0,getWidth(),getHeight(),Color.LIGHT_GRAY,Game.shadow);
	}
}