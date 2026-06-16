package com.wuyou.youpicturebackend.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 智能标签分类响应
 */
@Data
public class PictureAutoTagResponse implements Serializable {

    /**
     * AI 推荐的标签列表
     */
    private List<String> tags;

    /**
     * AI 推荐的分类
     */
    private String category;

    private static final long serialVersionUID = 1L;
}
