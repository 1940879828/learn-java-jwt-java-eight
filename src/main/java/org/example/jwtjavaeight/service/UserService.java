package org.example.jwtjavaeight.service;

import java.util.List;
import org.example.jwtjavaeight.domain.dto.ChangePasswordRequest;
import org.example.jwtjavaeight.domain.dto.PageResponse;
import org.example.jwtjavaeight.domain.dto.ResetPasswordRequest;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
import org.example.jwtjavaeight.domain.dto.UserCreateRequest;
import org.example.jwtjavaeight.domain.dto.UserDetailResponse;
import org.example.jwtjavaeight.domain.dto.UserQueryFilter;
import org.example.jwtjavaeight.domain.dto.UserResponse;
import org.example.jwtjavaeight.domain.dto.UserUpdateRequest;

public interface UserService {

    /**
     * 分页查询用户列表
     */
    PageResponse<UserResponse> findByFilter(UserQueryFilter filter);

    /**
     * 根据ID查询用户详情
     */
    UserDetailResponse findById(Long id);

    /**
     * 创建用户
     */
    Long createUser(UserCreateRequest request);

    /**
     * 更新用户信息
     */
    void updateUser(Long id, UserUpdateRequest request);

    /**
     * 删除用户（软删除）
     */
    void deleteUser(Long id);

    /**
     * 查询用户的角色列表
     */
    List<RoleResponse> findRolesByUserId(Long userId);

    /**
     * 全量替换用户角色
     */
    void replaceUserRoles(Long userId, List<Long> roleIds);

    /**
     * 追加用户角色
     */
    void addUserRoles(Long userId, List<Long> roleIds);

    /**
     * 解绑单个角色
     */
    void removeUserRole(Long userId, Long roleId);

    /**
     * 锁定用户
     */
    void lockUser(Long id);

    /**
     * 解锁用户
     */
    void unlockUser(Long id);

    /**
     * 管理员重置用户密码
     */
    void resetPassword(Long id, ResetPasswordRequest request);

    /**
     * 用户修改自己的密码
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 获取当前用户信息
     */
    UserDetailResponse getCurrentUser(Long userId);
}
