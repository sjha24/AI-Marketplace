package com.aimarketplace.shared.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("@annotation(com.aimarketplace.shared.aop.Logging)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().toShortString();
        long started = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("{} completed in {} ms", operation, System.currentTimeMillis() - started);
            return result;
        } catch (Throwable error) {
            log.warn("{} failed in {} ms: {}", operation, System.currentTimeMillis() - started, error.getMessage());
            throw error;
        }
    }
}
