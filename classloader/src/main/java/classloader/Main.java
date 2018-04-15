package classloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

  private static HelloService helloService;
  private static ClassLoader cl;

  public static void main(String args[]) {
    BufferedReader stdIn = new BufferedReader(
        new InputStreamReader(System.in));
    reloadService();
    label:
    while (true) {
      try {
        System.out.print("cmd: ");
        String cmd = stdIn.readLine();
        switch (cmd) {
          case "exit":
            break label;
          case "reload":
            reloadService();
            break;
          case "hello":
            helloService.sayHello();
            break;
          default:
            System.err.println("Cmd not support !");
            break;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void reloadService() {
    cl = new DynamicLoader();
    try {
      helloService = (HelloService) cl.loadClass(
          "classloader.impl.HelloServiceImpl").newInstance();
    } catch (Exception e) {
      throw new RuntimeException("load service error", e);
    }
  }
}
