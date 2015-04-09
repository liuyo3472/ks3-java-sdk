package com.ksyun.ks3.service.encryption;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.ksyun.ks3.config.Constants;
import com.ksyun.ks3.dto.CompleteMultipartUploadResult;
import com.ksyun.ks3.dto.CopyResult;
import com.ksyun.ks3.dto.GetObjectResult;
import com.ksyun.ks3.dto.InitiateMultipartUploadResult;
import com.ksyun.ks3.dto.Ks3Object;
import com.ksyun.ks3.dto.ListPartsResult;
import com.ksyun.ks3.dto.ObjectMetadata;
import com.ksyun.ks3.dto.PartETag;
import com.ksyun.ks3.dto.PutObjectResult;
import com.ksyun.ks3.exception.Ks3ClientException;
import com.ksyun.ks3.exception.Ks3ServiceException;
import com.ksyun.ks3.exception.serviceside.NoSuchKeyException;
import com.ksyun.ks3.http.HttpHeaders;
import com.ksyun.ks3.service.Ks3Client;
import com.ksyun.ks3.service.encryption.internal.CryptoModuleDispatcher;
import com.ksyun.ks3.service.encryption.internal.EncryptionUtils;
import com.ksyun.ks3.service.encryption.internal.S3CryptoModule;
import com.ksyun.ks3.service.encryption.model.CryptoConfiguration;
import com.ksyun.ks3.service.encryption.model.EncryptionMaterials;
import com.ksyun.ks3.service.encryption.model.EncryptionMaterialsProvider;
import com.ksyun.ks3.service.encryption.model.StaticEncryptionMaterialsProvider;
import com.ksyun.ks3.service.request.AbortMultipartUploadRequest;
import com.ksyun.ks3.service.request.CompleteMultipartUploadRequest;
import com.ksyun.ks3.service.request.CopyPartRequest;
import com.ksyun.ks3.service.request.DeleteObjectRequest;
import com.ksyun.ks3.service.request.GetObjectRequest;
import com.ksyun.ks3.service.request.InitiateMultipartUploadRequest;
import com.ksyun.ks3.service.request.PutObjectRequest;
import com.ksyun.ks3.service.request.UploadPartRequest;
import com.ksyun.ks3.service.response.CompleteMultipartUploadResponse;

/**
 * @author lijunwei[lijunwei@kingsoft.com]  
 * 
 * @date 2015年4月8日 上午10:30:50
 * 
 * @description 
 **/
public class Ks3EncryptionClient extends Ks3Client{
    private final S3CryptoModule<?> crypto;

    // ///////////////////// Constructors ////////////////
    public Ks3EncryptionClient(
    		String accesskeyid,
            String accesskeysecret,
            EncryptionMaterials encryptionMaterials) {
        this(accesskeyid,accesskeysecret, new StaticEncryptionMaterialsProvider(
                encryptionMaterials));
    }
    public Ks3EncryptionClient(
    		String accesskeyid,
            String accesskeysecret,
            EncryptionMaterialsProvider encryptionMaterialsProvider) {
        this(accesskeyid,accesskeysecret, encryptionMaterialsProvider,
                 new CryptoConfiguration());
    }


    public Ks3EncryptionClient(
    		String accesskeyid,
            String accesskeysecret,
            EncryptionMaterials encryptionMaterials,
            CryptoConfiguration cryptoConfig) {
        this(accesskeyid,accesskeysecret, new StaticEncryptionMaterialsProvider(
                encryptionMaterials), cryptoConfig);
    }


    public Ks3EncryptionClient(
            String accesskeyid,
            String accesskeysecret,
            EncryptionMaterialsProvider kekMaterialsProvider,
            CryptoConfiguration cryptoConfig) {
        super(accesskeyid, accesskeysecret);
        assertParameterNotNull(kekMaterialsProvider,
                "EncryptionMaterialsProvider parameter must not be null.");
        assertParameterNotNull(cryptoConfig,
                "CryptoConfiguration parameter must not be null.");
        this.crypto = new CryptoModuleDispatcher(new S3DirectImpl(),
                 kekMaterialsProvider
                 ,cryptoConfig);
    }

    private void assertParameterNotNull(Object parameterValue,
            String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest req) {
        return crypto.putObjectSecurely(req);
    }
    @Override
    public PutObjectResult putObject(String bucket,String key,File file){
    	return this.putObject(new PutObjectRequest(bucket,key,file));
    }
    @Override
    public PutObjectResult putObject(String bucket,String key,InputStream content,ObjectMetadata meta){
    	return this.putObject(new PutObjectRequest(bucket,key,content,meta));
    }

