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

import com.groocraft.havelock.annotation.EnableHavelock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * If the {@link SecurityFilterChain} approach is enabled (by {@link EnableHavelock#useSecurityFilter()}), this class is provided as a bean
 * for setting Havelock part of security. It provides {@link SecurityFilterChain} if there is any path marked by
 * {@link com.groocraft.havelock.annotation.Public} and {@link WebSecurityCustomizer} to remove any security for Swagger.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
public class HavelockSecurityConfiguration {

    private final HavelockHttpSecurityCustomizer httpSecurityCustomizer;
    private final WebSecurityCustomizer webSecurityCustomizer;

    @Order(-1)
    @Bean
    public SecurityFilterChain havelockSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        if(httpSecurityCustomizer.customize(httpSecurity)){
            return httpSecurity.build();
        }
        return null;
    }

    @Bean
    public WebSecurityCustomizer havelockWebSecurityCustomizer(){
        return webSecurityCustomizer;
    }

}
