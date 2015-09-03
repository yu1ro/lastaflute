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
package org.lastaflute.web.ruts.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.di.helper.beans.BeanDesc;
import org.lastaflute.di.helper.beans.PropertyDesc;
import org.lastaflute.di.helper.beans.factory.BeanDescFactory;
import org.lastaflute.web.exception.ActionFormCreateFailureException;
import org.lastaflute.web.exception.LonelyValidatorAnnotationException;
import org.lastaflute.web.ruts.VirtualActionForm;
import org.lastaflute.web.ruts.VirtualActionForm.RealFormSupplier;
import org.lastaflute.web.util.LaActionExecuteUtil;
import org.lastaflute.web.validation.ActionValidator;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class ActionFormMeta {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute; // not null
    protected final String formKey; // not null
    protected final Class<?> formType; // not null
    protected final OptionalThing<Parameter> listFormParameter; // not null
    protected final Map<String, ActionFormProperty> propertyMap; // not null
    protected final boolean validatorAnnotated; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionFormMeta(ActionExecute execute, String formKey, Class<?> formType, OptionalThing<Parameter> listFormParameter) {
        this.execute = execute;
        this.formKey = formKey;
        this.formType = formType;
        this.listFormParameter = listFormParameter;
        this.propertyMap = setupProperties(formType);
        this.validatorAnnotated = mightBeValidatorAnnotated();
        checkNestedBeanValidatorCalled();
    }

    protected Map<String, ActionFormProperty> setupProperties(Class<?> formType) {
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(formType);
        final int propertyDescSize = beanDesc.getPropertyDescSize();
        final Map<String, ActionFormProperty> map = new HashMap<String, ActionFormProperty>(propertyDescSize);
        for (int i = 0; i < propertyDescSize; i++) {
            final PropertyDesc pd = beanDesc.getPropertyDesc(i);
            if (pd.isReadable()) {
                final ActionFormProperty property = newActionFormProperty(pd);
                addProperty(map, property);
            }
        }
        return map;
    }

    protected ActionFormProperty newActionFormProperty(PropertyDesc pd) {
        return new ActionFormProperty(pd);
    }

    protected void addProperty(Map<String, ActionFormProperty> map, ActionFormProperty property) {
        map.put(property.getPropertyName(), property);
    }

    // ===================================================================================
    //                                                                  Validator Handling
    //                                                                  ==================
    protected boolean mightBeValidatorAnnotated() {
        for (ActionFormProperty property : propertyMap.values()) {
            final Field field = property.getPropertyDesc().getField();
            if (field == null) { // not field property
                continue;
            }
            for (Annotation anno : field.getAnnotations()) {
                if (isValidatorAnnotation(anno.annotationType())) {
                    return true; // first level only
                }
            }
        }
        return false;
    }

    protected void checkNestedBeanValidatorCalled() {
        if (execute.isSuppressValidatorCallCheck()) {
            return;
        }
        propertyMap.values().forEach(property -> doCheckNestedBeanValidatorCalled(property));
    }

    protected void doCheckNestedBeanValidatorCalled(ActionFormProperty property) {
        final Field field = property.getPropertyDesc().getField();
        if (field == null) { // not field property
            return;
        }
        final Class<?> fieldType = field.getType();
        if (isValidableAndCheckTarget(fieldType) && !hasNestedBeanAnnotation(field)) {
            if (Collection.class.isAssignableFrom(fieldType)) {
                final Class<?> genericType = getGenericType(field);
                if (genericType != null && isValidableAndCheckTarget(genericType)) {
                    detectLonelyNestedBean(field, genericType);
                }
            } else { // single bean
                detectLonelyNestedBean(field, fieldType);
            }
        }
    }

    protected boolean isValidableAndCheckTarget(Class<?> fieldType) {
        return !fieldType.isPrimitive() // e.g. int, boolean
                && !String.class.isAssignableFrom(fieldType) //
                && !Number.class.isAssignableFrom(fieldType) //
                && !java.util.Date.class.isAssignableFrom(fieldType) //
                && !DfTypeUtil.isAnyLocalDateType(fieldType) // e.g. LocalDate
                && !Boolean.class.isAssignableFrom(fieldType) //
                && !Classification.class.isAssignableFrom(fieldType) // means CDef
                && !Map.class.isAssignableFrom(fieldType) // check unsupported
                && !Object[].class.isAssignableFrom(fieldType) // check unsupported 
                ;
    }

    protected boolean hasNestedBeanAnnotation(Field field) {
        return ActionValidator.hasNestedBeanAnnotation(field);
    }

    protected void detectLonelyNestedBean(Field field, Class<?> beanType) {
        final BeanDesc beanDesc = BeanDescFactory.getBeanDesc(beanType);
        for (int i = 0; i < beanDesc.getFieldSize(); i++) {
            final Field nestedField = beanDesc.getField(i);
            for (Annotation anno : nestedField.getAnnotations()) {
                if (isValidatorAnnotation(anno.annotationType())) {
                    throwLonelyValidatorAnnotationException(field, nestedField); // only first level
                }
            }
        }
    }

    protected void throwLonelyValidatorAnnotationException(Field goofyField, Field lonelyField) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Lonely validator annotations, add Valid annotation.");
        br.addItem("Adivce");
        br.addElement("When any property in nested bean has validator annotations,");
        br.addElement("The field for nested bean should have the Valid annotation.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public class SeaForm {");
        br.addElement("        public LandBean land; // *NG: no annotation");
        br.addElement("");
        br.addElement("        public static class LandBean {");
        br.addElement("            @Required");
        br.addElement("            public String iks;");
        br.addElement("        }");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public class SeaForm {");
        br.addElement("        @Valid                // OK: javax.validation");
        br.addElement("        public LandBean land;");
        br.addElement("");
        br.addElement("        public static class LandBean {");
        br.addElement("            @Required");
        br.addElement("            public String iks;");
        br.addElement("        }");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute.toSimpleMethodExp());
        br.addItem("Field that needs Valid annotation");
        br.addElement(buildFieldExp(goofyField));
        br.addItem("Lonely Field");
        br.addElement(buildFieldExp(lonelyField));
        final String msg = br.buildExceptionMessage();
        throw new LonelyValidatorAnnotationException(msg);
    }

    protected String buildFieldExp(Field field) {
        final StringBuilder sb = new StringBuilder();
        sb.append(field.getDeclaringClass().getSimpleName());
        sb.append("#").append(field.getName()).append(": ").append(field.getType().getSimpleName());
        final Class<?> genericBeanType = getGenericType(field);
        sb.append(genericBeanType != null ? "<" + genericBeanType.getSimpleName() + ">" : "");
        return sb.toString();
    }

    protected Class<?> getGenericType(Field field) {
        final Type genericType = field.getGenericType();
        return genericType != null ? DfReflectionUtil.getGenericFirstClass(genericType) : null;
    }

    protected boolean isValidatorAnnotation(Class<?> annoType) {
        return ActionValidator.isValidatorAnnotation(annoType);
    }

    // ===================================================================================
    //                                                                   Property Handling
    //                                                                   =================
    public Collection<ActionFormProperty> properties() {
        return propertyMap.values();
    }

    public boolean hasProperty(String propertyName) {
        return getProperty(propertyName) != null;
    }

    public ActionFormProperty getProperty(String propertyName) {
        return propertyMap.get(propertyName);
    }

    // ===================================================================================
    //                                                                        Virtual Form
    //                                                                        ============
    public VirtualActionForm createActionForm() {
        return newVirtualActionForm(getActionFormSupplier(), this);
    }

    protected VirtualActionForm newVirtualActionForm(RealFormSupplier formSupplier, ActionFormMeta formMeta) {
        return new VirtualActionForm(formSupplier, formMeta);
    }

    protected RealFormSupplier getActionFormSupplier() {
        return () -> {
            try {
                checkInstantiatedFormType();
                return formType.newInstance();
            } catch (Exception e) {
                throwActionFormCreateFailureException(e);
                return null; // unreachable
            }
        };
    }

    protected void checkInstantiatedFormType() {
        if (List.class.isAssignableFrom(formType)) { // e.g. List<SeaForm>, JSON body of list type
            String msg = "Cannot instantiate the form because of list type, should not come here:" + formType;
            throw new IllegalStateException(msg);
        }
    }

    protected void throwActionFormCreateFailureException(Exception cause) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to create the action form (or body) for the type.");
        br.addItem("Advice");
        br.addElement("Confirm your action form definition,");
        br.addElement("e.g. action form should be located under 'web' package");
        br.addElement("and the name should end with 'Form' or 'Body'.");
        if (LaActionExecuteUtil.hasActionExecute()) { // just in case
            br.addItem("Action Execute");
            br.addElement(LaActionExecuteUtil.getActionExecute());
        }
        br.addItem("Form Type");
        br.addElement(formType);
        final String msg = br.buildExceptionMessage();
        throw new ActionFormCreateFailureException(msg, cause);
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("formMeta:{").append(formKey);
        sb.append(", ").append(listFormParameter.map(pm -> {
            return pm.getParameterizedType().getTypeName();
        }).orElse(formType.getName()));
        sb.append(", props=").append(propertyMap.size());
        sb.append("}");
        return sb.toString();
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public String getFormKey() {
        return formKey;
    }

    public Class<?> getFormType() {
        return formType;
    }

    // -----------------------------------------------------
    //                                             List Form
    //                                             ---------
    public OptionalThing<Parameter> getListFormParameter() {
        return listFormParameter;
    }

    public OptionalThing<Class<?>> getListFormParameterGenericType() {
        return listFormParameter.map(pm -> {
            /* always exists, already checked in romantic action customizer */
            return DfReflectionUtil.getGenericFirstClass(pm.getParameterizedType());
        });
    }

    public OptionalThing<ParameterizedType> getListFormParameterParameterizedType() {
        return listFormParameter.map(pm -> {
            /* always parameterized, already checked in romantic action customizer */
            return (ParameterizedType) pm.getParameterizedType();
        });
    }

    // -----------------------------------------------------
    //                                              Analyzed
    //                                              --------
    public boolean isValidatorAnnotated() {
        return validatorAnnotated;
    }
}