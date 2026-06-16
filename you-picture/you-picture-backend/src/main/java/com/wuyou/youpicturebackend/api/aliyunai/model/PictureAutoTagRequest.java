package com.wuyou.youpicturebackend.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 智能标签分类请求
 */
@Data
public class PictureAutoTagRequest implements Serializable {

    /**
     * 图片 ID（已有图片自动标注）
     */
    private Long pictureId;

    /**
     * 图片 URL（直接传 URL 标注）
     */
    private String pictureUrl;

    private static final long serialVersionUID = 1L;
}
