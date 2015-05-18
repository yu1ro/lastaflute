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
package org.lastaflute.web.validation;

import org.lastaflute.web.ruts.message.ActionMessages;

/**
 * @param <MESSAGES> The type of action message.
 * @author jflute
 */
public interface LaValidatableApi<MESSAGES extends ActionMessages> {

    default void validateApi(Object form, VaMore<MESSAGES> doValidateLambda) {
        createValidator().validateApi(form, doValidateLambda);
    }

    default void letsValidationErrorApi(VaMessenger<MESSAGES> messagesLambda) {
        createValidator().letsValidationErrorApi(() -> createMessages());
    }

    ActionValidator<MESSAGES> createValidator();

    MESSAGES createMessages();
}