package com.wuyou.youpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.wuyou.youpicturebackend.config.CosClientConfig;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.model.dto.file.UploadPictureResult;
import lombok.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
//废弃注解
@Deprecated
public class FileManager {

    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig  cosClientConfig;

    /**
     * 上传文件
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        checkFile(multipartFile);
        //图片上传地址
        //1.生成uuid
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        //2.这里保存文件不用文件名
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid, FileUtil.getSuffix(originFilename));
        //3.完整路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //上传文件
        //流程，先创建临时文件 --> 在上传到cos中
        File file = null;
        try {
            //1. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            //将上传的文件内容保存到本地文件系统
            multipartFile.transferTo(file);
            //2. 上传到cos中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //封装返回结果
            //1.获取信息包含图片尺寸、格式等元数据
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            String format = imageInfo.getFormat();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0/picHeight,2).doubleValue();
            //2.封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到cos失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 检验文件
     * @param multipartFile
     */
    public void checkFile(MultipartFile multipartFile) {
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

    /**
     * 删除临时文件
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        //删除临时文件
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error,filepath={}",file.getAbsolutePath());
        }
    }

    /**
     * URL上传文件
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl,String uploadPathPrefix) {
        //todo 校验
        validPicture(fileUrl);
        //图片上传地址
        //1.生成uuid
        String uuid = RandomUtil.randomString(16);
        String originFilename = FileUtil.mainName(fileUrl);
        //2.这里保存文件不用文件名
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid, FileUtil.getSuffix(originFilename));
        //3.完整路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //上传文件
        //流程，先创建临时文件 --> 在上传到cos中
        File file = null;
        try {
            //1. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            //将通过URL将文件内容保存到本地文件系统
            HttpUtil.downloadFile(fileUrl,file);
            //2. 上传到cos中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //封装返回结果
            //1.获取信息包含图片尺寸、格式等元数据
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            String format = imageInfo.getFormat();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0/picHeight,2).doubleValue();
            //2.封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到cos失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl),ErrorCode.PARAMS_ERROR,"文件地址不能为空");
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
