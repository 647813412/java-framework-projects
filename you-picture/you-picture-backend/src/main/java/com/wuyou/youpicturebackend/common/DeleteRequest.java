package com.wuyou.youpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -4307635270179223831L;
    /**
     * id
     */
    private Long id;
}
