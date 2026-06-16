package com.wuyou.youpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.wuyou.youpicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     *  将本地文件上传到 COS
     * @param key  文件上传的前缀/路径
     * @param file 上传的文件
     * @return
     */
    public PutObjectResult putObject(String key, File file){
        //创建上传请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //执行上传操作
       return cosClient.putObject(putObjectRequest);
    }

    //下载对象
    public COSObject getObject(String key){
        //创建下载请求
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        //执行下载操作,但是并没有开始下载，只是创建一个可以读取文件内容的通道，但还没有开始传输实际文件数据
        return cosClient.getObject(getObjectRequest);
    }

    /**
     *  上传对象（附带图片信息）
     * @param key 桶的唯一标识（类似于文件在文件系统中的完整路径）
     * @param file 文件
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        //创建请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 是否返回原图信息：1 表示返回原图信息
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> rules = new ArrayList<>();
        //1.图片压缩（转成webp格式）
        String webpKey = FileUtil.mainName(key)+".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        //2.图片缩略,仅对20KB的图片生成缩略图
        if (file.length() > 2 *1024){
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            //缩放规则/thumbnail/<Width>x<Height>>(如果大于原图宽高，则不处理)
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",128,128));
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key)+"_thumbnail";
            thumbnailRule.setFileId(thumbnailKey);
            rules.add(thumbnailRule);
        }
        //构造处理参数
        picOperations.setRules(rules);
        //将前面的配置的picOperations绑定到上传的请求中去
        putObjectRequest.setPicOperations(picOperations);
        //上传图片
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     */
    public void deleteObject(String key){
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
