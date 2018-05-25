import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

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
