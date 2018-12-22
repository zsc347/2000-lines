package com.scaiz.bio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {

  private static final String HOST = System.getProperty("host", "127.0.0.1");

  private void start() {
    try (Socket socket = new Socket(HOST, EchoServer.PORT);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
    ) {
      for (; ; ) {
        BufferedReader sysIn = new BufferedReader(
            new InputStreamReader(System.in));
        String cmd = sysIn.readLine();
        if (!"bye".equals(cmd)) {
          out.println(cmd);
          System.out.println("client: " + cmd);
          out.flush();
          String response = in.readLine();
          System.out.println("server: " + response);
        } else {
          out.println(cmd);
          System.out.println("client exist!");
          socket.close();
          in.close();
          out.close();
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new EchoClient().start();
  }
}
