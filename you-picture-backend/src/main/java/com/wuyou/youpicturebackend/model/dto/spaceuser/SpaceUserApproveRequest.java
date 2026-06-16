package com.wuyou.youpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserApproveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 空间成员记录 ID
     */
    private Long id;

    /**
     * 审批状态：1-通过 2-拒绝
     */
    private Integer status;
}
