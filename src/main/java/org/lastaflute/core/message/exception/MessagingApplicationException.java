/*
 * Copyright 2015-2016 the original author or authors.
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
package org.lastaflute.core.message.exception;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.exception.LaApplicationMessage;
import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;

/**
 * @author jflute
 */
public class MessagingApplicationException extends LaApplicationException {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MessagingApplicationException(String debugMsg, UserMessages messages) {
        super(debugMsg);
        assertUserMessages(messages);
        saveApplicationMessages(toApplicationMessageList(messages));
    }

    public MessagingApplicationException(String debugMsg, UserMessages messages, Throwable cause) {
        super(debugMsg, cause);
        assertUserMessages(messages);
        saveApplicationMessages(toApplicationMessageList(messages));
    }

    protected void assertUserMessages(UserMessages messages) {
        if (messages == null) {
            throw new IllegalArgumentException("The argument 'messages' should not be null.");
        }
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("The messages should not be empty: " + messages);
        }
        // resource check is in application message handling
    }

    // ===================================================================================
    //                                                                 Application Message
    //                                                                 ===================
    protected List<LaApplicationMessage> toApplicationMessageList(UserMessages messages) {
        final List<LaApplicationMessage> convertedList = new ArrayList<LaApplicationMessage>(messages.size());
        for (Iterator<UserMessage> ite = messages.accessByFlatIterator(); ite.hasNext();) {
            final UserMessage message = ite.next();
            verifyResourceMessage(messages, message);
            convertedList.add(toApplicationMessage(message));
        }
        return convertedList;
    }

    protected void verifyResourceMessage(UserMessages messages, UserMessage message) {
        if (!message.isResource()) {
            throw new IllegalArgumentException("Not allowed the non-resource message: " + message + " in " + messages);
        }
    }

    protected LaApplicationMessage toApplicationMessage(UserMessage message) {
        return new LaApplicationMessage(message.getMessageKey(), message.getValues());
    }
}
