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
package org.lastaflute.web.util;

import javax.servlet.ServletContext;

import org.lastaflute.core.util.ContainerUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public final class LaServletContextUtil {

    private LaServletContextUtil() {
    }

    public static ServletContext getServletContext() {
        return (ServletContext) ContainerUtil.retrieveExternalContext().getApplication();
    }

    public static String getViewPrefix() {
        return "/WEB-INF/view"; // TODO jflute lastaflute: [E] refactor: view prefix can be overridden, approach after thymeleaf
    }
}