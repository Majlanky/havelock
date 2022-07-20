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

package com.groocraft.havelock.annotation;

import com.groocraft.havelock.registration.HavelockRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that turning on all Havelock features based on the configuration in the annotation.
 *
 * @author Majlanky
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(HavelockRegistrar.class)
public @interface EnableHavelock {

    /**
     * Configuration value of {@link Public} endpoints regarding CSRF protection. If true is kept/set it leads to turned CSRF protection for public endpoint. It
     * means presence of CSRF token is checked and its absence leads to 403 status.
     *
     * @return status of CSRF protection for public endpoints. True by default.
     */
    boolean csrf() default true;

    /**
     * Configuration value of {@link Public} endpoints regarding CORS. If true it turns CORS on public endpoints. CORS is set without configuration it means
     * it uses default configuration or from context loaded {@link CorsConfigurationSource}(if provided) or from context
     * loaded {@link org.springframework.web.filter.CorsFilter}(if provided) or you can use {@link #corsConfigurationSource} to specify configuration source
     * special for public endpoints. Keep on mind that in case of providing only one {@link CorsConfigurationSource} it will be used as configuration source
     * for the rest of security hence you must customize CORS for your non-Havelock endpoints.
     *
     * @return status of CORS for public endpoints. False by default.
     */
    boolean cors() default false;

    /**
     * Specifies name ({@link org.springframework.beans.factory.annotation.Qualifier}) of {@link CorsConfigurationSource} that should be used for
     * configuration public endpoints CORS. It is used only when {@link #cors()} is set to true.
     *
     * @return name of bean. Empty string by default.
     * @see #cors()
     */
    String corsConfigurationSource() default "";

    /**
     * Specifies if the SpringDoc (means Swagger UI and ApiDocs) should be publicly accessible. Paths of Swagger UI and api docs will be ignored on
     * {@link org.springframework.security.config.annotation.web.builders.WebSecurity} level, means no CSRF nor CORS will be set there.
     *
     * @return false by default
     */
    boolean exposeSpringDoc() default false;

    /**
     * Specifies if the security configuration is done by {@link WebSecurityConfigurerAdapter} or
     * {@link org.springframework.security.web.SecurityFilterChain}. These two approaches must and can not be mixed so Havelock provides both
     * possibilities
     * @return false by default to keep backward compatibility
     */
    boolean useSecurityFilter() default false;

}
