package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocketClient {
    private static final AtomicLong REQUST_ID = new AtomicLong(0);

    private final URI uri;

    public WebSocketClient(URI uri) {
        this.uri = uri;
    }

    public void run() throws Exception {
        Bootstrap b = new Bootstrap();
        try {

            String protocol = uri.getScheme();
            if (!protocol.equals("ws")) {
                throw new IllegalArgumentException("Unsupported protocol: " + protocol);
            }

            HashMap<String, String> customHeaders = new HashMap<String, String>();
            customHeaders.put("MyHeader", "MyValue");

            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final WebSocketClientHandshaker handshaker =
                    new WebSocketClientHandshakerFactory().newHandshaker(
                            uri, WebSocketVersion.V13, null, false, customHeaders);

            b.group(new NioEventLoopGroup())
             .channel(NioSocketChannel.class)
             .remoteAddress(uri.getHost(), uri.getPort())
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline pipeline = ch.pipeline();
                     pipeline.addLast("decoder", new HttpResponseDecoder());
                     pipeline.addLast("encoder", new HttpRequestEncoder());
                     pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker));
                 }
             });

            System.out.println("WebSocket Client connecting");
            Channel ch = b.connect().sync().channel();
            handshaker.handshake(ch).sync();

            while (true) {
               SecureRandom random = new SecureRandom();
               String value = new BigInteger(500, random).toString(32);
               String json = "{\"request\":" + REQUST_ID.incrementAndGet() + ", \"timestamp\":" + System.currentTimeMillis() + ", \"address\":\"localhost\", \"execs\": \"" + value + "\"}\"";
               ch.write(new TextWebSocketFrame(json));
               try {
                  Thread.sleep(10);
               } catch (InterruptedException e) {
               }
            }

            //System.out.println("WebSocket Client sending ping");
            //ch.write(new PingWebSocketFrame(Unpooled.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));

            // Close
            //System.out.println("WebSocket Client sending close");
            //ch.write(new CloseWebSocketFrame());

            // WebSocketClientHandler will close the connection when the server
            // responds to the CloseWebSocketFrame.
            //ch.closeFuture().sync();
        } finally {
            b.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        final URI uri = args.length > 0 ? new URI(args[0]) : new URI("ws://localhost:8080/orders");
        BlockingQueue<Runnable> worksQueue = new ArrayBlockingQueue<Runnable>(2);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 1000, TimeUnit.SECONDS, worksQueue);
        executor.allowCoreThreadTimeOut(true);
        while (true) {
          executor.execute(new Runnable() {
            public void run() {
               try {
                  new WebSocketClient(uri).run();
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
          });
        }
    }
}
