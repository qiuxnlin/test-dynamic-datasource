/**
 * Copyright © 2018 organization baomidou
 * <pre>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <pre/>
 */
package com.qiuxinlin.testdynamicdatasource.aop;

import com.qiuxinlin.testdynamicdatasource.DynamicDataSourceClassResolver;
import com.qiuxinlin.testdynamicdatasource.annotation.DS;
import com.qiuxinlin.testdynamicdatasource.spel.DynamicDataSourceSpelParser;
import com.qiuxinlin.testdynamicdatasource.spel.DynamicDataSourceSpelResolver;
import com.qiuxinlin.testdynamicdatasource.toolkit.DynamicDataSourceContextHolder;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * 动态数据源AOP核心拦截器
 *
 * @author TaoYu
 * @since 1.2.0
 */
public class DynamicDataSourceAnnotationInterceptor implements MethodInterceptor {

    /**
     * SPEL参数标识
     */
    private static final String SPEL_PREFIX = "#";

    @Setter
    private DynamicDataSourceSpelResolver dynamicDataSourceSpelResolver;

    @Setter
    private DynamicDataSourceSpelParser dynamicDataSourceSpelParser;

    private DynamicDataSourceClassResolver dynamicDataSourceClassResolver = new DynamicDataSourceClassResolver();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            DynamicDataSourceContextHolder.setDataSourceLookupKey(determineDatasource(invocation));
            return invocation.proceed();
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceLookupKey();
        }
    }

    private String determineDatasource(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> declaringClass = dynamicDataSourceClassResolver.targetClass(invocation);
        DS ds = method.isAnnotationPresent(DS.class) ? method.getAnnotation(DS.class)
                : AnnotationUtils.findAnnotation(declaringClass, DS.class);
        String value = ds.value();
        if (!value.isEmpty() && value.startsWith(SPEL_PREFIX)) {
            String spelValue = dynamicDataSourceSpelParser.parse(invocation, value);
            return dynamicDataSourceSpelResolver.resolve(spelValue);
        }
        return value;
    }
}