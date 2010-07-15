/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UniqueIdVendor;
import javax.faces.context.FacesContext;
import javax.faces.view.AttachedObjectHandler;

import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;

/**
 * @since 2.0.1
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 899026 $ $Date: 2010-01-13 20:47:14 -0500 (Mié, 13 Ene 2010) $
 */
public class FaceletCompositionContextImpl extends FaceletCompositionContext
{
    private FacesContext _facesContext;
    
    private FaceletFactory _factory;

    private LinkedList<UIComponent> _compositeComponentStack;
    
    private LinkedList<UniqueIdVendor> _uniqueIdVendorStack;
    
    private LinkedList<String> _validationGroupsStack; 
    
    private LinkedList<String> _excludedValidatorIdsStack;
    
    private LinkedList<String> _enclosingValidatorIdsStack;
    
    private Boolean _isRefreshingTransientBuild;
    
    private Boolean _isMarkInitialState;
    
    private Boolean _refreshTransientBuildOnPSS;
    
    private Boolean _usingPSSOnThisView;
    
    private final Map<UIComponent, List<AttachedObjectHandler>> _attachedObjectHandlers;
    
    public FaceletCompositionContextImpl(FaceletFactory factory, FacesContext facesContext)
    {
        super();
        _factory = factory;
        _facesContext = facesContext;
        _attachedObjectHandlers = new HashMap<UIComponent, List<AttachedObjectHandler>>();
    }

    public FaceletFactory getFaceletFactory()
    {
        return _factory;
    }
    
    @Override
    public void release(FacesContext facesContext)
    {
        super.release(facesContext);
        _factory = null;
        _facesContext = null;
        _compositeComponentStack = null;
        _enclosingValidatorIdsStack = null;
        _excludedValidatorIdsStack = null;
        _uniqueIdVendorStack = null;
        _validationGroupsStack = null;
    }
   
    @Override
    public UIComponent getCompositeComponentFromStack()
    {
        if (_compositeComponentStack != null && !_compositeComponentStack.isEmpty())
        {
            return _compositeComponentStack.peek();
        }
        return null;
    }

    @Override
    public void pushCompositeComponentToStack(UIComponent parent)
    {
        if (_compositeComponentStack == null)
        {
            _compositeComponentStack = new LinkedList<UIComponent>();
        }
        _compositeComponentStack.addFirst(parent);
    }

    @Override
    public void popCompositeComponentToStack()
    {
        if (_compositeComponentStack != null && !_compositeComponentStack.isEmpty())
        {
            _compositeComponentStack.removeFirst();
        }
    }

    @Override
    public UniqueIdVendor getUniqueIdVendorFromStack()
    {
        if (_uniqueIdVendorStack != null && !_uniqueIdVendorStack.isEmpty())
        {
            return _uniqueIdVendorStack.peek();
        }
        return null;
    }

    @Override
    public void popUniqueIdVendorToStack()
    {
        if (_uniqueIdVendorStack != null && !_uniqueIdVendorStack.isEmpty())
        {
            _uniqueIdVendorStack.removeFirst();
        }
    }

    @Override
    public void pushUniqueIdVendorToStack(UniqueIdVendor parent)
    {
        if (_uniqueIdVendorStack == null)
        {
            _uniqueIdVendorStack = new LinkedList<UniqueIdVendor>();
        }
        _uniqueIdVendorStack.addFirst(parent);
    }
    
    /**
     * Gets the top of the validationGroups stack.
     * @return
     * @since 2.0
     */
    @Override
    public String getFirstValidationGroupFromStack()
    {
        if (_validationGroupsStack != null && !_validationGroupsStack.isEmpty())
        {
            return _validationGroupsStack.getFirst(); // top-of-stack
        }
        return null;
    }
    
    /**
     * Removes top of stack.
     * @since 2.0
     */
    @Override
    public void popValidationGroupsToStack()
    {
        if (_validationGroupsStack != null && !_validationGroupsStack.isEmpty())
        {
            _validationGroupsStack.removeFirst();
        }
    }
    
    /**
     * Pushes validationGroups to the stack.
     * @param validationGroups
     * @since 2.0
     */
    @Override
    public void pushValidationGroupsToStack(String validationGroups)
    {
        if (_validationGroupsStack == null)
        {
            _validationGroupsStack = new LinkedList<String>();
        }

        _validationGroupsStack.addFirst(validationGroups);
    }
    
