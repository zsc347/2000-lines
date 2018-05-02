AbstractQueueSchronizer 源码解析

AQS(AbstractQueueSchronizer)是Jdk源码中构建锁或同步装置的基础框架。
Java并发锁中，ReentrantLock, ReentrantReadWriteLock, CountdownLatch，Semaphore等经典同步工具
都是在此框架上实现的。AQS内部维护了一个FIFO(First in first out)队列来记录等待线程和激活的线程。
并使用一个32位的int来表示内部状态。对锁的获取将会构造成节点挂载在队列的尾部，当锁资源释放时，
则从头部开始向后开始唤醒相应的线程来获得锁。

基于AQS实现同步工具时，主要需要考虑以下五个protected方法

- protected boolean tryAcquire(long arg) 
- protected boolean tryRelease(long arg)

- protected long tryAcquireShared(long arg)
- protected boolean tryReleaseShared(long arg)
- protected boolean isHeldExclusively()

顾名思义，这五个方法允许子类操作AQS中维护的状态，并返回获取锁是否成功。
AQS既可以作为排他模式(Mutex, ReentrantLock, 只允许一个线程获得锁的所有权)使用，
也可以作为共享模式使用(CountdownLatch,允许多个线程获得锁的所有权).
ReadWriteLock同时具备共享和排他的特性。

对于个独占锁的获取和释放可以使用以下伪码来表示

````
// accquire
while(try_acquire) {
  if(lock_succeed) {
    break;
  } else {
    if(current thread not in queue) {
      en_queue_current_thread;
    } 
    block_current_thread;
  }
}

// release
if(release_succeed) {
  delete_head;
  unpark_successors
}
````


