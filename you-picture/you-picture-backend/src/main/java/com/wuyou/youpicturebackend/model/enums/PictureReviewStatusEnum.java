package com.wuyou.youpicturebackend.model.enums;

import lombok.Getter;
import java.util.Objects;

@Getter
public enum PictureReviewStatusEnum {
    REVIEWING("待审核",0),
    PASS("通过",1),
    REJECT("拒绝",2);

    private final String text;
    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static PictureReviewStatusEnum getEnumByValue(Integer value){
        if (Objects.isNull(value)){
            return null;
        }
        PictureReviewStatusEnum[] values = PictureReviewStatusEnum.values();
        for (PictureReviewStatusEnum status : values) {
            if(status.value==value){
                return status;
            }
        }
        return null;
    }
}
