package org.example.jwtjavaeight.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.example.jwtjavaeight.domain.dto.ChangePasswordRequest;
import org.example.jwtjavaeight.domain.dto.MenuTreeNode;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.ResetPasswordRequest;
import org.example.jwtjavaeight.domain.dto.UserCreateRequest;
import org.example.jwtjavaeight.domain.dto.UserDetailResponse;
import org.example.jwtjavaeight.domain.dto.UserQueryFilter;
import org.example.jwtjavaeight.domain.dto.UserResponse;
import org.example.jwtjavaeight.domain.dto.UserUpdateRequest;
import org.example.jwtjavaeight.domain.entity.SysRole;
import org.example.jwtjavaeight.domain.entity.SysUser;
import org.example.jwtjavaeight.domain.entity.SysUserRole;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.example.jwtjavaeight.exception.BusinessException;
import org.example.jwtjavaeight.exception.ResourceNotFoundException;
import org.example.jwtjavaeight.mapper.RoleMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.example.jwtjavaeight.mapper.UserRoleMapper;
import org.example.jwtjavaeight.service.MenuService;
import org.example.jwtjavaeight.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final MenuService menuService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            RoleMapper roleMapper,
            MenuService menuService,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.menuService = menuService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResponse<UserResponse> findByFilter(UserQueryFilter filter) {
        List<SysUser> users = userMapper.findByFilter(filter);
        long total = userMapper.countByFilter(filter);

        List<UserResponse> responses = users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(responses, filter.getPage(), filter.getSize(), total);
    }

    @Override
    public UserDetailResponse findById(Long id) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        UserDetailResponse response = new UserDetailResponse();
        BeanUtils.copyProperties(convertToResponse(user), response);

        List<SysRole> roles = findRolesByUserId(id);
        response.setRoles(roles);

        List<String> permissions = userMapper.findPermissionsByUserId(id);
        response.setPermissions(permissions);

        List<MenuTreeNode> menuTree = menuService.getMenuTreeByUserId(id);
        response.setMenuTree(menuTree);

        return response;
    }

    @Override
    @Transactional
    public Long createUser(UserCreateRequest request) {
        SysUser existingUser = userMapper.findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "用户名已存在");
        }

        if (request.getEmail() != null) {
            SysUser existingEmail = userMapper.findByEmail(request.getEmail());
            if (existingEmail != null) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "邮箱已被注册");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setCreateTime(new Date());

        userMapper.insert(user);
        log.info("[UserService] 创建用户成功, ID: {}, 用户名: {}", user.getId(), user.getUsername());

        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(Long id, UserUpdateRequest request) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        if (request.getEmail() != null) {
            SysUser existingEmail = userMapper.findByEmail(request.getEmail());
            if (existingEmail != null && !existingEmail.getId().equals(id)) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "邮箱已被其他用户使用");
            }
        }

        SysUser updateUser = new SysUser();
        updateUser.setId(id);
        updateUser.setEmail(request.getEmail());
        updateUser.setPhone(request.getPhone());
        updateUser.setStatus(request.getStatus());

        userMapper.updateById(updateUser);
        log.info("[UserService] 更新用户成功, ID: {}", id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
        log.info("[UserService] 删除用户成功, ID: {}", id);
    }

    @Override
    public List<org.example.jwtjavaeight.domain.dto.RoleResponse> findRolesByUserId(Long userId) {
        List<SysRole> roles = roleMapper.findRolesByUserId(userId);
        return roles.stream()
                .map(this::convertRoleToResponse)
                .collect(Collectors.toList());
    }

    private org.example.jwtjavaeight.domain.dto.RoleResponse convertRoleToResponse(SysRole role) {
        org.example.jwtjavaeight.domain.dto.RoleResponse response = new org.example.jwtjavaeight.domain.dto.RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setLevel(role.getLevel());
        if (role.getDataScope() != null) {
            response.setDataScope(org.example.jwtjavaeight.enums.DataScopeEnum.valueOf(role.getDataScope()));
        }
        response.setRemark(role.getRemark());
        return response;
    }

    @Override
    @Transactional
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        userRoleMapper.deleteByUserId(userId);

        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        SysUserRole userRole = new SysUserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());

            userRoleMapper.batchInsert(userRoles);
        }

        log.info("[UserService] 替换用户角色成功, 用户ID: {}, 角色数: {}", userId, roleIds.size());
    }

    @Override
    @Transactional
    public void addUserRoles(Long userId, List<Long> roleIds) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        List<SysUserRole> existingRoles = userRoleMapper.findByUserId(userId);
        List<Long> existingRoleIds = existingRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        List<SysUserRole> newRoles = roleIds.stream()
                .filter(roleId -> !existingRoleIds.contains(roleId))
                .map(roleId -> {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        if (!newRoles.isEmpty()) {
            userRoleMapper.batchInsert(newRoles);
            log.info("[UserService] 追加用户角色成功, 用户ID: {}, 新增角色数: {}", userId, newRoles.size());
        } else {
            log.info("[UserService] 无需追加角色, 用户ID: {}", userId);
        }
    }

    @Override
    @Transactional
    public void removeUserRole(Long userId, Long roleId) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        int deleted = userRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("UserRole", "userId=" + userId + ",roleId=" + roleId);
        }

        log.info("[UserService] 解绑用户角色成功, 用户ID: {}, 角色ID: {}", userId, roleId);
    }

    @Override
    @Transactional
    public void lockUser(Long id) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        userMapper.lockUser(id);
        log.info("[UserService] 锁定用户成功, ID: {}", id);
    }

    @Override
    @Transactional
    public void unlockUser(Long id) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        userMapper.unlockUser(id);
        log.info("[UserService] 解锁用户成功, ID: {}", id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(id, encodedPassword);
        log.info("[UserService] 管理员重置密码成功, 用户ID: {}", id);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = userMapper.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "原密码不正确");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userId, encodedPassword);
        log.info("[UserService] 用户修改密码成功, ID: {}", userId);
    }

    @Override
    public UserDetailResponse getCurrentUser(Long userId) {
        return findById(userId);
    }

    private UserResponse convertToResponse(SysUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setLocked(user.getLockTime() != null);
        return response;
    }
}
