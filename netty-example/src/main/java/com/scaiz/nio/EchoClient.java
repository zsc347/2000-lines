package com.scaiz.nio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EchoClient {

  private static final String HOST =
      System.getProperty("host", "127.0.0.1");
  private static final int PORT = Integer
      .parseInt(System.getProperty("port", "8086"));

  private void start() throws Exception {
    SocketChannel socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    new Thread(new WriteHandler(socketChannel)).start();
    try {
      TimeUnit.SECONDS.sleep(10);
      socketChannel.connect(new InetSocketAddress(HOST, PORT));

      Selector selector = Selector.open();
      socketChannel.register(selector,
          SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
      new Thread(new ReadHandler(selector)).start();
    } catch (Exception ignore) {

    }
  }

  public static void main(String[] args) throws Exception {
    new EchoClient().start();
  }

  private static class ReadHandler implements Runnable {

    private final Selector selector;

    ReadHandler(Selector selector) {
      this.selector = selector;
    }

    @Override
    public void run() {
      for (; ; ) {
        try {
          int num = this.selector.select(1000);
          if (num > 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
              if (key.isConnectable()) {
                System.out.println("client connectable");
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                byteBuffer.clear();
                key.attach(byteBuffer);
                SocketChannel channel = (SocketChannel) key.channel();
                if (channel.isConnectionPending()) {
                  while (!channel.finishConnect()) {
                    System.out.println("try finish connect ...");
                  }
                  System.out.println("client connected");
                }
              }

              if (key.isReadable()) {
                SocketChannel channel = (SocketChannel) key.channel();
                ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
                int count = channel.read(byteBuffer);
                if (count > 0) {
                  byteBuffer.flip();
                  byte[] bytes = new byte[byteBuffer.remaining()];
                  byteBuffer.get(bytes);
                  String body = new String(bytes, "utf-8");
                  System.out.println("client received: " + body);
                  byteBuffer.clear();
                }
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private class WriteHandler implements Runnable {

    private final SocketChannel socketChannel;

    WriteHandler(SocketChannel socketChannel) {
      this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
      for (; ; ) {
        BufferedReader sysIn = new BufferedReader(
            new InputStreamReader(System.in));
        try {
          String cmd = sysIn.readLine();
          ByteBuffer buffer = ByteBuffer.allocate(1024);
          buffer.put(cmd.getBytes(Charset.forName("utf-8")));
          buffer.flip();
          socketChannel.write(buffer);
          System.out.println("client send: " + cmd);
          buffer.clear();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
