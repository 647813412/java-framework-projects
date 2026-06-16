package com.wuyou.youpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
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
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.wuyou.youpicturebackend.config.CosClientConfig;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.manager.CosManager;
import com.wuyou.youpicturebackend.model.dto.file.UploadPictureResult;
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

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig  cosClientConfig;

    /**
     * 上传文件
     * @param fileSource  文件源
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object fileSource,String uploadPathPrefix) {
        //参数校验
        validPicture(fileSource);
        //图片上传地址
        //1.生成uuid
        String uuid = RandomUtil.randomString(16);
        //获取原文件名
        String originFilename = getOriginFilename(fileSource);
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
            processFile(fileSource,file);
            //2. 上传到cos中
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //封装返回结果
            //1.获取信息包含图片尺寸、格式等元数据
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //2. 获取规则处理后的数据
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){
                CIObject compressedCiObject = objectList.get(0);
                //缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                if (objectList.size() > 1){
                    thumbnailCiObject = objectList.get(1);
                }
                //封装压缩图返回结果
                return buildResult(originFilename,compressedCiObject,thumbnailCiObject,imageInfo);
            }
            //封装原图返回结果
            return getUploadPictureResult(imageInfo, uploadPath, originFilename, file);
        } catch (IOException e) {
            log.error("图片上传到cos失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 返回设置规则的图的结果
     * @param originFilename
     * @param compressedCiObject
     * @param thumbnailCiObject
     * @return
     */
    private UploadPictureResult buildResult(String originFilename,CIObject compressedCiObject,CIObject thumbnailCiObject,ImageInfo imageInfo){
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //图片基本信息
        int picWidth = compressedCiObject.getWidth();
        int picHeight= compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0/ picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        //设置缩略图url
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 返回原图封装结果
     * @param imageInfo
     * @param uploadPath
     * @param originFilename
     * @param file
     * @return
     */
    protected UploadPictureResult getUploadPictureResult(ImageInfo imageInfo, String uploadPath, String originFilename, File file) {
        String format = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0/picHeight,2).doubleValue();
        //2.封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+ uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 文件处理
     * @param fileSource
     * @param file
     */
    protected abstract void processFile(Object fileSource,File file) throws IOException;

    /**
     * 获取文件名
     * @param fileSource
     * @return
     */
    protected abstract String getOriginFilename(Object fileSource);

    /**
     * 校验处理
     * @param fileSource
     */
    protected abstract void validPicture(Object fileSource);

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

}
