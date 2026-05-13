package org.example.jwtjavaeight.enums;

public enum DataScopeEnum {
    ALL("全部数据权限"),
    DEPT("部门数据权限"),
    DEPT_AND_SUB("部门及子部门数据权限"),
    SELF("仅本人数据权限"),
    CUSTOM("自定义数据权限");

    private final String description;

    DataScopeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
