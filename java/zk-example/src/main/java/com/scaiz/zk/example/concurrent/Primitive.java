package com.scaiz.zk.example.concurrent;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;


@Slf4j
public class Primitive implements Watcher {

  static ZooKeeper zk = null;
  final Integer mutex;
  String root;

  Primitive(String address) {
    if (zk == null) {
      try {
        zk = new ZooKeeper(address, 3000, this);
        mutex = -1;
      } catch (IOException e) {
        log.error("err to connect zookeeper", e);
        zk = null;
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("connect to zookeeper failed");
    }
  }

  synchronized public void process(WatchedEvent event) {
    synchronized (mutex) {
      mutex.notify();
    }
  }
}
