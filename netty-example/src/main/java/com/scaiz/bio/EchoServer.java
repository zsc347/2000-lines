package com.scaiz.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {

  static int PORT = Integer.parseInt(System.getProperty("port", "8086"));

  private void start() {
    try (ServerSocket ss = new ServerSocket(PORT)) {
      while (true) {
        Socket s = ss.accept();
        System.out.println("Get new socket: " + s);
        try {
          new Thread(new ChildHandler(s)).start();
        } catch (Exception e) {
          s.close();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private static class ChildHandler implements Runnable {

    private InputStream in;
    private OutputStream out;

    private Socket socket;

    ChildHandler(Socket s) throws IOException {
      this.socket = s;
      in = s.getInputStream();
      out = s.getOutputStream();
    }

    public void run() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      PrintWriter pw = new PrintWriter(out, true);
      for (; ; ) {
        try {
          String request = reader.readLine();
          if (request != null) {
            System.out.println("receive: " + request);
            if ("bye".equals(request)) {
              break;
            } else {
              pw.println(request);
              pw.flush();
            }
          } else {
            System.out.println("get null message, socket close");
            this.socket.close();
            break;
          }
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
      }
    }
  }

  public static void main(String[] args) {
    new EchoServer().start();
  }
}
