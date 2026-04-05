package szczur4.client;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import szczur4.common.world.RegLoc;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class ClientCore extends JFrame{
	EventLoopGroup workerGroup=new NioEventLoopGroup();
	public static final ArrayList<BufferedImage>ss16img=new ArrayList<>();
	static Game game;
	public static Channel connection;
	public static InetAddress host;
	static Bootstrap boot=new Bootstrap();
	static ExecutorService executor=Executors.newFixedThreadPool(2);
	public ClientCore()throws IOException,InterruptedException{
		Runtime.getRuntime().addShutdownHook(new Thread(()->workerGroup.shutdownGracefully()));
		BufferedImage ss16=(ImageIO.read(ClientCore.class.getResource("/szczur4/ss16.png")));
		for(int i=0;i<13;i++)ss16img.add(ss16.getSubimage(i<<4,0,16,16));
		setIconImage(ss16img.get(9).getSubimage(2,2,12,12));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(game=new Game());
		setSize(500,300);
		setTitle("szczur4 Minesweeper");
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		boot.group(workerGroup);
		boot.channel(NioSocketChannel.class);
		boot.option(ChannelOption.SO_KEEPALIVE,true);
		boot.handler(new ChannelInitializer<SocketChannel>(){protected void initChannel(SocketChannel ch){ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
			ByteBuf dataBuffer=Unpooled.buffer();
			public void channelRegistered(ChannelHandlerContext ctx)throws Exception{
				super.channelRegistered(ctx);
				connection=ctx.channel();
			}
			public void channelUnregistered(ChannelHandlerContext ctx)throws Exception{
				super.channelUnregistered(ctx);
				IO.println("Disconnected from "+ctx.channel().remoteAddress());
				connection.close();
			}
			public void channelRead(ChannelHandlerContext ctx,Object msg){
				if(!dataBuffer.isWritable())dataBuffer=Unpooled.buffer();
				ByteBuf buf=(ByteBuf)msg;
				dataBuffer.writeBytes(buf);
				buf.release();
			}
			public void channelReadComplete(ChannelHandlerContext ctx)throws Exception{
				super.channelReadComplete(ctx);
				while(dataBuffer.readableBytes()>0)switch(dataBuffer.readByte()){
					case('s')->{
						game.stats.flags=dataBuffer.readLong();
						game.stats.cleared=dataBuffer.readLong();
						game.stats.lost=dataBuffer.readLong();
						game.autoUncover.setValue(dataBuffer.readBoolean());
					}
					case('c')->{
						RegLoc loc=new RegLoc(dataBuffer.readLong(),dataBuffer.readLong(),dataBuffer.readByte(),dataBuffer.readByte());
						for(int x=0;x<16;x++)for(int y=0;y<16;y++)game.getRegion(loc).getChunk(loc).tiles[x][y]=dataBuffer.readByte();
						game.getRegion(loc).getChunk(loc).locked=dataBuffer.readBoolean();
						game.getRegion(loc).getChunk(loc).lost=dataBuffer.readBoolean();
						game.getRegion(loc).getChunk(loc).createUnlockInfo();
					}
					case('l')->{
						RegLoc loc=new RegLoc(dataBuffer.readLong(),dataBuffer.readLong(),dataBuffer.readByte(),dataBuffer.readByte());
						game.getRegion(loc).getChunk(loc).locked=dataBuffer.readBoolean();
						game.getRegion(loc).getChunk(loc).lost=dataBuffer.readBoolean();
						game.getRegion(loc).getChunk(loc).mines=dataBuffer.readByte();
					}
					case('U')->{
						RegLoc loc=new RegLoc(dataBuffer.readLong(),dataBuffer.readLong(),dataBuffer.readByte(),dataBuffer.readByte(),dataBuffer.readByte(),dataBuffer.readByte());
						game.getRegion(loc).getChunk(loc).tiles[loc.tileX][loc.tileY]=dataBuffer.readByte();
					}
					case('r')->game.reset();
					case('o')->{
						game.autoUncover.setValue(dataBuffer.readBoolean());
						game.repaint();
					}
				}
				dataBuffer.clear();
				game.repaint();
			}
			public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){
				cause.printStackTrace();
				ctx.close();
			}});
		}});
		Set<InetAddress>tmpPeers=new HashSet<>();
		ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
		Channel channel=new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class).handler(new SimpleChannelInboundHandler<DatagramPacket>(){protected void channelRead0(ChannelHandlerContext ctx,DatagramPacket packet) throws UnknownHostException{
			String msg=packet.content().toString(CharsetUtil.US_ASCII);
			if(msg.startsWith("R"))tmpPeers.add(InetAddress.getByName(msg.substring(1)));
		}}).option(ChannelOption.SO_BROADCAST,true).bind(0).sync().channel();
		try{channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("D",CharsetUtil.US_ASCII),new InetSocketAddress(InetAddress.getByName("255.255.255.255"),25565))).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);}catch(Exception e){System.err.println("Failed to send broadcast: "+e.getMessage());}
		scheduler.scheduleAtFixedRate(()->{
            game.serverList.addPeers(tmpPeers);
			tmpPeers.clear();
			try{channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("D",CharsetUtil.US_ASCII),new InetSocketAddress(InetAddress.getByName("255.255.255.255"),25565))).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);}catch(Exception e){System.err.println("Failed to send broadcast: "+e.getMessage());}
		},2,5,TimeUnit.SECONDS);
		setVisible(true);
		connect(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
	}
	public static void connect(InetAddress host){
		if(host==ClientCore.host){
			System.err.println("Already connected to "+host);
			game.connectButton.setText("Already connected");
			return;
		}
		executor.submit(()->{
			game.reset();
			game.connectButton.setText("Connecting...");
			game.repaint();
			game.connectButton.setEnabled(false);
			game.serverList.disableAll();
			InetAddress previous=ClientCore.host;
			try{
				if(connection!=null)connection.close().sync();
				IO.println("Trying to connect to "+host);
				boot.connect(host,25565).sync();
				ClientCore.host=host;
				game.connectButton.setText("Connect");
			}catch(Exception _){
				System.err.println("Failed to connect to "+host);
				game.connectButton.setText("Failed");
				ClientCore.host=previous;
				try{
					connection.close().sync();
					IO.println("Trying to connect to previous host");
					boot.connect(ClientCore.host,25565).sync();
				}catch(Exception _){
					System.err.println("Failed to reconnect to previous host!");
					System.exit(1);
				}
			}
			game.connectButton.setEnabled(true);
			game.serverList.enableAll();
			game.repaint();
		});
	}
}