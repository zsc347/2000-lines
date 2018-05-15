package com.scaiz.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class EchoServer {

  static int PORT = Integer.parseInt(System.getProperty("port", "8086"));

  public static void main(String[] args) throws Exception {
    new EchoServer().start();
  }

  private void start() throws Exception {
    ServerSocketChannel serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);
    serverChannel.socket().bind(new InetSocketAddress(PORT));
    Selector selector = Selector.open();
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    new Thread(new ChannelHandler(selector)).start();
  }

  private class ChannelHandler implements Runnable {

    private Selector selector;

    ChannelHandler(Selector selector) {
      this.selector = selector;
    }

    private void handleInput(SelectionKey selectionKey) throws IOException {
      if (selectionKey.isValid()) {
        if (selectionKey.isAcceptable()) {
          ServerSocketChannel server = (ServerSocketChannel) selectionKey
              .channel();
          SocketChannel client = server.accept();
          client.configureBlocking(false);
          client.register(selector, SelectionKey.OP_READ);
        }
        if (selectionKey.isReadable()) {
          SocketChannel client = (SocketChannel) selectionKey.channel();
          ByteBuffer buffer = ByteBuffer.allocate(64);

          int count = client.read(buffer);
          while (count > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, "utf-8");
            // only read 64 bytes one time
            System.out.println("server received: " + body);
            buffer.rewind();
            while (buffer.hasRemaining()) {
              client.write(buffer);
            }
            buffer.clear();
            count = client.read(buffer);
          }
          buffer.clear();
        }
      }
    }

    @Override
    public void run() {
      for (; ; ) {
        try {
          selector.select();
          Set<SelectionKey> selectionKeys = selector.selectedKeys();
          Iterator<SelectionKey> iterator = selectionKeys.iterator();

          while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            try {
              handleInput(selectionKey);
            } catch (Exception e) {
              e.printStackTrace();
              if (selectionKey != null) {
                selectionKey.cancel();
                if (selectionKey.channel() != null) {
                  selectionKey.channel().close();
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
}
