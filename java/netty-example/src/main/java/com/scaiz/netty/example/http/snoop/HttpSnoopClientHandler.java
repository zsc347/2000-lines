package com.scaiz.netty.example.http.snoop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpSnoopClientHandler extends
    SimpleChannelInboundHandler<HttpObject> {

  private static final ObjectMapper om = new ObjectMapper();

  private Map<String, Object> result;

  static {
    om.configure(SerializationFeature.INDENT_OUTPUT, true);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
      throws Exception {
    Map<String, Object> map = new HashMap<>();

    if (msg instanceof HttpResponse) {
      result = new HashMap<>();
      HttpResponse response = (HttpResponse) msg;

      result.put("status", response.status().toString());
      result.put("version", response.protocolVersion().toString());

      Map<String, List<String>> headerMap = new HashMap<>();
      map.put("headers", headerMap);

      if (!response.headers().isEmpty()) {
        Set<String> names = response.headers().names();
        for (String name : names) {
          headerMap.put(name, response.headers().getAll(name));
        }
      }
      result.put("content-chunked",
          HttpUtil.isTransferEncodingChunked(response));
    }

    if (msg instanceof HttpContent) {
      HttpContent httpContent = (HttpContent) msg;
      result.put("content",
          httpContent.content().toString(CharsetUtil.UTF_8));
      System.out.println(om.writeValueAsString(result));
      ctx.close();
    }

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
