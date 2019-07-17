package org.codice.ddf.catalog.content.impl;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AmazonS3Client {

  private static AmazonS3 amazonS3;

  private static volatile AmazonS3Client amazonS3Client;

  private AmazonS3Client() {}

  public static AmazonS3Client getInstance() {
    AmazonS3Client localAmazonS3ClientRef = amazonS3Client;
    if (localAmazonS3ClientRef == null) {
      synchronized (AmazonS3Client.class) {
        localAmazonS3ClientRef = amazonS3Client;
        if (localAmazonS3ClientRef == null) {
          amazonS3Client = localAmazonS3ClientRef = new AmazonS3Client();
        }
      }
    }
    return localAmazonS3ClientRef;
  }

  public synchronized void init(
      String s3Endpoint, String s3Region, String s3AccessKey, String s3SecretKey) {
    AwsClientBuilder.EndpointConfiguration endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region);
    if (org.apache.commons.lang3.StringUtils.isNotBlank(s3AccessKey)) {
      AWSCredentials awsCredentials = new BasicAWSCredentials(s3AccessKey, s3SecretKey);
      AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
      amazonS3 =
          AmazonS3ClientBuilder.standard()
              .withCredentials(credentialsProvider)
              .withEndpointConfiguration(endpointConfiguration)
              .build();
    }
    amazonS3 =
        AmazonS3ClientBuilder.standard().withEndpointConfiguration(endpointConfiguration).build();
  }
  
  public AmazonS3 getAmazons3() {
      return amazonS3;
  }
}
