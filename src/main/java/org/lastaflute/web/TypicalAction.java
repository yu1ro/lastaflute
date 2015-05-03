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
package org.lastaflute.web;

import java.util.function.Supplier;

import javax.annotation.Resource;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.exception.ExceptionTranslator;
import org.lastaflute.core.exception.LaApplicationException;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.db.dbflute.accesscontext.AccessContextArranger;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.callback.ActionCallback;
import org.lastaflute.web.callback.ActionRuntimeMeta;
import org.lastaflute.web.callback.TypicalEmbeddedKeySupplier;
import org.lastaflute.web.callback.TypicalGodHandActionEpilogue;
import org.lastaflute.web.callback.TypicalGodHandActionPrologue;
import org.lastaflute.web.callback.TypicalGodHandExceptionMonologue;
import org.lastaflute.web.callback.TypicalGodHandResource;
import org.lastaflute.web.callback.TypicalKey.TypicalSimpleEmbeddedKeySupplier;
import org.lastaflute.web.exception.ActionApplicationExceptionHandler;
import org.lastaflute.web.exception.ForcedIllegalTransitionApplicationException;
import org.lastaflute.web.login.LoginManager;
import org.lastaflute.web.login.UserBean;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The typical action for your project. <br>
 * You should extend this class when making your project-base action. <br>
 * And you can add methods for all applications.
 * @author jflute
 */
