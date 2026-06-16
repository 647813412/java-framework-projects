package com.wuyou.youpicturebackend.model.enums;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum UserRoleEnum {
    ADMIN("管理员","admin"),
    USER("用户","user");
    private final String text;
    private final String value;

    UserRoleEnum(String key, String value) {
        this.text = key;
        this.value = value;
    }

    /**
     * 根据value获得枚举值
     * @param value
     * @return
     */
    public static UserRoleEnum getByValue(String value){
        if (Objects.isNull(value)){
            return null;
        }
        for (UserRoleEnum item : UserRoleEnum.values()){
            if (item.getValue().equals(value)){
                return item;
            }
        }
        return null;
    }
}
