package com.example.melms.controller;

import com.example.melms.mapper.LoginMapper;
import com.example.melms.mapper.LogMapper;
import com.example.melms.pojo.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class LoginController {
    @Resource
    private LoginMapper loginMapper;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private LogMapper logMapper;

    private final Map<String, CodeEntry> emailCodeCache = new ConcurrentHashMap<>();
    @PostMapping("/req/account/login")
    public Result login(@RequestBody Map<String, String> params) {
        String name = params.get("name");
        String password = params.get("password");
        Map<String,Object> res = loginMapper.login(name, password);
        if (res != null && res.get("account_id") != null) {
            try { logMapper.addNewLog("Login Success", String.valueOf(res.get("account_id"))); } catch (Exception ignore) {}
            return Result.success("ok", res);
        }
        Integer uid = null;
        try { uid = loginMapper.findIdByName(name); } catch (Exception ignore) {}
        try {
            if (uid == null) {
                logMapper.addNewLog("Login Unknown User (name=" + String.valueOf(name) + ")", "0");
            } else {
                logMapper.addNewLog("Login Failed (name=" + String.valueOf(name) + ")", String.valueOf(uid));
            }
        } catch (Exception ignore) {}
        return Result.fail("401","Invalid credentials",null);
    }

    @PostMapping("/req/account/resetPwd")
    public Result resetPassword(@RequestBody Map<String, String> params) {
        return Result.fail("501","Not implemented",null);
    }

    @GetMapping("/req/getVCode")
    public Result getVerificationCode(@RequestParam String email) {
        try {
            if (email == null || email.isBlank()) return Result.fail("400","Email is required",null);
            // check if email exists on any account
            Map<String, Object> row = loginMapper.findByEmail(email);
            if (row == null || row.get("account_id") == null) return Result.fail("404","Email not found",null);

            String code = sixDigits();
            long expiry = System.currentTimeMillis() + 120_000; // 120s
            emailCodeCache.put(email, new CodeEntry(code, expiry));

            sendCodeMail(email, code);
            return Result.success("ok", null);
        } catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @PostMapping("/req/account/verifyCode")
    public Result verifyCode(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "");
        String code = payload.getOrDefault("code", "");
        CodeEntry entry = emailCodeCache.get(email);
        if (entry == null || !entry.valid(code)) return Result.fail("400","Invalid or expired code", null);
        return Result.success("ok", null);
    }

    @PostMapping("/req/account/commitReset")
    public Result commitReset(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.getOrDefault("email", "");
            String code = payload.getOrDefault("code", "");
            String newPwd = payload.getOrDefault("newPwd", "");
            CodeEntry entry = emailCodeCache.get(email);
            if (entry == null || !entry.valid(code)) return Result.fail("400","Invalid or expired code", null);
            if (newPwd == null || newPwd.isBlank()) return Result.fail("400","Password required", null);
            int n = loginMapper.updatePasswordByEmail(email, newPwd);
            if (n > 0) {
                emailCodeCache.remove(email);
                try {
                    String uidStr = String.valueOf(loginMapper.findByEmail(email).get("account_id"));
                    logMapper.addNewLog("Password Reset Success (email=" + String.valueOf(email) + ")", uidStr);
                } catch (Exception ignore) {}
                return Result.success("ok", null);
            }
            return Result.fail("404","Email not found", null);
        } catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    private static String sixDigits() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void sendCodeMail(String to, String code) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("rf_diicsu_rb@163.com");
        helper.setTo(to);
        helper.setSubject("Return False Medical Equipment - Password Reset Code");
        String body = "" +
                "<div style=\"font-family:system-ui,Segoe UI,Roboto,Helvetica,Arial;line-height:1.6\">" +
                "<div style=\"font-weight:700;font-size:18px;margin-bottom:8px\">Return False Medical Equipment</div>" +
                "<div>You requested to reset your password.</div>" +
                "<div style=\"margin:8px 0\">Your verification code is: " +
                "<b style=\"font-size:20px;letter-spacing:3px\">" + code + "</b></div>" +
                "<div>(The code is valid for 120 seconds). If this was not you, please contact the administrator immediately.</div>" +
                "</div>";
        helper.setText(body, true);
        mailSender.send(message);
    }

    private static class CodeEntry {
        final String code; final long expiry;
        CodeEntry(String code, long expiry) { this.code = code; this.expiry = expiry; }
        boolean valid(String c) { return System.currentTimeMillis() < expiry && code.equals(c); }
    }
}
