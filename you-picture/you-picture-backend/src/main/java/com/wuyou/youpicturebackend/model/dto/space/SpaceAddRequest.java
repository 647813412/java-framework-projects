package com.wuyou.youpicturebackend.model.dto.space;

import lombok.Data;
import java.io.Serializable;


@Data
public class SpaceAddRequest implements Serializable {
    private static final long serialVersionUID = -1911572428668586518L;

    /**
     * 空间名称
     */
    private String spaceName;

    private String avatar;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;
}