    @Override
    public GetObjectResult getObject(GetObjectRequest req) {
        return crypto.getObjectSecurely(req);
    }
    @Override
    public GetObjectResult getObject(String bucket,String key) {
        return this.getObject(new GetObjectRequest(bucket,key));
    }

    @Override
    public void deleteObject(DeleteObjectRequest req) {
        req.getHeader().put(HttpHeaders.UserAgent.toString(),Constants.KS3_ENCRYPTION_CLIENT_USER_AGENT);
        // Delete the object
        super.deleteObject(req);
        // If it exists, delete the instruction file.
        DeleteObjectRequest instructionDeleteRequest = EncryptionUtils
                .createInstructionDeleteObjectRequest(req);
        try{
        	super.deleteObject(instructionDeleteRequest);
        }catch(NoSuchKeyException e){
        	//可能不存在
        }
    }
    @Override
    public void deleteObject(String bucket,String key) {
    	this.deleteObject(new DeleteObjectRequest(bucket,key));
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest req) {
        return crypto.completeMultipartUploadSecurely(req);
    }
    @Override
	public CompleteMultipartUploadResult completeMultipartUpload(
			ListPartsResult result) throws Ks3ClientException,
			Ks3ServiceException {
		return this.completeMultipartUpload(new CompleteMultipartUploadRequest(
				result));
	}
    @Override
	public CompleteMultipartUploadResult completeMultipartUpload(
			String bucketname, String objectkey, String uploadId,
			List<PartETag> partETags) throws Ks3ClientException,
			Ks3ServiceException {
		return this.completeMultipartUpload(new CompleteMultipartUploadRequest(
				bucketname, objectkey, uploadId, partETags));
	}
    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest req) {
        return crypto.initiateMultipartUploadSecurely(req);
    }
    @Override
	public InitiateMultipartUploadResult initiateMultipartUpload(
			String bucketname, String objectkey) throws Ks3ClientException,
			Ks3ServiceException {
		return this.initiateMultipartUpload(new InitiateMultipartUploadRequest(
				bucketname, objectkey));
	}
    /**
     * 注意，当使用分块上传时，需要依次按顺序上传各个块，不能多线程并发上传或者上传顺序不对。
     */
    @Override
    public PartETag uploadPart(UploadPartRequest uploadPartRequest)
            throws Ks3ClientException, Ks3ServiceException {
        return crypto.uploadPartSecurely(uploadPartRequest);
    }

    @Override
    public CopyResult copyPart(CopyPartRequest copyPartRequest) {
        return crypto.copyPartSecurely(copyPartRequest);
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest req) {
        crypto.abortMultipartUploadSecurely(req);
    }
    @Override
	public void abortMultipartUpload(String bucketname, String objectkey,
			String uploadId) throws Ks3ClientException, Ks3ServiceException {
		this.abortMultipartUpload(new AbortMultipartUploadRequest(bucketname,
				objectkey, uploadId));
	}

    // /////////////////// Access to the methods in the super class //////////
    /**
     * An internal implementation used to provide limited but direct access to
     * the underlying methods of AmazonS3Client without any encryption or
     * decryption operations.
     */
    private final class S3DirectImpl extends S3Direct {
        @Override
        public PutObjectResult putObject(PutObjectRequest req) {
            return Ks3EncryptionClient.super.putObject(req);
        }

        @Override
        public GetObjectResult getObject(GetObjectRequest req) {
            return Ks3EncryptionClient.super.getObject(req);
        }


        @Override
        public CompleteMultipartUploadResult completeMultipartUpload(
                CompleteMultipartUploadRequest req) {
            return Ks3EncryptionClient.super.completeMultipartUpload(req);
        }

        @Override
        public InitiateMultipartUploadResult initiateMultipartUpload(
                InitiateMultipartUploadRequest req) {
            return Ks3EncryptionClient.super.initiateMultipartUpload(req);
        }

        @Override
        public PartETag uploadPart(UploadPartRequest req)
                throws Ks3ClientException, Ks3ServiceException {
            return Ks3EncryptionClient.super.uploadPart(req);
        }

        @Override
        public CopyResult copyPart(CopyPartRequest req) {
            return Ks3EncryptionClient.super.copyPart(req);
        }

        @Override
        public void abortMultipartUpload(AbortMultipartUploadRequest req) {
            Ks3EncryptionClient.super.abortMultipartUpload(req);
        }
    }
}
