package szczur4.client.ui;
import javax.swing.*;
import java.awt.*;
public class TxtBox extends JTextField{
	private final String alt;
	public TxtBox(String alt){this.alt=alt;}
	public void repaint(long tm,int x,int y,int width,int height){if(getParent()!=null)getParent().repaint();}
	public void paint(Graphics g){
		g.setClip(null);
		g.setColor(getBackground());
		g.fillRect(0,0,getWidth(),getHeight());
		if(getText().isEmpty()){
			g.drawString(alt,3,17);
			g.setColor(getForeground());
			g.drawString(alt,1,15);
		}
		super.paint(g);
	}
}