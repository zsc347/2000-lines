package com.scaiz.netty.example.http.helloworld;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;

public class HttpHelloWorldServerHandler extends ChannelInboundHandlerAdapter {

  private static final byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o',
      'r', 'l', 'd'};
  private static final AsciiString CONTENT_TYPE = AsciiString
      .cached("Content-Type");
  private static final AsciiString CONTENT_LENGTH = AsciiString
      .cached("Content-Length");
  private static final AsciiString CONNECTION = AsciiString
      .cached("Connection");
  private static final AsciiString KEEP_ALIVE = AsciiString
      .cached("keep-alive");

  @Override
  public void channelReadComplete(ChannelHandlerContext chctx) {
    chctx.flush();
  }


  @Override
  public void channelRead(ChannelHandlerContext chctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      boolean keepAlive = HttpUtil.isKeepAlive(req);
      FullHttpResponse response = new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT));
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().set(CONTENT_LENGTH,
          response.content().readableBytes());

      if (!keepAlive) {
        chctx.write(response).addListener(ChannelFutureListener.CLOSE);
      } else {
        response.headers().set(CONNECTION, KEEP_ALIVE);
        chctx.write(response);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext chctx, Throwable cause) {
    cause.printStackTrace();
    chctx.close();
  }
}
