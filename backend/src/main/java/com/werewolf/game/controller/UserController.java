package com.werewolf.game.controller;

import com.werewolf.game.entity.User;
import com.werewolf.game.service.UserService;
import com.werewolf.game.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.werewolf.game.util.MapUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器。登录成功后签发 JWT，任何接口都不再返回密码字段。
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
        if (user == null) {
            return MapUtil.of("code", 400, "message", "用户名或密码错误");
        }
        String token = JwtUtil.createToken(user.getId());
        return MapUtil.of("code", 200, "message", "登录成功", "data", publicUser(user), "token", token);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()
                || user.getPassword() == null || user.getPassword().length() < 6) {
            return MapUtil.of("code", 400, "message", "用户名不能为空，密码至少6位");
        }
        boolean result = userService.register(user);
        if (result) {
            return MapUtil.of("code", 200, "message", "注册成功");
        } else {
            return MapUtil.of("code", 400, "message", "用户名已存在");
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getUserInfo(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            return MapUtil.of("code", 200, "data", publicUser(user));
        } else {
            return MapUtil.of("code", 400, "message", "用户不存在");
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Map<String, Object> updateUserInfo(@RequestBody User user) {
        if (user.getId() == null) {
            return MapUtil.of("code", 400, "message", "用户编号无效");
        }
        boolean result = userService.updateUserInfo(user);
        if (result) {
            return MapUtil.of("code", 200, "message", "更新成功");
        } else {
            return MapUtil.of("code", 400, "message", "更新失败");
        }
    }

    /**
     * 更新密码
     */
    @PutMapping("/updatePassword")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> params) {
        Long userId = parseLong(params.get("userId"));
        String oldPassword = params.get("oldPassword") == null ? "" : params.get("oldPassword").toString();
        String newPassword = params.get("newPassword") == null ? "" : params.get("newPassword").toString();
        if (userId == null || newPassword.length() < 6) {
            return MapUtil.of("code", 400, "message", "参数不完整或新密码过短");
        }
        boolean result = userService.updatePassword(userId, oldPassword, newPassword);
        if (result) {
            return MapUtil.of("code", 200, "message", "密码更新成功");
        } else {
            return MapUtil.of("code", 400, "message", "原密码错误");
        }
    }

    private Map<String, Object> publicUser(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatar", user.getAvatar());
        result.put("status", user.getStatus());
        result.put("createTime", user.getCreateTime());
        result.put("lastLoginTime", user.getLastLoginTime());
        return result;
    }

    private Long parseLong(Object value) {
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
