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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * <p>Java class for tTopologyTemplate complex type.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;complexType name="tTopologyTemplate">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.oasis-open.org/tosca/ns/2011/12}tExtensibleElements">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="NodeTemplate" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tNodeTemplate"/>
 *         &lt;element name="RelationshipTemplate" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tRelationshipTemplate"/>
 *       &lt;/choice>
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tTopologyTemplate", propOrder = {
    "nodeTemplateOrRelationshipTemplate"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TTopologyTemplate extends TExtensibleElements {
    @XmlElements( {
        @XmlElement(name = "RelationshipTemplate", type = TRelationshipTemplate.class),
        @XmlElement(name = "NodeTemplate", type = TNodeTemplate.class)
    })
    protected List<TEntityTemplate> nodeTemplateOrRelationshipTemplate;

    public TTopologyTemplate() {
    }

    public TTopologyTemplate(Builder builder) {
        super(builder);
        this.nodeTemplateOrRelationshipTemplate = builder.getNodeTemplateOrRelationshipTemplate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TTopologyTemplate)) return false;
        if (!super.equals(o)) return false;
        TTopologyTemplate that = (TTopologyTemplate) o;
        return Objects.equals(nodeTemplateOrRelationshipTemplate, that.nodeTemplateOrRelationshipTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeTemplateOrRelationshipTemplate);
    }

    /**
     * Gets the value of the nodeTemplateOrRelationshipTemplate property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nodeTemplateOrRelationshipTemplate property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNodeTemplateOrRelationshipTemplate().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TRelationshipTemplate }
     * {@link TNodeTemplate }
     */
    @JsonIgnore
    @NonNull
    public List<TEntityTemplate> getNodeTemplateOrRelationshipTemplate() {
        if (nodeTemplateOrRelationshipTemplate == null) {
            nodeTemplateOrRelationshipTemplate = new ArrayList<TEntityTemplate>();
        }
        return this.nodeTemplateOrRelationshipTemplate;
    }

    /**
     * @return all nodes templates of the topologyTemplate
     */
    @NonNull
    public List<TNodeTemplate> getNodeTemplates() {
        return this.getNodeTemplateOrRelationshipTemplate()
            .stream()
            .filter(x -> x instanceof TNodeTemplate)
            .map(TNodeTemplate.class::cast)
            .collect(Collectors.toList());
    }

    public void setNodeTemplates(List<TNodeTemplate> nodeTemplates) {
        this.nodeTemplateOrRelationshipTemplate = Stream.concat(
            nodeTemplates.stream().map(TEntityTemplate.class::cast),
            this.getRelationshipTemplates().stream().map(TEntityTemplate.class::cast))
            .collect(Collectors.toList());
    }

    /**
     * @return node template having the given id. null if not found
     */
    @Nullable
    public TNodeTemplate getNodeTemplate(String id) {
        Objects.requireNonNull(id);
        return this.getNodeTemplates().stream()
            .filter(x -> id.equals(x.getId()))
            .findAny()
            .orElse(null);
    }

    /**
     * @return all relationship templates of the topologyTemplate
     */
    @NonNull
    public List<TRelationshipTemplate> getRelationshipTemplates() {
        return this.getNodeTemplateOrRelationshipTemplate()
            .stream()
            .filter(x -> x instanceof TRelationshipTemplate)
            .map(TRelationshipTemplate.class::cast)
            .collect(Collectors.toList());
    }

    public void setRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
        this.nodeTemplateOrRelationshipTemplate = Stream.concat(
            this.getNodeTemplates().stream().map(TEntityTemplate.class::cast),
            relationshipTemplates.stream().map(TEntityTemplate.class::cast))
            .collect(Collectors.toList());
    }

    /**
     * @return relationship template having the given id. null if not found
     */
    @Nullable
    public TRelationshipTemplate getRelationshipTemplate(String id) {
        Objects.requireNonNull(id);
        return this.getRelationshipTemplates().stream()
            .filter(x -> id.equals(x.getId()))
            .findAny()
            .orElse(null);
    }

    public void addNodeTemplate(TNodeTemplate nt) {
        this.getNodeTemplateOrRelationshipTemplate().add(nt);
    }

    public void addRelationshipTemplate(TRelationshipTemplate rt) {
        this.getNodeTemplateOrRelationshipTemplate().add(rt);
    }

    public static class Builder extends TExtensibleElements.Builder<Builder> {
        private List<TNodeTemplate> nodeTemplates;
        private List<TRelationshipTemplate> relationshipTemplates;

        public Builder() {

        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder setNodeTemplates(List<TNodeTemplate> nodeTemplates) {
            this.nodeTemplates = nodeTemplates;
            return this;
        }

        public Builder setRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
            this.relationshipTemplates = relationshipTemplates;
            return this;
        }

        public Builder addNodeTemplates(List<TNodeTemplate> nodeTemplates) {
            if (nodeTemplates == null || nodeTemplates.isEmpty()) {
                return this;
            }

            if (this.nodeTemplates == null) {
                this.nodeTemplates = nodeTemplates;
            } else {
                this.nodeTemplates.addAll(nodeTemplates);
            }
            return this;
        }

        public Builder addNodeTemplates(TNodeTemplate nodeTemplates) {
            if (nodeTemplates == null) {
                return this;
            }

            List<TNodeTemplate> tmp = new ArrayList<>();
            tmp.add(nodeTemplates);
            return addNodeTemplates(tmp);
        }

        public Builder addRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
            if (relationshipTemplates == null || relationshipTemplates.isEmpty()) {
                return this;
            }

            if (this.relationshipTemplates == null) {
                this.relationshipTemplates = relationshipTemplates;
            } else {
                this.relationshipTemplates.addAll(relationshipTemplates);
            }
            return this;
        }

        public Builder addRelationshipTemplate(TRelationshipTemplate relationshipTemplates) {
            if (this.relationshipTemplates == null) {
                return this;
            }

            List<TRelationshipTemplate> tmp = new ArrayList<>();
            tmp.add(relationshipTemplates);
            return addRelationshipTemplates(tmp);
        }

        public List<TEntityTemplate> getNodeTemplateOrRelationshipTemplate() {
            List<TEntityTemplate> tmp = new ArrayList<>();
            Optional.ofNullable(nodeTemplates).ifPresent(tmp::addAll);
            Optional.ofNullable(relationshipTemplates).ifPresent(tmp::addAll);
            return tmp;
        }

        public TTopologyTemplate build() {
            return new TTopologyTemplate(this);
        }
    }
}
