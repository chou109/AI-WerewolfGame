package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.User;
import com.werewolf.game.mapper.UserMapper;
import com.werewolf.game.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

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
            String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());
            if (md5Password.equals(user.getPassword())) {
                user.setLastLoginTime(LocalDateTime.now());
                updateById(user);
                return user;
            }
        }
        return null;
    }

    @Override
    public boolean register(User user) {
        if (findByUsername(user.getUsername()) != null) {
            return false;
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        return save(user);
    }

    @Override
    public boolean updateUserInfo(User user) {
        user.setUpdateTime(LocalDateTime.now());
        return updateById(user);
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user != null) {
            String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
            if (md5OldPassword.equals(user.getPassword())) {
                user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
                user.setUpdateTime(LocalDateTime.now());
                return updateById(user);
            }
        }
        return false;
    }
}
