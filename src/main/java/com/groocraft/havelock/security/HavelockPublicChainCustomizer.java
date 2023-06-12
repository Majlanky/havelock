/*
 * Copyright 2022-2023 the original author or authors.
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

import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Customizer for security setting that is applied only on endpoint with are annotated by {@link com.groocraft.havelock.annotation.Public}.
 *
 * @author Majlanky
 */
@FunctionalInterface
public interface HavelockPublicChainCustomizer {

    /**
     * Method is called in the time of setting the security up.
     *
     * @param http representing security builder of endpoint annotated by {@link com.groocraft.havelock.annotation.Public}
     * @throws Exception in case of any exception during customization.
     */
    @SuppressWarnings("squid:S112")//Suppressed because the throw follows expected code API
    void customize(@NonNull HttpSecurity http) throws Exception;

}
