package com.scaiz.zk.example.watcher;

import env.Config;

public class Main {

  public static final String WATCH_NOE = "/watch";

  public static final String OUTPUT_FILE = System
      .getProperty("output", "/tmp/zk/output");

  public static void main(String[] args) {
    try {
      new Executor(Config.HOST_PORT, WATCH_NOE, OUTPUT_FILE,
          new String[]{"echo", "run process"}).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
