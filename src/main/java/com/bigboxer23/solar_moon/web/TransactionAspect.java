package com.bigboxer23.solar_moon.web;

/**
 * This class sets up (and tears down) transaction specific information in a threadlocal
 * (TransactionUtil) available for usage throughout items within the annotated method (and chain)
 */
// @Component
// @Aspect
public class TransactionAspect {
	// @Pointcut("@annotation(Transaction)")
	public void transactionPointcut() {}

	// @Before("transactionPointcut()")
	/*public void beforeMethodCallsAdvice(JoinPoint jp) {
		System.out.println("JP:" + jp.getArgs()[0]);
		if (jp.getArgs() != null && jp.getArgs().length > 0 && jp.getArgs()[0] instanceof RequestFacade) {
			TransactionUtil.newTransaction(Optional.of(jp.getArgs())
					.map(array -> array[0])
					.map(request -> (RequestFacade) request)
					.map(request -> Optional.ofNullable(request.getHeader("X-Forwarded-For"))
							.orElseGet(request::getRemoteAddr))
					.orElse(null));
			TransactionUtil.addToMDC();
		}
		if (jp.getArgs() != null && jp.getArgs().length > 0 && jp.getArgs()[0] instanceof LambdaRequest) {
			TransactionUtil.newTransaction((LambdaRequest)
					Optional.of(jp.getArgs()).map(array -> array[0]).orElse(null));
		}
	}*/

	/*@After("transactionPointcut()")
	public void afterMethodCallsAdvice(JoinPoint jp) {
		TransactionUtil.clear();
		MDC.clear();
	}*/
}
