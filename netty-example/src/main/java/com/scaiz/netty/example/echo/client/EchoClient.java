package com.scaiz.netty.example.echo.client;

import com.scaiz.netty.example.echo.client.handler.EchoClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public final class EchoClient {

  private static final boolean SSL = System.getProperty("ssl") != null;
  private static final String HOST = System.getProperty("host", "127.0.0.1");
  private static final int PORT = Integer
      .parseInt(System.getProperty("port", "8000"));

  private void start() throws Exception {
    // Configure SSL.git
    final SslContext sslCtx;
    if (SSL) {
      sslCtx = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

    } else {
      sslCtx = null;
    }

    // Configure the client.
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(group)
          .channel(NioSocketChannel.class)
          .option(ChannelOption.TCP_NODELAY, true)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
              ChannelPipeline p = ch.pipeline();
              if (sslCtx != null) {
                p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
              }
              p.addLast(new EchoClientHandler());
            }
          });

      // Start the client.
      ChannelFuture f = b.connect(HOST, PORT).sync();

      // Wait until the connection is closed.
      f.channel().closeFuture().sync();
    } finally {
      // Shut down the event loop to terminate all threads.
      group.shutdownGracefully();
    }
  }

  public static void main(String[] args) throws Exception {
    new EchoClient().start();
  }
}
