package collection;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.junit.Test;

public class ListTest {

  @Test
  public void testAddNull() {
    ArrayList list = new ArrayList();
    list.add(null);
    assertTrue(list.contains(null));
    assertTrue(list.containsAll(Collections.singleton(null)));
  }


  @Test
  public void testCollectionOp() {
    ArrayList<String> list1 = new ArrayList<>();
    list1.addAll(Arrays.asList("a", "b"));

    ArrayList<String> list2 = new ArrayList<>();
    list2.addAll(Arrays.asList("b", "c"));

    list1.retainAll(list2);
    assertTrue(list1.contains("b") && list1.size() == 1);

    list1.addAll(list2);
    assertTrue(list1.size() == 3);

    list1.removeAll(list2);
    assertTrue(list1.isEmpty());
  }


  @Test
  public void testListIter() {
    //LinkedList<String> list = new LinkedList<>();
    ArrayList<String> list = new ArrayList<>();
    list.addAll(Arrays.asList("a", "b", "c"));

    List<String> checkList = new LinkedList<>();
    ListIterator<String> listIter = list.listIterator();
    while (listIter.hasNext()) {
      checkList.add(listIter.next());
    }

    assertTrue(checkList.equals(list));
    checkList.clear();

    while (listIter.hasPrevious()) {
      checkList.add(listIter.previous());
    }
    Collections.reverse(checkList);
    assertTrue(checkList.equals(list));

    while (listIter.hasNext()) {
      listIter.next();
      listIter.remove();
    }
    assertTrue(list.isEmpty());
  }
}
