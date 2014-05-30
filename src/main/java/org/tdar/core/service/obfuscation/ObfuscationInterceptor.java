package org.tdar.core.service.obfuscation;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tdar.core.bean.Obfuscatable;
import org.tdar.core.bean.entity.TdarUser;
import org.tdar.core.configuration.TdarConfiguration;
import org.tdar.core.service.ObfuscationService;
import org.tdar.struts.action.AuthenticationAware;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Aspect
@Order
@Component
public class ObfuscationInterceptor {

    private ObfuscationService obfuscationService;

    protected transient Logger logger = LoggerFactory.getLogger(getClass());
    private final Cache<Integer, Boolean> seenSet;
    
    @Autowired
    public ObfuscationInterceptor(ObfuscationService obfuscationService) {
        this.obfuscationService = obfuscationService;
        seenSet = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(1000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    /*
     * First, match @Action or @Actions on methods and add hashCode to set.  We'll only obfuscate things after the @Action has been called
     * http://docs.spring.io/spring/docs/3.0.x/spring-framework-reference/html/aop.html
     */
    @Around("(@annotation(org.apache.struts2.convention.annotation.Action) || @annotation(org.apache.struts2.convention.annotation.Actions))") 
    public <T> T aroundAction(ProceedingJoinPoint pjp) throws Throwable {
        T result = (T) pjp.proceed();
        logger.debug("seen: {} [{}]",pjp.getTarget().getClass(), pjp.getTarget().hashCode());
        seenSet.put(pjp.getTarget().hashCode(),true);
        return result;
    }
    
    /*
     * Then we wrap all of the getters, if the hashCode is stored in our set, we remove it.
     * Match anything that is one of our Controllers; that has a public Getter, and does not have a @DoNotObfuscate annotation.
     * http://docs.spring.io/spring/docs/3.0.x/spring-framework-reference/html/aop.html
     */
    @Around("(bean(*Controller) || bean (*Action)) && execution(public * get*() ) && !@annotation(org.tdar.struts.interceptor.annotation.DoNotObfuscate)")
    public <T> T obfuscate(ProceedingJoinPoint pjp) throws Throwable {
        Boolean done = seenSet.getIfPresent(pjp.getTarget().hashCode());
        T retVal = (T)pjp.proceed();
        if (retVal == null) {
            return null;
        }
        if (TdarConfiguration.getInstance().obfuscationInterceptorDisabled() || 
                obfuscationService.isWritableSession() || 
                done != Boolean.TRUE || 
                (retVal != null && !isIterableObfuscatable(retVal) && !(retVal instanceof Obfuscatable))) {
            logger.debug("NOT OBFUSCATING: {} {}", pjp.getSignature(), pjp.getTarget().hashCode());
            return retVal;
        }
        logger.debug("OBFUSCATING: {} {}", pjp.getSignature(), pjp.getTarget().hashCode());
        TdarUser user = null;
        if (pjp.getTarget() instanceof AuthenticationAware) {
            user = ((AuthenticationAware)pjp.getTarget()).getAuthenticatedUser();
        }
        obfuscationService.obfuscateObject(retVal, user);
        return retVal;
    }

    private <T> boolean isIterableObfuscatable(Object retVal) {
        if (!Iterable.class.isAssignableFrom(retVal.getClass())) {
            return false;
        }
        Iterator<T> iterator = ((Iterable<T>) retVal).iterator();
        if (iterator.hasNext()) {
            T obj = iterator.next();
            logger.debug(obj.getClass().getCanonicalName());
            if (obj instanceof Obfuscatable) {
                return true;
            }
        }
        return false;
    }
}
