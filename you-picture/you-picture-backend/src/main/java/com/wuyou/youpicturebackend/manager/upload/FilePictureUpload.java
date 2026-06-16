package com.wuyou.youpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object fileSource) {
        MultipartFile multipartFile = (MultipartFile) fileSource;
        //校验图片
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        //1.校验文件大小,小于10MB
        long fileSize = multipartFile.getSize();
        final long ONE_M =  1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 10 * ONE_M,ErrorCode.PARAMS_ERROR,"文件不能超过10M");
        //2. 校验文件后缀
        //允许上传文件的后缀/类型
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "png", "gif", "bmp", "jpeg","webp");
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object fileSource) {
        MultipartFile multipartFile = (MultipartFile) fileSource;
        return multipartFile.getOriginalFilename();
    }
    @Override
    protected void processFile(Object fileSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) fileSource;
        multipartFile.transferTo(file);
    }


}
