package com.azad.yearn.deployer.logging;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApplicationLoggerAspect {

	//https://www.springboottutorial.com/spring-boot-and-aop-with-spring-boot-starter-aop

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Pointcut("execution(* com.azad.yearn.deployer.controllers.*.*(..))")
	public void definePackagePointcuts() {
		// empty method just to name the location specified in the pointcut
	}

	@Pointcut("execution(* com.azad.yearn.deployer.services.*XSource_CrawlerService.*(..))")
	public void definePackageConfigPointcuts() {
		// empty method just to name the location specified in the pointcut
	}
	
	@Around("definePackagePointcuts()")
	public Object logAround(ProceedingJoinPoint jp) {
		log.debug(" \n \n \n ");
		log.debug("************ Before Method Execution ************ \n {}.{} () with arguments[s] = {}",
				jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
		log.debug("_________________________________________________ \n \n \n");
		
		Object o = null;
		
		try {
			o = jp.proceed();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.debug("************ After Method Execution ************ \n {}.{} () with arguments[s] = {}",
				jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
		log.debug("_________________________________________________ \n \n \n");
		
		return o;
	}

	@Around("definePackageConfigPointcuts()")
	public void logAroundConfig(ProceedingJoinPoint jp) throws Throwable {
		log.debug("Before crawler service is called");
		jp.proceed();
		log.debug("After crawler service is called");

	}
}
