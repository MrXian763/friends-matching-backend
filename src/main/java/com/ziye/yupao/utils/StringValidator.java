package com.ziye.yupao.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户账号密码非法字符校验
 *
 * @author xianziye
 */
public class StringValidator {

    /**
     * 账号密码校验规则
     * @param isPassword 是否为密码
     * @param input 需要校验的数据
     * @return
     */
    public static String isValid(boolean isPassword, String input) {
        // 定义正则表达式模式
        String validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~]{8,}$";

        if (!isPassword) { // 账号校验规则
            validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?`~]{4,}$";
        }

        // 检查字符串长度是否大于8
        if (isPassword && input.length() < 8) {
            return "密码长度不能小于8";
        }

        // 检查字符串是否包含中文字符
        for (char c : input.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return "存在非法字符";
            }
        }

        // 使用正则表达式检查字符串是否符合规则
        Pattern pattern = Pattern.compile(validPattern);
        if (!isPassword && !pattern.matcher(input).matches()) {
            return "输入非法";
        }
        return null;
    }

    /**
     * QQ邮箱格式校验规则
     * @param email 需要校验的QQ邮箱
     * @return
     */
    private static final String QQ_EMAIL_PATTERN = "^[0-9a-zA-Z._%+-]+@(qq|vip\\.qq)\\.com$";
    private static final Pattern QQ_EMAIL_REGEX = Pattern.compile(QQ_EMAIL_PATTERN);
    public static boolean isValidQQEmail(String email) {
        if (email == null) {
            return false;
        }
        return QQ_EMAIL_REGEX.matcher(email).matches();
    }

    /**
     * 星球编号校验规则
     * @param planetCode 星球编号
     * @return 校验结果
     */
    public static String planetCodeValidator(String planetCode) {
        // 正则表达式匹配0到5位的数字
        String regex = "^[0-9]{1,5}$";
        boolean matchResult = planetCode.matches(regex);
        return matchResult ? null : "星球编号由1到5位数字组成";
    }

    /**
     * 电话号码校验规则
     * 号码的前三位符合中国运营商的前缀，并且整个号码恰好为11位
     * @param phoneNumber 电话号码
     * @return 校验结果
     */
    public static boolean phoneNumberValidator(String phoneNumber) {
        // 定义手机号码的正则表达式
        String regex = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";

        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(regex);

        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(phoneNumber);

        // 检查电话号码是否匹配正则表达式
        return matcher.matches();
    }
}
