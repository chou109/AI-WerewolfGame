package com.werewolf.game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.werewolf.game.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 用户登录
     */
    User login(String username, String password);

    /**
     * 用户注册
     */
    boolean register(User user);

    /**
     * 更新用户信息
     */
    boolean updateUserInfo(User user);

    /**
     * 更新用户密码
     */
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
}
