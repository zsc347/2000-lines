package com.scaiz.zk.example.concurrent;

import env.Config;
import java.nio.ByteBuffer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

@Slf4j
public class Queue extends Primitive {

  public Queue(String address, String name) {
    super(address);
    this.root = name;
    if (zk != null) {
      try {
        Stat s = zk.exists(root, false);
        if (s == null) {
          zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
              CreateMode.PERSISTENT);
        }
      } catch (KeeperException | InterruptedException e) {
        log.error("error occur: ", e);
      }
    }
  }


  void produce(int ele) throws KeeperException, InterruptedException {
    ByteBuffer b = ByteBuffer.allocate(4);
    byte[] value;
    b.putInt(ele);
    value = b.array();
    zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
        CreateMode.EPHEMERAL_SEQUENTIAL);
  }

  int consume() throws KeeperException, InterruptedException {
    Stat stat = null;
    while (true) {
      synchronized (mutex) {
        List<String> list = zk.getChildren(root, true);
        if (list.size() == 0) {
          mutex.wait();
        } else {
          Integer min = new Integer(list.get(0).substring(7));
          String minNode = list.get(0);
          for (String s : list) {
            Integer tmp = new Integer(s.substring(7));
            if (tmp < min) {
              min = tmp;
              minNode = s;
            }
          }
          String node = root + "/" + minNode;
          byte[] b = zk.getData(node, false, stat);
          zk.delete(node, 0);
          return ByteBuffer.wrap(b).getInt();
        }
      }
    }
  }


  public static void main(String[] args) {
    Queue q = new Queue(Config.HOST_PORT, "/app1");
    int queueMax = 10;

    new Thread(() -> {
      for (int i = 0; i < queueMax; i++) {
        try {
          q.produce(10 + i);
          log.info("{} produce {}", Thread.currentThread().getName(), 10 + i);
        } catch (Exception ignore) {

        }
      }
    }).start();

    new Thread(() -> {
      for (int i = 0; i < queueMax; i++) {
        try {
          int obj = q.consume();
          log.info("{} consume object: {}", Thread.currentThread().getName(),
              obj);
        } catch (Exception e) {
          log.error("error occur:", e);
        }
      }
    }).start();
  }
}
