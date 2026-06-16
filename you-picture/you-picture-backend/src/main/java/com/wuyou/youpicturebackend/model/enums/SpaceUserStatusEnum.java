package com.wuyou.youpicturebackend.model.enums;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum SpaceUserStatusEnum {
    PENDING("待审批", 0),
    APPROVED("已通过", 1),
    REJECTED("已拒绝", 2);

    private final String text;
    private final int value;

    SpaceUserStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static SpaceUserStatusEnum getEnumByValue(Integer value) {
        if (Objects.isNull(value)) {
            return null;
        }
        for (SpaceUserStatusEnum status : SpaceUserStatusEnum.values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }
}
