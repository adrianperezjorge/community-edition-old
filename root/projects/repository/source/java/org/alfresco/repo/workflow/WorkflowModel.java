/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.workflow;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;


/**
 * Workflow Model Constants
 */
public interface WorkflowModel
{
    
    //
    // Base Business Process Management Definitions
    //

    // package folder constants
    static final QName TYPE_PACKAGE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package");
    static final QName ASSOC_PACKAGE_CONTAINS= QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "packageContains");
    
    // task constants
    static final QName TYPE_TASK = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "task");
    static final QName PROP_TASK_ID = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "taskId");
    static final QName PROP_START_DATE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "startDate");
    static final QName PROP_DUE_DATE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "dueDate");
    static final QName PROP_COMPLETION_DATE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "completionDate");
    static final QName PROP_PRIORITY = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "priority");
    static final QName PROP_STATUS = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "status");
    static final QName PROP_PERCENT_COMPLETE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "percentComplete");
    static final QName PROP_COMPLETED_ITEMS = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "completedItems");
    static final QName PROP_COMMENT = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "comment");
    static final QName ASSOC_POOLED_ACTORS = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "pooledActors");

    // workflow task contstants
    static final QName TYPE_WORKFLOW_TASK = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowTask");
    static final QName PROP_CONTEXT = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "context");
    static final QName PROP_DESCRIPTION = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "description");
    static final QName PROP_OUTCOME = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "outcome");
    static final QName PROP_PACKAGE_ACTION_GROUP = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "packageActionGroup");
    static final QName PROP_PACKAGE_ITEM_ACTION_GROUP = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "packageItemActionGroup");
    static final QName PROP_HIDDEN_TRANSITIONS = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "hiddenTransitions");
    static final QName ASSOC_PACKAGE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package");

    // workflow task contstants
    static final QName TYPE_START_TASK = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "startTask");
    static final QName PROP_WORKFLOW_DESCRIPTION = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowDescription");
    static final QName PROP_WORKFLOW_PRIORITY = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowPriority");
    static final QName PROP_WORKFLOW_DUE_DATE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowDueDate");
    static final QName ASSOC_ASSIGNEE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "assignee");

    // workflow package
    static final QName ASPECT_WORKFLOW_PACKAGE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowPackage");
    static final QName PROP_IS_SYSTEM_PACKAGE = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "isSystemPackage");
    static final QName PROP_WORKFLOW_DEFINITION_ID = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowDefinitionId");
    static final QName PROP_WORKFLOW_DEFINITION_NAME = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowDefinitionName");
    static final QName PROP_WORKFLOW_INSTANCE_ID = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowInstanceId");
     
    // workflow definition
    static final QName TYPE_WORKFLOW_DEF = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "workflowDefinition");
    static final QName PROP_WORKFLOW_DEF_ENGINE_ID = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "engineId");
    static final QName PROP_WORKFLOW_DEF_NAME = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "definitionName");
    static final QName PROP_WORKFLOW_DEF_DEPLOYED = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "definitionDeployed");

}