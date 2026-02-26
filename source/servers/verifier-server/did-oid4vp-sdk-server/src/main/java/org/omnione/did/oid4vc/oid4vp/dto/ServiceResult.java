/*
 * Copyright 2026 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.oid4vc.oid4vp.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Generic service result DTO for decoupling service layer from Spring Framework.
 *
 * @param <T> the type of data payload
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceResult<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private String errorDescription;
    private String state;
    private int httpStatus;
    private String contentType;

    /**
     * Creates a successful result with data.
     */
    public static <T> ServiceResult<T> success(T data) {
        return ServiceResult.<T>builder()
                .success(true)
                .data(data)
                .httpStatus(200)
                .build();
    }

    /**
     * Creates a successful result with data and custom content type.
     */
    public static <T> ServiceResult<T> success(T data, String contentType) {
        return ServiceResult.<T>builder()
                .success(true)
                .data(data)
                .httpStatus(200)
                .contentType(contentType)
                .build();
    }

    /**
     * Creates a successful result without data.
     */
    public static <T> ServiceResult<T> success() {
        return ServiceResult.<T>builder()
                .success(true)
                .httpStatus(200)
                .build();
    }

    /**
     * Creates a failure result with error information.
     */
    public static <T> ServiceResult<T> failure(int httpStatus, String errorCode, String errorDescription) {
        return ServiceResult.<T>builder()
                .success(false)
                .httpStatus(httpStatus)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .build();
    }

    /**
     * Creates a failure result with error information and state.
     */
    public static <T> ServiceResult<T> failure(int httpStatus, String errorCode, String errorDescription, String state) {
        return ServiceResult.<T>builder()
                .success(false)
                .httpStatus(httpStatus)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .state(state)
                .build();
    }

    /**
     * Creates a failure result with default 400 status.
     */
    public static <T> ServiceResult<T> badRequest(String errorCode, String errorDescription) {
        return failure(400, errorCode, errorDescription);
    }

    /**
     * Creates a failure result with default 404 status.
     */
    public static <T> ServiceResult<T> notFound(String errorCode, String errorDescription) {
        return failure(404, errorCode, errorDescription);
    }

    /**
     * Creates a failure result with default 500 status.
     */
    public static <T> ServiceResult<T> serverError(String errorCode, String errorDescription) {
        return failure(500, errorCode, errorDescription);
    }
}