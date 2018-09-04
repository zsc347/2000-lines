package collection;

import org.junit.Test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MapTest {


    private static void assertNPE(Exception e) {
        if(! (e instanceof  NullPointerException)) {
            throw new IllegalStateException("Not an NPE");
        }
    }

    @Test
    public void testMapKeyNull() {
        Map<String, String>  hashMap = new HashMap<>();
        hashMap.put(null, "hash-null");
        assertEquals(hashMap.get(null), "hash-null");

        Map<String, String> treeMap = new HashMap<>();
        treeMap.put(null, "tree-null");
        assertEquals(treeMap.get(null), "tree-null");


        try {
            Hashtable<String, String> hashTable = new Hashtable<>();
            hashTable.put(null, "table-null");
            throw new IllegalStateException("should throw NPE");
        } catch (Exception e) {
            assertNPE(e);
        }

        try {
            ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
            concurrentMap.put(null, "con-null");
            System.out.println(concurrentMap.get(null));
        } catch (Exception e) {
            assertNPE(e);
        }
    }

    @Test
    public void testMapValueNull() {
        Map<String, String>  hashMap = new HashMap<>();
        hashMap.put("key", null);
        assertNull(hashMap.get("key"));

        Map<String, String> treeMap = new HashMap<>();
        treeMap.put("key", null);
        assertNull(treeMap.get("key"));


        try {
            Hashtable<String, String> hashTable = new Hashtable<>();
            hashTable.put("key", null);
            throw new IllegalStateException("should throw NPE");
        } catch (Exception e) {
            assertNPE(e);
        }

        try {
            ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
            concurrentMap.put("key", null);
            System.out.println(concurrentMap.get(null));
        } catch (Exception e) {
            assertNPE(e);
        }
    }

}
