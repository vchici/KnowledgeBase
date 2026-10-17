import java.util.ArrayList;
import java.util.List;

public class Palindrome {
    
}

class Solution {
    public List<List<String>> partition(String s) {
        int n = s.length();
        List<List<String>> res = new ArrayList<>();
        List<String> path = new ArrayList<>();
        dfs(s, 0, 0, path, res);
        return res;
    }

    private void dfs(String s, int i, int start, List<String> path, List<List<String>> res) {
        if (i == s.length()) {
            res.add(new ArrayList<>(path));
            return;
        }
        if (i < s.length()-1) {
            dfs(s, i+1, start, path, res);
        }
        if (isPalindrome(s, start, i)) {
            path.add(s.substring(start, i));
            dfs(s, i+1, i+1, path, res);
            path.removeLast();
        }
    }

    private boolean isPalindrome(String s, int i, int j) {
        while (i < j) {
            if (s.charAt(i++) != s.charAt(j--)) {
                return false;
            }
        }
        return true;
    }
}