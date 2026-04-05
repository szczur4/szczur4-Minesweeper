package szczur4.client;
import io.netty.buffer.Unpooled;
import szczur4.client.ui.Button;
import szczur4.client.ui.Checkbox;
import szczur4.client.ui.ServerList;
import szczur4.client.ui.TxtBox;
import szczur4.client.world.Region;
import szczur4.common.Stats;
import szczur4.common.world.RegLoc;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
public class Game extends JPanel implements MouseMotionListener,MouseWheelListener{
	public static AffineTransform DEFAULT_TRANSFORM;
	public static final Color highlight=new Color(0x3fffffff,true),shadow=new Color(0x54000000,true),bg=new Color(0x21ac00);
	int mouseX=-1,mouseY=-1,tx,ty,WIDTH,HEIGHT;
	final Map<RegLoc,Region>regions=new HashMap<>();
	RegLoc scrLoc=new RegLoc(),scrEndLoc=new RegLoc();
	Button resetButton=new Button("Reset",new MouseAdapter(){public void mouseClicked(MouseEvent e){ClientCore.connection.writeAndFlush(Unpooled.buffer().writeChar('r'));}}),connectButton=new Button("Connect",new MouseAdapter(){public void mouseClicked(MouseEvent e){try{ClientCore.connect(getAddress());}catch(UnknownHostException ex){connectButton.setText("Unknown host");}}});
	Checkbox checkEnabled=new Checkbox(true),autoUncover=new Checkbox(true);
	TxtBox addressBox=new TxtBox("localhost");
	Rectangle checkRect=new Rectangle(),uncoverRect=new Rectangle();
	final String coordFormat="Region %d %d, Chunk %d %d, Tile %d %d";
	String coords=String.format(coordFormat,0,0,0,0,0,0);
	public double scale=1;
	ServerList serverList;
	Stats stats=new Stats();
	Game(){
		setLayout(null);
		resetButton.setBounds(5,85,80,20);
		checkEnabled.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){checkEnabled.setValue(!checkEnabled.getValue());repaint();}});
		autoUncover.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
			ClientCore.connection.writeAndFlush(Unpooled.buffer().writeChar('o').writeBoolean(!autoUncover.getValue()));
			repaint();
		}});
		addressBox.setBackground(shadow);
		addressBox.setForeground(Color.LIGHT_GRAY);
		addressBox.setCaretColor(Color.LIGHT_GRAY);
		addressBox.setBorder(new LineBorder(Color.LIGHT_GRAY));
		addressBox.addKeyListener(new KeyAdapter(){public void keyTyped(KeyEvent e){
			if(e.getKeyChar()==' '||addressBox.getText().length()>252)e.consume();
			connectButton.setText("Connect");
		}});
		add(resetButton);
		add(checkEnabled);
		add(autoUncover);
		add(connectButton);
		add(addressBox);
		add(serverList=new ServerList(this));
		addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){
			fill(scrLoc.add(((mouseX=(int)(e.getX()/scale))+tx)>>4,((mouseY=(int)(e.getY()/scale))+ty)>>4),e.getButton()==3);
			repaint();
		}});
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){
			checkRect=new Rectangle(getWidth()-145,5,120,20);
			checkEnabled.setBounds(getWidth()-25,5,20,20);
			uncoverRect=new Rectangle(getWidth()-145,25,120,20);
			autoUncover.setBounds(getWidth()-25,25,20,20);
			connectButton.setBounds(getWidth()-185,getHeight()-45,180,20);
			addressBox.setBounds(getWidth()-185,getHeight()-25,180,20);
			WIDTH=(int)Math.ceil(getWidth()/scale);
			HEIGHT=(int)Math.ceil(getHeight()/scale);
			scrEndLoc=scrLoc.add((WIDTH>>4)+1,(HEIGHT>>4)+1);
			serverList.updateBounds();
			repaint();
		}});
		setBackground(Color.BLACK);
		setDoubleBuffered(true);
	}
	public InetAddress getAddress()throws UnknownHostException{
		return InetAddress.getByName(addressBox.getText().isEmpty()?InetAddress.getLocalHost().getHostAddress():addressBox.getText());
	}
	public Region getRegion(RegLoc loc){
		Region reg=regions.get(loc);
		if(reg==null)regions.put(loc,reg=new Region(loc.regX,loc.regY,this));
		return reg;
	}
	public byte getTile(RegLoc loc){return getRegion(loc).getTile(loc);}
	public void reset(){
		regions.clear();
		stats.reset();
		scrLoc=new RegLoc();
		scrEndLoc=scrLoc.add((WIDTH+16)>>4,(HEIGHT+16)>>4);
		tx=0;ty=0;
		repaint();
	}
	public void fill(RegLoc loc,boolean flag){
		if(getRegion(loc).getChunk(loc)!=null&&getRegion(loc).getChunk(loc).locked){
			if(getRegion(loc).getChunk(loc).lost)ClientCore.connection.writeAndFlush(Unpooled.buffer().writeChar('u').writeLong(loc.regX).writeLong(loc.regY).writeByte(loc.chunkX).writeByte(loc.chunkY));
			return;
		}
		ClientCore.connection.writeAndFlush(Unpooled.buffer().writeChar('f').writeLong(loc.regX).writeLong(loc.regY).writeByte(loc.chunkX).writeByte(loc.chunkY).writeByte(loc.tileX).writeByte(loc.tileY).writeBoolean(flag));
	}
	public void paint(Graphics gr){
		Graphics2D g=(Graphics2D)gr;
		if(DEFAULT_TRANSFORM==null)DEFAULT_TRANSFORM=g.getTransform();
		render(g);
		g.setColor(highlight);
		g.scale(scale,scale);
		g.fillRect(mouseX-(mouseX+tx)%16,mouseY-(mouseY+ty)%16,16,16);
		g.setTransform(DEFAULT_TRANSFORM);
		drawStringWithShadow(g,coords,5,5,0,20,Color.lightGray,shadow);
		g.setColor(shadow);
		g.fillRect(5,25,15,60);
		g.drawImage(ClientCore.ss16img.get(12),7,27,16,16,null);
		g.drawImage(ClientCore.ss16img.get(10),7,47,16,16,null);
		g.drawImage(ClientCore.ss16img.get(9),7,67,16,16,null);
		drawStringWithShadow(g,": "+stats.flags,20,25,0,20,Color.lightGray,shadow);
		drawStringWithShadow(g,": "+stats.cleared,20,45,0,20,Color.lightGray,shadow);
		drawStringWithShadow(g,": "+stats.lost,20,65,0,20,Color.lightGray,shadow);
		drawStringWithShadow(g,"Enable visual Check",checkRect,Color.LIGHT_GRAY,shadow);
		drawStringWithShadow(g,"Enable Auto Uncover",uncoverRect,Color.LIGHT_GRAY,shadow);
		paintChildren(g);
		g.dispose();
	}
	public void render(Graphics2D g){
		g.scale(scale,scale);
		if(scale<1){
			g.setColor(bg);
			g.fillRect(0,0,WIDTH,HEIGHT);
		}
		else for(int x=0;x<WIDTH+17;x+=16)for(int y=0;y<HEIGHT+17;y+=16)g.drawImage(ClientCore.ss16img.get(11),x-tx,y-ty,16,16,null);
		g.translate(-16*scrLoc.tileX-tx,-16*scrLoc.tileY-ty);
		RegLoc a=new RegLoc(scrLoc.regX,scrLoc.regY,scrLoc.chunkX,scrLoc.chunkY);
		while(a.isXLessThanOrEqual(scrEndLoc)){
			while(a.isYLessThanOrEqual(scrEndLoc)){
				if(regions.get(a)!=null&&regions.get(a).chunks[a.chunkX][a.chunkY]!=null)regions.get(a).chunks[a.chunkX][a.chunkY].render(g,a.chunkX-scrLoc.chunkX+(int)((a.regX-scrLoc.regX)<<5),a.chunkY-scrLoc.chunkY+(int)((a.regY-scrLoc.regY)<<5));
				a=a.add(0,16);
			}
			a=new RegLoc(a.regX,scrLoc.regY,a.chunkX,scrLoc.chunkY).add(16,0);
		}
		g.setTransform(DEFAULT_TRANSFORM);
	}
	public static void drawStringWithShadow(Graphics2D g,String str,Rectangle rect,Color font,Color shadow){drawStringWithShadow(g,str,rect.x,rect.y,rect.width,rect.height,font,shadow);}
	public static void drawStringWithShadow(Graphics2D g,String str,int x,int y,int w,int h,Color font,Color shadow){
		g.setColor(shadow);
		Rectangle2D rect=g.getFontMetrics().getStringBounds(str,g);
		g.fillRect(x,y,(int)Math.max(rect.getWidth()+6,w),(int)Math.max(rect.getHeight()+4,h));
		g.drawString(str,x+4+((int)Math.max(0,w-rect.getWidth())>>1),y+1+(int)rect.getHeight());
		g.setColor(font);
		g.drawString(str,x+2+((int)Math.max(0,w-rect.getWidth())>>1),y-1+(int)rect.getHeight());
	}
	public void mouseDragged(MouseEvent e){
		int x=(int)(e.getX()/scale),y=(int)(e.getY()/scale);
		tx+=mouseX-x;
		ty+=mouseY-y;
		scrLoc=scrLoc.add(tx>>4,ty>>4);
		scrEndLoc=scrLoc.add((WIDTH+16)>>4,(HEIGHT+16)>>4);
		tx&=15;
		ty&=15;
		mouseX=x;
		mouseY=y;
		repaint();
	}
	public void mouseMoved(MouseEvent e){
		mouseX=(int)(e.getX()/scale);
		mouseY=(int)(e.getY()/scale);
		RegLoc tmp=scrLoc.add((mouseX+tx)>>4,(mouseY+ty)>>4);
		coords=String.format(coordFormat,tmp.regX,tmp.regY,tmp.chunkX,tmp.chunkY,tmp.tileX,tmp.tileY);
		repaint();
	}
	public void mouseWheelMoved(MouseWheelEvent e){
		scale-=e.getPreciseWheelRotation()*scale;
		scale=Math.clamp(scale,.1,5);
		tx+=(int)Math.ceil(mouseX-e.getX()/scale);
		ty+=(int)Math.ceil(mouseY-e.getY()/scale);
		scrLoc=scrLoc.add(tx>>4,ty>>4);
		tx&=15;
		ty&=15;
		mouseX=(int)(e.getX()/scale);
		mouseY=(int)(e.getY()/scale);
		WIDTH=(int)Math.ceil(getWidth()/scale);
		HEIGHT=(int)Math.ceil(getHeight()/scale);
		scrEndLoc=scrLoc.add((WIDTH+16)>>4,(HEIGHT+16)>>4);
		repaint();
	}
}