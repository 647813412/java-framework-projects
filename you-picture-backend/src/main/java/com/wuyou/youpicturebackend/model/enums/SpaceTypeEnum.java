package com.wuyou.youpicturebackend.model.enums;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum SpaceTypeEnum {
    PRIVATE("私有空间",0),
    TEAM("团队空间",1);

    private final String text;
    private final int value;

    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static SpaceTypeEnum getEnumByValue(Integer value){
        if (Objects.isNull(value)){
            return null;
        }
        SpaceTypeEnum[] values = SpaceTypeEnum.values();
        for (SpaceTypeEnum status : values) {
            if(status.value==value){
                return status;
            }
        }
        return null;
    }
}
