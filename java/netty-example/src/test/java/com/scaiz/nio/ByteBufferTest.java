package com.scaiz.nio;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.junit.Test;

public class ByteBufferTest {

  /**
   * use ByteBuffer to read file
   */
  @Test
  public void testByteBuffer() throws Exception {
    RandomAccessFile raFile = new RandomAccessFile("data.txt", "rw");
    FileChannel inChannel = raFile.getChannel();

    ByteBuffer buf = ByteBuffer.allocate(48);
    int read = inChannel.read(buf);
    while (read != -1) {
      System.out.println("Read " + read);
      buf.flip();
      while (buf.hasRemaining()) {
        System.out.print((char) buf.get());
      }
      buf.clear();
      read = inChannel.read(buf);
    }
    raFile.close();
  }

}
