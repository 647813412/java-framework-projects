package com.wuyou.youpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 空间 ID
     */
    private Long spaceId;
}
