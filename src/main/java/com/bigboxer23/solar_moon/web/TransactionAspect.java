package com.bigboxer23.solar_moon.web;

import java.util.Optional;
import org.apache.catalina.connector.RequestFacade;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * This class sets up (and tears down) transaction specific information in a threadlocal
 * (TransactionUtil) available for usage throughout items within the annotated method (and chain)
 */
@Component
@Aspect
public class TransactionAspect {
	@Pointcut("@annotation(Transaction)")
	public void transactionPointcut() {}

	@Before("transactionPointcut()")
	public void beforeMethodCallsAdvice(JoinPoint jp) {
		if (jp.getArgs() != null && jp.getArgs().length > 0 && jp.getArgs()[0] instanceof RequestFacade) {
			TransactionUtil.newTransaction(Optional.of(jp.getArgs())
					.map(array -> array[0])
					.map(request -> (RequestFacade) request)
					.map(request -> Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElseGet(request::getRemoteAddr))
					.orElse(null));
		}
	}

	@After("transactionPointcut()")
	public void afterMethodCallsAdvice(JoinPoint jp) {
		TransactionUtil.clear();
	}
}
