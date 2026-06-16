package com.wuyou.youpicturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SpaceLevel implements Serializable {
    private static final long serialVersionUID = 5655456904714937511L;

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
