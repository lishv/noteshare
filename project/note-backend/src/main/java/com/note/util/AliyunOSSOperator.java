package com.note.util;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;

import com.note.config.AliyunOssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AliyunOSSOperator {
    @Autowired
    private AliyunOssProperties aliyunOssProperties;

    public String upload(byte[] content, String originalFilename) throws Exception {
        String endpoint=aliyunOssProperties.getEndpoint();
        String bucketName=aliyunOssProperties.getBucketName();
        String region=aliyunOssProperties.getRegion();
        // 优先从配置文件获取，若未配置则从系统环境变量获取
        CredentialsProvider credentialsProvider;
        if (aliyunOssProperties.getAccessKeyId() != null && !aliyunOssProperties.getAccessKeyId().trim().isEmpty()
                && aliyunOssProperties.getAccessKeySecret() != null && !aliyunOssProperties.getAccessKeySecret().trim().isEmpty()) {
            credentialsProvider = new com.aliyun.oss.common.auth.DefaultCredentialProvider(
                    aliyunOssProperties.getAccessKeyId().trim(),
                    aliyunOssProperties.getAccessKeySecret().trim()
            );
        } else {
            credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        }

        // 填写Object完整路径，例如202406/1.png。Object完整路径中不能包含Bucket名称。
        //获取当前系统日期的字符串,格式为 yyyy/MM
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        //生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = dir + "/" + newFileName;//文件夹+新文件名

        // 创建OSSClient实例。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();
        // 上传文件流。
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
        } finally {
            ossClient.shutdown();
        }
        //已知endpoint=https://oss-cn-beijing.aliyuncs.com,
        // bucketName=java-web2570,
        // objectName=202406/1.png
        //目标图片的url:https://java-web2570.oss-cn-beijing.aliyuncs.com/000.jpg
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
    }

}
