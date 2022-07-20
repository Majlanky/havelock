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

package com.groocraft.havelock;

import com.groocraft.havelock.annotation.Public;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolver responsible to get all paths which are marked as {@link com.groocraft.havelock.annotation.Public} and provide it to the
 * security configuring entity
 *
 * @author Majlanky
 */
@Slf4j
@RequiredArgsConstructor
public class PublicPathResolver {

    private static final List<Class<? extends Annotation>> MAPPINGS =
            Arrays.asList(GetMapping.class, PutMapping.class, PostMapping.class, DeleteMapping.class, RequestMapping.class);
    private static final String LOG_DELIMITER = ", ";

    @NonNull
    private final ListableBeanFactory listableBeanFactory;
    private Set<String> publicPaths;

    /**
     * @return all resolved paths (the ones marked by {@link Public}) in ant pattern format usable for configuration of
     * {@link org.springframework.security.config.annotation.web.builders.HttpSecurity} paths. The lookup is done in time of the first call,
     * result is cached and provided repeatedly.
     */
    public synchronized Set<String> getPublicPaths() {
        if (publicPaths == null) {
            publicPaths = resolvePaths(listableBeanFactory);
        }
        return publicPaths;
    }

    /**
     * @param listableBeanFactory must not be {@literal null}
     * @return Goes through all known controllers, discovers all methods annotated by {@link Public}, resolves ant matcher fits the path and return unique
     * set of it
     */
    private Set<String> resolvePaths(@NonNull ListableBeanFactory listableBeanFactory) {
        Set<String> paths = new HashSet<>();
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
            paths.addAll(controllerPublicPaths);
        }
        Set<String> publicAntMatchers = paths.stream().map(this::toAntMatcher).collect(Collectors.toSet());
        log.debug("Havelock resolved the following antMatchers for public endpoints {}", String.join(LOG_DELIMITER, publicAntMatchers));
        return publicAntMatchers;
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
     * Resolves if the given {@code element} is annotated by {@link Public}
     *
     * @param element must not be null
     * @return true if {@link Public} is present, false otherwise
     */
    private boolean isPublic(@NonNull AnnotatedElement element) {
        return AnnotationUtils.findAnnotation(element, Public.class) != null;
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
}
