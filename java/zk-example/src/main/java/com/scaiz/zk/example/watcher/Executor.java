package com.scaiz.zk.example.watcher;

import env.Config;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;


@Slf4j
public class Executor implements Watcher, DataMonitorListener {

  private final String filename;
  private final String[] exec;
  private final DataMonitor dm;
  private Process child;


  public Executor(String hostPort, String znode, String filename, String[] exec)
      throws IOException {
    this.filename = filename;
    this.exec = exec;

    ZooKeeper zk = new ZooKeeper(hostPort, Config.SESSION_TIMEOUT, this);
    dm = new DataMonitor(zk, znode, this);
  }


  public void run() {
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
        log.info("starting child");
        child = Runtime.getRuntime().exec(exec);
        new StreamWriter(child.getInputStream(), System.out);
        new StreamWriter(child.getErrorStream(), System.err);
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
