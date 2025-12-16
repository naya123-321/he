package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    public String login(LoginDTO loginDTO) {
        Optional<User> userOpt = userRepository.findByUsername(loginDTO.getUsername());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        // 简单返回token，实际应该使用JWT
        return "token_" + user.getId() + "_" + System.currentTimeMillis();
    }

    /**
     * 用户注册
     */
    public boolean register(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (registerDTO.getSecurityQuestion() == null || registerDTO.getSecurityQuestion().trim().isEmpty()
                || registerDTO.getSecurityAnswer() == null || registerDTO.getSecurityAnswer().trim().isEmpty()) {
            throw new RuntimeException("请设置密保问题和密保答案");
        }
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setRole(registerDTO.getRole() != null ? registerDTO.getRole() : 0);
        user.setSecurityQuestion(registerDTO.getSecurityQuestion().trim());
        user.setSecurityAnswerHash(passwordEncoder.encode(registerDTO.getSecurityAnswer().trim()));
        userRepository.save(user);
        return true;
    }

    /**
     * 获取用户信息
     */
    public UserVO getUserInfo(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setRoleName(getRoleName(user.getRole()));
        return userVO;
    }

    /**
     * 根据用户名获取用户信息
     */
    public UserVO getUserInfoByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setRoleName(getRoleName(user.getRole()));
        return userVO;
    }

    /**
     * 检查用户名是否存在
     */
    public boolean checkUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * 验证用户身份
     */
    public boolean verifyIdentity(String username, String email) {
        Optional<User> userOpt = userRepository.findByUsernameAndEmail(username, email);
        return userOpt.isPresent();
    }

    /**
     * 重置密码
     */
    public boolean resetPassword(String username, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * 获取密保问题
     */
    public String getSecurityQuestion(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty()) {
            throw new RuntimeException("该账号未设置密保问题");
        }
        return user.getSecurityQuestion();
    }

    /**
     * 通过密保重置密码
     */
    public boolean resetPasswordBySecurity(String username, String securityQuestion, String securityAnswer, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isEmpty() ||
                user.getSecurityAnswerHash() == null || user.getSecurityAnswerHash().isEmpty()) {
            throw new RuntimeException("该账号未设置密保信息");
        }
        if (securityQuestion == null || securityQuestion.isEmpty() || !securityQuestion.equals(user.getSecurityQuestion())) {
            throw new RuntimeException("密保问题不匹配");
        }
        if (securityAnswer == null || securityAnswer.isEmpty() || !passwordEncoder.matches(securityAnswer, user.getSecurityAnswerHash())) {
            throw new RuntimeException("密保答案错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能小于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * 用户设置/更新密保信息（需登录）
     */
    public boolean updateSecurityInfo(Long userId, String securityQuestion, String securityAnswer) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        if (securityQuestion == null || securityQuestion.trim().isEmpty()
                || securityAnswer == null || securityAnswer.trim().isEmpty()) {
            throw new RuntimeException("密保问题和密保答案不能为空");
        }
        User user = userOpt.get();
        user.setSecurityQuestion(securityQuestion.trim());
        user.setSecurityAnswerHash(passwordEncoder.encode(securityAnswer.trim()));
        userRepository.save(user);
        return true;
    }

    /**
     * 获取用户列表（分页）
     */
    public PageResult<UserVO> getUserList(Integer role, String keyword, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<User> page;
        
        // 根据角色和关键词查询
        if (role != null && (keyword != null && !keyword.isEmpty())) {
            // 按角色和关键词查询
            page = userRepository.findByRoleAndKeyword(role, keyword, pageable);
        } else if (role != null) {
            // 只按角色查询
            page = userRepository.findByRole(role, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            // 只按关键词查询
            page = userRepository.findByKeyword(keyword, pageable);
        } else {
            // 查询所有用户
            page = userRepository.findAll(pageable);
        }
        
        List<UserVO> userVOs = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(userVOs, page.getTotalElements(), pageSize, pageNum);
    }
    
    /**
     * 将User实体转换为UserVO
     */
    private UserVO convertToVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setRoleName(getRoleName(user.getRole()));
        return userVO;
    }

    /**
     * 更新用户信息（用户自己更新）
     */
    public UserVO updateUserInfo(Long userId, UserVO userVO) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        
        // 更新允许修改的字段（用户不能修改角色）
        if (userVO.getNickname() != null) {
            user.setNickname(userVO.getNickname());
        }
        if (userVO.getEmail() != null) {
            user.setEmail(userVO.getEmail());
        }
        if (userVO.getPhone() != null) {
            user.setPhone(userVO.getPhone());
        }
        if (userVO.getAvatar() != null) {
            user.setAvatar(userVO.getAvatar());
        }
        if (userVO.getGender() != null) {
            user.setGender(userVO.getGender());
        }
        if (userVO.getBirthday() != null) {
            user.setBirthday(userVO.getBirthday());
        }
        if (userVO.getBio() != null) {
            user.setBio(userVO.getBio());
        }
        
        User saved = userRepository.save(user);
        return convertToVO(saved);
    }

    /**
     * 更新用户信息（管理员功能）
     */
    public UserVO updateUser(Long userId, UserVO userVO) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        
        // 更新允许修改的字段
        if (userVO.getNickname() != null) {
            user.setNickname(userVO.getNickname());
        }
        if (userVO.getEmail() != null) {
            user.setEmail(userVO.getEmail());
        }
        if (userVO.getPhone() != null) {
            user.setPhone(userVO.getPhone());
        }
        if (userVO.getAvatar() != null) {
            user.setAvatar(userVO.getAvatar());
        }
        if (userVO.getRole() != null) {
            user.setRole(userVO.getRole());
        }
        if (userVO.getGender() != null) {
            user.setGender(userVO.getGender());
        }
        if (userVO.getBirthday() != null) {
            user.setBirthday(userVO.getBirthday());
        }
        if (userVO.getBio() != null) {
            user.setBio(userVO.getBio());
        }
        
        User saved = userRepository.save(user);
        return convertToVO(saved);
    }

    /**
     * 删除用户（管理员功能）
     */
    public boolean deleteUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        
        // 不能删除管理员账号
        if (user.getRole() != null && user.getRole() == 2) {
            throw new RuntimeException("不能删除管理员账号");
        }
        
        userRepository.deleteById(userId);
        return true;
    }

    /**
     * 管理员重置用户密码
     */
    public boolean adminResetPassword(Long userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    /**
     * 获取角色名称
     */
    private String getRoleName(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case 0: return "宠主";
            case 1: return "服务人员";
            case 2: return "管理员";
            default: return "未知";
        }
    }
}


