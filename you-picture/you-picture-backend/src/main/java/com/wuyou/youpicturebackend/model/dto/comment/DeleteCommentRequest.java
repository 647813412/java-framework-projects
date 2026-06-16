package com.wuyou.youpicturebackend.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除评论请求
 */
@Data
public class DeleteCommentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long id;
}
