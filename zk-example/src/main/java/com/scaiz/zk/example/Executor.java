package com.scaiz.zk.example;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class Executor implements Watcher, DataMonitorListener {

  private final String filename;
  private final String[] exec;
  private final ZooKeeper zk;
  private final DataMonitor dm;
  private Process child;


  static final String HOST_PORT = "localhost:2181";
  static String watch = "watch";

  private Executor(String hostPort, String znode, String filename, String[] exec)
      throws IOException {
    this.filename = filename;
    this.exec = exec;

    zk = new ZooKeeper(hostPort, 3000, this);
    dm = new DataMonitor(zk, znode, null, this);
  }

  public static void main(String[] args) {
    try {
      new Executor(HOST_PORT, "/watch", "/home/zsc347/output.txt",
          new String[]{"echo", "run process"}).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void run() {
    try {
      synchronized (this) {
        while (!dm.isDead()) {
          wait();
        }
      }
    } catch (InterruptedException ignore) {

    }

  }

  public void process(WatchedEvent event) {
    dm.process(event);
  }

  public void exists(byte[] data) {
    if (data == null) {
      if (child != null) {
        System.out.println("Killing process");
        child.destroy();
        try {
          child.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        child = null;
      }
    } else {
      if (child != null) {
        System.out.println("Stopping child");
        child.destroy();
        try {
          child.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      try {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(data);
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      try {
        System.out.println("starting child");
        child = Runtime.getRuntime().exec(exec);
        new StreamWriter(child.getInputStream(), System.out);
        new StreamWriter(child.getErrorStream(), System.err);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  static class StreamWriter extends Thread {

    OutputStream os;
    InputStream is;

    StreamWriter(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
      start();
    }

    public void run() {
      byte b[] = new byte[80];
      int rc;
      try {
        while ((rc = is.read(b)) > 0) {
          os.write(b, 0, rc);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void closing(Code rc) {
    synchronized (this) {
      notifyAll();
    }
  }
}
