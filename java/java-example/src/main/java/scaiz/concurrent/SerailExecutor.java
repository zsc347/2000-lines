package scaiz.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * 如果Executor不是线程安全的，传入的Executor如果被另外一个线程调用，
 * 两个同时调用execute会出现竞争
 *
 */
public class SerailExecutor implements Executor {

  private Queue<Runnable> tasks = new ArrayDeque<>(); // not thread safe
  private Runnable active = null;

  private final Executor executor;

  public SerailExecutor(Executor executor) {
    this.executor = executor;
  }


  @Override
  public synchronized void execute(Runnable command) {
    tasks.offer(() -> {
      try {
        command.run();
      } finally {
        scheduleNext();
      }
    });
    if (active == null) {
      scheduleNext();
    }
  }

  private synchronized void scheduleNext() {
    if ((active = tasks.poll()) != null) {
      executor.execute(active);
    }
  }
}
