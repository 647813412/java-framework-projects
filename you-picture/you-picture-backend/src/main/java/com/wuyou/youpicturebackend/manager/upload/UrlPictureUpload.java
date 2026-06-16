package com.wuyou.youpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate{
    @Override
    protected void processFile(Object fileSource, File file) throws IOException {
        String fileUrl = (String) fileSource;
        HttpUtil.downloadFile(fileUrl,file);
    }

    @Override
    protected String getOriginFilename(Object fileSource) {
        String fileUrl = (String) fileSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void validPicture(Object fileSource) {
        String fileUrl = (String) fileSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        try {
            //1.验证URL格式
            new URL(fileUrl); //验证是否是合法的URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }
        //2.校验URL协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR,"仅支持HTTP或HTTPS协议的文件地址");
        //3.发送HEAD请求验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            //未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES =
                        Arrays.asList("image/jpeg", "image/jpg", "image/gif", "image/webp", "image/png");
            }
            //5.校验文件大小
            String contentLengthStr = response.header("Content-length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    //限制文件大小不能超过10MB
                    final long TEN_M =  10 * 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > TEN_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过10M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小格式错误");
                }
            }
        }finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
