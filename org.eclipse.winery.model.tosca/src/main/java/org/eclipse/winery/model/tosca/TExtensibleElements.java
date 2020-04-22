/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.tosca;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.visitor.Visitor;

import io.github.adr.embedded.ADR;
import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tExtensibleElements", propOrder = {
    "documentation",
    "any"
})
@XmlSeeAlso( {
    TImport.class,
    TServiceTemplate.class,
    TEntityTypeImplementation.class,
    TOperation.class,
    TRequirementDefinition.class,
    TExtension.class,
    TCapabilityDefinition.class,
    TExtensions.class,
    TDeploymentArtifact.class,
    TPlan.class,
    TEntityTemplate.class,
    TEntityType.class,
    TPolicy.class,
    TImplementationArtifact.class,
    TTopologyTemplate.class,
    TDefinitions.class
})
public abstract class TExtensibleElements implements Serializable {

    protected List<TDocumentation> documentation;

    @XmlAnyElement(lax = true)
    protected List<Object> any;

    @XmlAnyAttribute
    @NonNull
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    public TExtensibleElements() {
    }

    public TExtensibleElements(Builder builder) {
        this.documentation = builder.documentation;
        this.any = builder.any;
        if (builder.otherAttributes == null) {
            this.otherAttributes.clear();
        } else {
            this.otherAttributes = builder.otherAttributes;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TExtensibleElements)) return false;
        TExtensibleElements that = (TExtensibleElements) o;
        return Objects.equals(documentation, that.documentation) &&
            Objects.equals(any, that.any) &&
            Objects.equals(otherAttributes, that.otherAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentation, any, otherAttributes);
    }

    @NonNull
    public List<TDocumentation> getDocumentation() {
        if (documentation == null) {
            documentation = new ArrayList<TDocumentation>();
        }
        return this.documentation;
    }

    @NonNull
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    @NonNull
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

    public abstract void accept(Visitor visitor);

    /**
     * Generic abstract Builder
     *
     * @param <T> the Builder which extends this abstract Builder
     */
    @ADR(11)
    public abstract static class Builder<T extends Builder<T>> {
        private List<TDocumentation> documentation;
        private List<Object> any;
        private Map<QName, String> otherAttributes;

        public Builder() {
        }

        public Builder(Builder builder) {
            this.documentation = builder.documentation;
            this.any = builder.any;
            this.otherAttributes = builder.otherAttributes;
        }

        public Builder(TExtensibleElements extensibleElements) {
            this.addDocumentation(extensibleElements.getDocumentation());
            this.addAny(extensibleElements.getAny());
            this.addOtherAttributes(extensibleElements.getOtherAttributes());
        }

        public T setDocumentation(List<TDocumentation> documentation) {
            this.documentation = documentation;
            return self();
        }

        public T setAny(List<Object> any) {
            this.any = any;
            return self();
        }

        public T setOtherAttributes(Map<QName, String> otherAttributes) {
            this.otherAttributes = otherAttributes;
            return self();
        }

        public T addDocumentation(List<TDocumentation> documentation) {
            if (documentation == null) {
                return self();
            }

            if (this.documentation == null) {
                this.documentation = documentation;
            } else {
                this.documentation.addAll(documentation);
            }
            return self();
        }

        public T addDocumentation(TDocumentation documentation) {
            if (documentation == null) {
                return self();
            }

            List<TDocumentation> tmp = new ArrayList<>();
            tmp.add(documentation);
            return addDocumentation(tmp);
        }

        public T addDocumentation(String documentation) {
            if (documentation == null || documentation.length() == 0) {
                return self();
            }

            TDocumentation tmp = new TDocumentation();
            tmp.getContent().add(documentation);
            return self().addDocumentation(tmp);
        }

        public T addDocumentation(Map<String, String> documentation) {
            if (documentation == null) {
                return self();
            }

            for (Map.Entry<String, String> entry : documentation.entrySet()) {
                this.addDocumentation(entry.getKey() + ": " + entry.getValue());
            }
            return self();
        }

        public T addAny(List<Object> any) {
            if (any == null || any.isEmpty()) {
                return self();
            }

            if (this.any == null) {
                this.any = any;
            } else {
                this.any.addAll(any);
            }
            return self();
        }

        public T addAny(Object any) {
            if (any == null) {
                return self();
            }

            List<Object> tmp = new ArrayList<>();
            tmp.add(any);
            return addAny(tmp);
        }

        public T addOtherAttributes(Map<QName, String> otherAttributes) {
            if (otherAttributes == null || otherAttributes.isEmpty()) {
                return self();
            }

            if (this.otherAttributes == null) {
                this.otherAttributes = otherAttributes;
            } else {
                this.otherAttributes.putAll(otherAttributes);
            }
            return self();
        }

        public T addOtherAttributes(QName key, String value) {
            if (key == null) {
                return self();
            }

            LinkedHashMap<QName, String> map = new LinkedHashMap<>();
            map.put(key, value);
            return addOtherAttributes(map);
        }

        @ADR(11)
        public abstract T self();
    }
}
