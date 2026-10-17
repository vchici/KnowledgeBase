import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuickSort {
    public static List<Integer> nums = new ArrayList<>(Arrays.asList(1,3,5,2,3,8,7,3,8,2,1));
    public static void quickSort(List<Integer> nums, int left, int right) {
        if (left >= right) {
            return;
        }
        int l = left, r = right;
        int pivot = left;
        int key = nums.get(pivot);
        while (left < right) {
            while (left < right && nums.get(right) >= key) {
                right--;
            }
            if (left < right) nums.set(left, nums.get(right));
            while (left < right && nums.get(left) < key) {
                left++;
            }
            if (left < right) nums.set(right, nums.get(left));
        }
        nums.set(left, key);
        quickSort(nums, l, left-1);
        quickSort(nums, left+1, r);
    }

    // public int minSubArrayLen(int target, int[] nums) {
        // Arrays.stream(nums)   
    // }

    public static void main(String[] args) {
        quickSort(nums, 0, nums.size()-1);
        System.out.println(nums);
    }
}

class LongestArray {
    public static int[] nums = new int[]{100,4,200,1,3,2};
    
}
