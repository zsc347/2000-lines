package scaiz.reflect.impl;

import scaiz.reflect.HelloService;

public class HelloServiceImpl implements HelloService {

  @Override
  public void sayHello() {
      System.out.println("Hello Reflect");
  }
}
