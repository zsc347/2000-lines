package com.scaiz.netty.example.securechat.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class ChatServer {

  static final int PORT = Integer.parseInt(System.getProperty("port", "8092"));

  private void start() throws Exception {

    SelfSignedCertificate ssc = new SelfSignedCertificate();
    SslContext sslContext = SslContextBuilder
        .forServer(ssc.certificate(), ssc.privateKey())
        .build();

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap boot = new ServerBootstrap();

      boot.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler(LogLevel.INFO))
          .childHandler(new ChatServerInitializer(sslContext));

      boot.bind(PORT).sync().channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public static void main(String[] args) throws Exception {
    new ChatServer().start();
  }
}
