package com.scaiz.zk.example.watcher;

public class Config {

  public static final String HOST_PORT = System.getProperty("HOST_PORT",
      "localhost:2181");

  public static final String OUTPUT_FILE = System.getProperty("output", "/tmp/zk/output");

  public static final String WATCH_NOE = "/watch";

  public static final int SESSION_TIMEOUT = 3000;
}
