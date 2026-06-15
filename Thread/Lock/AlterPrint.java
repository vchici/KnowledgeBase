package Lock;

public class AlterPrint {
    private int flag = 1;
    private final Object lock = new Object();
    public void printA() {
        synchronized (lock) {
            while (true) {
                while (flag != 1) {
                    try { lock.wait(); }    
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("a");
                flag = 2;
                lock.notifyAll();
            }
        }
    }
    public void printB() {
        synchronized (lock) {
            while (true) {
                while (flag != 2) {
                    try { lock.wait(); }    
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("b");
                flag = 3;
                lock.notifyAll();
            }
        }
    }
    public void printC() {
        synchronized (lock) {
            while (true) {
                while (flag != 3) {
                    try { lock.wait(); }    
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("c");
                flag = 1;
                lock.notifyAll();
            }
        }
    }
    public static void main(String[] args) {
        AlterPrint p = new AlterPrint();
        Thread a = new Thread(() -> {
            p.printA();
        });
        Thread b = new Thread(() -> {
            p.printB();
        });
        Thread c = new Thread(() -> {
            p.printC();
        });
        a.start();
        b.start();
        c.start();
    }
}
