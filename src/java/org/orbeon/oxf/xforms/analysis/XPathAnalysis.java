/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.analysis;

import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.xforms.XFormsStaticState;
import org.orbeon.oxf.xforms.analysis.controls.ControlAnalysis;
import org.orbeon.oxf.xforms.function.Instance;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Axis;

import java.io.PrintStream;
import java.util.*;

public class XPathAnalysis {

    public final String xpathString;
    public final PathMap pathmap;
//    public final int dependencies;

    final Set<String> atomizedPaths = new HashSet<String>();
    final Set<String> returnablePaths = new HashSet<String>();
    final Set<String> otherPaths = new HashSet<String>();

    final boolean figuredOutDependencies;

    public XPathAnalysis(XFormsStaticState staticState, Expression expression, String xpathString,
                         XPathAnalysis parentBindingAnalysis, Map<String, ControlAnalysis> inScopeVariables) {

        this.xpathString = xpathString;
//        this.dependencies = dependencies;

        try {
            final Map<String, PathMap> variables = new HashMap<String, PathMap>();
            if (inScopeVariables != null) {
                for (final Map.Entry<String, ControlAnalysis> entry: inScopeVariables.entrySet()) {
                    variables.put(entry.getKey(), entry.getValue().valueAnalysis.pathmap);
                }
            }

            final PathMap pathmap;
            if (parentBindingAnalysis == null) {
                // We are at the top, start with a new PathMap
                pathmap = new PathMap(expression, variables);
            } else {
                if ((expression.getDependencies() & StaticProperty.DEPENDS_ON_CONTEXT_ITEM) != 0) {
                    // Expression depends on the context item
                    // We clone and add to an existing PathMap
                    pathmap = parentBindingAnalysis.pathmap.clone();
                    pathmap.setInScopeVariables(variables);
                    pathmap.updateFinalNodes(expression.addToPathMap(pathmap, pathmap.findFinalNodes()));
                } else {
                    // Expression does not depend on the context item
                    pathmap = new PathMap(expression, variables);
                }
            }

            // TODO: Reduction of ancestor::foobar / ancestor-or-self::foobar / parent::node()
//            pathmap.simplifyAncestors();

//            final PathMap.PathMapRoot[] oldRoots = pathmap.getPathMapRoots();
//            final PathMap.PathMapRoot[] newRoots = new PathMap.PathMapRoot[oldRoots.length];
//            int rootIndex = 0;
//            for (final PathMap.PathMapRoot root: oldRoots) {
//                newRoots[rootIndex++] = pathmap.reduceToDownwardsAxes(root);
//                pathmap.removeRoot(root);
//            }
//            pathmap.addRoots(newRoots);

            this.pathmap = pathmap;

//            final int dependencies = expression.getDependencies();

            // Produce resulting paths
            figuredOutDependencies = processPaths();

        } catch (Exception e) {
            throw new OXFException("Exception while analyzing XPath expression: " + xpathString, e);
        }
    }

    public boolean intersectsValue(Set<String> touchedPaths) {
        // Return true if any path matches
        // TODO: for now naively just check exact paths
        for (final String path: atomizedPaths) {
            if (touchedPaths.contains(path))
            return true;
        }
        return false;
    }

    private boolean processPaths() {
        // TODO: need to deal with namespaces!
        final List<Expression> stack = new ArrayList<Expression>();
        for (final PathMap.PathMapRoot root: pathmap.getPathMapRoots()) {
            stack.add(root.getRootExpression());
            final boolean success = processNode(stack, root);
            if (!success)
                return false;
            stack.remove(stack.size() - 1);
        }
        return true;
    }

    private boolean processNode(List<Expression> stack, PathMap.PathMapNode node) {
        boolean success = true;
        if (node.getArcs().length == 0 || node.isReturnable()) {

            final StringBuilder sb = new StringBuilder();

            for (final Expression expression: stack) {
                if (expression instanceof Instance) {
                    // Instance function
                    final Expression instanceNameExpression = ((Instance) expression).getArguments()[0];
                    if (instanceNameExpression instanceof StringLiteral) {
                        sb.append("instance('");
                        sb.append(((StringLiteral) instanceNameExpression).getStringValue());
                        sb.append("')");
                    } else {
                        // Non-literal instance name
                        success = false;
                        break;
                    }
                } else if (expression instanceof AxisExpression) {
                    final AxisExpression axisExpression = (AxisExpression) expression;
                    if (axisExpression.getAxis() == Axis.SELF) {
                        // Self axis
                        // NOP
                    } else if (axisExpression.getAxis() == Axis.CHILD) {
                        // Child axis
                        if (sb.length() > 0)
                            sb.append('/');
                        final int fingerprint = axisExpression.getNodeTest().getFingerprint();
                        sb.append(expression.getExecutable().getConfiguration().getNamePool().getDisplayName(fingerprint));
                    } else {
                        // Unhandled axis
                        success = false;
                        break;
                    }
                } else {
                    success = false;
                    break;
                }
            }
            if (success) {
                if (node.isReturnable()) {
                    returnablePaths.add(sb.toString());
                } else if (node.isAtomized()) {
                    atomizedPaths.add(sb.toString());
                } else {
                    // Not sure if this can happen
                    otherPaths.add(sb.toString());
                }
            } else {
                // We can't deal with this path
                return false;
            }
        }

        // Process children nodes
        if (node.getArcs().length > 0) {
            for (final PathMap.PathMapArc arc: node.getArcs()) {
                stack.add(arc.getStep());
                success &= processNode(stack, arc.getTarget());
                if (!success) {
                    return false;
                }
                stack.remove(stack.size() - 1);
            }
        }

        // We managed to deal with this path
        return true;
    }

    public void dump(PrintStream out, int indent) {

        final String pad = "                                           ".substring(0, indent);

        out.println(pad + "PATHMAP - expression: " + xpathString);
        out.println(pad + "ok: " + figuredOutDependencies);
        out.println(pad + "dependent:");
        for (final String path: atomizedPaths) {
            out.println(pad + "  path: " + path);
        }

        out.println(pad + "returnable:");
        for (final String path: returnablePaths) {
            out.println(pad + "  path: " + path);
        }

        out.println(pad + "other:");
        for (final String path: otherPaths) {
            out.println(pad + "  path: " + path);
        }

//        pathmap.diagnosticDump(out);
    }

//    public void dumpDependencies(PrintStream out) {
//        if ((dependencies & StaticProperty.DEPENDS_ON_CONTEXT_ITEM) != 0) {
//            out.println("  DEPENDS_ON_CONTEXT_ITEM");
//        }
//        if ((dependencies & StaticProperty.DEPENDS_ON_CURRENT_ITEM) != 0) {
//            out.println("  DEPENDS_ON_CURRENT_ITEM");
//        }
//        if ((dependencies & StaticProperty.DEPENDS_ON_CONTEXT_DOCUMENT) != 0) {
//            out.println("  DEPENDS_ON_CONTEXT_DOCUMENT");
//        }
//        if ((dependencies & StaticProperty.DEPENDS_ON_LOCAL_VARIABLES) != 0) {
//            out.println("  DEPENDS_ON_LOCAL_VARIABLES");
//        }
//        if ((dependencies & StaticProperty.NON_CREATIVE) != 0) {
//            out.println("  NON_CREATIVE");
//        }
//    }
}