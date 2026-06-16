package com.wuyou.youpicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = -8082358902398121067L;

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用次数
     */
    private Long count;
}
