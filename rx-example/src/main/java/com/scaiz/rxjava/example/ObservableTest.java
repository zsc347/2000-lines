package com.scaiz.rxjava.example;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.operators.observable.ObserverResourceWrapper;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ObservableTest {

  public static void testSubscribe() {
    Observable<String> observable = Observable.fromArray("1", "2", "3");

    observable.subscribe(new Observer<String>() {
      public void onSubscribe(Disposable d) {
        System.out.println("subscribe");
      }

      public void onNext(String o) {

        System.out.println("next " + o);
      }

      public void onError(Throwable e) {

        e.printStackTrace();
        System.out.println("error");
      }

      public void onComplete() {
        System.out.println("complete");
      }
    });
  }

  public static void testOb2() {

    Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
      emitter.onNext(1);
      emitter.onNext(2);
      emitter.onNext(3);
      emitter.onNext(4);
    }).subscribeOn(Schedulers.io())
        .observeOn(Schedulers.newThread())
        .subscribe(new Observer<Integer>() {

          private Disposable mDisposable;

          @Override
          public void onSubscribe(Disposable d) {
            mDisposable = d;
          }

          @Override
          public void onNext(Integer obj) {

            System.out.println(obj);
            if (obj == 4) {
              mDisposable.dispose();
            }
          }

          @Override
          public void onError(Throwable e) {
            e.printStackTrace();
          }

          @Override
          public void onComplete() {

          }
        });
  }


  private static void testThread() throws InterruptedException {

    CountDownLatch countDownLatch = new CountDownLatch(1);
    Scheduler mainScheduler = Schedulers.newThread();

    Observable<Integer> observable = Observable.create(emitter -> {
      System.out.println(Thread.currentThread().getName());
      emitter.onNext(1);
    });

    Consumer<Integer> consumer = integer -> {
      System.out.println(Thread.currentThread().getName());
      countDownLatch.countDown();
    };

    observable
        .subscribeOn(Schedulers.newThread())
        .observeOn(mainScheduler)
        .subscribe(consumer);
    countDownLatch.await();
  }


  private static void testNoBp1() throws InterruptedException {
    Scheduler mainScheduler = Schedulers.newThread();

    Observable<Integer> observable1 = Observable.create(
        (ObservableOnSubscribe<Integer>) emitter -> {
          for (int i = 0; ; i++) {
            emitter.onNext(i);
          }
        }).subscribeOn(Schedulers.io());

    Observable<String> observable2 = Observable.create(
        (ObservableOnSubscribe<String>) emitter -> {
          emitter.onNext("A");
        }
    ).subscribeOn(Schedulers.io());

    Observable.zip(observable1, observable2,
        (integer, s) -> integer + s)
        .observeOn(mainScheduler)
        .subscribe(System.out::println, Throwable::printStackTrace);

    CountDownLatch latch = new CountDownLatch(1);
    latch.await();
  }


  private static void testNoBp2() {
    Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
      for (int i = 0; ; i++) {
        emitter.onNext(i);
      }
    }).observeOn(Schedulers.newThread())
        .subscribe(i -> {
          TimeUnit.SECONDS.sleep(1);
          System.out.println("consume " + i);
        });
  }


  private static void testFlowControl() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Scheduler mainThread = Schedulers.io();
    Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
      for (int i = 0; ; i++) {
        emitter.onNext(i);
        TimeUnit.SECONDS.sleep(1);
      }
    })
        .subscribeOn(Schedulers.io())
        .observeOn(mainThread)
        .subscribe(integer -> {
          System.out.println("consume " + integer);
        });
    latch.await();
  }

  private static void testFlowControl2() throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    Scheduler mainThread = Schedulers.io();

    Observable<Integer> observable = Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
      for (int i = 0; ; i++) {
        emitter.onNext(1);
      }
    }).subscribeOn(Schedulers.io())
        .sample(2, TimeUnit.SECONDS);

    Observable<String> observable2 = Observable.create((ObservableOnSubscribe<String>) emitter -> {
      emitter.onNext("A");
    }).subscribeOn(Schedulers.io());

    Observable.zip(observable, observable2, (integer, s) -> integer + s)
        .observeOn(mainThread)
        .subscribe(s -> System.out.println("consume " + s),
            Throwable::printStackTrace);
    latch.await();
  }

  public static void main(String[] args) throws Exception {
    // testOb2();
    // testThread();
    // testBackpressure();
    // testNoBp2();
    // testFlowControl();
    testFlowControl2();
  }
}
