/**
 *  Copyright (C) 2004 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.servlet;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.InitUtils;
import org.orbeon.oxf.pipeline.api.ExternalContext;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.pipeline.api.ProcessorDefinition;
import org.orbeon.oxf.util.AttributesToMap;
import org.orbeon.oxf.webapp.WebAppContext;
import org.orbeon.oxf.webapp.ProcessorService;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * OXFServlet and OPSServletDelegate are the Servlet entry point of OPS. OPSServlet simply delegates to
 * OPSServletDelegate and provides an option of using the OPS Class Loader.
 *
 * Several OPSServlet and OPSPortlet instances can be used in the same Web or Portlet application.
 * They all share the same Servlet context initialization parameters, but each Servlet can be
 * configured with its own main processor and inputs.
 *
 * All OPSServlet and OPSPortlet instances in a given Web application share the same resource
 * manager.
 *
 * WARNING: OPSServlet must only depend on the Servlet API and the OPS Class Loader.
 */
public class OPSServletDelegate extends HttpServlet {

    private static final String HTTP_DEFAULT_ACCEPT_METHODS = "get,post,head";

    private static final String INIT_PROCESSOR_PROPERTY_PREFIX = "oxf.servlet-initialized-processor.";
    private static final String INIT_PROCESSOR_INPUT_PROPERTY = "oxf.servlet-initialized-processor.input.";
    private static final String DESTROY_PROCESSOR_PROPERTY_PREFIX = "oxf.servlet-destroyed-processor.";
    private static final String DESTROY_PROCESSOR_INPUT_PROPERTY = "oxf.servlet-destroyed-processor.input.";

    private static final String LOG_MESSAGE_PREFIX = "Servlet";

    private ProcessorService processorService;

    // Web application context instance shared between all components of a Web of Portlet application
    private WebAppContext webAppContext;

    // Accepted methods for this servlet
    private Map acceptedMethods = new HashMap();

    public void init() throws ServletException {
        try {
            // Make sure the Web app context is initialized
            final ServletContext servletContext = getServletContext();
            webAppContext = WebAppContext.instance(servletContext);

            // Get main processor definition
            ProcessorDefinition mainProcessorDefinition;
            {
                // Try to obtain a local processor definition
                mainProcessorDefinition
                    = InitUtils.getDefinitionFromMap(new ServletInitMap(this), ProcessorService.MAIN_PROCESSOR_PROPERTY_PREFIX,
                            ProcessorService.MAIN_PROCESSOR_INPUT_PROPERTY_PREFIX);
                // Try to obtain a processor definition from the properties
                if (mainProcessorDefinition == null)
                    mainProcessorDefinition = InitUtils.getDefinitionFromProperties(ProcessorService.MAIN_PROCESSOR_PROPERTY_PREFIX,
                        ProcessorService.MAIN_PROCESSOR_INPUT_PROPERTY_PREFIX);
                // Try to obtain a processor definition from the context
                if (mainProcessorDefinition == null)
                    mainProcessorDefinition = InitUtils.getDefinitionFromServletContext(servletContext, ProcessorService.MAIN_PROCESSOR_PROPERTY_PREFIX,
                        ProcessorService.MAIN_PROCESSOR_INPUT_PROPERTY_PREFIX);
            }
            // Get error processor definition
            final Map servletInitMap =  new ServletInitMap(this);
            ProcessorDefinition errorProcessorDefinition;
            {
                // Try to obtain a local processor definition
                errorProcessorDefinition
                        = InitUtils.getDefinitionFromMap(servletInitMap, ProcessorService.ERROR_PROCESSOR_PROPERTY_PREFIX,
                                ProcessorService.ERROR_PROCESSOR_INPUT_PROPERTY_PREFIX);
                // Try to obtain a processor definition from the properties
                if (errorProcessorDefinition == null)
                    errorProcessorDefinition = InitUtils.getDefinitionFromProperties(ProcessorService.ERROR_PROCESSOR_PROPERTY_PREFIX,
                            ProcessorService.ERROR_PROCESSOR_INPUT_PROPERTY_PREFIX);
                // Try to obtain a processor definition from the context
                if (errorProcessorDefinition == null)
                    errorProcessorDefinition = InitUtils.getDefinitionFromServletContext(servletContext, ProcessorService.ERROR_PROCESSOR_PROPERTY_PREFIX,
                        ProcessorService.ERROR_PROCESSOR_INPUT_PROPERTY_PREFIX);
            }

            // Initialize accepted methods
            {
                String acceptMethods = (String) servletInitMap.get(ProcessorService.HTTP_ACCEPT_METHODS_PROPERTY);
                if (acceptMethods == null)
                    acceptMethods = HTTP_DEFAULT_ACCEPT_METHODS;
                final StringTokenizer st = new StringTokenizer(acceptMethods, ",");
                while (st.hasMoreTokens()) {
                    final String method = st.nextToken().trim().toLowerCase();
                    acceptedMethods.put(method, method);
                }
            }

            // Create and initialize service
            processorService = new ProcessorService();
            processorService.init(mainProcessorDefinition, errorProcessorDefinition);

            // Run listeners
            try {
                InitUtils.run(servletContext, null, new ServletInitMap(this), ProcessorService.logger, LOG_MESSAGE_PREFIX, "Servlet initialized.", INIT_PROCESSOR_PROPERTY_PREFIX, INIT_PROCESSOR_INPUT_PROPERTY);
            } catch (Exception e) {
                ProcessorService.logger.error(LOG_MESSAGE_PREFIX + " - Exception when running Servlet initialization processor.", OXFException.getRootThrowable(e));
                throw new OXFException(e);
            }

        } catch (Exception e) {
            throw new ServletException(OXFException.getRootThrowable(e));
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Filter on supported methods
            String httpMethod = request.getMethod();
            if (acceptedMethods.get(httpMethod.toLowerCase()) == null)
                throw new OXFException("HTTP method not accepted: " + httpMethod
                        + ". You can configure methods in your web.xml using the parameter: " + ProcessorService.HTTP_ACCEPT_METHODS_PROPERTY);

            // Run service
            PipelineContext pipelineContext = new PipelineContext();
            ExternalContext externalContext = new ServletExternalContext(getServletContext(), pipelineContext, webAppContext.getServletInitParametersMap(), request, response);
            processorService.service(true, externalContext, pipelineContext);
        } catch (Exception e) {
            throw new ServletException(OXFException.getRootThrowable(e));
        }
    }

