package com.scaiz.zk.example.watcher;

import org.apache.zookeeper.KeeperException.Code;

public interface DataMonitorListener {

  void exists(byte data[]);

  void closing(Code rc);
}
