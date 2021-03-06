package com.ksyun.ks3.service.response;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import com.ksyun.ks3.exception.Ks3ClientException;
import com.ksyun.ks3.http.HttpHeaders;

/**
 * @author lijunwei[lijunwei@kingsoft.com]  
 * 
 * @date 2014年10月20日 下午5:13:22
 * 
 * @description 
 **/
public abstract class Ks3WebServiceDefaultResponse<T> implements Ks3WebServiceResponse<T>{
	protected T result = null;
	protected HttpResponse response;
	protected HttpRequest request;
	public T handleResponse(HttpRequest request,HttpResponse response) {
		this.response = response;
		this.request = request;
		preHandle();
		this.abortRequest();
		return result;
	}
	public abstract void preHandle();
	public HttpResponse getResponse() {
		return this.response;
	}
	protected Header[] getHeaders(String key)
	{
		return response.getHeaders(key);
	}
	protected String getHeader(String key)
	{
		Header[] headers = getHeaders(key);
		if(headers.length>0)
		{
			return headers[0].getValue();
		}
		return "";
	}
	protected InputStream getContent()
	{
		try {
			return response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Ks3ClientException("无法读取http response的body("+e+")",e);
		}
	}
	public String getRequestId()
	{
		return this.getHeader(HttpHeaders.RequestId.toString());
	}
	private void abortRequest(){
		if(this.request!=null&&this.request instanceof HttpRequestBase)
			((HttpRequestBase) this.request).abort();
	}
}
