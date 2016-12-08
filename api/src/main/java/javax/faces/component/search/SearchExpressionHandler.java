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
package javax.faces.component.search;

import java.util.List;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;

/**
 *
 */
public abstract class SearchExpressionHandler
{

    protected static final char[] EXPRESSION_SEPARATOR_CHARS = new char[]
    {
        ',', ' '
    };
    protected static final String KEYWORD_PREFIX = "@";

    public abstract String resolveClientId(SearchExpressionContext searchExpressionContext, String expressions);

    public abstract List<String> resolveClientIds(
            SearchExpressionContext searchExpressionContext, String expressions);

    public abstract void resolveComponent(SearchExpressionContext searchExpressionContext, String expression,
            ContextCallback callback);

    public abstract void resolveComponents(SearchExpressionContext searchExpressionContext, String expressions,
            ContextCallback callback);

    public abstract void invokeOnComponent(SearchExpressionContext searchExpressionContext,
            UIComponent last, String expression, ContextCallback topCallback);

    public void invokeOnComponent(SearchExpressionContext searchExpressionContext,
            UIComponent last, String[] expressions, ContextCallback topCallback)
    {
        for (String expression : expressions)
        {
            invokeOnComponent(searchExpressionContext, last, expression, topCallback);
        }
    }

    public abstract String[] splitExpressions(String expressions);

    public abstract boolean isPassthroughExpression(SearchExpressionContext searchExpressionContext, String expression);

    public abstract boolean isValidExpression(SearchExpressionContext searchExpressionContext, String expression);

}