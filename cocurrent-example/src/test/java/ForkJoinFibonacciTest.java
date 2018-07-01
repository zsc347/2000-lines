import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import org.junit.Test;

public class ForkJoinFibonacciTest {

  static final long[] FIBONACCI = {1, 1, 2, 3, 5, 8, 13, 21};

  @Test
  public void testFibonacci() {
    int n = 40;

    long t0 = System.nanoTime();
    long rs = fibRecur(n);
    long t1 = System.nanoTime();
    System.out.printf(
        "recur fibonacci cost %d ms , result %d \n", (t1 - t0) / 1000000, rs);

    ForkJoinPool pool = new ForkJoinPool();
    FibTask task = new FibTask(n);
    t0 = System.nanoTime();
    pool.invoke(task);
    t1 = System.nanoTime();
    System.out.printf(
        "fork/join fibonacci cost %d ms, result %d \n", (t1 - t0) / 1000000,
        rs);
  }


  private static long fibRecur(int n) {
    if (n < FIBONACCI.length) {
      return FIBONACCI[n];
    }
    return fibRecur(n - 1) + fibRecur(n - 2);
  }

  private static class FibTask extends RecursiveTask<Long> {

    static final int threshold = FIBONACCI.length - 1;
    volatile int number;

    FibTask(int n) {
      number = n;
    }

    @Override
    protected Long compute() {
      int n = number;
      if (n < threshold) {
        return FIBONACCI[n];
      } else {
        FibTask task1 = new FibTask(n - 1);
        FibTask task2 = new FibTask(n - 2);
        task1.fork();
        task2.fork();
        return task1.join() + task2.join();
      }
    }
  }
}
