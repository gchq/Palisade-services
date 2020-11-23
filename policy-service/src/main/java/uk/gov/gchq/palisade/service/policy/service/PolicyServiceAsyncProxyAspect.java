/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.service.policy.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;


/**
 * For the AOP handling of the application level exceptions.  This works as a proxy that will intercept the use of the
 * lager- note on the different types of error
 */
@Aspect
public class PolicyServiceAsyncProxyAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceAsyncProxyAspect.class);

    /**
     * Pointcut for all public methods of {@link PolicyServiceAsyncProxy}
     */
    @Pointcut("execution(* uk.gov.gchq.palisade.service.policy.service.PolicyServiceAsyncProxy.*(..))")
    public void serviceMethods() {
        // Pointcut definition, no implementation
    }

    /**
     * Around aspect for method calls, handle and swallow exceptions
     *
     * @param pjp method call metadata
     * @return audit object and method return value container
     */
    @Around("serviceMethods()")
    public Object executeServiceMethods(final ProceedingJoinPoint pjp) {
        Object result = null;

        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            LOGGER.error("Exception thrown from method {}() : ", pjp.getSignature().getName(), t);
            result = policyException(pjp.getArgs(), t);
        }

        return result;
    }

    //for the moment only create AuditErrorMessage think we will need the wrapper when we factor in the chain
    private static AuditErrorMessage policyException(final @NonNull Object[] args, final @NonNull Throwable reason) {
        PolicyRequest request = (PolicyRequest) args[0];
        return AuditErrorMessage.Builder.create(request, null).withError(reason);
    }
}
