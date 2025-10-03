package cn.popcraft.invitesystem.util;

import java.security.SecureRandom;

/**
 * 邀请码生成器
 */
public class CodeGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 生成指定长度的随机邀请码
     * @param length 邀请码长度
     * @return 随机邀请码
     */
    public static String generateCode(int length) {
        if (length <= 0) {
            length = 8; // 默认长度
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成默认长度的随机邀请码
     * @return 随机邀请码
     */
    public static String generateCode() {
        return generateCode(8);
    }
}