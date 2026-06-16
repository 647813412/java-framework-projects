package com.wuyou.youpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞请求
 */
@Data
public class LikeRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}