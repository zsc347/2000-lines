package scaiz.reflect.proxy;

import java.lang.reflect.Proxy;

public class LogProxyFactory {

  public static Object getProxy(Object target) {
    LogProxyHandler proxy = new LogProxyHandler();
    proxy.setTarget(target);
    return Proxy.newProxyInstance(target.getClass().getClassLoader(),
        target.getClass().getInterfaces(), proxy);
  }

}
