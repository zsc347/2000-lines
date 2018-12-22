package balance;

import com.sun.deploy.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class LoadBalanceTest {

  public static class Peer {

    String name;
    int weight;
    int effectiveWeight;
    int currentWeight;

    Peer(String name, int weight) {
      this.name = name;
      this.weight = weight;
      this.effectiveWeight = weight;
      this.currentWeight = 0;
    }

    @Override
    public String toString() {
      return "[" + this.weight + "," + this.effectiveWeight + ","
          + this.currentWeight + "]";
    }
  }

  @Test
  public void testRoundRobin() {
    List<Peer> peers = new ArrayList<>();
    peers.add(new Peer("a", 1));
    peers.add(new Peer("b", 2));
    peers.add(new Peer("c", 3));

    int n_REQUESTS = 12;
    for (int i = 0; i < n_REQUESTS; i++) {
      int total = 0;
      int max = -1;
      int index = 0;
      for (int j = 0; j < peers.size(); j++) {
        Peer peer = peers.get(j);
        peer.currentWeight += peer.effectiveWeight;
        total += peer.effectiveWeight;
        if (peer.currentWeight > max) {
          max = peer.currentWeight;
          index = j;
        }
      }
      peers.get(index).currentWeight -= total;
      System.out.print("select: " + peers.get(index).name + " ");
      System.out.println("state: " + peers);
    }
  }
}
