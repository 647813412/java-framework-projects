package com.wuyou.youpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图像编辑请求（前端 DTO）
 */
@Data
public class CreateImageEditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 文本提示词（描述编辑内容）
     */
    private String prompt;
}
