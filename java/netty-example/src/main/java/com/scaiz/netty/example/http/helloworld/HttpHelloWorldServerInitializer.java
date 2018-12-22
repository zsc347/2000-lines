package com.scaiz.netty.example.http.helloworld;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;

public class HttpHelloWorldServerInitializer extends
    ChannelInitializer<SocketChannel> {

  private final SslContext sslContext;

  HttpHelloWorldServerInitializer(SslContext sslContext) {
    this.sslContext = sslContext;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      pipeline.addLast(sslContext.newHandler(ch.alloc()));
    }
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new HttpServerExpectContinueHandler());
    pipeline.addLast(new HttpHelloWorldServerHandler());
  }
}
