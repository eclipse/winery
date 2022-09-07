/*******************************************************************************
 * Copyright (c) 2020-2021 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.xml.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.winery.model.tosca.HasId;
import org.eclipse.winery.model.tosca.RelationshipSourceOrTarget;
import org.eclipse.winery.model.tosca.TArtifact;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.TCapabilityRef;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TCondition;
import org.eclipse.winery.model.tosca.TConstraint;
import org.eclipse.winery.model.tosca.TDefinitions;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TDocumentation;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TEntityTypeImplementation;
import org.eclipse.winery.model.tosca.TExportedInterface;
import org.eclipse.winery.model.tosca.TExportedOperation;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TExtension;
import org.eclipse.winery.model.tosca.TGroupDefinition;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TPlan;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TPolicyTemplate;
import org.eclipse.winery.model.tosca.TPolicyType;
import org.eclipse.winery.model.tosca.TPropertyConstraint;
import org.eclipse.winery.model.tosca.TPropertyMapping;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.TRequiredContainerFeature;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TRequirementDefinition;
import org.eclipse.winery.model.tosca.TRequirementRef;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMapping;
import org.eclipse.winery.model.tosca.extensions.OTBehaviorPatternMapping;
import org.eclipse.winery.model.tosca.extensions.OTComplianceRule;
import org.eclipse.winery.model.tosca.extensions.OTDeploymentArtifactMapping;
import org.eclipse.winery.model.tosca.extensions.OTParticipant;
import org.eclipse.winery.model.tosca.extensions.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTPermutationMapping;
import org.eclipse.winery.model.tosca.extensions.OTPrmMapping;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTRelationMapping;
import org.eclipse.winery.model.tosca.extensions.OTStayMapping;
import org.eclipse.winery.model.tosca.extensions.OTStringList;
import org.eclipse.winery.model.tosca.extensions.OTTestRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.extensions.kvproperties.OTPropertyKV;
import org.eclipse.winery.model.tosca.extensions.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.xml.XDefinitions;
import org.eclipse.winery.model.tosca.xml.XHasId;
import org.eclipse.winery.model.tosca.xml.XRelationshipSourceOrTarget;
import org.eclipse.winery.model.tosca.xml.XTArtifact;
import org.eclipse.winery.model.tosca.xml.XTArtifactReference;
import org.eclipse.winery.model.tosca.xml.XTArtifactTemplate;
import org.eclipse.winery.model.tosca.xml.XTArtifactType;
import org.eclipse.winery.model.tosca.xml.XTBoolean;
import org.eclipse.winery.model.tosca.xml.XTBoundaryDefinitions;
import org.eclipse.winery.model.tosca.xml.XTCapability;
import org.eclipse.winery.model.tosca.xml.XTCapabilityDefinition;
import org.eclipse.winery.model.tosca.xml.XTCapabilityRef;
import org.eclipse.winery.model.tosca.xml.XTCapabilityType;
import org.eclipse.winery.model.tosca.xml.XTCondition;
import org.eclipse.winery.model.tosca.xml.XTConstraint;
import org.eclipse.winery.model.tosca.xml.XTDefinitions;
import org.eclipse.winery.model.tosca.xml.XTDeploymentArtifact;
import org.eclipse.winery.model.tosca.xml.XTDocumentation;
import org.eclipse.winery.model.tosca.xml.XTEntityTemplate;
import org.eclipse.winery.model.tosca.xml.XTEntityType;
import org.eclipse.winery.model.tosca.xml.XTEntityTypeImplementation;
import org.eclipse.winery.model.tosca.xml.XTExportedInterface;
import org.eclipse.winery.model.tosca.xml.XTExportedOperation;
import org.eclipse.winery.model.tosca.xml.XTExtensibleElements;
import org.eclipse.winery.model.tosca.xml.XTExtension;
import org.eclipse.winery.model.tosca.xml.XTImplementationArtifact;
import org.eclipse.winery.model.tosca.xml.XTImport;
import org.eclipse.winery.model.tosca.xml.XTInstanceState;
import org.eclipse.winery.model.tosca.xml.XTInterface;
import org.eclipse.winery.model.tosca.xml.XTNodeTemplate;
import org.eclipse.winery.model.tosca.xml.XTNodeType;
import org.eclipse.winery.model.tosca.xml.XTNodeTypeImplementation;
import org.eclipse.winery.model.tosca.xml.XTOperation;
import org.eclipse.winery.model.tosca.xml.XTParameter;
import org.eclipse.winery.model.tosca.xml.XTPlan;
import org.eclipse.winery.model.tosca.xml.XTPolicy;
import org.eclipse.winery.model.tosca.xml.XTPolicyTemplate;
import org.eclipse.winery.model.tosca.xml.XTPolicyType;
import org.eclipse.winery.model.tosca.xml.XTPropertyConstraint;
import org.eclipse.winery.model.tosca.xml.XTPropertyMapping;
import org.eclipse.winery.model.tosca.xml.XTRelationshipTemplate;
import org.eclipse.winery.model.tosca.xml.XTRelationshipType;
import org.eclipse.winery.model.tosca.xml.XTRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.xml.XTRequiredContainerFeature;
import org.eclipse.winery.model.tosca.xml.XTRequirement;
import org.eclipse.winery.model.tosca.xml.XTRequirementDefinition;
import org.eclipse.winery.model.tosca.xml.XTRequirementRef;
import org.eclipse.winery.model.tosca.xml.XTRequirementType;
import org.eclipse.winery.model.tosca.xml.XTServiceTemplate;
import org.eclipse.winery.model.tosca.xml.XTTag;
import org.eclipse.winery.model.tosca.xml.XTTopologyTemplate;
import org.eclipse.winery.model.tosca.xml.extensions.XOTAttributeMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTAttributeMappingType;
import org.eclipse.winery.model.tosca.xml.extensions.XOTBehaviorPatternMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTComplianceRule;
import org.eclipse.winery.model.tosca.xml.extensions.XOTDeploymentArtifactMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTPatternRefinementModel;
import org.eclipse.winery.model.tosca.xml.extensions.XOTPermutationMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTPrmMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTPropertyKV;
import org.eclipse.winery.model.tosca.xml.extensions.XOTRefinementModel;
import org.eclipse.winery.model.tosca.xml.extensions.XOTRelationDirection;
import org.eclipse.winery.model.tosca.xml.extensions.XOTRelationMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTStayMapping;
import org.eclipse.winery.model.tosca.xml.extensions.XOTStringList;
import org.eclipse.winery.model.tosca.xml.extensions.XOTTestRefinementModel;
import org.eclipse.winery.model.tosca.xml.extensions.XOTTopologyFragmentRefinementModel;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.common.ListUtils.listIsNotNullOrEmpty;

@SuppressWarnings("DuplicatedCode")
public class FromCanonical {

    private static final Logger LOGGER = LoggerFactory.getLogger(FromCanonical.class);

    public FromCanonical() {
    }

    public XTDefinitions convert(TDefinitions canonical) {
        return convert(canonical, false);
    }

    /**
     * Converts a canonical TDefinitions collection to XML TDefinitions.
     */
    public XTDefinitions convert(TDefinitions canonical, boolean convertImports) {
        if (convertImports) {
            LOGGER.warn("Converting imports is not yet implemented!");
        }

        // FIXME need to correctly deal with convertImports flag to create a self-contained Definitions to export as CSAR if it is set.
        XDefinitions.Builder builder = new XDefinitions.Builder(canonical.getId(), canonical.getTargetNamespace())
            .setImport(convertList(canonical.getImport(), this::convert))
            .addExtensions(convertExtensions(canonical.getExtensions()))
            .addTypes(convertTypes(canonical.getTypes()))
            .setServiceTemplates(convertList(canonical.getServiceTemplates(), this::convert))
            .setNodeTypes(convertList(canonical.getNodeTypes(), this::convert))
            .setNodeTypeImplementations(convertList(canonical.getNodeTypeImplementations(), this::convert))
            .setRelationshipTypes(convertList(canonical.getRelationshipTypes(), this::convert))
            .setRelationshipTypeImplementations(convertList(canonical.getRelationshipTypeImplementations(), this::convert))
            .setCapabilityTypes(convertList(canonical.getCapabilityTypes(), this::convert))
            .setArtifactTypes(convertList(canonical.getArtifactTypes(), this::convert))
            .setArtifactTemplates(convertList(canonical.getArtifactTemplates(), this::convert))
            .setPolicyTypes(convertList(canonical.getPolicyTypes(), this::convert))
            .setPolicyTemplate(convertList(canonical.getPolicyTemplates(), this::convert))
            .setRequirementTypes(convertList(canonical.getRequirementTypes(), this::convert))
            .setName(canonical.getName());
        // this handles the "conversion" – basically copying of data – required by the disjoint TExtensibleElements
        //  acting as baseclass for all the extensions we support
        builder.addNonStandardElements(convertList(canonical.getPatternRefinementModels(), this::convert))
            .addNonStandardElements(convertList(canonical.getTestRefinementModels(), this::convert))
            .addNonStandardElements(convertList(canonical.getComplianceRules(), this::convert));
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTRelationshipType convert(TRelationshipType canonical) {
        XTRelationshipType.Builder builder = new XTRelationshipType.Builder(canonical.getIdFromIdOrNameField())
            .addSourceInterfaces(convertInterfaces(canonical.getSourceInterfaces()))
            .addTargetInterfaces(convertInterfaces(canonical.getTargetInterfaces()))
            .addInterfaces(convertInterfaces(canonical.getInterfaces()));
        if (canonical.getValidSource() != null) {
            builder.setValidSource(canonical.getValidSource().getTypeRef());
        }
        if (canonical.getValidTarget() != null) {
            builder.setValidTarget(canonical.getValidTarget().getTypeRef());
        }
        if (canonical.getInstanceStates() != null) {
            List<XTInstanceState> instanceStates = canonical.getInstanceStates().stream()
                .map(c -> new XTInstanceState(c.getState())).collect(Collectors.toList());
            builder.setInstanceStates(instanceStates);
        }
        if (canonical.getInterfaceDefinitions() != null) {
            LOGGER.warn("Converting YAML InterfaceDefinitions to TOSCA XML is currently not supported");
        }
        fillEntityTypeProperties(builder, canonical);
        return builder.build();
    }

    private List<XTInterface> convertInterfaces(@Nullable List<TInterface> interfaces) {
        if (interfaces == null) {
            return Collections.emptyList();
        }
        return interfaces.stream()
            .map(this::convert)
            .collect(Collectors.toList());
    }

    private XTInterface convert(TInterface canonical) {
        return new XTInterface.Builder(canonical.getName(), convertOperations(canonical.getOperations())).build();
    }

    private List<XTOperation> convertOperations(List<TOperation> operation) {
        return operation.stream().map(this::convert).collect(Collectors.toList());
    }

    private XTOperation convert(TOperation canonical) {
        XTOperation.Builder builder = new XTOperation.Builder(canonical.getName());
        if (canonical.getInputParameters() != null) {
            builder.addInputParameters(canonical.getInputParameters().stream()
                .map(this::convert).collect(Collectors.toList()));
        }
        if (canonical.getOutputParameters() != null) {
            builder.addOutputParameters(canonical.getOutputParameters().stream()
                .map(this::convert).collect(Collectors.toList()));
        }
        return builder.build();
    }

    private XTParameter convert(TParameter canonical) {
        return new XTParameter.Builder(canonical.getName(), canonical.getType(), canonical.getRequired()).build();
    }

    private XTRelationshipTypeImplementation convert(TRelationshipTypeImplementation canonical) {
        XTRelationshipTypeImplementation.Builder builder = new XTRelationshipTypeImplementation.Builder(canonical.getName(), canonical.getRelationshipType());
        fillEntityTypeImplementationProperties(builder, canonical);
        if (canonical.getDerivedFrom() != null) {
            XTRelationshipTypeImplementation.DerivedFrom derived = new XTRelationshipTypeImplementation.DerivedFrom();
            derived.setRelationshipTypeImplementationRef(canonical.getDerivedFrom().getRelationshipTypeImplementationRef());
            builder.setDerivedFrom(derived);
        }
        return builder.build();
    }

    private <Builder extends XTEntityTypeImplementation.Builder<Builder>, Value extends TEntityTypeImplementation>
    void fillEntityTypeImplementationProperties(Builder builder, Value canonical) {
        if (canonical.getRequiredContainerFeatures() != null) {
            builder.addRequiredContainerFeatures(
                canonical.getRequiredContainerFeatures().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        if (canonical.getTags() != null) {
            builder.addTags(convertList(canonical.getTags(), this::convert));
        }
        if (canonical.getImplementationArtifacts() != null) {
            builder.addImplementationArtifacts(canonical.getImplementationArtifacts().stream()
                .map(this::convert).collect(Collectors.toList()));
        }
        builder.setTargetNamespace(canonical.getTargetNamespace());
        builder.setAbstract(canonical.getAbstract() ? XTBoolean.YES : XTBoolean.NO);
        builder.setFinal(canonical.getFinal() ? XTBoolean.YES : XTBoolean.NO);
        fillExtensibleElementsProperties(builder, canonical);
    }

    private XTImplementationArtifact convert(TImplementationArtifact canonical) {
        return new XTImplementationArtifact.Builder(canonical.getArtifactType())
            .setName(canonical.getName())
            .setInterfaceName(canonical.getInterfaceName())
            .setOperationName(canonical.getOperationName())
            .setArtifactRef(canonical.getArtifactRef())
            .build();
    }

    private XTTag convert(TTag canonical) {
        return new XTTag.Builder(canonical.getName(), canonical.getValue())
            .build();
    }

    private XTRequiredContainerFeature convert(TRequiredContainerFeature canonical) {
        return new XTRequiredContainerFeature(canonical.getFeature());
    }

    private XTPolicyTemplate convert(TPolicyTemplate canonical) {
        XTPolicyTemplate.Builder builder = new XTPolicyTemplate.Builder(canonical.getId(), canonical.getType());
        builder.setName(canonical.getName());
        fillEntityTemplateProperties(builder, canonical);
        return builder.build();
    }

    private XTPolicyType convert(TPolicyType canonical) {
        XTPolicyType.Builder builder = new XTPolicyType.Builder(canonical.getName());
        if (!canonical.getAppliesTo().isEmpty()) {
            builder.setAppliesTo(
                canonical.getAppliesTo().stream()
                    .map(c -> new XTPolicyType.XNodeTypeReference(c.getTypeRef()))
                    .collect(Collectors.toList())
            );
        }
        fillEntityTypeProperties(builder, canonical);
        return builder.build();
    }

    private <Builder extends XTEntityType.Builder<Builder>, Value extends TEntityType>
    void fillEntityTypeProperties(Builder builder, Value canonical) {
        if (canonical.getTags() != null) {
            builder.addTags(convertList(canonical.getTags(), this::convert));
        }
        if (canonical.getDerivedFrom() != null) {
            XTEntityType.DerivedFrom derived = new XTEntityType.DerivedFrom();
            derived.setTypeRef(canonical.getDerivedFrom().getTypeRef());
            builder.setDerivedFrom(derived);
        }
        if (canonical.getProperties() != null) {
            TEntityType.PropertiesDefinition properties = canonical.getProperties();
            if (properties instanceof WinerysPropertiesDefinition) {
                // do the magic thingy of storing the properties' definition in the any element.
                builder.addAny(properties);
            } else if (properties instanceof TEntityType.XmlElementDefinition) {
                XTEntityType.PropertiesDefinition propertiesDefinition = new XTEntityType.PropertiesDefinition();
                propertiesDefinition.setElement(((TEntityType.XmlElementDefinition) properties).getElement());
                builder.setPropertiesDefinition(propertiesDefinition);
            } else if (properties instanceof TEntityType.XmlTypeDefinition) {
                XTEntityType.PropertiesDefinition propertiesDefinition = new XTEntityType.PropertiesDefinition();
                propertiesDefinition.setType(((TEntityType.XmlTypeDefinition) properties).getType()
                );
                builder.setPropertiesDefinition(propertiesDefinition);
            } else if (properties instanceof TEntityType.YamlPropertiesDefinition) {
                // unsupported!? (currently)
                LOGGER.warn("Trying to convert YAML-based type definition [{}] to XML. Properties are incorrect!", canonical.getQName());
            }
        }
        builder.setAbstract(canonical.getAbstract() ? XTBoolean.YES : XTBoolean.NO);
        builder.setFinal(canonical.getFinal() ? XTBoolean.YES : XTBoolean.NO);
        builder.setTargetNamespace(canonical.getTargetNamespace());
        fillExtensibleElementsProperties(builder, canonical);
    }

    private <Builder extends XTExtensibleElements.Builder<Builder>, Value extends TExtensibleElements>
    void fillExtensibleElementsProperties(Builder builder, Value canonical) {
        builder.setDocumentation(canonical.getDocumentation().stream().map(this::convert).collect(Collectors.toList()));
        builder.setOtherAttributes(canonical.getOtherAttributes());
        builder.addAny(canonical.getAny());
    }

    private XTRequirementType convert(TRequirementType canonical) {
        XTRequirementType.Builder builder = new XTRequirementType.Builder(canonical.getName());
        builder.setRequiredCapabilityType(canonical.getRequiredCapabilityType());
        fillEntityTypeProperties(builder, canonical);
        return builder.build();
    }

    private XTNodeTypeImplementation convert(TNodeTypeImplementation canonical) {
        XTNodeTypeImplementation.Builder builder = new XTNodeTypeImplementation.Builder(canonical.getName(), canonical.getNodeType());
        if (canonical.getDeploymentArtifacts() != null) {
            builder.setDeploymentArtifacts(convertList(canonical.getDeploymentArtifacts(), this::convert));
        }
        if (canonical.getDerivedFrom() != null) {
            XTNodeTypeImplementation.DerivedFrom derived = new XTNodeTypeImplementation.DerivedFrom();
            derived.setNodeTypeImplementationRef(canonical.getDerivedFrom().getNodeTypeImplementationRef());
            builder.setDerivedFrom(derived);
        }
        fillEntityTypeImplementationProperties(builder, canonical);
        return builder.build();
    }

    private XTDeploymentArtifact convert(TDeploymentArtifact canonical) {
        XTDeploymentArtifact.Builder builder = new XTDeploymentArtifact.Builder(canonical.getName(), canonical.getArtifactType());
        builder.setArtifactRef(canonical.getArtifactRef());
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTNodeType convert(TNodeType canonical) {
        XTNodeType.Builder builder = new XTNodeType.Builder(canonical.getName());
        if (listIsNotNullOrEmpty(canonical.getRequirementDefinitions())) {
            builder.setRequirementDefinitions(convertList(canonical.getRequirementDefinitions(), this::convert));
        }
        if (canonical.getCapabilityDefinitions() != null) {
            builder.setCapabilityDefinitions(convertList(canonical.getCapabilityDefinitions(), this::convert));
        }
        if (canonical.getInstanceStates() != null) {
            List<XTInstanceState> instanceStates = canonical.getInstanceStates().stream()
                .map(c -> new XTInstanceState(c.getState())).collect(Collectors.toList());
            builder.setInstanceStates(instanceStates);
        }
        if (canonical.getInterfaces() != null) {
            builder.setInterfaces(convertList(canonical.getInterfaces(), this::convert));
        }
        if (canonical.getInterfaceDefinitions() != null) {
            LOGGER.warn("Converting YAML InterfaceDefinitions to XML is currently not supported!");
        }
        if (canonical.getArtifacts() != null) {
            builder.addToAny(
                this.convertList(canonical.getArtifacts(), this::convert)
            );
        }
        fillEntityTypeProperties(builder, canonical);
        return builder.build();
    }

    private XTArtifact convert(TArtifact canonical) {
        XTArtifact.Builder builder = new XTArtifact.Builder(canonical.getName(), canonical.getType());
        builder.setDeployPath(canonical.getDeployPath());
        builder.setDescription(canonical.getDescription());
        builder.setFile(canonical.getFile());
        fillEntityTemplateProperties(builder, canonical);
        return builder.build();
    }

    private XTRequirementDefinition convert(TRequirementDefinition canonical) {
        // requirementType can be null in the canonical model because YAML mode doesn't use it.
        //  it's required for us, though, so we just assume it's present
        XTRequirementDefinition.Builder builder = new XTRequirementDefinition.Builder(canonical.getName(), canonical.getRequirementType());
        if (canonical.getConstraints() != null) {
            builder.setConstraints(
                canonical.getConstraints().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        builder.setLowerBound(canonical.getLowerBound());
        builder.setUpperBound(canonical.getUpperBound());
        // FIXME capability, node and relationship are YAML things, they should not be moved around here
        builder.setCapability(canonical.getCapability());
        builder.setNode(canonical.getNode());
        builder.setRelationship(canonical.getRelationship());
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTCapabilityDefinition convert(TCapabilityDefinition canonical) {
        XTCapabilityDefinition.Builder builder = new XTCapabilityDefinition.Builder(canonical.getName(), canonical.getCapabilityType());
        if (canonical.getConstraints() != null) {
            canonical.getConstraints().stream()
                .map(this::convert)
                .forEach(builder::addConstraint);
        }
        builder.setLowerBound(canonical.getLowerBound());
        builder.setUpperBound(canonical.getUpperBound());
        builder.setValidSourceTypes(canonical.getValidSourceTypes());
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTConstraint convert(TConstraint canonical) {
        XTConstraint.Builder<?> constraint = new XTConstraint.Builder<>();
        constraint.setAny(canonical.getAny());
        constraint.setConstraintType(canonical.getConstraintType());
        return constraint.build();
    }

    private XTCapability convert(TCapability canonical) {
        XTCapability.Builder builder = new XTCapability.Builder(canonical.getId(), canonical.getType(), canonical.getName());
        fillRelationshipSourceOrTargetProperties(builder, canonical);
        return builder.build();
    }

    private <Builder extends XRelationshipSourceOrTarget.Builder<Builder>, Value extends RelationshipSourceOrTarget>
    void fillRelationshipSourceOrTargetProperties(Builder builder, Value canonical) {
        // no specific properties to fill, just traverse the hierarchy
        fillEntityTemplateProperties(builder, canonical);
    }

    private <Builder extends XTEntityTemplate.Builder<Builder>, Value extends TEntityTemplate>
    void fillEntityTemplateProperties(Builder builder, Value canonical) {
        if (canonical.getProperties() != null) {
            builder.setProperties(convertProperties(canonical.getProperties()));
        }
        if (canonical.getPropertyConstraints() != null) {
            builder.setPropertyConstraints(
                canonical.getPropertyConstraints().stream()
                    .map(this::convert).collect(Collectors.toList())
            );
        }
        fillExtensibleElementsProperties(builder, canonical);
    }

    private XTEntityTemplate.Properties convertProperties(TEntityTemplate.Properties canonical) {
        XTEntityTemplate.Properties props = new XTEntityTemplate.Properties();
        if (canonical instanceof TEntityTemplate.XmlProperties) {
            props.setAny(((TEntityTemplate.XmlProperties) canonical).getAny());
        } else if (canonical instanceof TEntityTemplate.WineryKVProperties) {
            final TEntityTemplate.WineryKVProperties wineryKV = (TEntityTemplate.WineryKVProperties) canonical;
            props.setKVProperties(wineryKV.getNamespace(), wineryKV.getElementName(), wineryKV.getKVProperties());
        } else if (canonical instanceof TEntityTemplate.YamlProperties) {
            // this is the messy case of converting from YAML to XML
            LOGGER.warn("KVProperties with complex entries are not supported for XML serialization");
        }
        return props;
    }

    private XTPropertyConstraint convert(TPropertyConstraint canonical) {
        XTPropertyConstraint.Builder constraint = new XTPropertyConstraint.Builder();
        constraint.setAny(canonical.getAny());
        constraint.setConstraintType(canonical.getConstraintType());
        constraint.setProperty(canonical.getProperty());
        return constraint.build();
    }

    private XTExtension convert(TExtension canonical) {
        XTExtension.Builder builder = new XTExtension.Builder(canonical.getNamespace());
        builder.setMustUnderstand(canonical.getMustUnderstand() ? XTBoolean.YES : XTBoolean.NO);
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTDocumentation convert(TDocumentation canonical) {
        XTDocumentation.Builder xml = new XTDocumentation.Builder();
        xml.setContent(canonical.getContent());
        xml.setSource(canonical.getSource());
        xml.setLang(canonical.getLang());
        return xml.build();
    }

    private XTCapabilityType convert(TCapabilityType canonical) {
        XTCapabilityType.Builder builder = new XTCapabilityType.Builder(canonical.getName());
        // FIXME validSourceTypes are apparently a YAML feature (again)
        builder.setValidSourceTypes(canonical.getValidNodeTypes());
        fillEntityTypeProperties(builder, canonical);
        return builder.build();
    }

    private XTArtifactType convert(TArtifactType canonical) {
        XTArtifactType.Builder builder = new XTArtifactType.Builder(canonical.getName());
        builder.setMimeType(canonical.getMimeType());
        builder.setFileExtensions(canonical.getFileExtensions());
        fillEntityTypeProperties(builder, canonical);
        return builder.build();
    }

    private XTArtifactTemplate convert(TArtifactTemplate canonical) {
        XTArtifactTemplate.Builder builder = new XTArtifactTemplate.Builder(canonical.getId(), canonical.getType());
        builder.setName(canonical.getName());
        if (canonical.getArtifactReferences() != null) {
            canonical.getArtifactReferences().stream()
                .map(this::convert)
                .forEach(builder::addArtifactReference);
        }
        fillEntityTemplateProperties(builder, canonical);
        return builder.build();
    }

    private XTArtifactReference convert(TArtifactReference canonical) {
        XTArtifactReference.Builder xml = new XTArtifactReference.Builder(canonical.getReference());
        xml.setIncludeOrExclude(convertIncludeOrExclude(canonical.getIncludeOrExclude()));
        return xml.build();
    }

    private List<XTArtifactReference.IncludeOrExclude> convertIncludeOrExclude(List<TArtifactReference.IncludeOrExclude> includeOrExclude) {
        List<XTArtifactReference.IncludeOrExclude> results = new ArrayList<>();
        for (TArtifactReference.IncludeOrExclude item : includeOrExclude) {
            if (item instanceof TArtifactReference.Include) {
                results.add(
                    new XTArtifactReference.Include(
                        item.getPattern()
                    )
                );
            }
            if (item instanceof TArtifactReference.Exclude) {
                results.add(
                    new XTArtifactReference.Exclude(
                        item.getPattern()
                    )
                );
            }
        }
        return results;
    }

    private XTImport convert(TImport canonical) {
        XTImport.Builder builder = new XTImport.Builder(canonical.getImportType());
        builder.setNamespace(canonical.getNamespace());
        builder.setLocation(canonical.getLocation());
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTServiceTemplate convert(TServiceTemplate canonical) {
        XTServiceTemplate.Builder builder = new XTServiceTemplate.Builder(canonical.getId(), convert(canonical.getTopologyTemplate()));
        builder.setName(canonical.getName());
        builder.setTargetNamespace(canonical.getTargetNamespace());
        if (canonical.getTags() != null) {
            builder.addTags(convertList(canonical.getTags(), this::convert));
        }
        builder.setBoundaryDefinitions(convert(canonical.getBoundaryDefinitions()));
        if (canonical.getPlans() != null) {
            builder.setPlans(
                canonical.getPlans().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        builder.setSubstitutableNodeType(canonical.getSubstitutableNodeType());
        fillExtensibleElementsProperties(builder, canonical);

        // add defined groups as tags
        if (canonical.getTopologyTemplate() != null && canonical.getTopologyTemplate().getGroups() != null) {
            builder.addTags(convertList(canonical.getTopologyTemplate().getGroups(), this::convert));
        }

        // handle participant extension
        if (canonical.getTopologyTemplate() != null && canonical.getTopologyTemplate().getParticipants() != null) {

            builder.addTags(convertList(canonical.getTopologyTemplate().getParticipants(), this::convert));
        }

        return builder.build();
    }

    @Nullable
    private XTTag convert(@Nullable TGroupDefinition group) {
        if (group == null) {
            return null;
        }
        String name = "group:" + group.getName();
        String value = group.getDescription() == null ? "" : group.getDescription();
        return new XTTag.Builder(name, value).build();
    }

    @Nullable
    private XTTag convert(@Nullable OTParticipant participant) {
        if (participant == null) {
            return null;
        }
        String name = "participant:" + participant.getName();
        String value = participant.getUrl() == null ? "" : participant.getUrl();
        return new XTTag.Builder(name, value).build();
    }

    private XTPlan convert(TPlan canonical) {
        XTPlan.Builder builder = new XTPlan.Builder(canonical.getId(), canonical.getPlanType(), canonical.getPlanLanguage());
        if (canonical.getPrecondition() != null) {
            builder.setPrecondition(convert(canonical.getPrecondition()));
        }
        if (canonical.getInputParameters() != null) {
            builder.setInputParameters(
                canonical.getInputParameters().stream()
                    .map(this::convert
                    ).collect(Collectors.toList())
            );
        }
        if (canonical.getOutputParameters() != null) {
            builder.setOutputParameters(
                canonical.getOutputParameters().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        if (canonical.getPlanModel() != null) {
            XTPlan.PlanModel model = new XTPlan.PlanModel();
            model.setAny(canonical.getAny());
            builder.setPlanModel(model);
        }
        if (canonical.getPlanModelReference() != null) {
            XTPlan.PlanModelReference ref = new XTPlan.PlanModelReference();
            ref.setReference(canonical.getPlanModelReference().getReference());
            builder.setPlanModelReference(ref);
        }
        builder.setName(canonical.getName());
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTCondition convert(TCondition canonical) {
        return new XTCondition.Builder()
            .setExpressionLanguage(canonical.getExpressionLanguage())
            .setAny(canonical.getAny())
            .build();
    }

    @Nullable
    private XTBoundaryDefinitions convert(@Nullable TBoundaryDefinitions canonical) {
        if (canonical == null) {
            return null;
        }
        XTBoundaryDefinitions.Builder builder = new XTBoundaryDefinitions.Builder();
        if (canonical.getProperties() != null) {
            XTBoundaryDefinitions.Properties props = new XTBoundaryDefinitions.Properties();
            props.setAny(canonical.getProperties().getAny());
            if (canonical.getProperties().getPropertyMappings() != null) {
                props.setPropertyMappings(
                    canonical.getProperties().getPropertyMappings().stream()
                        .map(this::convert)
                        .collect(Collectors.toList())
                );
            }
            builder.setProperties(props);
        }
        if (canonical.getRequirements() != null) {
            builder.setRequirements(
                canonical.getRequirements().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        if (canonical.getCapabilities() != null) {
            builder.setCapabilities(
                canonical.getCapabilities().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        if (canonical.getPolicies() != null) {
            builder.setPolicies(
                canonical.getPolicies().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        if (canonical.getInterfaces() != null) {
            builder.setInterfaces(
                canonical.getInterfaces().stream()
                    .map(this::convert)
                    .collect(Collectors.toList())
            );
        }
        if (canonical.getPropertyConstraints() != null) {
            builder.setPropertyConstraints(
                convertList(canonical.getPropertyConstraints(), this::convert)
            );
        }
        return builder.build();
    }

    private XTExportedInterface convert(TExportedInterface canonical) {
        return new XTExportedInterface.Builder(
            canonical.getName(),
            canonical.getOperation().stream()
                .map(this::convert)
                .collect(Collectors.toList())
        ).build();
    }

    private XTExportedOperation convert(TExportedOperation canonical) {
        XTExportedOperation.Builder xml = new XTExportedOperation.Builder();
        xml.setName(canonical.getName());
        if (canonical.getNodeOperation() != null) {
            xml.setNodeOperation(convert(canonical.getNodeOperation()));
        }
        if (canonical.getRelationshipOperation() != null) {
            xml.setRelationshipOperation(convert(canonical.getRelationshipOperation()));
        }
        if (canonical.getPlan() != null) {
            XTExportedOperation.Plan plan = new XTExportedOperation.Plan();
            if (canonical.getPlan().getPlanRef() instanceof String) {
                plan.setPlanRef(canonical.getPlan().getPlanRef());
            } else {
                plan.setPlanRef(convert((TPlan) canonical.getPlan().getPlanRef()));
            }
            xml.setPlan(plan);
        }
        return xml.build();
    }

    private XTExportedOperation.RelationshipOperation convert(TExportedOperation.RelationshipOperation canonical) {
        XTExportedOperation.RelationshipOperation xml = new XTExportedOperation.RelationshipOperation();
        if (canonical.getRelationshipRef() instanceof String) {
            xml.setRelationshipRef(canonical.getRelationshipRef());
        } else {
            xml.setRelationshipRef(convert((TRelationshipTemplate) canonical.getRelationshipRef()));
        }
        xml.setInterfaceName(canonical.getInterfaceName());
        xml.setOperationName(canonical.getOperationName());
        return xml;
    }

    private XTExportedOperation.NodeOperation convert(TExportedOperation.NodeOperation canonical) {
        XTExportedOperation.NodeOperation xml = new XTExportedOperation.NodeOperation();
        if (canonical.getNodeRef() instanceof String) {
            xml.setNodeRef(canonical.getNodeRef());
        } else {
            xml.setNodeRef(convert((TNodeTemplate) canonical.getNodeRef()));
        }
        xml.setInterfaceName(canonical.getInterfaceName());
        xml.setOperationName(canonical.getOperationName());
        return xml;
    }

    private XTPolicy convert(TPolicy canonical) {
        XTPolicy.Builder builder = new XTPolicy.Builder(canonical.getPolicyType());
        builder.setName(canonical.getName());
        builder.setPolicyRef(canonical.getPolicyRef());
        builder.setTargets(canonical.getTargets());
        if (canonical.getProperties() != null) {
            builder.setProperties(convertProperties(canonical.getProperties()));
        }
        return builder.build();
    }

    private XTCapabilityRef convert(TCapabilityRef canonical) {
        XTCapabilityRef.Builder xml = new XTCapabilityRef.Builder();
        xml.setName(canonical.getName());
        xml.setRef(convert(canonical.getRef()));
        return xml.build();
    }

    private XTRequirementRef convert(TRequirementRef canonical) {
        XTRequirementRef.Builder builder = new XTRequirementRef.Builder();
        builder.setName(canonical.getName());
        builder.setRef(convert(canonical.getRef()));
        return builder.build();
    }

    private XTRequirement convert(TRequirement canonical) {
        XTRequirement.Builder builder = new XTRequirement.Builder(canonical.getId(), canonical.getName(), canonical.getType());
        builder.setCapability(canonical.getCapability());
        builder.setRelationship(canonical.getRelationship());
        builder.setNode(canonical.getNode());
        fillRelationshipSourceOrTargetProperties(builder, canonical);
        return builder.build();
    }

    private XTPropertyMapping convert(TPropertyMapping canonical) {
        return new XTPropertyMapping.Builder(
            canonical.getServiceTemplatePropertyRef(),
            convert(canonical.getTargetObjectRef()),
            canonical.getTargetPropertyRef()
        ).build();
    }

    @Nullable
    private XTTopologyTemplate convert(@Nullable TTopologyTemplate canonical) {
        if (canonical == null) {
            return null;
        }
        XTTopologyTemplate.Builder builder = new XTTopologyTemplate.Builder();
        builder.setNodeTemplates(convertList(canonical.getNodeTemplates(), this::convert));
        builder.setRelationshipTemplates(convertList(canonical.getRelationshipTemplates(), this::convert));
        // policies, inputs and outputs from canonical are YAML-only
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private XTNodeTemplate convert(TNodeTemplate canonical) {
        XTNodeTemplate.Builder builder = new XTNodeTemplate.Builder(canonical.getId(), canonical.getType());
        if (listIsNotNullOrEmpty(canonical.getRequirements())) {
            builder.setRequirements(convertList(canonical.getRequirements(), this::convert));
        }
        if (canonical.getCapabilities() != null) {
            builder.setCapabilities(convertList(canonical.getCapabilities(), this::convert));
        }
        if (canonical.getPolicies() != null) {
            builder.addPolicies(
                convertList(canonical.getPolicies(), this::convert)
            );
        }
        if (canonical.getDeploymentArtifacts() != null) {
            builder.setDeploymentArtifacts(
                convertList(canonical.getDeploymentArtifacts(), this::convert)
            );
        }
        builder.setName(canonical.getName());
        builder.setMinInstances(canonical.getMinInstances());
        builder.setMaxInstances(canonical.getMaxInstances());
        builder.setX(canonical.getX());
        builder.setY(canonical.getY());
        fillRelationshipSourceOrTargetProperties(builder, canonical);
        return builder.build();
    }

    private XTRelationshipTemplate convert(TRelationshipTemplate canonical) {
        XTRelationshipTemplate.Builder builder = new XTRelationshipTemplate.Builder(canonical.getId(), canonical.getType(),
            convert(canonical.getSourceElement()), convert(canonical.getTargetElement()));
        builder.setName(canonical.getName());
        if (canonical.getRelationshipConstraints() != null) {
            XTRelationshipTemplate.RelationshipConstraints constraints = new XTRelationshipTemplate.RelationshipConstraints();
            constraints.getRelationshipConstraint().addAll(canonical.getRelationshipConstraints().getRelationshipConstraint().stream()
                .map(this::convert).collect(Collectors.toList()));
            builder.setRelationshipConstraints(constraints);
        }
        if (canonical.getPolicies() != null) {
            builder.addPolicies(
                convertList(canonical.getPolicies(), this::convert)
            );
        }
        fillEntityTemplateProperties(builder, canonical);
        return builder.build();
    }

    private XTRelationshipTemplate.RelationshipConstraints.RelationshipConstraint convert(TRelationshipTemplate.RelationshipConstraints.RelationshipConstraint canonical) {
        XTRelationshipTemplate.RelationshipConstraints.RelationshipConstraint xml = new XTRelationshipTemplate.RelationshipConstraints.RelationshipConstraint();
        xml.setAny(canonical.getAny());
        xml.setConstraintType(canonical.getConstraintType());
        return xml;
    }

    private XTRelationshipTemplate.SourceOrTargetElement convert(TRelationshipTemplate.SourceOrTargetElement canonical) {
        XTRelationshipTemplate.SourceOrTargetElement xml = new XTRelationshipTemplate.SourceOrTargetElement();
        xml.setRef(convert(canonical.getRef()));
        return xml;
    }

    @Nullable
    private XTEntityTemplate convertEntityTemplate(TEntityTemplate canonical) {
        if (canonical instanceof RelationshipSourceOrTarget) {
            return convert((RelationshipSourceOrTarget) canonical);
        }
        if (canonical instanceof TArtifact) {
            return convert((TArtifact) canonical);
        }
        if (canonical instanceof TArtifactTemplate) {
            return convert((TArtifactTemplate) canonical);
        }
        if (canonical instanceof TPolicyTemplate) {
            return convert((TPolicyTemplate) canonical);
        }
        if (canonical instanceof TRelationshipTemplate) {
            return convert((TRelationshipTemplate) canonical);
        }
        LOGGER.warn("Trying to convert unknown subtype of TEntityTemplate to xml {}", canonical.getClass());
        return null;
    }

    private XRelationshipSourceOrTarget convert(RelationshipSourceOrTarget canonical) {
        // Capability or NodeTemplate or Requirement
        if (canonical instanceof TCapability) {
            return convert((TCapability) canonical);
        }
        if (canonical instanceof TNodeTemplate) {
            return convert((TNodeTemplate) canonical);
        }
        if (canonical instanceof TRequirement) {
            return convert((TRequirement) canonical);
        }
        throw new IllegalStateException(String.format("Tried to convert unknown RelationshipSourceOrTarget implementation %s", canonical.getClass().getName()));
    }

    private List<XTExtension> convertExtensions(List<TExtension> canonical) {
        if (canonical == null) {
            return null;
        }
        return canonical.stream()
            .map(this::convert)
            .collect(Collectors.toList());
    }

    private XTDefinitions.Types convertTypes(TDefinitions.@Nullable Types canonical) {
        if (canonical == null) {
            return new XTDefinitions.Types();
        }
        XTDefinitions.Types result = new XTDefinitions.Types();
        result.getAny().addAll(canonical.getAny());
        return result;
    }

    private XHasId convert(HasId canonical) {
        if (canonical instanceof TDefinitions) {
            // this case should never ever come true
            throw new IllegalStateException("Attempted to convert a TDefinitions instance through HasId overload.");
        }
        if (canonical instanceof RelationshipSourceOrTarget) {
            return convert((RelationshipSourceOrTarget) canonical);
        }
        if (canonical instanceof TArtifact) {
            return convert((TArtifact) canonical);
        }
        if (canonical instanceof TArtifactTemplate) {
            return convert((TArtifactTemplate) canonical);
        }
        if (canonical instanceof TPolicyTemplate) {
            return convert((TPolicyTemplate) canonical);
        }
        if (canonical instanceof TRelationshipTemplate) {
            return convert((TRelationshipTemplate) canonical);
        }
        throw new IllegalStateException("Attempted to convert unknown element deriving from HasId with type " + canonical.getClass().getName());
    }

    private <R, I> List<R> convertList(@Nullable List<I> canonical, Function<I, R> convert) {
        if (canonical == null) {
            return Collections.emptyList();
        }
        return canonical.stream()
            .map(convert)
            .collect(Collectors.toList());
    }

    private XOTComplianceRule convert(OTComplianceRule canonical) {
        XOTComplianceRule.Builder builder = new XOTComplianceRule.Builder(canonical.getId());
        builder.setIdentifier(convert(canonical.getIdentifier()));
        builder.setName(canonical.getName());
        builder.setRequiredStructure(convert(canonical.getRequiredStructure()));
        fillExtensibleElementsProperties(builder, canonical);
        return builder.build();
    }

    private <Builder extends XOTPrmMapping.Builder<Builder>, Value extends OTPrmMapping>
    void fillOTPrmMappingProperties(Builder builder, Value value) {
        builder.setDetectorElement(convertEntityTemplate(value.getDetectorElement()));
        builder.setRefinementElement(convertEntityTemplate(value.getRefinementElement()));
        fillExtensibleElementsProperties(builder, value);
    }

    private XOTAttributeMapping convert(OTAttributeMapping canonical) {
        XOTAttributeMapping.Builder builder = new XOTAttributeMapping.Builder(canonical.getId());
        builder.setType(XOTAttributeMappingType.fromValue(canonical.getType().value()));
        builder.setDetectorProperty(canonical.getDetectorProperty());
        builder.setRefinementProperty(canonical.getRefinementProperty());
        fillOTPrmMappingProperties(builder, canonical);
        return builder.build();
    }

    private XOTDeploymentArtifactMapping convert(OTDeploymentArtifactMapping canonical) {
        XOTDeploymentArtifactMapping.Builder builder = new XOTDeploymentArtifactMapping.Builder(canonical.getId());
        builder.setArtifactType(canonical.getArtifactType());
        builder.setTargetArtifactType(canonical.getTargetArtifactType());
        fillOTPrmMappingProperties(builder, canonical);
        return builder.build();
    }

    private <Builder extends XOTTopologyFragmentRefinementModel.Builder, Value extends OTTopologyFragmentRefinementModel> void
    fillOTTopologyFragmentRefinementModelProperties(Builder builder, Value value) {
        builder.setRefinementStructure(convert(value.getRefinementStructure()));
        builder.setDeploymentArtifactMappings(convertList(value.getDeploymentArtifactMappings(), this::convert));
        builder.setStayMappings(convertList(value.getStayMappings(), this::convert));
        builder.setPermutationMappings(convertList(value.getPermutationMappings(), this::convert));
        builder.setPermutationOptions(convertList(value.getPermutationOptions(), this::convert));
        builder.setAttributeMappings(convertList(value.getAttributeMappings(), this::convert));
        fillOTRefinementModelProperties(builder, value);
    }

    private <Builder extends XOTRefinementModel.Builder<Builder>, Value extends OTRefinementModel> void
    fillOTRefinementModelProperties(Builder builder, Value value) {
        builder.setName(value.getName());
        builder.setDetector(convert(value.getDetector()));
        builder.setTargetNamespace(value.getTargetNamespace());
        builder.setRelationMappings(convertList(value.getRelationMappings(), this::convert));
        fillExtensibleElementsProperties(builder, value);
    }

    private XOTPatternRefinementModel convert(OTPatternRefinementModel canonical) {
        XOTPatternRefinementModel.Builder builder = new XOTPatternRefinementModel.Builder();
        builder.setIsPdrm(canonical.isPdrm() ? XTBoolean.YES : XTBoolean.NO);
        builder.setBehaviorPatternMappings(convertList(canonical.getBehaviorPatternMappings(), this::convert));
        fillOTTopologyFragmentRefinementModelProperties(builder, canonical);
        return builder.build();
    }

    private XOTRelationMapping convert(OTRelationMapping canonical) {
        XOTRelationMapping.Builder builder = new XOTRelationMapping.Builder(canonical.getId());
        builder.setDirection(XOTRelationDirection.fromValue(canonical.getDirection().value()));
        builder.setRelationType(canonical.getRelationType());
        builder.setValidSourceOrTarget(canonical.getValidSourceOrTarget());
        fillOTPrmMappingProperties(builder, canonical);
        return builder.build();
    }

    private XOTStayMapping convert(OTStayMapping canonical) {
        XOTStayMapping.Builder builder = new XOTStayMapping.Builder(canonical.getId());
        fillOTPrmMappingProperties(builder, canonical);
        return builder.build();
    }

    private XOTPermutationMapping convert(OTPermutationMapping canonical) {
        XOTPermutationMapping.Builder builder = new XOTPermutationMapping.Builder(canonical.getId());
        fillOTPrmMappingProperties(builder, canonical);
        return builder.build();
    }

    private XOTStringList convert(OTStringList xml) {
        XOTStringList.Builder builder = new XOTStringList.Builder(xml.getValues());
        return builder.build();
    }

    private XOTTestRefinementModel convert(OTTestRefinementModel canonical) {
        XOTTestRefinementModel.Builder builder = new XOTTestRefinementModel.Builder();
        builder.setTestFragment(convert(canonical.getTestFragment()));
        fillOTRefinementModelProperties(builder, canonical);
        return builder.build();
    }

    private XOTTopologyFragmentRefinementModel convert(OTTopologyFragmentRefinementModel canonical) {
        XOTTopologyFragmentRefinementModel.Builder builder = new XOTTopologyFragmentRefinementModel.Builder();
        fillOTTopologyFragmentRefinementModelProperties(builder, canonical);
        return builder.build();
    }

    private XOTBehaviorPatternMapping convert(OTBehaviorPatternMapping canonical) {
        XOTBehaviorPatternMapping.Builder builder = new XOTBehaviorPatternMapping.Builder(canonical.getId());
        builder.setBehaviorPattern(canonical.getBehaviorPattern());
        builder.setProperty(convert(canonical.getProperty()));
        fillOTPrmMappingProperties(builder, canonical);
        return builder.build();
    }

    private XOTPropertyKV convert(OTPropertyKV canonical) {
        return new XOTPropertyKV(canonical.getKey(), canonical.getValue());
    }
}
