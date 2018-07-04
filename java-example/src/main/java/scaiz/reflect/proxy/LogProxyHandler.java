package scaiz.reflect.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class LogProxyHandler implements InvocationHandler {

  private Object target;


  public void setTarget(Object target) {
    this.target = target;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    log.info("execute method {}, args {}", method, args);
    Object result;
    try {
      result = method.invoke(target, args);
    } catch (Exception e) {
      log.error("exception occur: ", e);
      throw e;
    }
    return result;
  }
}
