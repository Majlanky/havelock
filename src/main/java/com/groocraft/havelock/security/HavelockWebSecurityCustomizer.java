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

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Makes SpringDoc paths ignored on web security layer if configured to do so. Paths are read from configuration or default values are used.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
public class HavelockWebSecurityCustomizer implements WebSecurityCustomizer {

    @NonNull
    private final Environment environment;

    /**
     * Finds all swagger related paths based on configuration and default SpringDoc values and makes them ignored on {@link WebSecurity} level
     * <p>
     * {@inheritDoc}
     *
     * @param web must not be {@literal null}
     */
    @Override
    public void customize(@NonNull WebSecurity web) {
        String staticSwaggerMatcher = "/swagger-ui";
        String uiPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");
        String docsPath = environment.getProperty("springdoc.api-docs.path", "/v3/api-docs");
        web.ignoring().requestMatchers(
                allStartingWith(staticSwaggerMatcher), allPathsStartingWith(staticSwaggerMatcher),
                allStartingWith(docsPath), allPathsStartingWith(docsPath),
                uiPath);
    }

    /**
     * Helper method to create ant matcher for everything what start by the given path.
     *
     * @param start must not be {@literal null}
     * @return ant matcher
     */
    @NonNull
    private String allStartingWith(@NonNull String start) {
        return start + "**";
    }

    /**
     * Helper method to create ant matcher for every path what start by the given path.
     *
     * @param start must not be {@literal null}
     * @return ant matcher
     */
    @NonNull
    private String allPathsStartingWith(@NonNull String start) {
        return start + "/**";
    }
}
