package com.wuyou.youpicturebackend.model.dto.picture;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = -7805184282642204759L;

    //图片id（用于修改）
    private Long id;

    //文件地址
    private String fileUrl;

    //图片名称
    private String picName;

    //空间名字
    private Long spaceId;
}
