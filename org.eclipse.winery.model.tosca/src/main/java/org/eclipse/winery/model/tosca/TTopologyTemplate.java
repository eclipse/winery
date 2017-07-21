/*******************************************************************************
 * Copyright (c) 2013-2017 University of Stuttgart
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *    Oliver Kopp - initial code generation using vhudson-jaxb-ri-2.1-2
 *******************************************************************************/

package org.eclipse.winery.model.tosca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;


/**
 * <p>Java class for tTopologyTemplate complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
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
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tTopologyTemplate", propOrder = {
    "nodeTemplateOrRelationshipTemplate"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TTopologyTemplate
    extends TExtensibleElements
{

    @XmlElements({
        @XmlElement(name = "RelationshipTemplate", type = TRelationshipTemplate.class),
        @XmlElement(name = "NodeTemplate", type = TNodeTemplate.class)
    })
    protected List<TEntityTemplate> nodeTemplateOrRelationshipTemplate;

    /**
     * Gets the value of the nodeTemplateOrRelationshipTemplate property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nodeTemplateOrRelationshipTemplate property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNodeTemplateOrRelationshipTemplate().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TRelationshipTemplate }
     * {@link TNodeTemplate }
     *
     *
     */
	@JsonIgnore
    public List<TEntityTemplate> getNodeTemplateOrRelationshipTemplate() {
        if (nodeTemplateOrRelationshipTemplate == null) {
            nodeTemplateOrRelationshipTemplate = new ArrayList<TEntityTemplate>();
        }
        return this.nodeTemplateOrRelationshipTemplate;
    }

	/**
	 * @return all nodes templates of the topologyTemplate
	 */
	public List<TNodeTemplate> getNodeTemplates() {
		return this.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TNodeTemplate)
				.map(TNodeTemplate.class::cast)
				.collect(Collectors.toList());
	}

	/**
	 * @return node template having the given id. null if not found
	 */
	public TNodeTemplate getNodeTemplate(String id) {
		Objects.requireNonNull(id);
		return this.getNodeTemplates().stream()
				.filter(x -> id.equals(x.getId()))
				.findAny()
				.orElse(null);
	}

	public void setNodeTemplates(List<TNodeTemplate> nodeTemplates) {
		this.nodeTemplateOrRelationshipTemplate = Stream.concat(
				nodeTemplates.stream().map(TEntityTemplate.class::cast),
				this.getRelationshipTemplates().stream().map(TEntityTemplate.class::cast))
				.collect(Collectors.toList());
	}

	/**
	 * @return all relationship templates of the topologyTemplate
	 */
	public List<TRelationshipTemplate> getRelationshipTemplates() {
		return this.getNodeTemplateOrRelationshipTemplate()
				.stream()
				.filter(x -> x instanceof TRelationshipTemplate)
				.map(TRelationshipTemplate.class::cast)
				.collect(Collectors.toList());
	}

	/**
	 * @return relationship template having the given id. null if not found
	 */
	public TRelationshipTemplate getRelationshipTemplate(String id) {
		Objects.requireNonNull(id);
		return this.getRelationshipTemplates().stream()
				.filter(x -> id.equals(x.getId()))
				.findAny()
				.orElse(null);
	}

	public void setRelationshipTemplates(List<TRelationshipTemplate> relationshipTemplates) {
		this.nodeTemplateOrRelationshipTemplate = Stream.concat(
				this.getNodeTemplates().stream().map(TEntityTemplate.class::cast),
				relationshipTemplates.stream().map(TEntityTemplate.class::cast))
				.collect(Collectors.toList());
	}

	public void addNodeTemplate(TNodeTemplate nt) {
		this.getNodeTemplateOrRelationshipTemplate().add(nt);
	}

	public void addRelationshipTemplate(TRelationshipTemplate rt) {
		this.getNodeTemplateOrRelationshipTemplate().add(rt);
	}

}
