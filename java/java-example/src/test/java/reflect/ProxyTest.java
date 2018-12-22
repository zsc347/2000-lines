package reflect;

import org.junit.Test;
import scaiz.reflect.HelloService;
import scaiz.reflect.impl.HelloServiceImpl;
import scaiz.reflect.proxy.LogProxyFactory;

public class ProxyTest {

  @Test
  public void testProxy() {
    HelloService helloService = (HelloService) LogProxyFactory
        .getProxy(new HelloServiceImpl());
    helloService.sayHello();
  }
}
