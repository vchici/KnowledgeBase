// 测试可见性
public class Test {
    int i = 50;
    int j = 0;

    public void update() {
        i = 100;
    }

    public int get() {
        j = i;
        return j;
    }

    public static void main(String[] args) {
        Test t = new Test();
        Thread t1 = new Thread(() -> {
            t.update();
        });
        Thread t2 = new Thread(() -> {
            System.out.println(t.get());
        });
        t1.start();
        t2.start();
    }

}
