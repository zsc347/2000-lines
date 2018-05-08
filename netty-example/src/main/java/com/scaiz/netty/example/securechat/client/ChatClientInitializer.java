package com.scaiz.netty.example.securechat.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

public class ChatClientInitializer extends ChannelInitializer<SocketChannel> {

  private final SslContext sslContext;

  ChatClientInitializer(SslContext sslContext) {
    this.sslContext = sslContext;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(sslContext.newHandler(ch.alloc(),
        ChatClient.HOST, ChatClient.PORT))
        .addLast(new DelimiterBasedFrameDecoder(8192,
            Delimiters.lineDelimiter())).addLast(new StringDecoder())
        .addLast(new StringEncoder())
        .addLast(new ChatClientHandler());
  }
}
