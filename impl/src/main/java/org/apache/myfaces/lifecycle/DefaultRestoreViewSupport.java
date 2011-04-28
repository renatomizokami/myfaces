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
package org.apache.myfaces.lifecycle;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.render.ResponseStateManager;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared_impl.application.FacesServletMapping;
import org.apache.myfaces.shared_impl.application.InvalidViewIdException;
import org.apache.myfaces.shared_impl.renderkit.RendererUtils;
import org.apache.myfaces.shared_impl.util.Assert;

/**
 * @author Mathias Broekelmann (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class DefaultRestoreViewSupport implements RestoreViewSupport
{
    private static final String JAVAX_SERVLET_INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";

    private static final String JAVAX_SERVLET_INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
    
    /**
     * Constant defined on javax.portlet.faces.Bridge class that helps to 
     * define if the current request is a portlet request or not.
     */
    private static final String PORTLET_LIFECYCLE_PHASE = "javax.portlet.faces.phase";
    
    private static final String CACHED_SERVLET_MAPPING =
        DefaultRestoreViewSupport.class.getName() + ".CACHED_SERVLET_MAPPING";


    //private final Log log = LogFactory.getLog(DefaultRestoreViewSupport.class);
    private final Logger log = Logger.getLogger(DefaultRestoreViewSupport.class.getName());

    @JSFWebConfigParam(defaultValue = "500", since = "2.0.2")
    private static final String CHECKED_VIEWID_CACHE_SIZE_ATTRIBUTE = "org.apache.myfaces.CHECKED_VIEWID_CACHE_SIZE";
    private static final int CHECKED_VIEWID_CACHE_DEFAULT_SIZE = 500;

    @JSFWebConfigParam(defaultValue = "true", since = "2.0.2")
    private static final String CHECKED_VIEWID_CACHE_ENABLED_ATTRIBUTE = "org.apache.myfaces.CHECKED_VIEWID_CACHE_ENABLED";
    private static final boolean CHECKED_VIEWID_CACHE_ENABLED_DEFAULT = true;
    
    private static final String SKIP_ITERATION_HINT = "javax.faces.visit.SKIP_ITERATION";

    private Map<String, Boolean> _checkedViewIdMap = null;
    private Boolean _checkedViewIdCacheEnabled = null;

    public void processComponentBinding(FacesContext facesContext, UIComponent component)
    {
        // JSF 2.0: Old hack related to t:aliasBean was fixed defining a event that traverse
        // whole tree and let components to override UIComponent.processEvent() method to include it.
        
        // TODO: Remove this hack and use VisitHints.SKIP_ITERATION in JSF 2.1
        facesContext.getAttributes().put(SKIP_ITERATION_HINT, Boolean.TRUE);
        
        component.visitTree(VisitContext.createVisitContext(facesContext), new RestoreStateCallback());
        
        facesContext.getAttributes().remove(SKIP_ITERATION_HINT);
        
        /*
        ValueExpression binding = component.getValueExpression("binding");
        if (binding != null)
        {
            binding.setValue(facesContext.getELContext(), component);
        }

        // This part is for make compatibility with t:aliasBean, because
        // this components has its own code before and after binding is
        // set for child components.
        RestoreStateUtils.recursivelyHandleComponentReferencesAndSetValid(facesContext, component);

        // The required behavior for the spec is call recursively this method
        // for walk the component tree.
        // for (Iterator<UIComponent> iter = component.getFacetsAndChildren(); iter.hasNext();)
        // {
        // processComponentBinding(facesContext, iter.next());
        // }
         */
    }

    public String calculateViewId(FacesContext facesContext)
    {
        Assert.notNull(facesContext);
        ExternalContext externalContext = facesContext.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();

        String viewId = null;
        boolean traceEnabled = log.isLoggable(Level.FINEST);
        
        if (requestMap.containsKey(PORTLET_LIFECYCLE_PHASE))
        {
            viewId = (String) externalContext.getRequestPathInfo();
        }
        else
        {
            viewId = (String) requestMap.get(JAVAX_SERVLET_INCLUDE_PATH_INFO);
            if (viewId != null)
            {
                if (traceEnabled)
                {
                    log.finest("Calculated viewId '" + viewId + "' from request param '" + JAVAX_SERVLET_INCLUDE_PATH_INFO
                            + "'");
                }
            }
            else
            {
                viewId = externalContext.getRequestPathInfo();
                if (viewId != null && traceEnabled)
                {
                    log.finest("Calculated viewId '" + viewId + "' from request path info");
                }
            }
    
            if (viewId == null)
            {
                viewId = (String) requestMap.get(JAVAX_SERVLET_INCLUDE_SERVLET_PATH);
                if (viewId != null && traceEnabled)
                {
                    log.finest("Calculated viewId '" + viewId + "' from request param '"
                            + JAVAX_SERVLET_INCLUDE_SERVLET_PATH + "'");
                }
            }
        }
        
        if (viewId == null)
        {
            viewId = externalContext.getRequestServletPath();
            if (viewId != null && traceEnabled)
            {
                log.finest("Calculated viewId '" + viewId + "' from request servlet path");
            }
        }

        if (viewId == null)
        {
            throw new FacesException("Could not determine view id.");
        }

        return viewId;
    }

    public boolean isPostback(FacesContext facesContext)
    {
        ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
        String renderkitId = viewHandler.calculateRenderKitId(facesContext);
        ResponseStateManager rsm = RendererUtils.getResponseStateManager(facesContext, renderkitId);
        return rsm.isPostback(facesContext);
    }
        
    private static class RestoreStateCallback implements VisitCallback
    {
        private PostRestoreStateEvent event;

        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (event == null)
            {
                event = new PostRestoreStateEvent(target);
            }
            else
            {
                event.setComponent(target);
            }

            // call the processEvent method of the current component.
            // The argument event must be an instance of AfterRestoreStateEvent whose component
            // property is the current component in the traversal.
            target.processEvent(event);
            
            return VisitResult.ACCEPT;
        }
    }
    
    public String deriveViewId(FacesContext context, String viewId)
    {
        //If no viewId found, don't try to derive it, just continue.
        if (viewId == null)
        {
            return null;
        }
        FacesServletMapping mapping = getFacesServletMapping(context);
        if (mapping == null || mapping.isExtensionMapping())
        {
            viewId = handleSuffixMapping(context, viewId);
        }
        else if(mapping.isPrefixMapping())
        {
            viewId = handlePrefixMapping(viewId,mapping.getPrefix());
        }
        else if (viewId != null && mapping.getUrlPattern().startsWith(viewId))
        {
            throw new InvalidViewIdException(viewId);
        }

        //if(viewId != null)
        //{
        //    return (checkResourceExists(context,viewId) ? viewId : null);
        //}

        return viewId;    // return null if no physical resource exists
    }
    
    protected String[] getContextSuffix(FacesContext context)
    {
        String defaultSuffix = context.getExternalContext().getInitParameter(ViewHandler.DEFAULT_SUFFIX_PARAM_NAME);
        if (defaultSuffix == null)
        {
            defaultSuffix = ViewHandler.DEFAULT_SUFFIX;
        }
        return defaultSuffix.split(" ");
    }
    
    protected String getFaceletsContextSuffix(FacesContext context)
    {
        String defaultSuffix = context.getExternalContext().getInitParameter(ViewHandler.FACELETS_SUFFIX_PARAM_NAME);
        if (defaultSuffix == null)
        {
            defaultSuffix = ViewHandler.DEFAULT_FACELETS_SUFFIX;
        }
        return defaultSuffix;
    }
    
    
    
    protected String[] getFaceletsViewMappings(FacesContext context)
    {
        String faceletsViewMappings= context.getExternalContext().getInitParameter(ViewHandler.FACELETS_VIEW_MAPPINGS_PARAM_NAME);
        if(faceletsViewMappings == null)    //consider alias facelets.VIEWMAPPINGS
        {
            faceletsViewMappings= context.getExternalContext().getInitParameter("facelets.VIEWMAPPINGS");
        }
        
        return faceletsViewMappings == null ? null : faceletsViewMappings.split(";");
    }
    
    /**
     * Return the normalized viewId according to the algorithm specified in 7.5.2 
     * by stripping off any number of occurrences of the prefix mapping from the viewId.
     * <p/>
     * For example, both /faces/view.xhtml and /faces/faces/faces/view.xhtml would both return view.xhtml
     * F 
     */
    protected String handlePrefixMapping(String viewId, String prefix)
    {
        /*  If prefix mapping (such as "/faces/*") is used for FacesServlet, normalize the viewId according to the following
            algorithm, or its semantic equivalent, and return it.
               
            Remove any number of occurrences of the prefix mapping from the viewId. For example, if the incoming value
            was /faces/faces/faces/view.xhtml the result would be simply view.xhtml.
         */
        String uri = viewId;
        prefix = prefix + '/';  //need to make sure its really /faces/* and not /facesPage.xhtml
        while (uri.startsWith(prefix) || uri.startsWith("//")) 
        {
            if(uri.startsWith(prefix))
                    uri = uri.substring(prefix.length() - 1);    //cut off only /faces, leave the trailing '/' char for the next iteration
            else //uri starts with '//'
                uri = uri.substring(1); //cut off the leading slash, leaving the second slash to compare for the next iteration
        }
        //now delete any remaining leading '/'
        // TODO: CJH: I don't think this is correct, considering that getActionURL() expects everything to
        // start with '/', and in the suffix case we only mess with the suffix and leave leading
        // slashes alone.  Please review...
        /*if(uri.startsWith("/"))
        {
            uri = uri.substring(1);
        }*/
        
        return uri;
    }
    
    /**
     * Return the viewId with any non-standard suffix stripped off and replaced with
     * the default suffix configured for the specified context.
     * <p/>
     * For example, an input parameter of "/foo.jsf" may return "/foo.jsp".
     */
    protected String handleSuffixMapping(FacesContext context, String requestViewId)
    {
        String[] faceletsViewMappings = getFaceletsViewMappings(context);
        String[] jspDefaultSuffixes = getContextSuffix(context);
        
        int slashPos = requestViewId.lastIndexOf('/');
        int extensionPos = requestViewId.lastIndexOf('.');
        
        //Try to locate any resource that match with the expected id
        for (String defaultSuffix : jspDefaultSuffixes)
        {
            StringBuilder builder = new StringBuilder(requestViewId);
           
            if (extensionPos > -1 && extensionPos > slashPos)
            {
                builder.replace(extensionPos, requestViewId.length(), defaultSuffix);
            }
            else
            {
                builder.append(defaultSuffix);
            }
            String candidateViewId = builder.toString();
            
            if( faceletsViewMappings != null && faceletsViewMappings.length > 0 )
            {
                for (String mapping : faceletsViewMappings)
                {
                    if(mapping.startsWith("/"))
                    {
                        continue;   //skip this entry, its a prefix mapping
                    }
                    if(mapping.equals(candidateViewId))
                    {
                        return candidateViewId;
                    }
                    if(mapping.startsWith(".")) //this is a wildcard entry
                    {
                        builder.setLength(0); //reset/reuse the builder object 
                        builder.append(candidateViewId); 
                        builder.replace(candidateViewId.lastIndexOf('.'), candidateViewId.length(), mapping);
                        String tempViewId = builder.toString();
                        if(checkResourceExists(context,tempViewId))
                            return tempViewId;
                    }
                }
            }

            // forced facelets mappings did not match or there were no entries in faceletsViewMappings array
            if(checkResourceExists(context,candidateViewId))
                return candidateViewId;
        
        }
        
        //jsp suffixes didn't match, try facelets suffix
        String faceletsDefaultSuffix = this.getFaceletsContextSuffix(context);
        if (faceletsDefaultSuffix != null)
        {
            for (String defaultSuffix : jspDefaultSuffixes)
            {
                if (faceletsDefaultSuffix.equals(defaultSuffix))
                {
                    faceletsDefaultSuffix = null;
                    break;
                }
            }
        }
        if (faceletsDefaultSuffix != null)
        {
            StringBuilder builder = new StringBuilder(requestViewId);
            
            if (extensionPos > -1 && extensionPos > slashPos)
            {
                builder.replace(extensionPos, requestViewId.length(), faceletsDefaultSuffix);
            }
            else
            {
                builder.append(faceletsDefaultSuffix);
            }
            
            String candidateViewId = builder.toString();
            if(checkResourceExists(context,candidateViewId))
                return candidateViewId;
        }

        // Otherwise, if a physical resource exists with the name requestViewId let that value be viewId.
        if(checkResourceExists(context,requestViewId))
            return requestViewId;
        
        //Otherwise return null.
        return null;
    }

    protected boolean checkResourceExists(FacesContext context, String viewId)
    {
        try
        {
            if (isCheckedViewIdCachingEnabled(context))
            {
                Boolean resourceExists = getCheckedViewIDMap(context).get(
                        viewId);
                if (resourceExists == null)
                {
                    resourceExists = context.getExternalContext().getResource(
                            viewId) != null;
                    getCheckedViewIDMap(context).put(viewId, resourceExists);
                }
                return resourceExists;
            }

            if (context.getExternalContext().getResource(viewId) != null)
            {
                return true;
            }
        }
        catch(MalformedURLException e)
        {
            //ignore and move on
        }     
        return false;
    }

    /**
     * Read the web.xml file that is in the classpath and parse its internals to
     * figure out how the FacesServlet is mapped for the current webapp.
     */
    protected FacesServletMapping getFacesServletMapping(FacesContext context)
    {
        Map<Object, Object> attributes = context.getAttributes();

        // Has the mapping already been determined during this request?
        FacesServletMapping mapping = (FacesServletMapping) attributes.get(CACHED_SERVLET_MAPPING);
        if (mapping == null)
        {
            ExternalContext externalContext = context.getExternalContext();
            mapping = calculateFacesServletMapping(externalContext.getRequestServletPath(),
                    externalContext.getRequestPathInfo());

            attributes.put(CACHED_SERVLET_MAPPING, mapping);
        }
        return mapping;
    }

    /**
     * Determines the mapping of the FacesServlet in the web.xml configuration
     * file. However, there is no need to actually parse this configuration file
     * as runtime information is sufficient.
     *
     * @param servletPath The servletPath of the current request
     * @param pathInfo    The pathInfo of the current request
     * @return the mapping of the FacesServlet in the web.xml configuration file
     */
    protected static FacesServletMapping calculateFacesServletMapping(
        String servletPath, String pathInfo)
    {
        if (pathInfo != null)
        {
            // If there is a "extra path", it's definitely no extension mapping.
            // Now we just have to determine the path which has been specified
            // in the url-pattern, but that's easy as it's the same as the
            // current servletPath. It doesn't even matter if "/*" has been used
            // as in this case the servletPath is just an empty string according
            // to the Servlet Specification (SRV 4.4).
            return FacesServletMapping.createPrefixMapping(servletPath);
        }
        else
        {
            // In the case of extension mapping, no "extra path" is available.
            // Still it's possible that prefix-based mapping has been used.
            // Actually, if there was an exact match no "extra path"
            // is available (e.g. if the url-pattern is "/faces/*"
            // and the request-uri is "/context/faces").
            int slashPos = servletPath.lastIndexOf('/');
            int extensionPos = servletPath.lastIndexOf('.');
            if (extensionPos > -1 && extensionPos > slashPos)
            {
                String extension = servletPath.substring(extensionPos);
                return FacesServletMapping.createExtensionMapping(extension);
            }
            else
            {
                // There is no extension in the given servletPath and therefore
                // we assume that it's an exact match using prefix-based mapping.
                return FacesServletMapping.createPrefixMapping(servletPath);
            }
        }
    }
    
    private Map<String, Boolean> getCheckedViewIDMap(FacesContext context)
    {
        if (_checkedViewIdMap == null)
        {
            _checkedViewIdMap = Collections.synchronizedMap(new _CheckedViewIDMap<String, Boolean>(getViewIDCacheMaxSize(context)));
        }
        return _checkedViewIdMap;
    }

    private boolean isCheckedViewIdCachingEnabled(FacesContext context)
    {
        if (_checkedViewIdCacheEnabled == null)
        {
            //first, check to make sure that ProjectStage is production, if not, skip caching
            if (!context.isProjectStage(ProjectStage.Production))
            {
                return _checkedViewIdCacheEnabled = Boolean.FALSE;
            }

            //if in production, make sure that the cache is not explicitly disabled via context param
            String configParam = context.getExternalContext().getInitParameter(
                    CHECKED_VIEWID_CACHE_ENABLED_ATTRIBUTE);
            _checkedViewIdCacheEnabled = configParam == null ? CHECKED_VIEWID_CACHE_ENABLED_DEFAULT
                    : Boolean.parseBoolean(configParam);

            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "MyFaces ViewID Caching Enabled="
                        + _checkedViewIdCacheEnabled);
            }
        }
        return _checkedViewIdCacheEnabled;
    }

    private int getViewIDCacheMaxSize(FacesContext context)
    {
        ExternalContext externalContext = context.getExternalContext();

        String configParam = externalContext == null ? null : externalContext
                .getInitParameter(CHECKED_VIEWID_CACHE_SIZE_ATTRIBUTE);
        return configParam == null ? CHECKED_VIEWID_CACHE_DEFAULT_SIZE
                : Integer.parseInt(configParam);
    }

    private class _CheckedViewIDMap<K, V> extends LinkedHashMap<K, V>
    {
        private static final long serialVersionUID = 1L;
        private int maxCapacity;

        public _CheckedViewIDMap(int cacheSize)
        {
            // create map at max capacity and 1.1 load factor to avoid rehashing
            super(cacheSize + 1, 1.1f, true);
            maxCapacity = cacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
        {
            return size() > maxCapacity;
        }
    }
}