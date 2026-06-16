package com.wuyou.youpicturebackend.model.dto.picturelike;

import lombok.Data;

@Data
public class PictureQueryRequest {
    /**
     * 当前用户是否点赞（用于筛选）
     */
    private Boolean isLiked;
}
