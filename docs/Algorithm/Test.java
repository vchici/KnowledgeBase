import java.util.LinkedHashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) {
        LRU lru = new LRU(10);
    }
}

class LRU {
    private final int cap;
    private final LinkedHashMap<Integer, Integer> map;
    LRU(int cap) {
        this.cap = cap;
        map = new LinkedHashMap<>(cap, 0.75f, true){
            @Override 
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > cap;
            }
        };
    }
    int get(int key) {
        return map.getOrDefault(key, -1);
    }
    void put(int key, int val) {
        map.put(key, val);
    }
}
