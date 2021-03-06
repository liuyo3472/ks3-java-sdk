package com.ksyun.ks3.exception.serviceside;
import com.ksyun.ks3.exception.Ks3ServiceException;
/**
 * @author lijunwei[lijunwei@kingsoft.com] 
 * @date 2014年11月7日 上午10:39:47
 * @description The target bucket for logging does not exist, is not owned by you, or does not have the appropriate grants for the log-delivery group.
 **/
public class InvalidTargetBucketForLoggingException extends Ks3ServiceException{
private static final long serialVersionUID = 2177914202944479049L;
}