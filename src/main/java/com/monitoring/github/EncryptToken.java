package com.monitoring.github;

public class EncryptToken {
    public static void main(String[] args) throws Exception {
        String token = "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"; // put your actual token here
        String encrypted = EncryptionUtil.encrypt(token);
        System.out.println("Encrypted token:");
        System.out.println(encrypted);
    }
}