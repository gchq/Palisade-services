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

package uk.gov.gchq.palisade.service.attributemask.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingResponse;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Aspect
public class AttributeMaskingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeMaskingAspect.class);
    private static final String SAR = "storeAuthorisedRequest";
    private static final String MRA = "maskResourceAttributes";

    @Pointcut("execution(* uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService.*(..))")
    public void serviceMethods() { }

    @Around("serviceMethods()")
    public Object executeServiceMethods(final ProceedingJoinPoint pjp) {
        Object result = null;

        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            LOGGER.error("Exception thrown from method {}() : ", pjp.getSignature().getName(), t);
            switch (pjp.getSignature().getName()) {
                case "storeAuthorisedRequest":
                    result = CompletableFuture.completedFuture(this.auditStorageException(pjp.getArgs(), t));
                    break;
                case "maskResourceAttributes":
                    result = this.auditMaskException(pjp.getArgs(), t);
                    break;
                default:
                    LOGGER.error("Unknown method: {} has thrown exception {}", pjp.getSignature().getName(), t);
            }
        }

        return result;
    }

    private AuditableAttributeMaskingResponse auditMaskException(final @NonNull Object[] args, final @NonNull Throwable reason) {
        AttributeMaskingRequest request = (AttributeMaskingRequest) args[0];
        return AuditableAttributeMaskingResponse.Builder.create().withAttributeMaskingResponse(null)
                .withAuditErrorMessage(AuditErrorMessage.Builder.create().withUserId(request.getUserId())
                .withResourceId(request.getResourceId())
                .withContextNode(request.getContextNode())
                .withAttributes(Collections.singletonMap("method", MRA))
                .withError(reason));
    }

    private AuditableAttributeMaskingRequest auditStorageException(final @NonNull Object[] args, final @NonNull Throwable reason) {
        AttributeMaskingRequest request = (AttributeMaskingRequest) args[1];
        return AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(null)
                .withAuditErrorMessage(AuditErrorMessage.Builder.create().withUserId(request.getUserId())
                .withResourceId(request.getResourceId())
                .withContextNode(request.getContextNode())
                .withAttributes(Collections.singletonMap("method", SAR))
                .withError(reason));
    }

}
