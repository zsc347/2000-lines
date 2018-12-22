package com.scaiz.netty.example.http.snoop;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslContext;

public class HttpSnoopClientInitializer extends
    ChannelInitializer<SocketChannel> {

  private final SslContext sslContext;

  HttpSnoopClientInitializer(SslContext sslContext) {
    this.sslContext = sslContext;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    if (sslContext != null) {
      pipeline.addLast(sslContext.newHandler(ch.alloc()));
    }
    pipeline.addLast(new HttpClientCodec());
    pipeline.addLast(new HttpContentDecompressor());
    pipeline.addLast(new HttpSnoopClientHandler());
  }
}
