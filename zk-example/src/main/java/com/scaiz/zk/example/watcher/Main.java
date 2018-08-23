package com.scaiz.zk.example.watcher;

public class Main {

  public static void main(String[] args) {
    try {
      new Executor(Config.HOST_PORT,
          Config.WATCH_NOE,
          Config.OUTPUT_FILE,
          new String[]{"echo", "run process"}).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
