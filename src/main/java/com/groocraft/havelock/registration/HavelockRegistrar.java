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

package com.groocraft.havelock.registration;

import com.groocraft.havelock.CorsConfigurationResolver;
import com.groocraft.havelock.PublicPathResolver;
import com.groocraft.havelock.annotation.EnableHavelock;
import com.groocraft.havelock.security.HavelockHttpSecurityCustomizer;
import com.groocraft.havelock.security.HavelockSecurityConfiguration;
import com.groocraft.havelock.security.HavelockWebSecurity;
import com.groocraft.havelock.security.HavelockWebSecurityCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.util.function.Supplier;

/**
 * Registrar for registration of {@link HavelockWebSecurity} or {@link HavelockSecurityConfiguration} both with the configuration
 * provided in {@link EnableHavelock} annotation.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
@Slf4j
public class HavelockRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String BEAN_NAME = "havelockWebSecurity";

    @NonNull
    private final BeanFactory beanFactory;
    @NonNull
    private final Environment environment;

    /**
     * Registers {@link HavelockWebSecurity} under {@link #BEAN_NAME} into the given registry.
     * {@inheritDoc}
     *
     * @param importingClassMetadata must contain {@link EnableHavelock} and must not be {@literal null}
     * @param registry               must not be {@literal null}
     */
    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        EnableHavelock enableHavelock = importingClassMetadata.getAnnotations().get(EnableHavelock.class)
                .synthesize(MergedAnnotation::isPresent)
                .orElseThrow(() -> new IllegalArgumentException("Havelock registrar not invoked by EnableHavelock annotated class"));
        ListableBeanFactory listableBeanFactory = makeListableIfNecessary(beanFactory);
        PublicPathResolver publicPathResolver = new PublicPathResolver(listableBeanFactory);
        CorsConfigurationResolver corsConfigurationResolver = new CorsConfigurationResolver(listableBeanFactory, enableHavelock);
        HavelockWebSecurityCustomizer webSecurityCustomizer = new HavelockWebSecurityCustomizer(environment, enableHavelock);
        HavelockHttpSecurityCustomizer httpSecurityCustomizer = new HavelockHttpSecurityCustomizer(publicPathResolver,
                corsConfigurationResolver, enableHavelock.cors(), enableHavelock.csrf());
        if (enableHavelock.useSecurityFilter()) {
            registry.registerBeanDefinition(BEAN_NAME, infrastructureBeanDefinition(HavelockSecurityConfiguration.class,
                    () -> new HavelockSecurityConfiguration(httpSecurityCustomizer, webSecurityCustomizer)));
        } else {
            registry.registerBeanDefinition(BEAN_NAME, infrastructureBeanDefinition(HavelockWebSecurity.class,
                    () -> new HavelockWebSecurity(httpSecurityCustomizer, webSecurityCustomizer)));
        }
        log.debug("Havelock registered");
    }

    /**
     * Helper to create bean definition of {@link BeanDefinition#ROLE_INFRASTRUCTURE} of the given class.
     *
     * @param clazz            of the bean. Must not be {@literal null}
     * @param instanceSupplier must not be {@literal null}
     * @param <T>              type of bean class
     * @return definition with the given supplier, clazz and {@link BeanDefinition#ROLE_INFRASTRUCTURE}
     */
    private <T> GenericBeanDefinition infrastructureBeanDefinition(@NonNull Class<T> clazz, @NonNull Supplier<T> instanceSupplier) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(clazz);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        definition.setInstanceSupplier(instanceSupplier);
        return definition;
    }

    /**
     * @param beanFactory must not be {@literal null}
     * @return the given {@code beanFactory} retyped to {@link ListableBeanFactory} if it is an instance of the class or new instance of the
     * {@link ListableBeanFactory} which is using the given {@code beanFactory} as source.
     */
    @NonNull
    private ListableBeanFactory makeListableIfNecessary(@NonNull BeanFactory beanFactory) {
        if (ListableBeanFactory.class.isAssignableFrom(beanFactory.getClass())) {
            return (ListableBeanFactory) beanFactory;
        }
        return new DefaultListableBeanFactory(beanFactory);
    }
}
