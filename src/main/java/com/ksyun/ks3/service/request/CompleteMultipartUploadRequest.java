package com.ksyun.ks3.service.request;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.ksyun.ks3.RepeatableInputStream;
import com.ksyun.ks3.dto.CallBackConfiguration;
import com.ksyun.ks3.dto.Adp;
import com.ksyun.ks3.dto.ListPartsResult;
import com.ksyun.ks3.dto.Part;
import com.ksyun.ks3.dto.PartETag;
import com.ksyun.ks3.dto.CallBackConfiguration.MagicVariables;

import static com.ksyun.ks3.exception.client.ClientIllegalArgumentExceptionGenerator.notNull;
import static com.ksyun.ks3.exception.client.ClientIllegalArgumentExceptionGenerator.notNullInCondition;

import com.ksyun.ks3.http.HttpHeaders;
import com.ksyun.ks3.http.HttpMethod;
import com.ksyun.ks3.utils.HttpUtils;
import com.ksyun.ks3.utils.StringUtils;
import com.ksyun.ks3.utils.XmlWriter;

/**
 * @author lijunwei[lijunwei@kingsoft.com]  
 * 
 * @date 2014年10月23日 下午1:55:03
 * 
 * @description 完成分块上传,使Ks3服务器将之前上传的块整合成一个object
 **/
public class CompleteMultipartUploadRequest extends Ks3WebServiceRequest {
	/**
	 * 通过Init Multipart Upload 初始化得到的uploadId
	 */
	private String uploadId;
	/**
	 * 通过Upload Part返回的内容集合
	 */
	private List<PartETag> partETags = new ArrayList<PartETag>();
	/**
	 * 设置callback
	 */
	private  CallBackConfiguration callBackConfiguration; 
	/**
	 * 要进行的数据处理任务
	 */
	private List<Adp> adps = new ArrayList<Adp>();
	/**
	 * 数据处理任务完成后通知的url
	 */
	private String notifyURL;
	/**
	 * 
	 * @param bucketname
	 * @param objectkey
	 * @param uploadId
	 * @param eTags
	 */
	public CompleteMultipartUploadRequest(String bucketname,String objectkey,String uploadId,List<PartETag> eTags)
	{
		this.setBucketname(bucketname);
		this.setObjectkey(objectkey);
		this.uploadId = uploadId;
		if(eTags == null)
			eTags = new ArrayList<PartETag>();
		this.partETags = eTags;
	}
	/**
	 * List Parts返回的结果
	 * @param result
	 */
	public CompleteMultipartUploadRequest(ListPartsResult result)
	{
		this.setBucketname(result.getBucketname());
		this.setObjectkey(result.getKey());
		this.uploadId = result.getUploadId();
		for(Part p : result.getParts())
		{
			PartETag tag = new PartETag();
			tag.seteTag(p.getETag());
			tag.setPartNumber(p.getPartNumber());
		    this.partETags.add(tag);
		}
	}
	/**
	 * 这个构造函数生成的request是无法直接使用的，得调用setUploadId、setPartETags
	 * @param bucketname
	 * @param objectkey
	 */
	public CompleteMultipartUploadRequest(String bucketname,String objectkey)
	{
		super.setBucketname(bucketname);
		super.setObjectkey(objectkey);
	}
	public void addETag(PartETag eTag)
	{
		if(this.partETags==null)
		{
			partETags = new ArrayList<PartETag>();
		}
		this.partETags.add(eTag);
	}
	@Override
	protected void configHttpRequest() {
		XmlWriter writer = new XmlWriter();
		writer.start("CompleteMultipartUpload");
		for(PartETag tag:this.partETags)
		{
			writer.start("Part").start("PartNumber").value(tag.getPartNumber()).end().start("ETag").value(tag.geteTag()).end().end();
		}
		writer.end();
		String xml = writer.toString();
		this.setRequestBody(new ByteArrayInputStream(xml.getBytes()));
		this.setHttpMethod(HttpMethod.POST);
		this.addParams("uploadId", this.uploadId);
		
		if(this.callBackConfiguration!=null){
			this.addHeader(HttpHeaders.XKssCallbackUrl, callBackConfiguration.getCallBackUrl());
			StringBuffer body = new StringBuffer();
			if(callBackConfiguration.getBodyMagicVariables()!=null){
				for(Entry<String,MagicVariables> mvs : callBackConfiguration.getBodyMagicVariables().entrySet()){
					body.append(mvs.getKey()+"=${"+mvs.getValue()+"}&");
				}
			}
			if(callBackConfiguration.getBodyKssVariables()!=null){
				for(Entry<String,String> mvs : callBackConfiguration.getBodyKssVariables().entrySet()){
					body.append(mvs.getKey()+"=${kss-"+mvs.getKey()+"}&");
					this.addHeader("kss-"+mvs.getKey(), mvs.getValue());
				}
			}
			String bodyString  = body.toString();
			if(bodyString.endsWith("&")){
				bodyString = bodyString.substring(0,bodyString.length()-1);
			}
			this.addHeader(HttpHeaders.XKssCallbackBody,bodyString);
		}
		if(this.adps!=null&&adps.size()>0){
			this.addHeader(HttpHeaders.AsynchronousProcessingList, URLEncoder.encode(HttpUtils.convertAdps2String(adps)));
			if(!StringUtils.isBlank(notifyURL))
				this.addHeader(HttpHeaders.NotifyURL, notifyURL);
		}
	}

	@Override
	protected void validateParams() throws IllegalArgumentException {
		if(StringUtils.isBlank(this.getBucketname()))
			throw notNull("bucketname");
		if(StringUtils.isBlank(this.getObjectkey()))
			throw notNull("objectKey");
		if(StringUtils.isBlank(this.uploadId))
			throw notNull("uploadId");
		if(this.partETags == null)
			throw notNull("partETags");
		if(this.callBackConfiguration!=null){
			if(StringUtils.isBlank(this.callBackConfiguration.getCallBackUrl())){
				throw notNull("callBackConfiguration.callBackUrl");
			}
		}
		if(adps!=null&&adps.size()>0){
			for(Adp adp : adps){
				if(StringUtils.isBlank(adp.getCommand())){
					throw notNullInCondition("adps.command","adps不为空");
				}
			}
			if(StringUtils.isBlank(notifyURL))
				throw notNullInCondition("notifyURL","adps不为空");
		}
	}
	/**
	 * 通过Init Multipart Upload 初始化得到的uploadId
	 */
	public String getUploadId() {
		return uploadId;
	}
	/**
	 * 通过Init Multipart Upload 初始化得到的uploadId
	 */
	public void setUploadId(String uploadId) {
		this.uploadId = uploadId;
	}
	/**
	 * 通过Upload Part返回的内容集合
	 */
	public List<PartETag> getPartETags() {
		return partETags;
	}
	/**
	 * 通过Upload Part返回的内容集合
	 */
	public void setPartETags(List<PartETag> partETags) {
		this.partETags = partETags;
	}
	public CallBackConfiguration getCallBackConfiguration() {
		return callBackConfiguration;
	}
	public void setCallBackConfiguration(CallBackConfiguration callBackConfiguration) {
		this.callBackConfiguration = callBackConfiguration;
	}
	public List<Adp> getAdps() {
		return adps;
	}
	public void setAdps(List<Adp> adps) {
		this.adps = adps;
	}
	public String getNotifyURL() {
		return notifyURL;
	}
	public void setNotifyURL(String notifyURL) {
		this.notifyURL = notifyURL;
	}
	
}
