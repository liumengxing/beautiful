package com.juju.beautiful.algorithm;

import java.util.Stack;

/**
 * @author juju_liu
 * @date 2020/6/20
 */
public class BracketMatch {
    public static void main(String[] args) {
        String str = "((]";
        Stack<Character> stack = new Stack<>();
        boolean result = true;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')' || c == ']' || c == '}') {
                if (!stack.empty()) {
                    Character pop = stack.pop();
                    if ((c == ')' && pop == '(') || (c == ']' && pop == '[')
                            || (c == '}' && pop == '{')) {
                    } else {
                        result = false;
                        break;
                    }
                } else{
                    result = false;
                    break;
                }
            }
        }
        System.out.println(String.format("result = %s", result));
    }
}