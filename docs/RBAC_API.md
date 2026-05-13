# RBAC API Documentation

## Overview

This project implements a complete Role-Based Access Control (RBAC) system with:
- User authentication with JWT
- Role management
- Menu/Permission management
- User-Role assignment
- Role-Menu assignment

## Data Model

```
sys_user (用户表)
  └── sys_user_role (用户-角色关联)
        └── sys_role (角色表)
              └── sys_role_menu (角色-菜单关联)
                    └── sys_menu (菜单/权限表)
```

## API Endpoints

### Authentication

#### POST /auth/login
Login and get JWT token with roles and permissions

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer"
  }
}
```

The JWT token contains:
- `sub`: username
- `userId`: user ID
- `authorities`: list of roles and permissions (e.g., ["ROLE_ADMIN", "user:list", "user:add"])

### Role Management

#### GET /api/roles
Get all roles (requires `role:list` permission)

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "roleCode": "ROLE_ADMIN",
      "roleName": "超级管理员",
      "level": 1
    }
  ]
}
```

#### GET /api/roles/{id}
Get role by ID (requires `role:list` permission)

#### POST /api/roles
Create a new role (requires `role:add` permission)

**Request:**
```json
{
  "roleCode": "ROLE_MANAGER",
  "roleName": "部门经理",
  "level": 50,
  "remark": "部门经理角色"
}
```

#### PUT /api/roles/{id}
Update a role (requires `role:edit` permission)

#### DELETE /api/roles/{id}
Delete a role (requires `role:delete` permission)

#### POST /api/roles/{roleId}/menus
Assign menus to a role (requires `role:edit` permission)

**Request:**
```json
{
  "menuIds": [1, 2, 3, 4, 5]
}
```

### Menu Management

#### GET /api/menus
Get all menus (requires `role:list` permission)

#### GET /api/menus/{id}
Get menu by ID

#### GET /api/menus/role/{roleId}
Get menus assigned to a role

#### POST /api/menus
Create a new menu (requires `role:add` permission)

#### PUT /api/menus/{id}
Update a menu (requires `role:edit` permission)

#### DELETE /api/menus/{id}
Delete a menu (requires `role:delete` permission)

### User Management

#### POST /api/users/{userId}/roles
Assign roles to a user (requires `user:edit` permission)

**Request:**
```json
{
  "roleIds": [1, 2]
}
```

#### GET /api/users/{userId}/roles
Get user's roles (requires `user:list` permission)

## Permission Model

Permissions are stored in `sys_menu` table with `perms` field:
- `user:list` - View users
- `user:add` - Add users
- `user:edit` - Edit users
- `user:delete` - Delete users
- `role:list` - View roles
- `role:add` - Add roles
- `role:edit` - Edit roles
- `role:delete` - Delete roles

## Testing

### Create admin user
```sql
INSERT INTO sys_user (username, password, status) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 1);

INSERT INTO sys_user_role (user_id, role_id) 
VALUES (1, 1);
```

### Test with curl

```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get roles
curl -X GET http://localhost:8080/api/roles \
  -H "Authorization: Bearer {token}"

# Assign menus to role
curl -X POST http://localhost:8080/api/roles/1/menus \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"menuIds":[1,2,3]}'
```

## Default Data

The system includes default roles and permissions:

**Roles:**
- `ROLE_ADMIN` (id=1) - Super administrator with all permissions
- `ROLE_USER` (id=2) - Regular user with limited permissions

**Permissions:**
- System management menus
- User management permissions
- Role management permissions

## Security

- All API endpoints (except `/auth/**`) require authentication
- Method-level security with `@PreAuthorize` annotations
- JWT tokens contain roles and permissions for efficient authorization
- Passwords are hashed with BCrypt
