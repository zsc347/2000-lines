ForkJoinPool在Java7中开始提供。它将一个任务拆分成多个小任务并行计算，从而充分使用cpu的能力。Fork/Join并行算法是我们所熟悉的分治算法的并行版本，典型的用法如下：

````
Result solve(Problem problem) {
    if (problem is small) {
        directly solve problem
    } else {
        split problem into independent parts
        fork new subtasks to solve each part
        join all subtasks
        compose result from subresults
    }
}
````

ForkJoin框架针对其执行的任务特性对执行框架进行了定制性的优化，体现在以下几个方面,

1. 线程池是已经准备好的，通常工作线程个数与系统CPU个数相同以充分使用CPU
2. 所有的ForkJoin任务都是轻量级执行类的实例，而不是线程实例
3. 采用特殊的队列和调度原则来管理任务并通过工作线程来执行任务。
4. 提供一个简单的控制管理类来启动工作线程池


ForkJoin框架的核心在于提供轻量级调度机制。它的基本调度策略如下：
1. 每个工作线程维护自己的调度队列中的可运行任务
2. 队列以双端队列的形式被维护，支持后进先出以及先进先出
3. 对于一个给定的工作线程而言，任务所产生的子任务会被放入工作者自己的双端队列中
4. 工作线程使用后进先出，通过弹出任务来处理队列中的任务
5. 当一个工作线程本地没有任务去运行的时候，它将使用先进先出的规则尝试从随机的从别的工作线程中拿一个任务执行
6. 当一个工作线程触发了join操作，如果可能的话它将处理其他任务，
直到目标任务被告知已经结束。所有的任务都会无阻塞的完成。
7. 当一个工作线程无法再从其他线程中获取任务和失败处理的时候，它就会退出，
并经过一段时间之后再次尝试直到所有的工作线程都被告知他们处于空闲的状态。在这种情况下，他们都会阻塞到直到其他任务再度被上层调用。

核心特点： 后进先出处理自己的任务，先进先出处理窃取的任务。
优先处理自己的任务可以获得更好的局部性。从其他队列窃取任务的开销会比在自己的队列中执行pop操作的开销大。
窃取时从相反的方向进行操作可以减小竞争。
更早期窃取可以获得更大的单元任务，从而窃取线程可以在将来进行递归分解。

可以通过ForkJoin计算Fibonacci的例子来学习ForkJoin并行框架的使用
````
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
````
输出结果为
````
recur fibonacci cost 39 ms , result 165580141 
fork/join fibonacci cost 960 ms, result 165580141 
````
fork/join反而比普通的递归算法慢。个人认为原因可能是Fibonacci计算本身简单，因此导致fork/join的任务分配和管理带来的开销远超于并行计算带来的提升。