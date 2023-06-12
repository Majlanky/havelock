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
import com.groocraft.havelock.HavelockSetting;
import com.groocraft.havelock.PublicPathResolver;
import com.groocraft.havelock.actuator.PublicPathEndpoint;
import com.groocraft.havelock.annotation.EnableHavelock;
import com.groocraft.havelock.security.HavelockHttpSecurityCustomizer;
import com.groocraft.havelock.security.HavelockSecurityConfiguration;
import com.groocraft.havelock.security.HavelockWebSecurityCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Registrar for registration of {@link HavelockSecurityConfiguration} with the configuration provided in {@link EnableHavelock} annotation.
 *
 * @author Majlanky
 */
@RequiredArgsConstructor
@Slf4j
public class HavelockRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String SETTING_BEAN_NAME = "havelockSetting";
    private static final String CONFIGURATION_BEAN_NAME = "havelockSecurityConfiguration";
    private static final String RESOLVER_BEAN_NAME = "havelockPublicPathResolver";
    private static final String CORS_RESOLVER_BEAN_NAME = "havelockCorsConfigurationResolver";

    private static final String HTTP_CUSTOMIZER_BEAN_NAME = "havelockHttpSecurityCustomizer";
    private static final String WEB_CUSTOMIZER_BEAN_NAME = "havelockWebSecurityCustomizer";
    private static final String ENDPOINT_NAME = "publicPathEndpoint";

    /**
     * Registers all necessary Havelock beans
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

        registry.registerBeanDefinition(RESOLVER_BEAN_NAME, infrastructureBeanDefinition(PublicPathResolver.class));
        registry.registerBeanDefinition(SETTING_BEAN_NAME, infrastructureBeanDefinition(HavelockSetting.class,
                List.of(enableHavelock.cors(), enableHavelock.csrf(), enableHavelock.corsConfigurationSource())));

        registry.registerBeanDefinition(CORS_RESOLVER_BEAN_NAME, infrastructureBeanDefinition(CorsConfigurationResolver.class));
        registry.registerBeanDefinition(HTTP_CUSTOMIZER_BEAN_NAME, infrastructureBeanDefinition(HavelockHttpSecurityCustomizer.class));
        registry.registerBeanDefinition(CONFIGURATION_BEAN_NAME, infrastructureBeanDefinition(HavelockSecurityConfiguration.class));

        if (enableHavelock.exposeSpringDoc()) {
            registry.registerBeanDefinition(WEB_CUSTOMIZER_BEAN_NAME, infrastructureBeanDefinition(HavelockWebSecurityCustomizer.class));
        }
        if (enableHavelock.publicPathsEndpoint()) {
            registry.registerBeanDefinition(ENDPOINT_NAME, infrastructureBeanDefinition(PublicPathEndpoint.class));
        }
        log.debug("Havelock registered");
    }

    /**
     * Helper to create bean definition of {@link BeanDefinition#ROLE_INFRASTRUCTURE} of the given class.
     *
     * @param clazz                of the bean. Must not be {@literal null}
     * @param constructorArguments must not be {@literal null}
     * @param <T>                  type of bean class
     * @return definition with the given supplier, clazz and {@link BeanDefinition#ROLE_INFRASTRUCTURE}
     */
    @NonNull
    private <T> GenericBeanDefinition infrastructureBeanDefinition(@NonNull Class<T> clazz, @NonNull List<Object> constructorArguments) {
        GenericBeanDefinition definition = infrastructureBeanDefinition(clazz);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        for (int i = 0; i < constructorArguments.size(); i++) {
            constructorArgumentValues.addIndexedArgumentValue(i, constructorArguments.get(i));
        }
        definition.setConstructorArgumentValues(constructorArgumentValues);
        return definition;
    }

    /**
     * Helper to create bean definition of {@link BeanDefinition#ROLE_INFRASTRUCTURE} of the given class.
     *
     * @param clazz of the bean. Must not be {@literal null}
     * @param <T>   type of bean class
     * @return definition with the given supplier, clazz and {@link BeanDefinition#ROLE_INFRASTRUCTURE}
     */
    @NonNull
    private <T> GenericBeanDefinition infrastructureBeanDefinition(@NonNull Class<T> clazz) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(clazz);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        return definition;
    }

}
