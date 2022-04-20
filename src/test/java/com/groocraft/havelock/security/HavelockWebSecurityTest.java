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
import com.groocraft.havelock.mock.AllMappingsController;
import com.groocraft.havelock.mock.BasePathController;
import com.groocraft.havelock.mock.ControllerWithPublicEndpoints;
import com.groocraft.havelock.mock.MappingController;
import com.groocraft.havelock.mock.MappingPublicController;
import com.groocraft.havelock.mock.MatcherController;
import com.groocraft.havelock.mock.MultipleController;
import com.groocraft.havelock.mock.NoMappingController;
import com.groocraft.havelock.mock.PrivateController;
import com.groocraft.havelock.mock.PublicController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class HavelockWebSecurityTest {

    @Mock
    ListableBeanFactory listableBeanFactory;
    @Mock
    Environment environment;
    @Mock
    EnableHavelock enableHavelock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    WebSecurity webSecurity;
    @Mock
    ApplicationContext applicationContext;
    @Mock
    ObjectPostProcessor<Object> objectPostProcessor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AuthenticationEventPublisher authenticationEventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AuthenticationConfiguration authenticationConfiguration;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AuthenticationManagerBuilder authenticationManagerBuilder;

    HavelockWebSecurity havelockWebSecurity;

    @Test
    void testSpringDocsAreExposedWhenConfiguredWithoutSwaggerConfig() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(true);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        when(environment.getProperty(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));

        havelockWebSecurity.init(webSecurity);

        ArgumentCaptor<String> ignoredPathsCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSecurity.ignoring()).antMatchers(ignoredPathsCaptor.capture());

        assertTrue(ignoredPathsCaptor.getAllValues()
                .containsAll(Arrays.asList("/swagger-ui.html", "/swagger-ui**", "/swagger-ui/**", "/v3/api-docs**", "/v3/api-docs/**")));
    }

    @Test
    void testSpringDocsAreExposedWhenConfiguredWithSwaggerConfig() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(true);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        String configuredSwaggerUiPath = "/hello.html";
        String configuredDocsPath = "/v3/docs";

        when(environment.getProperty(eq("springdoc.swagger-ui.path"), anyString())).thenReturn(configuredSwaggerUiPath);
        when(environment.getProperty(eq("springdoc.api-docs.path"), anyString())).thenReturn(configuredDocsPath);

        havelockWebSecurity.init(webSecurity);

        ArgumentCaptor<String> ignoredPathsCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSecurity.ignoring()).antMatchers(ignoredPathsCaptor.capture());

        assertTrue(ignoredPathsCaptor.getAllValues()
                .containsAll(Arrays.asList(configuredSwaggerUiPath, "/swagger-ui**", "/swagger-ui/**", configuredDocsPath + "**", configuredDocsPath + "/**")));
    }

    @Test
    void testSpringDocsAreNotExposedWhenConfiguredNotTo() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        havelockWebSecurity.init(webSecurity);

        verify(webSecurity.ignoring(), never()).antMatchers(any(String.class));
    }

    @Test
    void testNoHttpSecurityAddedWhenNoPublicEndpoints() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        PrivateController controller = new PrivateController();
        Map<String, Object> controllers = new HashMap<>();
        controllers.put("publicController", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        havelockWebSecurity.init(webSecurity);

        verify(webSecurity, never()).addSecurityFilterChainBuilder(any());
        verify(webSecurity, never()).postBuildAction(any());
    }

    @Test
    void testNoHttpSecurityAddedAndExceptionThrownWhenNoMappings() {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        NoMappingController controller = new NoMappingController();
        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        assertThrows(IllegalArgumentException.class, () -> havelockWebSecurity.init(webSecurity));

        verify(webSecurity, never()).addSecurityFilterChainBuilder(any());
        verify(webSecurity, never()).postBuildAction(any());
    }

    @Test
    void testCorsIsEnabledWhenConfigured() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(true);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.corsConfigurationSource()).thenReturn("");
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        when(applicationContext.getBean(ObjectPostProcessor.class)).thenReturn(objectPostProcessor);
        when(applicationContext.getBeanNamesForType(AuthenticationEventPublisher.class)).thenReturn(new String[1]);
        when(applicationContext.getBean(AuthenticationEventPublisher.class)).thenReturn(authenticationEventPublisher);
        when(applicationContext.getBean(AuthenticationManagerBuilder.class)).thenReturn(authenticationManagerBuilder);
        havelockWebSecurity.setAuthenticationConfiguration(authenticationConfiguration);
        havelockWebSecurity.setApplicationContext(applicationContext);
        havelockWebSecurity.setObjectPostProcessor(objectPostProcessor);

        PublicController controller = new PublicController();
        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        ArgumentCaptor<Customizer<CsrfConfigurer<HttpSecurity>>> csrfCustomizer = ArgumentCaptor.forClass(Customizer.class);
        FilterSecurityInterceptor filterSecurityInterceptor = mock(FilterSecurityInterceptor.class);
        ArgumentCaptor<Runnable> postActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        try (MockedConstruction<HttpSecurity> httpSecurityMock = Mockito.mockConstruction(
                HttpSecurity.class,
                withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS),
                (m, c) -> {
                    when(m.getSharedObject(FilterSecurityInterceptor.class)).thenReturn(filterSecurityInterceptor);
                    when(matcherConfigurer.and()).thenReturn(m);
                    when(m.authorizeRequests()).thenReturn(urlRegistry);
                    when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
                    when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
                    when(urlRegistry.and()).thenReturn(m);
                    when(m.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
                    when(m.cors(corsCustomizer.capture())).thenReturn(m);
                    when(m.csrf(csrfCustomizer.capture())).thenReturn(m);
                })) {
            havelockWebSecurity.init(webSecurity);
            verify(webSecurity).addSecurityFilterChainBuilder(httpSecurityMock.constructed().get(0));
            verify(webSecurity).postBuildAction(postActionCaptor.capture());
            postActionCaptor.getValue().run();
            verify(webSecurity).securityInterceptor(filterSecurityInterceptor);
        }

        assertEquals(4, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Arrays.asList("/get/test", "/put/test", "/post/test", "/delete/test")));
        verify(authorizedUrl).permitAll();

        CorsConfigurer<HttpSecurity> corsConfigurer = mock(CorsConfigurer.class);
        CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class);
        corsCustomizer.getValue().customize(corsConfigurer);
        csrfCustomizer.getValue().customize(csrfConfigurer);
        verify(csrfConfigurer).disable();
        verify(corsConfigurer, never()).disable();
    }

    @Test
    void testCsrfIsEnabledWhenConfigured() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(true);
        when(enableHavelock.corsConfigurationSource()).thenReturn("");
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        when(applicationContext.getBean(ObjectPostProcessor.class)).thenReturn(objectPostProcessor);
        when(applicationContext.getBeanNamesForType(AuthenticationEventPublisher.class)).thenReturn(new String[1]);
        when(applicationContext.getBean(AuthenticationEventPublisher.class)).thenReturn(authenticationEventPublisher);
        when(applicationContext.getBean(AuthenticationManagerBuilder.class)).thenReturn(authenticationManagerBuilder);
        havelockWebSecurity.setAuthenticationConfiguration(authenticationConfiguration);
        havelockWebSecurity.setApplicationContext(applicationContext);
        havelockWebSecurity.setObjectPostProcessor(objectPostProcessor);

        PublicController controller = new PublicController();
        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        ArgumentCaptor<Customizer<CsrfConfigurer<HttpSecurity>>> csrfCustomizer = ArgumentCaptor.forClass(Customizer.class);
        FilterSecurityInterceptor filterSecurityInterceptor = mock(FilterSecurityInterceptor.class);
        ArgumentCaptor<Runnable> postActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        try (MockedConstruction<HttpSecurity> httpSecurityMock = Mockito.mockConstruction(
                HttpSecurity.class,
                withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS),
                (m, c) -> {
                    when(m.getSharedObject(FilterSecurityInterceptor.class)).thenReturn(filterSecurityInterceptor);
                    when(matcherConfigurer.and()).thenReturn(m);
                    when(m.authorizeRequests()).thenReturn(urlRegistry);
                    when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
                    when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
                    when(urlRegistry.and()).thenReturn(m);
                    when(m.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
                    when(m.cors(corsCustomizer.capture())).thenReturn(m);
                    when(m.csrf(csrfCustomizer.capture())).thenReturn(m);
                })) {
            havelockWebSecurity.init(webSecurity);
            verify(webSecurity).addSecurityFilterChainBuilder(httpSecurityMock.constructed().get(0));
            verify(webSecurity).postBuildAction(postActionCaptor.capture());
            postActionCaptor.getValue().run();
            verify(webSecurity).securityInterceptor(filterSecurityInterceptor);
        }

        assertEquals(4, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Arrays.asList("/get/test", "/put/test", "/post/test", "/delete/test")));
        verify(authorizedUrl).permitAll();

        CorsConfigurer<HttpSecurity> corsConfigurer = mock(CorsConfigurer.class);
        CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class);
        corsCustomizer.getValue().customize(corsConfigurer);
        csrfCustomizer.getValue().customize(csrfConfigurer);
        verify(csrfConfigurer, never()).disable();
        verify(corsConfigurer).disable();
    }

    @Test
    void testCorsIsUsingProperConfigurationSourceWhenConfigured() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(true);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.corsConfigurationSource()).thenReturn("configuredSource");

        CorsConfigurationSource configurationSource = mock(CorsConfigurationSource.class);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        when(applicationContext.getBean(ObjectPostProcessor.class)).thenReturn(objectPostProcessor);
        when(applicationContext.getBeanNamesForType(AuthenticationEventPublisher.class)).thenReturn(new String[1]);
        when(applicationContext.getBean(AuthenticationEventPublisher.class)).thenReturn(authenticationEventPublisher);
        when(applicationContext.getBean(AuthenticationManagerBuilder.class)).thenReturn(authenticationManagerBuilder);
        when(listableBeanFactory.getBean("configuredSource", CorsConfigurationSource.class)).thenReturn(configurationSource);
        havelockWebSecurity.setAuthenticationConfiguration(authenticationConfiguration);
        havelockWebSecurity.setApplicationContext(applicationContext);
        havelockWebSecurity.setObjectPostProcessor(objectPostProcessor);

        PublicController controller = new PublicController();
        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        FilterSecurityInterceptor filterSecurityInterceptor = mock(FilterSecurityInterceptor.class);
        ArgumentCaptor<Runnable> postActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        try (MockedConstruction<HttpSecurity> httpSecurityMock = Mockito.mockConstruction(
                HttpSecurity.class,
                withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS),
                (m, c) -> {
                    when(m.getSharedObject(FilterSecurityInterceptor.class)).thenReturn(filterSecurityInterceptor);
                    when(matcherConfigurer.and()).thenReturn(m);
                    when(m.authorizeRequests()).thenReturn(urlRegistry);
                    when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
                    when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
                    when(urlRegistry.and()).thenReturn(m);
                    when(m.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
                    when(m.csrf(any())).thenReturn(m);
                    when(m.cors(corsCustomizer.capture())).thenReturn(m);
                })) {
            havelockWebSecurity.init(webSecurity);
            verify(webSecurity).addSecurityFilterChainBuilder(httpSecurityMock.constructed().get(0));
            verify(webSecurity).postBuildAction(postActionCaptor.capture());
            postActionCaptor.getValue().run();
            verify(webSecurity).securityInterceptor(filterSecurityInterceptor);
        }

        assertEquals(4, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Arrays.asList("/get/test", "/put/test", "/post/test", "/delete/test")));
        verify(authorizedUrl).permitAll();

        CorsConfigurer<HttpSecurity> corsConfigurer = mock(CorsConfigurer.class);
        corsCustomizer.getValue().customize(corsConfigurer);
        verify(corsConfigurer, never()).disable();
        verify(corsConfigurer).configurationSource(configurationSource);
    }

    @Test
    void testCorsIsUsingDefaultConfigurationSourceWhenAvailable() throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(true);
        when(enableHavelock.csrf()).thenReturn(false);
        when(enableHavelock.corsConfigurationSource()).thenReturn("");

        CorsConfigurationSource configurationSource = mock(CorsConfigurationSource.class);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        when(applicationContext.getBean(ObjectPostProcessor.class)).thenReturn(objectPostProcessor);
        when(applicationContext.getBeanNamesForType(AuthenticationEventPublisher.class)).thenReturn(new String[1]);
        when(applicationContext.getBean(AuthenticationEventPublisher.class)).thenReturn(authenticationEventPublisher);
        when(applicationContext.getBean(AuthenticationManagerBuilder.class)).thenReturn(authenticationManagerBuilder);
        when(listableBeanFactory.getBeansOfType(CorsConfigurationSource.class)).thenReturn(Collections.singletonMap("configuratinoSource", configurationSource));
        havelockWebSecurity.setAuthenticationConfiguration(authenticationConfiguration);
        havelockWebSecurity.setApplicationContext(applicationContext);
        havelockWebSecurity.setObjectPostProcessor(objectPostProcessor);

        PublicController controller = new PublicController();
        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        FilterSecurityInterceptor filterSecurityInterceptor = mock(FilterSecurityInterceptor.class);
        ArgumentCaptor<Runnable> postActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        try (MockedConstruction<HttpSecurity> httpSecurityMock = Mockito.mockConstruction(
                HttpSecurity.class,
                withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS),
                (m, c) -> {
                    when(m.getSharedObject(FilterSecurityInterceptor.class)).thenReturn(filterSecurityInterceptor);
                    when(matcherConfigurer.and()).thenReturn(m);
                    when(m.authorizeRequests()).thenReturn(urlRegistry);
                    when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
                    when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
                    when(urlRegistry.and()).thenReturn(m);
                    when(m.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
                    when(m.csrf(any())).thenReturn(m);
                    when(m.cors(corsCustomizer.capture())).thenReturn(m);
                })) {
            havelockWebSecurity.init(webSecurity);
            verify(webSecurity).addSecurityFilterChainBuilder(httpSecurityMock.constructed().get(0));
            verify(webSecurity).postBuildAction(postActionCaptor.capture());
            postActionCaptor.getValue().run();
            verify(webSecurity).securityInterceptor(filterSecurityInterceptor);
        }

        assertEquals(4, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Arrays.asList("/get/test", "/put/test", "/post/test", "/delete/test")));
        verify(authorizedUrl).permitAll();

        CorsConfigurer<HttpSecurity> corsConfigurer = mock(CorsConfigurer.class);
        corsCustomizer.getValue().customize(corsConfigurer);
        verify(corsConfigurer, never()).disable();
        verify(corsConfigurer).configurationSource(configurationSource);
    }

    @ParameterizedTest
    @MethodSource("controllerVariants")
    void testPermittedPathResolving(Object controller, List<String> expectedPaths) throws Exception {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);
        when(enableHavelock.cors()).thenReturn(false);
        when(enableHavelock.csrf()).thenReturn(false);
        havelockWebSecurity = new HavelockWebSecurity(listableBeanFactory, environment, enableHavelock);

        when(applicationContext.getBean(ObjectPostProcessor.class)).thenReturn(objectPostProcessor);
        when(applicationContext.getBeanNamesForType(AuthenticationEventPublisher.class)).thenReturn(new String[1]);
        when(applicationContext.getBean(AuthenticationEventPublisher.class)).thenReturn(authenticationEventPublisher);
        when(applicationContext.getBean(AuthenticationManagerBuilder.class)).thenReturn(authenticationManagerBuilder);
        havelockWebSecurity.setAuthenticationConfiguration(authenticationConfiguration);
        havelockWebSecurity.setApplicationContext(applicationContext);
        havelockWebSecurity.setObjectPostProcessor(objectPostProcessor);

        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        FilterSecurityInterceptor filterSecurityInterceptor = mock(FilterSecurityInterceptor.class);
        ArgumentCaptor<Runnable> postActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        try (MockedConstruction<HttpSecurity> httpSecurityMock = Mockito.mockConstruction(
                HttpSecurity.class,
                withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS),
                (m, c) -> {
                    when(m.getSharedObject(FilterSecurityInterceptor.class)).thenReturn(filterSecurityInterceptor);
                    when(matcherConfigurer.and()).thenReturn(m);
                    when(m.authorizeRequests()).thenReturn(urlRegistry);
                    when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
                    when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
                    when(urlRegistry.and()).thenReturn(m);
                    when(m.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
                })) {
            havelockWebSecurity.init(webSecurity);
            verify(webSecurity).addSecurityFilterChainBuilder(httpSecurityMock.constructed().get(0));
            verify(webSecurity).postBuildAction(postActionCaptor.capture());
            postActionCaptor.getValue().run();
            verify(webSecurity).securityInterceptor(filterSecurityInterceptor);
        }

        assertEquals(expectedPaths.size(), permittedPaths.getAllValues().size());
        assertAll(() -> {
            for (String permittedPath : permittedPaths.getAllValues()) {
                assertTrue(expectedPaths.contains(permittedPath), "Permitted path " + permittedPath + " should not be there");
            }
            for (String expectedPath : expectedPaths) {
                assertTrue(permittedPaths.getAllValues().contains(expectedPath), "Permitted paths are missing " + expectedPath);
            }
        });
        verify(authorizedUrl).permitAll();
    }

    private static Stream<Arguments> controllerVariants() {
        return Stream.of(
                Arguments.of(new PublicController(), Arrays.asList("/get/test", "/put/test", "/post/test", "/delete/test")),
                Arguments.of(new ControllerWithPublicEndpoints(), Arrays.asList("/get/public", "/put/public", "/post/public", "/delete/public")),
                Arguments.of(new AllMappingsController(), Arrays.asList("/get/test", "/put/test", "/post/test", "/delete/test", "/common/test")),
                Arguments.of(new MappingPublicController(), Arrays.asList("/the/only/one")),
                Arguments.of(new MappingController(), Arrays.asList("/the/only/one")),
                Arguments.of(new MatcherController(), Arrays.asList("/test/*/inpath", "/test/*")),
                Arguments.of(new BasePathController(), Arrays.asList("/base/get/test", "/base/put/test", "/base/post/test", "/base/delete/test")),
                Arguments.of(new MultipleController(),
                        Arrays.asList("/base/get/test", "/base/put/test", "/base/post/test", "/base/delete/test",
                                "/base/get2/test", "/base/put2/test", "/base/post2/test", "/base/delete2/test",
                                "/base2/get/test", "/base2/put/test", "/base2/post/test", "/base2/delete/test",
                                "/base2/get2/test", "/base2/put2/test", "/base2/post2/test", "/base2/delete2/test")));
    }


}