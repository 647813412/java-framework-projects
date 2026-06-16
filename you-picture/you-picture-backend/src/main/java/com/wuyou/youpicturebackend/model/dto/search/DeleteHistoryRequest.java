package com.wuyou.youpicturebackend.model.dto.search;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除单条搜索历史请求
 */
@Data
public class DeleteHistoryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 历史记录ID
     */
    private Long id;
}
