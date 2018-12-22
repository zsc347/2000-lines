package com.scaiz.zk.example.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ForkJoinPoolTest {

  @Test
  public void test() throws Exception {
    PrintTask task = new PrintTask(0, 10000);
    ForkJoinPool pool = new ForkJoinPool();
    pool.submit(task);
    pool.awaitTermination(2, TimeUnit.MINUTES);
    pool.shutdown();
  }

  private static class PrintTask extends RecursiveAction {

    private static final int THRESHOLD = 50;
    private final int start;
    private final int end;

    PrintTask(int start, int end) {
      this.start = start;
      this.end = end;
    }

    @Override
    protected void compute() {
      if (end - start < THRESHOLD) {
        for (int i = start; i < end; i++) {
          System.out.println(
              Thread.currentThread().getName() + " execute " + i);
        }
      } else {
        int middle = start + (end - start) / 2;
        PrintTask left = new PrintTask(start, middle);
        PrintTask right = new PrintTask(middle, end);
        left.fork();
        right.fork();
      }
    }
  }
}
