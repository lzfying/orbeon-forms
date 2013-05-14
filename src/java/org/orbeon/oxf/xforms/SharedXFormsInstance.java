/**
 *  Copyright (C) 2007 Orbeon, Inc.
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
package org.orbeon.oxf.xforms;

import org.dom4j.Element;
import org.orbeon.saxon.om.DocumentInfo;

/**
 * XFormsInstance that can be shared among multiple users. It must be passed a DocumentInfo and it is not possible to
 * replace the instance document.
 */
public class SharedXFormsInstance extends XFormsInstance {

    public SharedXFormsInstance(Element containerElement) {
        super(containerElement);
    }

    public SharedXFormsInstance(String modelId, String instanceId, DocumentInfo instanceDocumentInfo, String instanceSourceURI, String username, String password, boolean applicationShared, long timeToLive, String validation) {
        super(modelId, instanceId, instanceDocumentInfo, instanceSourceURI, username, password, applicationShared, timeToLive, validation);
    }

    public void synchronizeInstanceDataEventState() {
        // NOP
    }
}