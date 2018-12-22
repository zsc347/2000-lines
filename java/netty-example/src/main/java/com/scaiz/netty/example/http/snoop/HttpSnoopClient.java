package com.scaiz.netty.example.http.snoop;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;

public class HttpSnoopClient {

  static final String URL = System.getProperty("url", "http://127.0.0.1:8000");

  public static void main(String[] args) throws Exception {
    URI uri = new URI(URL);
    String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
    String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();

    int port = uri.getPort();
    if (port == -1) {
      if ("http".equalsIgnoreCase(scheme)) {
        port = 80;
      } else {
        port = 443;
      }
    }

    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      throw new UnsupportedOperationException("only http(s) is supported");
    }

    boolean ssl = "https".equalsIgnoreCase(scheme);
    SslContext sslContext;
    if (ssl) {
      sslContext = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } else {
      sslContext = null;
    }

    EventLoopGroup eventExecutors = new NioEventLoopGroup();

    try {
      Bootstrap b = new Bootstrap();
      b.group(eventExecutors)
          .channel(NioSocketChannel.class)
          .handler(new HttpSnoopClientInitializer(sslContext));

      Channel channel = b.connect(host, port).sync().channel();

      HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
          HttpMethod.GET, uri.getRawPath());
      request.headers().set(HttpHeaderNames.HOST, host);
      request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      request.headers()
          .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.CLOSE);

      request.headers().set(HttpHeaderNames.COOKIE,
          ClientCookieEncoder.STRICT
              .encode(new DefaultCookie("my-cookie", "foo")));

      channel.writeAndFlush(request);
      channel.closeFuture().sync();
    } finally {
      eventExecutors.shutdownGracefully();
    }
  }
}
