package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.User;
import com.werewolf.game.mapper.UserMapper;
import com.werewolf.game.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    @Override
    public User login(String username, String password) {
        User user = findByUsername(username);
        if (user != null) {
            String md5Password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
            if (MessageDigest.isEqual(
                    md5Password.getBytes(StandardCharsets.UTF_8),
                    user.getPassword().getBytes(StandardCharsets.UTF_8))) {
                user.setLastLoginTime(LocalDateTime.now());
                updateById(user);
                return user;
            }
        }
        return null;
    }

    @Override
    public int register(User user) {
        if (findByUsername(user.getUsername()) != null) {
            return 1;
        }
        if (lambdaQuery().eq(User::getEmail, user.getEmail()).one() != null) {
            return 2;
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        return save(user) ? 0 : -1;
    }

    @Override
    public boolean updateUserInfo(User user) {
        // 个人信息接口不允许修改密码，防止请求体覆盖密码字段。
        user.setPassword(null);
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user != null) {
            String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes(StandardCharsets.UTF_8));
            if (MessageDigest.isEqual(
                    md5OldPassword.getBytes(StandardCharsets.UTF_8),
                    user.getPassword().getBytes(StandardCharsets.UTF_8))) {
                user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes(StandardCharsets.UTF_8)));
                user.setUpdateTime(LocalDateTime.now());
                return updateById(user);
            }
        }
        return false;
    }
}