    public void destroy() {

        // Run listeners
        try {
            final ServletContext servletContext = getServletContext();
            InitUtils.run(servletContext, null, new ServletInitMap(this), ProcessorService.logger, LOG_MESSAGE_PREFIX, "Servlet destroyed.", DESTROY_PROCESSOR_PROPERTY_PREFIX, DESTROY_PROCESSOR_INPUT_PROPERTY);
        } catch (Exception e) {
            ProcessorService.logger.error(LOG_MESSAGE_PREFIX + " - Exception when running Servlet destruction processor.", OXFException.getRootThrowable(e));
            throw new OXFException(e);
        }

        processorService.destroy();
        processorService = null;
        webAppContext = null;
    }

    /**
     * Present a read-only view of the Servlet initialization parameters as a Map.
     */
    public class ServletInitMap extends AttributesToMap {
        public ServletInitMap(final OPSServletDelegate servletDelegate) {
            super(new AttributesToMap.Attributeable() {
                public Object getAttribute(String s) {
                    return servletDelegate.getInitParameter(s);
                }

                public Enumeration getAttributeNames() {
                    return servletDelegate.getInitParameterNames();
                }

                public void removeAttribute(String s) {
                    throw new UnsupportedOperationException();
                }

                public void setAttribute(String s, Object o) {
                    throw new UnsupportedOperationException();
                }
            });
        }
    }

/*
    {
        if (config.waitPageProcessorDefinition != null) {
                // Create and schedule the task
                Task task = new Task() {
                    public String getStatus() {
                        return null;
                    }

                    public void run() {
                        // Scenarios:
                        // 1. GET -> redirect
                        // 2. GET -> content (*)
                        // 3. POST -> redirect (*)
                        // 4. POST -> content

                        // Check (synchronized on output) whether the response was committed

                        // If it was, return immediately, there is nothing we can do

                        // If it was not, bufferize regular output and run pipeline

                        // When processing instruction is found,
                    }
                };
                task.setSchedule(System.currentTimeMillis() + config.waitPageDelay, 0);
            }
    }

    {
        InitUtils.ProcessorDefinition waitPageProcessorDefinition;
        long waitPageDelay;
    }
*/
}