package com.scaiz.netty.example.http.snoop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class HttpSnoopServer {

  static final boolean SSL = System.getProperty("ssl") != null;
  static final Integer port = Integer.parseInt(
      System.getProperty("port", SSL ? "8443" : "8080"));

  public static void main(String[] args) throws Exception {
    SslContext sslContext;

    if (SSL) {
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      sslContext = SslContextBuilder.forServer(
          ssc.certificate(), ssc.privateKey()).build();
    } else {
      sslContext = null;
    }

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler())
          .childHandler(new HttpSnoopServerInitializer(sslContext));

      Channel ch = bootstrap.bind(port).sync().channel();
      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
