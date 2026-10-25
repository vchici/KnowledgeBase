/**
 * 滑动窗口两道模板题（可运行版）
 *
 * 配套笔记：03-双指针.md → 滑动窗口小节
 * 编译运行：javac -encoding UTF-8 SlidingWindow.java && java SlidingWindow
 *
 * 同一个「右扩左缩、两指针只前进」骨架的两个镜像方向：
 *   LC 209 求最短窗口：不满足就扩，一满足就缩
 *   LC 3   求最长窗口：不满足才缩，能扩就扩
 * O(n) 的原因：l、r 各自最多前进 n 步，总移动 ≤ 2n，内层 while 不破坏线性。
 */
public class SlidingWindow {

    public static void main(String[] args) {
        demoMinSubArrayLen();           // LC 209
        demoLengthOfLongestSubstring(); // LC 3
    }

    // ==================== LC 209. 长度最小的子数组（求最短） ====================

    /**
     * 正整数数组，找和 >= target 的最短连续子数组长度，不存在返回 0。
     * 思路：右扩张加数；一满足 sum >= target 就尽量缩左边——正数保证缩完
     * 还可能仍满足，所以每次缩都更新最短值。
     */
    public static int minSubArrayLen(int target, int[] nums) {
        int l = 0, sum = 0, ans = Integer.MAX_VALUE;
        for (int r = 0; r < nums.length; r++) {
            sum += nums[r];                    // 右扩张
            while (sum >= target) {            // 满足 → 左收缩逼近最短
                ans = Math.min(ans, r - l + 1);
                sum -= nums[l++];
            }
        }
        return ans == Integer.MAX_VALUE ? 0 : ans;
    }

    static void demoMinSubArrayLen() {
        System.out.println("== LC 209. 长度最小的子数组（求最短窗口） ==");
        System.out.println("target=7,  [2,3,1,2,4,3] → "
                + minSubArrayLen(7, new int[]{2, 3, 1, 2, 4, 3}) + "（期望 2，子数组 [4,3]）");
        System.out.println("target=11, [1,2,3,4,5]   → "
                + minSubArrayLen(11, new int[]{1, 2, 3, 4, 5}) + "（期望 3，子数组 [3,4,5]）");
        System.out.println("target=100,[1,2,3]       → "
                + minSubArrayLen(100, new int[]{1, 2, 3}) + "（期望 0，不存在）");
        System.out.println();
    }

    // ==================== LC 3. 无重复字符的最长子串（求最长） ====================

    /**
     * 找不含重复字符的最长子串长度。
     * 思路：与 209 镜像。r 扩张新字符；若造成重复，l 收缩到重复消除为止；
     * 窗口合法时更新最长。128 计数数组代替 Set，O(1) 空间。
     */
    public static int lengthOfLongestSubstring(String s) {
        int[] cnt = new int[128];
        int l = 0, ans = 0;
        for (int r = 0; r < s.length(); r++) {
            cnt[s.charAt(r)]++;                // 右扩张
            while (cnt[s.charAt(r)] > 1) {     // 出现重复 → 收缩到消除
                cnt[s.charAt(l++)]--;
            }
            ans = Math.max(ans, r - l + 1);    // 此刻窗口合法，更新最长
        }
        return ans;
    }

    static void demoLengthOfLongestSubstring() {
        System.out.println("== LC 3. 无重复字符的最长子串（求最长窗口） ==");
        System.out.println("abcabcbb → " + lengthOfLongestSubstring("abcabcbb")
                + "（期望 3，abc）");
        System.out.println("bbbbb    → " + lengthOfLongestSubstring("bbbbb")
                + "（期望 1，b）");
        System.out.println("pwwkew   → " + lengthOfLongestSubstring("pwwkew")
                + "（期望 3，wke）");
        System.out.println("空串     → " + lengthOfLongestSubstring("") + "（期望 0）");
        System.out.println();
    }
}
