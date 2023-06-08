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

import com.groocraft.havelock.CorsConfigurationResolver;
import com.groocraft.havelock.PublicPathResolver;
import com.groocraft.havelock.annotation.EnableHavelock;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class for centralization of logic and its re-usability in @link {@link HavelockSecurityConfiguration}
 * about {@link HttpSecurity} customization. Main purpose it to set the http security based on Havelock annotations.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
public class HavelockHttpSecurityCustomizer {

    @NonNull
    private final PublicPathResolver publicPathResolver;
    @NonNull
    private final CorsConfigurationResolver corsConfigurationResolver;
    @NonNull
    private final HavelockPublicChainCustomizer publicChainCustomizer;
    private final boolean cors;
    private final boolean csrf;

    /**
     * Customizing the given {@code http} especially setting without security permitted, CORS and CSRF based on Havelock annotations and
     * {@link org.springframework.stereotype.Controller} found.
     *
     * @param http that will be used to create a {@link org.springframework.security.web.SecurityFilterChain}. Must not be {@literal null}
     * @return true if the customization was done (if there are any paths that should be permitted), false otherwise
     * @throws Exception in case the customization was not successful
     */
    public boolean customize(@NonNull HttpSecurity http) throws Exception {
        Set<String> publicPaths = publicPathResolver.getPublicPaths();
        if (!publicPaths.isEmpty()) {
            List<String> publicMatchers = new ArrayList<>(publicPaths);
            publicChainCustomizer.customize(
                    http.securityMatcher(publicMatchers.toArray(new String[0]))
                            .authorizeHttpRequests(c -> c.anyRequest().permitAll())
                            .csrf(this::configureCsrf)
                            .cors(this::configureCors));
        }
        return !publicPaths.isEmpty();
    }

    /**
     * Disables the given {@code csrfConfigurer} based on configuration of {@link EnableHavelock#csrf()}
     *
     * @param csrfConfigurer must not be {@literal null}
     */
    private void configureCsrf(@NonNull CsrfConfigurer<HttpSecurity> csrfConfigurer) {
        if (!csrf) {
            csrfConfigurer.disable();
        }
    }

    /**
     * Disables the given {@code corsConfigurer} based on configuration of {@link EnableHavelock#cors()} and if the configuration is true, it uses
     * {@link CorsConfigurationSource} with configured name or default one or none based on application context.
     *
     * @param corsCustomizer must not be {@literal null}
     */
    private void configureCors(@NonNull CorsConfigurer<HttpSecurity> corsCustomizer) {
        if (cors) {
            corsConfigurationResolver.getConfigurationSource().ifPresent(corsCustomizer::configurationSource);
        } else {
            corsCustomizer.disable();
        }
    }

}
