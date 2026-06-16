package com.wuyou.youpicturebackend.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增评论请求
 */
@Data
public class AddCommentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID，为 null 表示顶级评论
     */
    private Long parentId;

    /**
     * 被回复用户ID，仅回复时有值
     */
    private Long replyUserId;
}
