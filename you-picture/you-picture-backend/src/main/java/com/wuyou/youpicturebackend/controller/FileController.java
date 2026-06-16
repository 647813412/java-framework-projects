package com.wuyou.youpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.utils.IOUtils;
import com.wuyou.youpicturebackend.annotation.AuthCheck;
import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.manager.CosManager;
import com.wuyou.youpicturebackend.manager.upload.FilePictureUpload;
import com.wuyou.youpicturebackend.manager.upload.UrlPictureUpload;
import com.wuyou.youpicturebackend.model.constant.UserConstant;
import com.wuyou.youpicturebackend.model.dto.file.UploadPictureResult;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 用于头像上传
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UserService userService;

    /**
     * 头像上传（文件上传）
     */
    @PostMapping("/upload")
    public BaseResponse<String> fileUpload(@RequestPart("file") MultipartFile multipartFile, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String uploadPathPrefix = String.format("Avatar/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(multipartFile, uploadPathPrefix);
        return ResultUtils.success(uploadPictureResult.getUrl());
    }

    /**
     * 头像上传(url上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<String> uploadByUrl(@RequestParam("url") String fileUrl) {
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
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
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
        //直接返回url,不进行存储
        return ResultUtils.success(fileUrl);
    }


    /**
     * 文件上传
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {

        //文件目录
        // 1. 获取原始文件名
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/upload/%s", filename);
        File  file = null;
        try {
            //上传文件,createTempFile创建新的空临时文件
             file = File.createTempFile(filepath, null);
            // 3. 将上传文件转移到临时文件
             multipartFile.transferTo(file);
             //上传到对象存储中
            cosManager.putObject(filepath,file);
            //返回可访问地址
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("file upload error,filepath="+filepath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        } finally {
            if (file != null) {
                //删除临时文件
                boolean delete  = file.delete();
                if (!delete) {
                    log.error("file delete error,filepath = {}",filepath);
                }
            }
        }
    }

    /**
     * 文件下载
     * @param filepath
     * @param response
     * @throws IOException
     */
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @PostMapping("/download")
    public void DownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            //获取文件内容输入流
            // 当你开始读取这个inputStream时，文件数据才从服务器传输到客户端
            cosObjectInput = cosObject.getObjectContent();
            //处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            //写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error,filepath="+filepath,e);
           throw new  BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }
}



