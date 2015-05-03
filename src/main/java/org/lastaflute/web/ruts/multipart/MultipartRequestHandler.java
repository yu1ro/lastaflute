/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.web.ruts.multipart;

import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.lastaflute.web.ruts.multipart.exception.MultipartExceededException;

/**
 * @author modified by jflute (originated in Struts)
 */
public interface MultipartRequestHandler {

    String MAX_LENGTH_EXCEEDED_KEY = "lastaflute.multipart.SizeLimitExceededException";

    public static MultipartExceededException findExceededException(HttpServletRequest request) {
        return (MultipartExceededException) request.getAttribute(MAX_LENGTH_EXCEEDED_KEY);
    }

    void handleRequest(HttpServletRequest request) throws ServletException;

    void rollback();

    void finish();

    Hashtable<String, Object> getAllElements();

    Hashtable<String, MultipartFormFile> getFileElements();

    Hashtable<String, String[]> getTextElements();
}