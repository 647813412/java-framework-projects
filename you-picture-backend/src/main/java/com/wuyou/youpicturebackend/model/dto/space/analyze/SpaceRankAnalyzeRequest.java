package com.wuyou.youpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -8283615630858214210L;
    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;
}