    /**
     * Gets all validationIds on the stack.
     * @return
     * @since 2.0
     */
    @Override
    public Iterator<String> getExcludedValidatorIds()
    {
        if (_excludedValidatorIdsStack != null && !_excludedValidatorIdsStack.isEmpty())
        {
            return _excludedValidatorIdsStack.iterator();
        }
        return null;
    }
    
    /**
     * Removes top of stack.
     * @since 2.0
     */
    @Override
    public void popExcludedValidatorIdToStack()
    {
        if (_excludedValidatorIdsStack != null && !_excludedValidatorIdsStack.isEmpty())
        {
            _excludedValidatorIdsStack.removeFirst();
        }
    }
    
    /**
     * Pushes validatorId to the stack of excluded validatorIds.
     * @param validatorId
     * @since 2.0
     */
    @Override
    public void pushExcludedValidatorIdToStack(String validatorId)
    {
        if (_excludedValidatorIdsStack == null)
        {
            _excludedValidatorIdsStack = new LinkedList<String>();
        }

        _excludedValidatorIdsStack.addFirst(validatorId);
    }
    
    /**
     * Gets all validationIds on the stack.
     * @return
     * @since 2.0
     */
    @Override
    public Iterator<String> getEnclosingValidatorIds()
    {
        if (_enclosingValidatorIdsStack != null && !_enclosingValidatorIdsStack.isEmpty())
        {
            return _enclosingValidatorIdsStack.iterator(); 
        }
        return null;
    }
    
    /**
     * Removes top of stack.
     * @since 2.0
     */
    @Override
    public void popEnclosingValidatorIdToStack()
    {
        if (_enclosingValidatorIdsStack != null && !_enclosingValidatorIdsStack.isEmpty())
        {
            _enclosingValidatorIdsStack.removeFirst();
        }
    }
    
    /**
     * Pushes validatorId to the stack of all enclosing validatorIds.
     * @param validatorId
     * @since 2.0
     */
    @Override
    public void pushEnclosingValidatorIdToStack(String validatorId)
    {
        if (_enclosingValidatorIdsStack == null)
        {
            _enclosingValidatorIdsStack = new LinkedList<String>();
        }

        _enclosingValidatorIdsStack.addFirst(validatorId);
    }

    @Override
    public boolean isRefreshingTransientBuild()
    {
        if (_isRefreshingTransientBuild == null)
        {
            _isRefreshingTransientBuild = FaceletViewDeclarationLanguage.
                isRefreshingTransientBuild(_facesContext);
        }
        return _isRefreshingTransientBuild;
    }

    @Override
    public boolean isMarkInitialState()
    {
        if (_isMarkInitialState == null)
        {
            _isMarkInitialState = FaceletViewDeclarationLanguage.
                isMarkInitialState(_facesContext);
        }
        return _isMarkInitialState;
    }

    @Override
    public boolean isRefreshTransientBuildOnPSS()
    {
        if (_refreshTransientBuildOnPSS == null)
        {
            _refreshTransientBuildOnPSS = FaceletViewDeclarationLanguage.
                isRefreshTransientBuildOnPSS(_facesContext);
        }
        return _refreshTransientBuildOnPSS;
    }

    @Override
    public boolean isUsingPSSOnThisView()
    {
        if (_usingPSSOnThisView == null)
        {
            _usingPSSOnThisView = FaceletViewDeclarationLanguage.
                isUsingPSSOnThisView(_facesContext);
        }
        return _usingPSSOnThisView;
    }
    
    public boolean isMarkInitialStateAndIsRefreshTransientBuildOnPSS()
    {
        return isMarkInitialState() && isRefreshTransientBuildOnPSS();
    }

    @Override
    public void addAttachedObjectHandler(UIComponent compositeComponentParent, AttachedObjectHandler handler)
    {
        List<AttachedObjectHandler> list = _attachedObjectHandlers.get(compositeComponentParent);

        if (list == null)
        {
            list = new ArrayList<AttachedObjectHandler>();
            _attachedObjectHandlers.put(compositeComponentParent, list);
        }

        list.add(handler);
    }

    @Override
    public void removeAttachedObjectHandlers(UIComponent compositeComponentParent)
    {
        _attachedObjectHandlers.remove(compositeComponentParent);
    }

    @Override
    public List<AttachedObjectHandler> getAttachedObjectHandlers(UIComponent compositeComponentParent)
    {
        return _attachedObjectHandlers.get(compositeComponentParent);
    }
}
