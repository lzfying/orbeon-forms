<?xml version="1.0" encoding="iso-8859-1"?>
<!--
    Copyright (C) 2005 Orbeon, Inc.

    This program is free software; you can redistribute it and/or modify it under the terms of the
    GNU Lesser General Public License as published by the Free Software Foundation; either version
    2.1 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
-->
<html xsl:version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xforms="http://www.w3.org/2002/xforms"
    xmlns:xxforms="http://orbeon.org/oxf/xml/xforms"
    xmlns:xi="http://www.w3.org/2001/XInclude"
    xmlns:f="http://orbeon.org/oxf/xml/formatting"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns="http://www.w3.org/1999/xhtml">

    <head>
        <title>Home</title>
    </head>
    <body>
        <div class="maincontent">
            <p>
                Welcome to the OPS Blog sample application!
            </p>
            <h2>What is it?</h2>
            <p>
                This is a sample application for <a
                href="http://www.orbeon.com/software/presentation-server">Orbeon PresentationServer</a>.
                It illustrates the following functionality:
            </p>
            <ul>
                <li>Implementing XML-RPC services</li>
                <li>Connecting to a native XML database</li>
                <li>Using XForms with events to create responsive user interfaces</li>
                <li>Producing XHTML, RSS and other formats from a single data source</li>
                <li>Implementing configurable themes with XSLT</li>
                <li>Creating "clean" URLs in a REST perspective</li>
            </ul>
            <p>
                The OPS Blog example is also a fully-functioning blog application, which you can deploy
                on your server!
            </p>
            <h2>Configured Blogs</h2>
            <ul>
                <xsl:for-each select="/blogs/blog">
                    <li>
                        <a href="/blog/{username}/{blog-id}"><xsl:value-of select="name"/></a>
                    </li>
                </xsl:for-each>
            </ul>
            <h2>Administration</h2>
            <p>
                Initially, no user or blog is configured. In order to configure a blog, first use
                the Users administration page to create a user. Then use the Blogs administration
                page to create a blog for that user.
            </p>
            <ul>
                <li>
                    <a href="/blog/admin/users">Users</a>
                </li>
                <li>
                    <a href="/blog/admin/blogs">Blogs</a>
                </li>
            </ul>
        </div>
    </body>
</html>