package szczur4.server;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import szczur4.common.world.RegLoc;
import szczur4.common.Stats;
import szczur4.server.world.Chunk;
import szczur4.server.world.Region;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
public class ServerCore{
	public static final Random rand=new Random();
	public static EventLoopGroup bossGroup=new NioEventLoopGroup(),workerGroup=new NioEventLoopGroup();
	public static ArrayList<Channel>connections=new ArrayList<>();
	Game game;
	ChannelFuture channelFuture;
	public static boolean host=true;
	public ServerCore()throws IOException,InterruptedException{
		game=new Game();
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			try{channelFuture.channel().closeFuture().sync();}catch(Exception _){}
			IO.println("Server closed");
		}));
		int port=25565;
		channelFuture=new ServerBootstrap().group(bossGroup,workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>(){protected void initChannel(SocketChannel sc){sc.pipeline().addLast(new ChannelInboundHandlerAdapter(){
			ByteBuf dataBuffer=Unpooled.buffer();
			public void channelRegistered(ChannelHandlerContext ctx)throws Exception{
				super.channelRegistered(ctx);
				if(!host&&!connections.isEmpty()){
					ctx.channel().close();
					ctx.close();
					return;
				}
				Channel ch=ctx.channel();
				connections.add(ch);
				for(Region reg:game.regions.values())for(int x=0;x<32;x++)for(int y=0;y<32;y++)if(reg.chunks[x][y]!=null)ch.write(reg.chunks[x][y]);
				ch.writeAndFlush(game.stats);
			}
			public void channelUnregistered(ChannelHandlerContext ctx)throws Exception{
				super.channelUnregistered(ctx);
				connections.remove(ctx.channel());
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
					case('r')->{
						game.reset();
						broadcast('r');
					}
					case('f')->{
						RegLoc loc=new RegLoc(dataBuffer.readLong(),dataBuffer.readLong(),dataBuffer.readByte(),dataBuffer.readByte(),dataBuffer.readByte(),dataBuffer.readByte());
						game.fill(loc,dataBuffer.readBoolean());
						broadcast(game.getRegion(loc).getChunk(loc));
					}
					case('u')->{
						RegLoc loc=new RegLoc(dataBuffer.readLong(),dataBuffer.readLong(),dataBuffer.readByte(),dataBuffer.readByte());
						broadcast(game.getRegion(loc).getChunk(loc).unlock());
					}
					case('o')->broadcast(Unpooled.buffer().writeChar('o').writeBoolean(game.uncoverEnabled=dataBuffer.readBoolean()));
				}
				if(dataBuffer.refCnt()>0)dataBuffer.release();
				System.gc();
			}
			public void exceptionCaught(final ChannelHandlerContext ctx,final Throwable cause){
				cause.printStackTrace();
				ctx.close();
			}}).addLast(new MessageToByteEncoder<Stats>(){protected void encode(ChannelHandlerContext ctx, Stats stats, ByteBuf buf){buf.writeChar('s').writeLong(stats.flags).writeLong(stats.cleared).writeLong(stats.lost).writeBoolean(game.uncoverEnabled);}}).addLast(new MessageToByteEncoder<Character>(){protected void encode(ChannelHandlerContext channelHandlerContext, Character c, ByteBuf buf){buf.writeChar(c);}}).addLast(new MessageToByteEncoder<Chunk>(){protected void encode(ChannelHandlerContext ctx, Chunk chunk, ByteBuf buf){
			buf.writeChar('c').writeLong(chunk.parent.regX).writeLong(chunk.parent.regY).writeByte(chunk.chunkX).writeByte(chunk.chunkY);
			for(int x=0;x<16;x++)for(int y=0;y<16;y++)buf.writeByte(chunk.tiles[x][y]);
			buf.writeBoolean(chunk.locked).writeBoolean(chunk.lost);
		}});}}).option(ChannelOption.SO_BACKLOG,128).childOption(ChannelOption.SO_KEEPALIVE,true).childOption(ChannelOption.TCP_NODELAY,true).bind(port).sync();
		new Bootstrap().group(bossGroup).channel(NioDatagramChannel.class).handler(new SimpleChannelInboundHandler<DatagramPacket>(){protected void channelRead0(ChannelHandlerContext ctx,DatagramPacket packet)throws UnknownHostException{if(packet.content().toString(CharsetUtil.US_ASCII).equals("D")){ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("R"+InetAddress.getLocalHost().getHostAddress(),CharsetUtil.US_ASCII),packet.sender()));}}}).bind(25565).sync();
		IO.println("Server started on port "+port);
	}
	public static void broadcast(Object msg){for(Channel channel:connections)channel.writeAndFlush(msg);}
}