package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class WebSocketServer {

	private final int port;

	public WebSocketServer(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		ServerBootstrap b = new ServerBootstrap();
		try {
			b.group(new NioEventLoopGroup(), new NioEventLoopGroup())
					.channel(NioServerSocketChannel.class).localAddress(port)
					.childHandler(new WebSocketServerInitializer());

			Channel ch = b.bind().sync().channel();

			ch.closeFuture().sync();
		} finally {
			b.shutdown();
		}
	}

	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		new WebSocketServer(port).run();
	}
}
