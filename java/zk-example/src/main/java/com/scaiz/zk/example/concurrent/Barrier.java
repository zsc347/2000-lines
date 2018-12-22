package com.scaiz.zk.example.concurrent;

import env.Config;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;


@Slf4j
public class Barrier extends Primitive {

  int size;

  Barrier(String address, String root, int size) {
    super(address);
    this.root = root;
    this.size = size;

    if (zk != null) {
      try {
        Stat s = zk.exists(root, false);
        if (s == null) {
          String s1 = zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
              CreateMode.PERSISTENT);
          log.info("create path: {}", s1);
        }
      } catch (KeeperException | InterruptedException e) {
        log.error("exception occur:", e);
        throw new RuntimeException(e);
      }
    }
  }

  String enter(String name) throws KeeperException, InterruptedException {
    String realPath = zk.create(root + "/" + name, new byte[0],
        Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    while (true) {
      synchronized (mutex) {
        List<String> list = zk.getChildren(root, true);
        if (list.size() < size) {
          mutex.wait();
        } else {
          return realPath;
        }
      }
    }
  }

  void leave(String name) throws KeeperException, InterruptedException {
    zk.delete(name, 0);
    while (true) {
      synchronized (mutex) {
        List<String> list = zk.getChildren(root, true);
        if (list.size() > 0) {
          mutex.wait();
        } else {
          return;
        }
      }
    }
  }

  public static class TestRunner implements Runnable {

    private final String name;
    private final Barrier barrier;

    TestRunner(String name, Barrier barrier) {
      this.name = name;
      this.barrier = barrier;
    }

    @Override
    public void run() {
      try {
        log.info("{} before enter", name);
        String realName = barrier.enter(name);
        log.info("{} enter ...", realName);
        TimeUnit.SECONDS.sleep(3);
        log.info("{} before leave", realName);
        barrier.leave(realName);
        log.info("{} leave", realName);
      } catch (Exception e) {
        log.error("exception occur: ", e);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    System.out.println(InetAddress.getLocalHost().getCanonicalHostName());
    Barrier barrier = new Barrier(Config.HOST_PORT, "/barrier", 3);

    for (int i = 0; i < 3; i++) {
      new Thread(new TestRunner("thread" + i, barrier)).start();
    }
  }
}
