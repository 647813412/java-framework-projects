package com.wuyou.youpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 搜索历史
 * @TableName search_history
 */
@TableName(value ="search_history")
@Data
public class SearchHistory implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索类型：picture/user/space
     */
    private String searchType;

    /**
     * 搜索时间
     */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}