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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HavelockRegistrarTest {

    @Mock
    BeanFactory beanFactory;
    @Mock
    ListableBeanFactory listableBeanFactory;
    @Mock
    Environment environment;
    @Mock
    AnnotationMetadata annotationMetadata;
    @Mock
    BeanDefinitionRegistry beanDefinitionRegistry;

    HavelockRegistrar registrar;

    @BeforeEach
    void setUp() {
        registrar = new HavelockRegistrar(beanFactory, environment);
    }

    @Test
    void testRegistrarFailsWhenEnableHavelockMissing() {
        when(annotationMetadata.getAnnotations()).thenReturn(MergedAnnotations.of(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry));
    }

    @Test
    void testRegisteredDefinitionInitializerPassesProperValues() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = mock(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());
        AtomicReference<List<?>> constructorArguments = new AtomicReference<>();
        try (MockedConstruction<HavelockWebSecurity> mockedConstruction = mockConstruction(HavelockWebSecurity.class,
                (m, c) -> constructorArguments.set(c.arguments()))) {
            definitionCaptor.getValue().getInstanceSupplier().get();
        }
        assertSame(enableHavelock, constructorArguments.get().get(2));
        assertSame(environment, constructorArguments.get().get(1));
        assertTrue(HierarchicalBeanFactory.class.isAssignableFrom(constructorArguments.get().get(0).getClass()));
        assertSame(beanFactory, ((HierarchicalBeanFactory) constructorArguments.get().get(0)).getParentBeanFactory());
    }

    @Test
    void testListableBeanFactoryIsUsedAsIs(){
        registrar = new HavelockRegistrar(listableBeanFactory, environment);
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = mock(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());
        AtomicReference<List<?>> constructorArguments = new AtomicReference<>();
        try (MockedConstruction<HavelockWebSecurity> mockedConstruction = mockConstruction(HavelockWebSecurity.class,
                (m, c) -> constructorArguments.set(c.arguments()))) {
            definitionCaptor.getValue().getInstanceSupplier().get();
        }
        assertSame(listableBeanFactory, constructorArguments.get().get(0));
    }

    @Test
    void testRegistrationIsSuccessfullyDone() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = mock(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

        ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
        verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());

        definitionCaptor.getValue().getInstanceSupplier().get();
    }

}