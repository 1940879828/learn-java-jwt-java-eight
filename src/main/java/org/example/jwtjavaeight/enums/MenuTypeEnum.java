package org.example.jwtjavaeight.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum MenuTypeEnum {
    DIR(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final Integer code;
    private final String description;

    MenuTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

    public static MenuTypeEnum fromCode(Integer code) {
        for (MenuTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid menu type code: " + code);
    }
}
