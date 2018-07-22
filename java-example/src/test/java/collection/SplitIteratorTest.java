package collection;

import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import org.junit.Test;

public class SpliiteratorTest {

  @Test
  public void testSplit() {
    List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h");
    Spliterator<String> spIter = list.spliterator();

    System.out.println(spIter.characteristics());
    System.out.println(spIter.estimateSize());
    System.out.println(spIter.getExactSizeIfKnown());

  }

}
