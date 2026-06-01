package org.example.smartlearning.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.smartlearning.entity.User;
import org.example.smartlearning.exception.BusinessException;
import org.example.smartlearning.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.of("用户不存在");
        }
        return user;
    }

    public User getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    public void updateProfile(Long userId, User updateInfo) {
        User user = getById(userId);
        if (updateInfo.getNickname() != null) {
            user.setNickname(updateInfo.getNickname());
        }
        if (updateInfo.getAvatar() != null) {
            user.setAvatar(updateInfo.getAvatar());
        }
        if (updateInfo.getEmail() != null) {
            user.setEmail(updateInfo.getEmail());
        }
        if (updateInfo.getPhone() != null) {
            user.setPhone(updateInfo.getPhone());
        }
        if (updateInfo.getGrade() != null) {
            user.setGrade(updateInfo.getGrade());
        }
        if (updateInfo.getAge() != null) {
            user.setAge(updateInfo.getAge());
        }
        userMapper.updateById(user);
    }
}
