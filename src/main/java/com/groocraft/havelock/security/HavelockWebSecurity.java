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
import com.groocraft.havelock.annotation.Public;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link org.springframework.security.config.annotation.web.WebSecurityConfigurer} that excludes all endpoints declared in
 * controller classes and annotated by {@link Public} annotation from security (by making it permitAll on the particular path).
 * It also provides basic exclusion of SpringDoc UI/ApiDocs endpoint for web security if configured.
 */
@Order(-1)
@Slf4j
public class HavelockWebSecurity extends WebSecurityConfigurerAdapter {

    private static final List<Class<? extends Annotation>> MAPPINGS =
            Arrays.asList(GetMapping.class, PutMapping.class, PostMapping.class, DeleteMapping.class, RequestMapping.class);
    private static final String LOG_DELIMITER = ", ";

    private final ListableBeanFactory listableBeanFactory;
    private final Environment environment;
    private final boolean csrf;
    private final boolean cors;
    private final boolean exposeSpringDoc;
    private final String corsConfigurationSourceName;

    public HavelockWebSecurity(@NonNull ListableBeanFactory listableBeanFactory,
                               @NonNull Environment environment,
                               @NonNull EnableHavelock enableHavelock) {
        super(false);
        this.listableBeanFactory = listableBeanFactory;
        this.environment = environment;
        this.csrf = enableHavelock.csrf();
        this.cors = enableHavelock.cors();
        this.corsConfigurationSourceName = enableHavelock.corsConfigurationSource();
        this.exposeSpringDoc = enableHavelock.exposeSpringDoc();
    }

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
        handleSpringDoc(web);
        Set<String> publicPaths = new HashSet<>();
        Map<String, Object> controllers = listableBeanFactory.getBeansWithAnnotation(Controller.class);
        log.debug("Havelock process the following controllers {}", String.join(LOG_DELIMITER, controllers.keySet()));
        for (Object bean : controllers.values()) {
            Class<?> controllerUserClass = ClassUtils.getUserClass(bean.getClass());
            boolean controllerPublic = isPublic(controllerUserClass);
            List<String> basePaths = findMappingPaths(controllerUserClass, Collections.emptyList());
            Set<String> controllerPublicPaths = Arrays.stream(controllerUserClass.getDeclaredMethods())
                    .filter(m -> controllerPublic || isPublic(m))
                    .flatMap(m -> findMappingPaths(m, basePaths).stream())
                    .collect(Collectors.toSet());
            if (controllerPublicPaths.isEmpty() && controllerPublic) {
                Assert.isTrue(!basePaths.isEmpty(), "No mappings found on @Public controller " + controllerUserClass.getName());
                controllerPublicPaths.addAll(basePaths);
            }
            publicPaths.addAll(controllerPublicPaths);
        }
        log.debug("Havelock found the following public paths {}", String.join(LOG_DELIMITER, publicPaths));
        configureWithPermitAll(web, publicPaths);
    }

    /**
     * Makes SpringDoc paths ignored on web security layer if configured to do so. Paths are read from configuration or default values are used.
     *
     * @param web must not be {@literal null}
     */
    private void handleSpringDoc(@NonNull WebSecurity web) {
        if (exposeSpringDoc) {
            String staticSwaggerMatcher = "/swagger-ui";
            String uiPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");
            String docsPath = environment.getProperty("springdoc.api-docs.path", "/v3/api-docs");
            web.ignoring().antMatchers(
                    allStartingWith(staticSwaggerMatcher), allPathsStartingWith(staticSwaggerMatcher),
                    allStartingWith(docsPath), allPathsStartingWith(docsPath),
                    uiPath);
        }
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

    /**
     * If there is at least one public path given, it adds new filter to the given web security with resolved configured.
     *
     * @param web         must not be {@literal null}
     * @param publicPaths must not be {@literal null}
     * @throws Exception when configuration of web security fails for a reason.
     */
    @SuppressWarnings("squid:S1874")
    private void configureWithPermitAll(@NonNull WebSecurity web, @NonNull Set<String> publicPaths) throws Exception {
        if (!publicPaths.isEmpty()) {
            List<String> publicAntMatchers = publicPaths.stream().map(this::toAntMatcher).collect(Collectors.toList());
            log.debug("Havelock resolved the following antMatchers for public endpoints {}", String.join(LOG_DELIMITER, publicAntMatchers));
            HttpSecurity http = getHttp();
            http.requestMatchers().antMatchers(publicAntMatchers.toArray(new String[publicAntMatchers.size()])).and()
                    .authorizeRequests().anyRequest().permitAll().and()
                    .csrf(this::configureCsrf)
                    .cors(this::configureCors);
            web.addSecurityFilterChainBuilder(http).postBuildAction(() -> {
                FilterSecurityInterceptor securityInterceptor = http.getSharedObject(FilterSecurityInterceptor.class);
                web.securityInterceptor(securityInterceptor);
            });
        }
    }

    /**
     * Resolves if the given {@code element} is annotated by {@link Public}
     *
     * @param element must not be null
     * @return true if {@link Public} is present, false otherwise
     */
    private boolean isPublic(@NonNull AnnotatedElement element) {
        return AnnotationUtils.findAnnotation(element, Public.class) != null;
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
            resolveCorsConfigurationSource().ifPresent(corsCustomizer::configurationSource);
        } else {
            corsCustomizer.disable();
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

    /**
     * Finds all mappings on the given {@code element} and returns declared value/paths of the present mapping or basePaths if annotation is present without
     * value/paths
     *
     * @param element   must not be {@literal null}
     * @param basePaths must not be {@literal null}
     * @return paths declared on {@code element} mapping annotations or {@code basePaths} if mapping annotation is present without a value
     */
    @NonNull
    private List<String> findMappingPaths(@NonNull AnnotatedElement element, @NonNull List<String> basePaths) {
        List<String> paths = new ArrayList<>();
        for (Class<? extends Annotation> annotationClass : MAPPINGS) {
            Annotation a = AnnotationUtils.findAnnotation(element, annotationClass);
            if (a != null) {
                paths.addAll(merge(basePaths, Arrays.asList((String[]) AnnotationUtils.getValue(a))));
                break;
            }
        }
        return paths;
    }

    /**
     * Creates fully qualified paths where {@code mappingPaths} are paths that can or can not be prefixed by basePaths.
     *
     * @param basePaths    must not be {@literal null}
     * @param mappingPaths must not be {@literal null}
     * @return if there is multiple {@code basePaths) and multiple {@code mappingPaths} result is all combinations of paths where basePaths are used only as
     * prefixes.
     */
    @NonNull
    private List<String> merge(@NonNull List<String> basePaths, @NonNull List<String> mappingPaths) {
        if (mappingPaths.isEmpty()) {
            return basePaths;
        } else {
            return basePaths.isEmpty() ? mappingPaths : basePaths.stream().flatMap(b -> mappingPaths.stream().map(m -> b + m)).collect(Collectors.toList());
        }
    }

    /**
     * Creates ant matcher from mapping path.
     *
     * @param path must not be {@literal null}
     * @return ant matcher where all paths variable placeholders are replaced by *
     */
    private @NonNull
    String toAntMatcher(@NonNull String path) {
        return path.replaceAll("\\{[^\"]*\\}", "*");
    }

    /**
     * @return {@link CorsConfigurationSource} from application context if present and no name is specified in {@link EnableHavelock}, or the one with
     * specified name, or none when none in application context. Exception is thrown in case name is configured in {@link EnableHavelock} and bean with the
     * name no exist or there is no name confgiured and more {@link CorsConfigurationSource} is present in the context.
     */
    @NonNull
    private Optional<CorsConfigurationSource> resolveCorsConfigurationSource() {
        if (StringUtils.hasText(corsConfigurationSourceName)) {
            return Optional.of(listableBeanFactory.getBean(corsConfigurationSourceName, CorsConfigurationSource.class));
        } else {
            Map<String, CorsConfigurationSource> sources = listableBeanFactory.getBeansOfType(CorsConfigurationSource.class);
            Assert.isTrue(sources.size() < 2,
                    "There is more CorsConfigurationSource but corsConfigurationSource on @EnableHavelock is not specified");
            return sources.values().stream().findAny();
        }

    }

}
