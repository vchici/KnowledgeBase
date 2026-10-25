/**
 * 双指针题型汇总（可运行版）
 *
 * 配套笔记：03-双指针.md
 * 编译运行：javac -encoding UTF-8 TwoPointers.java && java TwoPointers
 */
public class TwoPointers {

    public static void main(String[] args) {
        demoTwoSumII();
        demoMaxArea();
        demoThreeSum();
        demoRemoveElement();
        demoRemoveDuplicates();
        demoMoveZeroes();
        System.out.println("环形链表/入环口/合并数组：需构造链表或调用静态方法验证");
    }

    // ==================== 对撞指针 ====================

    // LC 167. 两数之和 II
    public static int[] twoSumII(int[] numbers, int target) {
        int l = 0, r = numbers.length - 1;
        while (l < r) {
            int sum = numbers[l] + numbers[r];
            if (sum == target) return new int[]{l + 1, r + 1};
            if (sum > target) r--;
            else l++;
        }
        return new int[]{-1, -1};
    }

    // LC 11. 盛最多水的容器
    public static int maxArea(int[] height) {
        int l = 0, r = height.length - 1, ans = 0;
        while (l < r) {
            ans = Math.max(ans, Math.min(height[l], height[r]) * (r - l));
            if (height[l] < height[r]) l++;
            else r--;
        }
        return ans;
    }

    // LC 15. 三数之和
    public static java.util.List<java.util.List<Integer>> threeSum(int[] nums) {
        java.util.Arrays.sort(nums);
        java.util.List<java.util.List<Integer>> ans = new java.util.ArrayList<>();
        for (int i = 0; i < nums.length - 2; i++) {
            if (nums[i] > 0) break;
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            int l = i + 1, r = nums.length - 1;
            while (l < r) {
                int sum = nums[i] + nums[l] + nums[r];
                if (sum < 0) l++;
                else if (sum > 0) r--;
                else {
                    ans.add(java.util.List.of(nums[i], nums[l], nums[r]));
                    while (l < r && nums[l] == nums[l + 1]) l++;
                    while (l < r && nums[r] == nums[r - 1]) r--;
                    l++; r--;
                }
            }
        }
        return ans;
    }

    // ==================== 同向快慢指针 ====================

    // LC 27. 移除元素
    public static int removeElement(int[] nums, int val) {
        int slow = 0;
        for (int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != val) nums[slow++] = nums[fast];
        }
        return slow;
    }

    // LC 26. 删除有序数组中的重复项
    public static int removeDuplicates(int[] nums) {
        int slow = 0;
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) nums[++slow] = nums[fast];
        }
        return slow + 1;
    }

    // LC 283. 移动零
    public static void moveZeroes(int[] nums) {
        int slow = 0;
        for (int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != 0) nums[slow++] = nums[fast];
        }
        while (slow < nums.length) nums[slow++] = 0;
    }

    // ==================== 快慢指针（速度差） ====================

    // LC 141. 环形链表
    public static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;
        }
        return false;
    }

    // LC 142. 环形链表 II
    public static ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                ListNode p = head;
                while (p != slow) { p = p.next; slow = slow.next; }
                return p;
            }
        }
        return null;
    }

    // ==================== 归并双指针 ====================

    // LC 88. 合并两个有序数组
    public static void merge(int[] nums1, int m, int[] nums2, int n) {
        int i = m - 1, j = n - 1, p = m + n - 1;
        while (j >= 0) {
            if (i >= 0 && nums1[i] > nums2[j]) nums1[p--] = nums1[i--];
            else nums1[p--] = nums2[j--];
        }
    }

    // ==================== demo ====================

    static void demoTwoSumII() {
        int[] nums = {2, 7, 11, 15};
        int[] res = twoSumII(nums, 9);
        System.out.println("LC 167: " + java.util.Arrays.toString(res) + "（期望 [1, 2]）");
    }

    static void demoMaxArea() {
        int[] h = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("LC 11: " + maxArea(h) + "（期望 49）");
    }

    static void demoThreeSum() {
        int[] nums = {-1, 0, 1, 2, -1, -4};
        System.out.println("LC 15: " + threeSum(nums) + "（期望 [[-1,-1,2],[-1,0,1]]）");
    }

    static void demoRemoveElement() {
        int[] nums = {3, 2, 2, 3};
        System.out.println("LC 27: k=" + removeElement(nums, 2) + ", arr=" + java.util.Arrays.toString(nums));
    }

    static void demoRemoveDuplicates() {
        int[] nums = {1, 1, 2};
        System.out.println("LC 26: k=" + removeDuplicates(nums) + ", arr=" + java.util.Arrays.toString(nums));
    }

    static void demoMoveZeroes() {
        int[] nums = {0, 1, 0, 3, 12};
        moveZeroes(nums);
        System.out.println("LC 283: " + java.util.Arrays.toString(nums) + "（期望 [1,3,12,0,0]）");
    }

    // ==================== 链表节点 ====================

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int x) { val = x; }
    }
}
