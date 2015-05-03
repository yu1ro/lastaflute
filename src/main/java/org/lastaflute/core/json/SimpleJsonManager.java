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
package org.lastaflute.core.json;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.TypeReference;

import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.direction.FwAssistantDirector;
import org.lastaflute.core.direction.OptionalCoreDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jflute
 * @author awaawa
 */
public class SimpleJsonManager implements JsonManager {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger LOG = LoggerFactory.getLogger(SimpleJsonManager.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The assistant directory (AD) for framework. (NotNull: after initialization) */
    @Resource
    protected FwAssistantDirector assistantDirector;

    /** The real parser of JSON. (NotNull: after initialization) */
    protected RealJsonParser realJsonParser;

    /** Is development here? */
    protected boolean developmentHere;

    // ===================================================================================
    //                                                                          Initialize
    //                                                                          ==========
    /**
     * Initialize this component. <br>
     * This is basically called by DI setting file.
     */
    @PostConstruct
    public synchronized void initialize() {
        final OptionalCoreDirection direction = assistOptionalCoreDirection();
        final JsonResourceProvider provider = direction.assistJsonResourceProvider();
        final RealJsonParser provided = provider != null ? provider.provideJsonParser() : null;
        realJsonParser = provided != null ? provided : createDefaultJsonParser();
        developmentHere = direction.isDevelopmentHere();
        showBootLogging();
    }

    protected OptionalCoreDirection assistOptionalCoreDirection() {
        return assistantDirector.assistOptionalCoreDirection();
    }

    protected RealJsonParser createDefaultJsonParser() {
        return newJSonicRealJsonParser();
    }

    protected void showBootLogging() {
        if (LOG.isInfoEnabled()) {
            LOG.info("[JSON Manager]");
            LOG.info(" realJsonParser: " + DfTypeUtil.toClassTitle(realJsonParser));
        }
    }

    // ===================================================================================
    //                                                                              JSONIC
    //                                                                              ======
    protected static final Class<?> JSONIC_DUMMY_TYPE = new Object() {
    }.getClass();

    protected RealJsonParser newJSonicRealJsonParser() {
        return new RealJsonParser() {
            @Override
            public String encode(Object bean) {
                return JSON.encode(bean, developmentHere);
            }

            @Override
            public <BEAN> BEAN decode(String json, Class<BEAN> beanType) {
                return JSON.decode(json, beanType);
            }

            @Override
            public <BEAN> List<BEAN> decodeList(String json, Class<BEAN> beanType) {
                return JSON.decode(json, new TypeReference<List<BEAN>>() {
                    // nothing is overridden
                });
            }

            @SuppressWarnings("unchecked")
            @Override
            public <BEAN> BEAN mappingJsonTo(String json, Supplier<BEAN> beanSupplier) {
                BEAN bean = beanSupplier.get();
                return (BEAN) new JSON() {
                    @Override
                    protected <T> T create(Context context, Class<? extends T> beanType) throws Exception {
                        if (context.getDepth() == 0) {
                            return (T) bean; // first bean instance is provided, basically for action form
                        }
                        return super.create(context, beanType);
                    }
                }.parse(json, bean.getClass());
            }

            @Override
            public <BEAN> List<BEAN> mappingJsonToList(String json, Supplier<BEAN> beanSupplier) {
                return new JSON() {
                    private Boolean asList;

                    @SuppressWarnings("unchecked")
                    @Override
                    protected <T> T create(Context context, Class<? extends T> beanType) throws Exception {
                        if (asList != null && context.getDepth() == 0 && List.class.isAssignableFrom(beanType)) {
                            asList = true;
                        }
                        if (asList != null && asList && context.getDepth() == 1) {
                            return (T) beanSupplier.get(); // element bean instance is provided, basically for action form
                        }
                        return super.create(context, beanType);
                    }
                }.parse(json, new TypeReference<List<BEAN>>() {
                    // nothing is overridden
                });
            }
        };
    }

    // ===================================================================================
    //                                                                       Encode/Decode
    //                                                                       =============
    @Override
    public String encode(Object bean) {
        return realJsonParser.encode(bean);
    }

    @Override
    public <BEAN> BEAN decode(String json, Class<BEAN> beanType) {
        return realJsonParser.decode(json, beanType);
    }

    @Override
    public <BEAN> List<BEAN> decodeList(String json, Class<BEAN> beanType) {
        return realJsonParser.decodeList(json, beanType);
    }

    @Override
    public <BEAN> BEAN mappingJsonTo(String json, Supplier<BEAN> beanSupplier) {
        return realJsonParser.mappingJsonTo(json, beanSupplier);
    }

    @Override
    public <BEAN> List<BEAN> mappingJsonToList(String json, Supplier<BEAN> beanSupplier) {
        return realJsonParser.mappingJsonToList(json, beanSupplier);
    }
}