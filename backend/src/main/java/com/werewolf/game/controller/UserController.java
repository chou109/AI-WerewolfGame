package com.werewolf.game.controller;

import com.werewolf.game.entity.User;
import com.werewolf.game.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        User user = userService.login(username, password);
        if (user != null) {
            return Map.of("code", 200, "message", "登录成功", "data", user);
        } else {
            return Map.of("code", 400, "message", "用户名或密码错误");
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        boolean result = userService.register(user);
        if (result) {
            return Map.of("code", 200, "message", "注册成功");
        } else {
            return Map.of("code", 400, "message", "用户名已存在");
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getUserInfo(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            return Map.of("code", 200, "data", user);
        } else {
            return Map.of("code", 400, "message", "用户不存在");
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Map<String, Object> updateUserInfo(@RequestBody User user) {
        boolean result = userService.updateUserInfo(user);
        if (result) {
            return Map.of("code", 200, "message", "更新成功");
        } else {
            return Map.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 更新密码
     */
    @PutMapping("/updatePassword")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        String oldPassword = params.get("oldPassword").toString();
        String newPassword = params.get("newPassword").toString();
        boolean result = userService.updatePassword(userId, oldPassword, newPassword);
        if (result) {
            return Map.of("code", 200, "message", "密码更新成功");
        } else {
            return Map.of("code", 400, "message", "原密码错误");
        }
    }
}
