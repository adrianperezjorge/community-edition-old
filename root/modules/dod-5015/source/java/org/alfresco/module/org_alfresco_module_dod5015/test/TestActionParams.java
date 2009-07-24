package org.alfresco.module.org_alfresco_module_dod5015.test;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.module.org_alfresco_module_dod5015.action.RMActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.NodeRef;

public class TestActionParams extends RMActionExecuterAbstractBase
{
    public static final String NAME = "testActionParams";
    public static final String PARAM_DATE = "paramDate";
    
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
    {
        Object dateValue = action.getParameterValue(PARAM_DATE);
        if ((dateValue instanceof java.util.Date) == false)
        {
            throw new AlfrescoRuntimeException("Param we not a Date as expected.");
        }
    }      
}
