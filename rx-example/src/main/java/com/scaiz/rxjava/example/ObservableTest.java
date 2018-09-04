package com.scaiz.rxjava.example;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.functions.Consumer;
import java.util.concurrent.CountDownLatch;

public class ObservableTest {

    public static void testSubscribe() {
        Observable observable = Observable.fromArray("1", "2", "3");

        observable.subscribe(new Observer() {
            public void onSubscribe(Disposable d) {
                System.out.println("subscribe");
            }

            public void onNext(Object o) {

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
                if(obj == 4) {
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
    
    public static void main(String[] args) throws Exception {
        // testOb2();
        testThread();
    }
}
