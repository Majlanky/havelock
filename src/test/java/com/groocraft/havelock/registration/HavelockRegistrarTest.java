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
import com.groocraft.havelock.security.HavelockPublicChainCustomizer;
import com.groocraft.havelock.security.HavelockSecurityConfiguration;
import com.groocraft.havelock.security.HavelockWebSecurity;
import com.groocraft.havelock.security.HavelockWebSecurityCustomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ObjectProvider objectProvider;
    @Mock
    HttpSecurity httpSecurity;

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
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        AtomicReference<List<?>> pathResolverArguments = new AtomicReference<>();
        AtomicReference<List<?>> corsResolverArguments = new AtomicReference<>();
        AtomicReference<List<?>> securityCustomizerArguments = new AtomicReference<>();
        try (MockedConstruction<PublicPathResolver> mockedPathResolverConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> {
                    pathResolverArguments.set(c.arguments());
                    when(m.getPublicPaths()).thenReturn(publicPaths);
                });
             MockedConstruction<CorsConfigurationResolver> mockedCorsResolverConstruction = mockConstruction(CorsConfigurationResolver.class,
                     (m, c) -> corsResolverArguments.set(c.arguments()));
             MockedConstruction<HavelockWebSecurityCustomizer> mockedCustomizerConstruction = mockConstruction(HavelockWebSecurityCustomizer.class,
                     (m, c) -> securityCustomizerArguments.set(c.arguments()))) {
            registrar = new HavelockRegistrar(beanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

            ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
            verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());
            assertEquals(HavelockWebSecurity.class, definitionCaptor.getValue().getInstanceSupplier().get().getClass());
        }
        assertSame(environment, securityCustomizerArguments.get().get(0));

        assertTrue(HierarchicalBeanFactory.class.isAssignableFrom(pathResolverArguments.get().get(0).getClass()));
        assertSame(beanFactory, ((HierarchicalBeanFactory) pathResolverArguments.get().get(0)).getParentBeanFactory());

        assertTrue(HierarchicalBeanFactory.class.isAssignableFrom(corsResolverArguments.get().get(0).getClass()));
        assertSame(beanFactory, ((HierarchicalBeanFactory) corsResolverArguments.get().get(0)).getParentBeanFactory());
        assertSame(enableHavelock, corsResolverArguments.get().get(1));
    }

    @Test
    void testFilterChainIsUsedWhenConfigured() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);
        when(enableHavelock.useSecurityFilter()).thenReturn(true);

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        try (MockedConstruction<PublicPathResolver> mockedPathResolverConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> when(m.getPublicPaths()).thenReturn(publicPaths))) {
            registrar = new HavelockRegistrar(beanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);

            ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
            verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());
            assertEquals(HavelockSecurityConfiguration.class, definitionCaptor.getValue().getInstanceSupplier().get().getClass());
        }
    }

    @Test
    void testListableBeanFactoryIsUsedAsIs() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        AtomicReference<List<?>> constructorArguments = new AtomicReference<>();
        try (MockedConstruction<PublicPathResolver> mockedConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> {
                    constructorArguments.set(c.arguments());
                    when(m.getPublicPaths()).thenReturn(publicPaths);
                })) {
            registrar = new HavelockRegistrar(listableBeanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);
            ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
            verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());
            definitionCaptor.getValue().getInstanceSupplier().get();
        }
        assertSame(listableBeanFactory, constructorArguments.get().get(0));
    }

    @Test
    void testRegistrationIsSuccessfullyDone() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        try (MockedConstruction<PublicPathResolver> mockedConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> when(m.getPublicPaths()).thenReturn(publicPaths))) {
            registrar = new HavelockRegistrar(listableBeanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);
            ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
            verify(beanDefinitionRegistry).registerBeanDefinition(eq("havelockWebSecurity"), definitionCaptor.capture());
            definitionCaptor.getValue().getInstanceSupplier().get();
        }
    }

    @Test
    void testRegistrationOfPublicPathEndpointIsNotDoneByDefault() {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        try (MockedConstruction<PublicPathResolver> mockedConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> when(m.getPublicPaths()).thenReturn(publicPaths))) {
            registrar = new HavelockRegistrar(listableBeanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);
            verify(beanDefinitionRegistry, never()).registerBeanDefinition(eq("publicPathEndpoint"), any());
        }
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

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        try (MockedConstruction<PublicPathResolver> mockedConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> when(m.getPublicPaths()).thenReturn(publicPaths))) {
            registrar = new HavelockRegistrar(listableBeanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);
            ArgumentCaptor<AbstractBeanDefinition> definitionCaptor = ArgumentCaptor.forClass(AbstractBeanDefinition.class);
            verify(beanDefinitionRegistry).registerBeanDefinition(eq("publicPathEndpoint"), definitionCaptor.capture());
            definitionCaptor.getValue().getInstanceSupplier().get();
        }
    }

    @Test
    void testPublicChainCustomizerIsUsedLazily() throws Exception {
        MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
        MergedAnnotation<EnableHavelock> mergedAnnotation = mock(MergedAnnotation.class);
        EnableHavelock enableHavelock = spy(EnableHavelock.class);
        when(mergedAnnotations.get(EnableHavelock.class)).thenReturn(mergedAnnotation);
        when(mergedAnnotation.synthesize(any())).thenReturn(Optional.of(enableHavelock));
        when(annotationMetadata.getAnnotations()).thenReturn(mergedAnnotations);
        when(enableHavelock.publicPathsEndpoint()).thenReturn(true);
        when(listableBeanFactory.getBeanProvider(HavelockPublicChainCustomizer.class)).thenReturn(objectProvider);
        when(objectProvider.getIfAvailable(any())).thenAnswer(i -> ((Supplier<HavelockPublicChainCustomizer>)i.getArgument(0)).get());

        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        AtomicReference<HavelockPublicChainCustomizer> publicChainCustomizer = new AtomicReference<>();
        try (MockedConstruction<PublicPathResolver> mockedConstruction = mockConstruction(PublicPathResolver.class,
                (m, c) -> when(m.getPublicPaths()).thenReturn(publicPaths));
             MockedConstruction<HavelockHttpSecurityCustomizer> mockedConstruction2 = mockConstruction(HavelockHttpSecurityCustomizer.class,
                     (m, c) -> publicChainCustomizer.set((HavelockPublicChainCustomizer) c.arguments().get(2)))) {

            registrar = new HavelockRegistrar(listableBeanFactory, environment);
            registrar.registerBeanDefinitions(annotationMetadata, beanDefinitionRegistry);
            verifyNoInteractions(objectProvider);

            publicChainCustomizer.get().customize(httpSecurity);

            verify(objectProvider).getIfAvailable(any());
        }
    }

}