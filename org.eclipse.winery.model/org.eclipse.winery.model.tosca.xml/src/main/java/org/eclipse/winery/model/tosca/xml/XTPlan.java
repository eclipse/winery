/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.model.tosca.xml;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.winery.model.tosca.xml.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.w3c.dom.Element;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlan", propOrder = {
    "precondition",
    "inputParameters",
    "outputParameters",
    "planModel",
    "planModelReference"
})
public class XTPlan extends XTExtensibleElements {

    @XmlElement(name = "Precondition")
    protected XTCondition precondition;

    @XmlElementWrapper(name = "InputParameters")
    @XmlElement(name = "InputParameter", required = true)
    protected List<XTParameter> inputParameters;

    @XmlElementWrapper(name = "OutputParameters")
    @XmlElement(name = "OutputParameter", required = true)
    protected List<XTParameter> outputParameters;

    @XmlElement(name = "PlanModel")
    protected XTPlan.PlanModel planModel;

    @XmlElement(name = "PlanModelReference")
    protected XTPlan.PlanModelReference planModelReference;

    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    @XmlAttribute(name = "name")
    protected String name;

    @XmlAttribute(name = "planType", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String planType;

    @XmlAttribute(name = "planLanguage", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String planLanguage;

    @Deprecated // required for XML deserialization
    public XTPlan() {
    }

    public XTPlan(Builder builder) {
        super(builder);
        this.precondition = builder.precondition;
        this.inputParameters = builder.inputParameters;
        this.outputParameters = builder.outputParameters;
        this.planModel = builder.planModel;
        this.planModelReference = builder.planModelReference;
        this.id = builder.id;
        this.name = builder.name;
        this.planType = builder.planType;
        this.planLanguage = builder.planLanguage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XTPlan)) return false;
        if (!super.equals(o)) return false;
        XTPlan tPlan = (XTPlan) o;
        return Objects.equals(precondition, tPlan.precondition) &&
            Objects.equals(inputParameters, tPlan.inputParameters) &&
            Objects.equals(outputParameters, tPlan.outputParameters) &&
            Objects.equals(planModel, tPlan.planModel) &&
            Objects.equals(planModelReference, tPlan.planModelReference) &&
            Objects.equals(id, tPlan.id) &&
            Objects.equals(name, tPlan.name) &&
            Objects.equals(planType, tPlan.planType) &&
            Objects.equals(planLanguage, tPlan.planLanguage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), precondition, inputParameters, outputParameters, planModel, planModelReference, id, name, planType, planLanguage);
    }

    @Nullable
    public XTCondition getPrecondition() {
        return precondition;
    }

    public void setPrecondition(@Nullable XTCondition value) {
        this.precondition = value;
    }

    public List<XTParameter> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(List<XTParameter> value) {
        this.inputParameters = value;
    }

    public List<XTParameter> getOutputParameters() {
        return outputParameters;
    }

    public void setOutputParameters(List<XTParameter> value) {
        this.outputParameters = value;
    }

    public XTPlan.@Nullable PlanModel getPlanModel() {
        return planModel;
    }

    public void setPlanModel(XTPlan.@Nullable PlanModel value) {
        this.planModel = value;
    }

    public XTPlan.@Nullable PlanModelReference getPlanModelReference() {
        return planModelReference;
    }

    public void setPlanModelReference(XTPlan.@Nullable PlanModelReference value) {
        this.planModelReference = value;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String value) {
        this.id = value;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String value) {
        this.name = value;
    }

    @NonNull
    public String getPlanType() {
        return planType;
    }

    public void setPlanType(@NonNull String value) {
        this.planType = Objects.requireNonNull(value);
    }

    @NonNull
    public String getPlanLanguage() {
        return planLanguage;
    }

    public void setPlanLanguage(@NonNull String value) {
        this.planLanguage = Objects.requireNonNull(value);
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "any"
    })
    public static class PlanModel implements Serializable {

        @XmlAnyElement(lax = true)
        protected Object any;

        /**
         * Gets the value of the any property.
         *
         * @return possible object is {@link Element } {@link Object }
         */
        @Nullable
        public Object getAny() {
            return any;
        }

        /**
         * Sets the value of the any property.
         *
         * @param value allowed object is {@link Element } {@link Object }
         */
        public void setAny(Object value) {
            this.any = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlanModel planModel = (PlanModel) o;
            return Objects.equals(any, planModel.any);
        }

        @Override
        public int hashCode() {

            return Objects.hash(any);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class PlanModelReference implements Serializable {

        @XmlAttribute(name = "reference", required = true)
        @XmlSchemaType(name = "anyURI")
        protected String reference;

        /**
         * Gets the value of the reference property.
         *
         * @return possible object is {@link String }
         */
        @NonNull
        public String getReference() {
            return reference;
        }

        /**
         * Sets the value of the reference property.
         *
         * @param value allowed object is {@link String }
         */
        public void setReference(String value) {
            this.reference = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlanModelReference that = (PlanModelReference) o;
            return Objects.equals(reference, that.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference);
        }
    }

    public static class Builder extends XTExtensibleElements.Builder<Builder> {
        private final String id;
        private final String planType;
        private final String planLanguage;

        private XTCondition precondition;
        private List<XTParameter> inputParameters;
        private List<XTParameter> outputParameters;
        private PlanModel planModel;
        private PlanModelReference planModelReference;
        private String name;

        public Builder(String id, String planType, String planLanguage) {
            this.id = id;
            this.planType = planType;
            this.planLanguage = planLanguage;
        }

        public Builder setPrecondition(XTCondition precondition) {
            this.precondition = precondition;
            return this;
        }

        public Builder setInputParameters(List<XTParameter> inputParameters) {
            this.inputParameters = inputParameters;
            return this;
        }

        public Builder setOutputParameters(List<XTParameter> outputParameters) {
            this.outputParameters = outputParameters;
            return this;
        }

        public Builder setPlanModel(PlanModel planModel) {
            this.planModel = planModel;
            return this;
        }

        public Builder setPlanModelReference(PlanModelReference planModelReference) {
            this.planModelReference = planModelReference;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public XTPlan build() {
            return new XTPlan(this);
        }
    }
}