public abstract class TypicalAction extends LastaAction implements ActionCallback {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(TypicalAction.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The manager of time. (NotNull) */
    @Resource
    private TimeManager timeManager;

    /** The translator of exception. (NotNull) */
    @Resource
    private ExceptionTranslator exceptionTranslator;

    /** The manager of request. (NotNull) */
    @Resource
    private RequestManager requestManager;

    /** The manager of response. (NotNull) */
    @Resource
    private ResponseManager responseManager;

    /** The manager of session. (NotNull) */
    @Resource
    private SessionManager sessionManager;

    /** The manager of API. (NotNull) */
    @Resource
    private ApiManager apiManager;

    // ===================================================================================
    //                                                                            Callback
    //                                                                            ========
    // [typical callback process]
    // read the source code for the details
    // (because of no comment here)
    // -----------------------------------------------------
    //                                                Before
    //                                                ------
    @Override
    public ActionResponse godHandActionPrologue(ActionRuntimeMeta runtimeMeta) { // fixed process
        return newTypicalGodHandActionPrologue().performPrologue(runtimeMeta);
    }

    protected TypicalGodHandActionPrologue newTypicalGodHandActionPrologue() {
        final TypicalGodHandResource resource = newTypicalGodHandResource();
        final AccessContextArranger arranger = newAccessContextArranger();
        return newTypicalGodHandActionPrologue(resource, arranger, () -> myUserBean(), () -> myAppType());
    }

    /**
     * New the arranger of access context.
     * @return The instance of arranger. (NotNull)
     */
    protected abstract AccessContextArranger newAccessContextArranger();

    protected TypicalGodHandActionPrologue newTypicalGodHandActionPrologue(TypicalGodHandResource resource, AccessContextArranger arranger,
            Supplier<OptionalThing<? extends UserBean>> userBeanSupplier, Supplier<String> appTypeSupplier) {
        return new TypicalGodHandActionPrologue(resource, arranger, userBeanSupplier, appTypeSupplier);
    }

    @Override
    public ActionResponse godHandBefore(ActionRuntimeMeta runtimeMeta) { // application's super class may override
        return ActionResponse.empty();
    }

    @Override
    public ActionResponse callbackBefore(ActionRuntimeMeta runtimeMeta) { // application may override
        return ActionResponse.empty();
    }

    // -----------------------------------------------------
    //                                            on Failure
    //                                            ----------
    @Override
    public ActionResponse godHandExceptionMonologue(ActionRuntimeMeta runtimeMeta) { // fixed process
        return newTypicalGodHandExceptionMonologue().performMonologue(runtimeMeta);
    }

    protected TypicalGodHandExceptionMonologue newTypicalGodHandExceptionMonologue() {
        final TypicalGodHandResource resource = newTypicalGodHandResource();
        final TypicalEmbeddedKeySupplier supplier = newTypicalEmbeddedKeySupplier();
        final ActionApplicationExceptionHandler handler = newActionApplicationExceptionHandler();
        return newTypicalGodHandExceptionMonologue(resource, supplier, handler);
    }

    protected TypicalEmbeddedKeySupplier newTypicalEmbeddedKeySupplier() {
        return new TypicalSimpleEmbeddedKeySupplier();
    }

    protected ActionApplicationExceptionHandler newActionApplicationExceptionHandler() {
        return new ActionApplicationExceptionHandler() {
            public ActionResponse handle(LaApplicationException appEx) {
                return handleApplicationException(appEx);
            }
        };
    }

    /**
     * Handle the application exception before framework's handling process.
     * @param appEx The thrown application exception. (NotNull)
     * @return The response for the exception. (NullAllowed: if null, to next handling step)
     */
    protected ActionResponse handleApplicationException(LaApplicationException appEx) { // application may override
        return ActionResponse.empty();
    }

    protected TypicalGodHandExceptionMonologue newTypicalGodHandExceptionMonologue(TypicalGodHandResource resource,
            TypicalEmbeddedKeySupplier supplier, ActionApplicationExceptionHandler handler) {
        return new TypicalGodHandExceptionMonologue(resource, supplier, handler);
    }

    // -----------------------------------------------------
    //                                               Finally
    //                                               -------
    @Override
    public void callbackFinally(ActionRuntimeMeta runtimeMeta) { // application may override
    }

    @Override
    public void godHandFinally(ActionRuntimeMeta runtimeMeta) { // application's super class may override
    }

    @Override
    public void godHandActionEpilogue(ActionRuntimeMeta runtimeMeta) { // fixed process
        newTypicalGodHandActionEpilogue().performEpilogue(runtimeMeta);
    }

    protected TypicalGodHandActionEpilogue newTypicalGodHandActionEpilogue() {
        final TypicalGodHandResource resource = newTypicalGodHandResource();
        return newTypicalGodHandActionEpilogue(resource);
    }

    protected TypicalGodHandActionEpilogue newTypicalGodHandActionEpilogue(TypicalGodHandResource resource) {
        return new TypicalGodHandActionEpilogue(resource);
    }

    // -----------------------------------------------------
    //                                      Resource Factory
    //                                      ----------------
    protected TypicalGodHandResource newTypicalGodHandResource() {
        final OptionalThing<LoginManager> loginManager = myLoginManager();
        return new TypicalGodHandResource(requestManager, responseManager, sessionManager, loginManager, apiManager, exceptionTranslator);
    }

    // ===================================================================================
    //                                                                         My Resource
    //                                                                         ===========
    protected abstract String myAppType();

    /**
     * Get the bean of login user on session as interface type. (for application)
     * @return The optional thing of found user bean. (NotNull, EmptyAllowed: when not login)
     */
    protected abstract OptionalThing<? extends UserBean> myUserBean();

    /**
     * Get the user type of this applicatoin's login.
     * @return The optional expression of user type. (NotNull, EmptyAllowed: if no login handling) 
     */
    protected abstract OptionalThing<String> myUserType();

    /**
     * Get my (application's) login manager. (for framework)
     * @return The optional instance of login manager. (NotNull, EmptyAllowed: if no login handling)
     */
    protected abstract OptionalThing<LoginManager> myLoginManager();

    // ===================================================================================
    //                                                                   Application Check
    //                                                                   =================
    // -----------------------------------------------------
    //                                       Check Parameter
    //                                       ---------------
    protected void checkParameter(boolean expectedBool) { // application may call
        logger.debug("...Checking the parameter is true: {}", expectedBool);
        if (!expectedBool) {
            throwParameterFailure();
        }
    }

    protected void checkParameterExists(Object parameter) { // application may call
        logger.debug("...Checking the parameter exists: {}", parameter);
        if (parameter == null || (parameter instanceof String && ((String) parameter).isEmpty())) {
            throwParameterFailure();
        }
    }

    protected void checkParameterPlusNumber(long num) { // application may call
        logger.debug("...Checking the parameter is plus number: {}", num);
        if (num <= 0) {
            throwParameterFailure();
        }
    }

    protected void checkParameterZeroOrPlusNumber(long num) { // application may call
        logger.debug("...Checking the parameter is zero or plus number: {}", num);
        if (num < 0) {
            throwParameterFailure();
        }
    }

    protected void throwParameterFailure() {
        lets404();
    }

    // -----------------------------------------------------
    //                                          Check or ...
    //                                          ------------
    /**
     * Check the condition is true or it throws 404 not found forcedly. <br>
     * You can use this in your action process against invalid URL parameters.
     * @param expectedBool The expected determination for your business, true or false. (false: 404 not found)
     */
    protected void checkOr404NotFound(boolean expectedBool) { // application may call
        logger.debug("...Checking the condition is true or 404 not found: {}", expectedBool);
        if (!expectedBool) {
            lets404();
        }
    }

    /**
     * Check the condition is true or it throws illegal transition forcedly. <br>
     * You can use this in your action process against strange request parameters.
     * @param expectedBool The expected determination for your business, true or false. (false: illegal transition)
     */
    protected void checkOrIllegalTransition(boolean expectedBool) { // application may call
        logger.debug("...Checking the condition is true or illegal transition: {}", expectedBool);
        if (!expectedBool) {
            letsIllegalTransition();
        }
    }

    protected void letsIllegalTransition() {
        final TypicalEmbeddedKeySupplier supplier = newTypicalEmbeddedKeySupplier();
        throw new ForcedIllegalTransitionApplicationException(supplier.getErrorsAppIllegalTransitionKey());
    }

    // ===================================================================================
    //                                                                            Document
    //                                                                            ========
    // TODO jflute lastaflute: [C] function: make document()
    /**
     * <pre>
     * [AtMark]Execute
     * public HtmlResponse index() {
     *     ListResultBean&lt;Product&gt; memberList = productBhv.selectList(cb -> {
     *         cb.query().addOrderBy_RegularPrice_Desc();
     *         cb.fetchFirst(3);
     *     });
     *     List&lt;MypageProductBean&gt; beans = memberList.stream().map(member -> {
     *         return new MypageProductBean(member);
     *     }).collect(Collectors.toList());
     *     return asHtml(path_Mypage_MypageJsp).renderWith(data -> {
     *         data.register("beans", beans);
     *     });
     * }
     * </pre>
     */
    protected void documentOfAll() {
    }

    /**
     * <pre>
     * o validate(form, error call): Hibernate Validator's Annotation only
     * o validateMore(form, your validation call, error call): annotation + by-method validation
     * 
     * o asHtml(HTML template): return response as HTML by template e.g. JSP
     * o asJson(JSON bean): return response as JSON from bean
     * o asStream(input stream): return response as stream from input stream
     * </pre>
     */
    protected void documentOfMethods() {
    }
}