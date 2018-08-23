package com.scaiz.zk.example.watcher;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


@Slf4j
public class DataMonitor implements Watcher, StatCallback {

  private ZooKeeper zk;
  private String znode;
  private boolean dead;
  private DataMonitorListener listener;
  private byte prevData[];


  public DataMonitor(ZooKeeper zk, String znode,
      DataMonitorListener listener) {
    this.zk = zk;
    this.znode = znode;
    this.listener = listener;
    zk.exists(znode, true, this, null);
  }

  public boolean isDead() {
    return dead;
  }

  /**
   * Watcher callback
   */
  public void process(WatchedEvent event) {
    log.info("event {}", event);
    String path = event.getPath();
    if (event.getType() == EventType.None) {
      switch (event.getState()) {
        case SyncConnected:
          break;
        case Expired:
          dead = true;
          listener.closing(Code.SESSIONEXPIRED);
          break;
      }
    } else {
      if (path != null && path.equals(znode)) {
        zk.exists(znode, true, this, null);
      }
    }
  }

  /**
   * StatCallback callback
   */
  public void processResult(int i, String path, Object ctx, Stat stat) {
    Code rc = Code.get(i);
    log.info("process result, code {}, path {}, ctx {}, stat {}", rc, path, ctx, stat);
    boolean exists = false;
    switch (rc) {
      case OK:
        exists = true;
        break;
      case NONODE:
        exists = false;
        break;
      case NOAUTH:
        dead = true;
        listener.closing(rc);
        return;
      default:
        zk.exists(znode, true, this, null);
    }

    byte b[] = null;
    if (exists) {
      try {
        b = zk.getData(znode, false, null);
      } catch (KeeperException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        return;
      }
    }

    if ((b == null && prevData != null)
        || (b != null && !Arrays.equals(prevData, b))) {
      listener.exists(b);
      prevData = b;
    }
  }
}
