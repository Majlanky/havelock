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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HavelockRegistrarTest {

    @Mock
    AnnotationMetadata annotationMetadata;
    @Mock
    BeanDefinitionRegistry beanDefinitionRegistry;

    HavelockRegistrar registrar;

    @BeforeEach
    void setUp() {
        registrar = new HavelockRegistrar();
    }

    @Test
    void testRegistrarFailsWhenEnableHavelockMissing() {
        when(annotationMetadata.getAnnotations()).thenReturn(MergedAnnotations.of(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry));
    }

    @Test
    void testAllBeansAreRegisteredWithProperValues() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.publicPathsEndpoint()).thenReturn(false);
        when(enableHavelock.corsConfigurationSource()).thenReturn("test");

        registrar = new HavelockRegistrar();
        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<BeanDefinition> settingCaptor = ArgumentCaptor.forClass(BeanDefinition.class);

        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockSetting"), settingCaptor.capture());
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockSecurityConfiguration"), any());
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockPublicPathResolver"), any());
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockCorsConfigurationResolver"), any());
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockHttpSecurityCustomizer"), any());

        assertEquals(false, settingCaptor.getValue().getConstructorArgumentValues().getArgumentValue(0, Boolean.class).getValue());
        assertEquals(false, settingCaptor.getValue().getConstructorArgumentValues().getArgumentValue(1, Boolean.class).getValue());
        assertEquals("test", settingCaptor.getValue().getConstructorArgumentValues().getArgumentValue(2, String.class).getValue());
    }

    @Test
    void testRegistrationOfPublicPathEndpointAndWebSecurityCustomizerAreNotDoneByDefault() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.publicPathsEndpoint()).thenReturn(false);
        when(enableHavelock.corsConfigurationSource()).thenReturn("");

        registrar = new HavelockRegistrar();
        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<BeanDefinition> settingCaptor = ArgumentCaptor.forClass(BeanDefinition.class);

        verify(beanDefinitionRegistry, never()).registerBeanDefinition(eq("publicPathEndpoint"), settingCaptor.capture());
        verify(beanDefinitionRegistry, never()).registerBeanDefinition(eq("havelockWebSecurityCustomizer"), settingCaptor.capture());
    }

    @Test
    void testRegistrationOfPublicPathEndpointIsSuccessfullyDone() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);
        when(enableHavelock.publicPathsEndpoint()).thenReturn(true);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.publicPathsEndpoint()).thenReturn(true);
        when(enableHavelock.corsConfigurationSource()).thenReturn("");

        registrar = new HavelockRegistrar();
        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<BeanDefinition> settingCaptor = ArgumentCaptor.forClass(BeanDefinition.class);

        verify(beanDefinitionRegistry).registerBeanDefinition(eq("publicPathEndpoint"), settingCaptor.capture());
    }

    @Test
    void testRegistrationOfWebSecurityCustomizerIsSuccessfullyDone() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);
        when(enableHavelock.exposeSpringDoc()).thenReturn(true);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.exposeSpringDoc()).thenReturn(true);
        when(enableHavelock.publicPathsEndpoint()).thenReturn(false);
        when(enableHavelock.corsConfigurationSource()).thenReturn("");

        registrar = new HavelockRegistrar();
        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<BeanDefinition> settingCaptor = ArgumentCaptor.forClass(BeanDefinition.class);

        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurityCustomizer"), settingCaptor.capture());
    }


}