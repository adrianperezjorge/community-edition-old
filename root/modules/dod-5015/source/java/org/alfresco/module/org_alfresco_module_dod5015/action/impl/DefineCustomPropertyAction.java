/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.org_alfresco_module_dod5015.action.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_dod5015.CustomModelUtil;
import org.alfresco.module.org_alfresco_module_dod5015.CustomisableRmElement;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.dictionary.M2Aspect;
import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.repo.dictionary.M2Property;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Neil McErlean
 */
public class DefineCustomPropertyAction extends DefineCustomElementAbstractAction
{
    private static Log logger = LogFactory.getLog(DefineCustomPropertyAction.class);

    public static final String PARAM_ELEMENT = "element";
    
    //TODO Some of these parameters are unnecessary and could be deleted.
    public static final String PARAM_DATATYPE = "dataType";
    public static final String PARAM_TITLE = "title";
    public static final String PARAM_DESCRIPTION = "description";
    public static final String PARAM_DEFAULT_VALUE = "defaultValue";
    public static final String PARAM_MULTI_VALUED = "multiValued";
    public static final String PARAM_MANDATORY = "mandatory";
    public static final String PARAM_PROTECTED = "protected";

    /**
	 * 
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action,
	 *      org.alfresco.service.cmr.repository.NodeRef)
	 */
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
	{
        Map<String, Serializable> params = action.getParameterValues();

        CustomModelUtil customModelUtil = new CustomModelUtil();
        customModelUtil.setContentService(contentService);

        M2Model deserializedModel = customModelUtil.readCustomContentModel();

        // Need to select the correct aspect in the customModel to which we'll add the property.
        String customisableElement = (String)params.get(PARAM_ELEMENT);
        CustomisableRmElement ce = CustomisableRmElement.getEnumFor(customisableElement);
        String aspectName = ce.getCorrespondingAspect();

        if (logger.isDebugEnabled())
        {
            StringBuilder msg = new StringBuilder();
            msg.append("Creating [")
                .append(ce.toString())
                .append("] custom property:");
            for (String n : params.keySet())
            {
                msg.append(n).append(" = ");
                msg.append(params.get(n));
                msg.append(LINE_SEPARATOR);
            }
            logger.debug(msg.toString());
        }

        M2Aspect customPropsAspect = deserializedModel.getAspect(aspectName);

        String qname = (String)params.get(PARAM_NAME);
		QName propQName = QName.createQName(qname, namespaceService);
        String propQNameAsString = propQName.toPrefixString(namespaceService);

        //TODO Handle a post where the prop already exists - replace.

        M2Property newProp = customPropsAspect.createProperty(propQNameAsString);
        newProp.setName(qname);

        //TODO According to the wireframes, type here can only be date|text|number
        Serializable serializableType = params.get(PARAM_DATATYPE);
        QName type = null;
        if (serializableType instanceof String)
        {
            type = QName.createQName((String)serializableType);
        }
        else
        {
            type = (QName)serializableType;
        }

        newProp.setType(type.toPrefixString(namespaceService));
        newProp.setTitle((String)params.get(PARAM_TITLE));
        newProp.setDescription((String)params.get(PARAM_DESCRIPTION));
        newProp.setDefaultValue((String)params.get(PARAM_DEFAULT_VALUE));

        // 'mandatory' should be an available parameter.
        Serializable serializableParam = params.get(PARAM_MANDATORY);
        if (serializableParam != null)
        {
            Boolean bool = Boolean.valueOf(serializableParam.toString());
            newProp.setMandatory(bool);
        }
        serializableParam = params.get(PARAM_PROTECTED);
        if (serializableParam != null)
        {
            Boolean bool = Boolean.valueOf(serializableParam.toString());
            newProp.setProtected(bool);
        }
        serializableParam = params.get(PARAM_MULTI_VALUED);
        if (serializableParam != null)
        {
            Boolean bool = Boolean.valueOf(serializableParam.toString());
            newProp.setMultiValued(bool);
        }

        customModelUtil.writeCustomContentModel(deserializedModel);
    }

    @Override
    protected boolean isExecutableImpl(NodeRef filePlanComponent, Map<String, Serializable> parameters, boolean throwException)
    {
        return true;
    }

    /**
	 * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
	 */
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList)
	{
        paramList.add(new ParameterDefinitionImpl(PARAM_NAME, DataTypeDefinition.TEXT, true, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_TITLE, DataTypeDefinition.TEXT, false, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_DESCRIPTION, DataTypeDefinition.TEXT, false, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_DEFAULT_VALUE, DataTypeDefinition.TEXT, false, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_DATATYPE, DataTypeDefinition.QNAME, true, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_MULTI_VALUED, DataTypeDefinition.BOOLEAN, false, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_MANDATORY, DataTypeDefinition.BOOLEAN, false, null));
        paramList.add(new ParameterDefinitionImpl(PARAM_PROTECTED, DataTypeDefinition.BOOLEAN, false, null));
 	}
	
	private static String LINE_SEPARATOR = System.getProperty("line.separator");
}
