package com.scaiz.netty.example.securechat.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;

public class ChatServerHandler extends SimpleChannelInboundHandler<String> {

  static ChannelGroup channels = new DefaultChannelGroup(
      GlobalEventExecutor.INSTANCE);

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    ctx.pipeline()
        .get(SslHandler.class)
        .handshakeFuture()
        .addListener(new GenericFutureListener<Future<Channel>>() {
          public void operationComplete(Future<Channel> future)
              throws Exception {
            ctx.writeAndFlush("Welcome to " +
                InetAddress.getLocalHost().getHostName() + " chat service");
            ctx.writeAndFlush("Your session is protected by " +
                ctx.pipeline().get(SslHandler.class).engine().getSession()
                    .getCipherSuite()
                + " cipher suite");
            channels.add(ctx.channel());
          }
        });
  }


  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg)
      throws Exception {
    for (Channel c : channels) {
      if (c != ctx.channel()) {
        c.writeAndFlush(
            "[" + ctx.channel().remoteAddress() + "] " + msg + '\n');
      } else {
        c.writeAndFlush("[you] " + msg + '\n');
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
