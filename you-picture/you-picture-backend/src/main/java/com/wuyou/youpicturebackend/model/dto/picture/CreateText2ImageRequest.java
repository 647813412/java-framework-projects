package com.wuyou.youpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 文生图请求（前端 DTO）
 */
@Data
public class CreateText2ImageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文本提示词
     */
    private String prompt;

    /**
     * 图片尺寸，例如 "1024*1024"
     */
    private String size;
}
