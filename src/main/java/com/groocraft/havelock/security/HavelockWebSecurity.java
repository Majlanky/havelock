/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.groocraft.havelock.security;

import com.groocraft.havelock.annotation.Public;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

/**
 * Implementation of {@link org.springframework.security.config.annotation.web.WebSecurityConfigurer} that excludes all endpoints declared in
 * controller classes and annotated by {@link Public} annotation from security (by making it permitAll on the particular path).
 * It also provides basic exclusion of SpringDoc UI/ApiDocs endpoint for web security if configured.
 *
 * @author Majlanky
 */
@Order(-1)
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("squid:S1874")//It is there for backward compatibility so the warning is invalid in the context
public class HavelockWebSecurity extends WebSecurityConfigurerAdapter {

    @NonNull
    private final HavelockHttpSecurityCustomizer httpSecurityCustomizer;
    @NonNull
    private final WebSecurityCustomizer webSecurityCustomizer;

    /**
     * Goes through all known controllers, discovers all methods annotated by {@link Public}, resolves ant matcher fits the path and makes the matching paths
     * permitAll. If configured it makes SpringDoc paths ignored on web security layer.
     * {@inheritDoc}
     *
     * @param web must not be {@literal null}
     * @throws Exception when configuration of web security fails for a reason.
     */
    @Override
    public void init(@NonNull WebSecurity web) throws Exception {
        webSecurityCustomizer.customize(web);
        configureWithPermitAll(web);
    }

    /**
     * If there is at least one public path given, it adds new filter to the given web security with resolved configured.
     *
     * @param web must not be {@literal null}
     * @throws Exception when configuration of web security fails for a reason.
     */
    @SuppressWarnings("squid:S1874")
    private void configureWithPermitAll(@NonNull WebSecurity web) throws Exception {
        HttpSecurity http = getHttp();
        if(httpSecurityCustomizer.customize(http)) {
            web.addSecurityFilterChainBuilder(http).postBuildAction(() -> {
                FilterSecurityInterceptor securityInterceptor = http.getSharedObject(FilterSecurityInterceptor.class);
                web.securityInterceptor(securityInterceptor);
            });
        }
    }

    /**
     * Fails safe to prevent default configuration done in ancestor.
     *
     * @param http is never used.
     */
    @Override
    protected void configure(HttpSecurity http) {
        //Do nothing as there is nothing to do
    }


}
