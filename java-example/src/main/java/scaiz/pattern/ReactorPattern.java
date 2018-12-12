package scaiz.pattern;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class ReactorPattern {

  public static class Event {

    String type;
    String input;

    Event(String type, String input) {
      this.type = type;
      this.input = input;
    }

    public String toString() {
      return "[type: " + type + ", input: " + input + " ]";
    }
  }


  public static class Demultiplexer {

    private BlockingQueue<Handle> blockingQueue = new LinkedBlockingDeque<>();

    Handle select() {
      try {
        return blockingQueue.take();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class Handle { // Event Producer

    private Event event;

    Demultiplexer demultiplexer;

    Handle(Demultiplexer demultiplexer) {
      this.demultiplexer = demultiplexer;
    }

    Event getEvent() {
      Event e = this.event;
      this.event = null;
      return e;
    }

    void putEvent(Event event) {
      this.event = event;
      this.demultiplexer.blockingQueue.add(this);
    }
  }

  public interface EventHandler {

    void handle(Event event);
  }

  public static class ConcreteEventHandler implements EventHandler {

    private final String type;

    ConcreteEventHandler(String type) {
      this.type = type;
    }

    @Override
    public void handle(Event event) {
      if (Objects.equals(this.type, event.type)) {
        System.out.println("Event " + event + " handled by " + Thread.currentThread().getName());
      }
    }
  }


  public static class InitiationDispatcher {

    private List<EventHandler> handlers = new LinkedList<>();

    void handle(Event event) {
      for (EventHandler handler : handlers) {
        handler.handle(event);
      }
    }

    void registerHandler(EventHandler handler) {
      handlers.add(handler);
    }

    void removeHandler(EventHandler handler) {
      handlers.remove(handler);
    }
  }


  public static void main(String args[]) {
    Demultiplexer demultiplexer = new Demultiplexer();
    Handle handle1 = new Handle(demultiplexer);
    Handle handle2 = new Handle(demultiplexer);

    new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        handle1.putEvent(new Event("accept",
            UUID.randomUUID().toString()));
      }

    }).start();

    new Thread(() -> {
      for (int i = 0; i < 10; i++) {
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        handle1.putEvent(new Event("input",
            UUID.randomUUID().toString()));
      }
    }).start();

    new Thread(() -> handle2.putEvent(new Event("input",
        UUID.randomUUID().toString()))).start();

    InitiationDispatcher dispatcher = new InitiationDispatcher();
    dispatcher.registerHandler(new ConcreteEventHandler("accept"));
    dispatcher.registerHandler(new ConcreteEventHandler("input"));

    new Thread(() -> {
      Handle handle;
      do {
        handle = demultiplexer.select();
        if (handle != null) {
          dispatcher.handle(handle.getEvent());
        }
      } while (handle != null);

    }).start();
  }
}
