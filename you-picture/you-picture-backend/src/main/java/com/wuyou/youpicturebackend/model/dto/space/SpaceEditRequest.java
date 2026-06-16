package com.wuyou.youpicturebackend.model.dto.space;

import lombok.Data;
import java.io.Serializable;

@Data
public class SpaceEditRequest implements Serializable {

    private static final long serialVersionUID = 7846765948297125072L;

    /**
     * id
     */
    private Long id;

    private String avatar;

    /**
     * 空间名称
     */
    private String spaceName;
}
