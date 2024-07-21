package com.ziye.yupao.utils.nickname;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 用户昵称校验规则
 */
public class NicknameValidator {
    // 设置昵称的最小和最大长度
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 10;

    // 设置允许的字符集（字母、数字、下划线、汉字）
    private static final String VALID_CHARACTERS_REGEX = "^[\\w\\u4e00-\\u9fa5]+$";

    private static Trie sensitiveWordsTrie;

    static {
        sensitiveWordsTrie = new Trie();
        try {
            loadSensitiveWords("src/main/resources/sensitive_words.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadSensitiveWords(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sensitiveWordsTrie.insert(line.trim().toLowerCase());
            }
        }
    }

    public static String validateNickname(String nickname) {
        // 长度校验
        if (nickname.length() < MIN_LENGTH || nickname.length() > MAX_LENGTH) {
            return "用户昵称长度非法";
        }

        // 字符校验
        if (!Pattern.matches(VALID_CHARACTERS_REGEX, nickname)) {
            return "用户昵称存在非法字符";
        }

        // 敏感词校验
        for (int i = 0; i < nickname.length(); i++) {
            for (int j = i + 1; j <= nickname.length(); j++) {
                if (sensitiveWordsTrie.search(nickname.substring(i, j).toLowerCase())) {
                    return "用户昵称存在敏感词";
                }
            }
        }

        return null;
    }

}
