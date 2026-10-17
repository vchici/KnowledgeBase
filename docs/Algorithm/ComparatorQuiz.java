import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

class Student implements Comparable<Student>{
    String name;
    int age;
    @Override 
    public int compareTo(Student s) {
        return this.name.compareTo(s.name);
    }
}

public class ComparatorQuiz {
    public static void main(String[] args) {
        Integer[] a = {1,3,2,4};
        int[] c =  {1,3,2,4};
        Arrays.sort(c);
        char[] b = "acdsf".toCharArray();
        PriorityQueue<Student> pq = new PriorityQueue<>(Comparator.naturalOrder());
        Arrays.sort(b);
        Arrays.sort(a, (Integer a1, Integer b1) -> a1 - b1);


        // 实测结论：有具体目标类型时，隐式 lambda 能推断，编译通过
        Comparator<String> cmp = Comparator.comparing(s -> s.length());
        System.out.println(cmp.compare("banana", "apple"));   // 期望正数（banana 更长）

        // Q3 真正会挂的形态：没有可用目标类型。取消注释看报错，再用兜底三件套修复
        // Object o = Comparator.comparing(s -> s.length());
    }
}
