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
package org.lastaflute.web.exception;

import javax.servlet.http.HttpServletResponse;

import org.lastaflute.web.servlet.filter.RequestLoggingFilter.RequestDelicateErrorException;

/**
 * @author jflute
 */
public class ForcedRequest400BadRequestException extends RequestDelicateErrorException {

    private static final long serialVersionUID = 1L;

    protected static final String TITLE = "400 Bad Request";
    protected static final int STATUS = HttpServletResponse.SC_BAD_REQUEST;

    public ForcedRequest400BadRequestException(String msg) {
        super(msg, TITLE, STATUS);
    }

    public ForcedRequest400BadRequestException(String msg, Throwable cause) {
        super(msg, TITLE, STATUS, cause);
    }
}