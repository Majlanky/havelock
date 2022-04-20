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

import com.groocraft.havelock.annotation.EnableHavelock;
import com.groocraft.havelock.security.HavelockWebSecurity;
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

/**
 * Registrar for registration of {@link HavelockWebSecurity} with the configuration provided in {@link EnableHavelock} annotation.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
@Slf4j
public class HavelockRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String BEAN_NAME = "havelockWebSecurity";

    private final @NonNull
    BeanFactory beanFactory;
    private final @NonNull
    Environment environment;

    /**
     * Registers {@link HavelockWebSecurity} under {@link #BEAN_NAME} into the given registry.
     * {@inheritDoc}
     *
     * @param importingClassMetadata must contain {@link EnableHavelock} and must not be {@literal null}
     * @param registry               must not be {@literal null}
     */
    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(HavelockWebSecurity.class);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        EnableHavelock enableHavelock = importingClassMetadata.getAnnotations().get(EnableHavelock.class)
                .synthesize(MergedAnnotation::isPresent)
                .orElseThrow(() -> new IllegalArgumentException("Havelock registrar not invoked by EnableHavelock annotated class"));
        definition.setInstanceSupplier(() -> new HavelockWebSecurity(makeListableIfNecessary(beanFactory), environment, enableHavelock));
        registry.registerBeanDefinition(BEAN_NAME, definition);
        log.debug("Havelock registered");
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
