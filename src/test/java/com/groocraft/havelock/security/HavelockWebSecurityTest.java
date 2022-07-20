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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class HavelockWebSecurityTest {

    @Mock
    WebSecurity webSecurity;
    @Mock
    WebSecurityCustomizer webSecurityCustomizer;
    @Mock
    HavelockHttpSecurityCustomizer httpSecurityCustomizer;
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

    @BeforeEach
    void setUp() {
        havelockWebSecurity = new HavelockWebSecurity(httpSecurityCustomizer, webSecurityCustomizer);
    }

    @Test
    void testCustomizersAreCalledWhenInitAndCustomizedHttpUsed() throws Exception {
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        when(httpSecurityCustomizer.customize(any())).thenReturn(true);

        when(applicationContext.getBean(ObjectPostProcessor.class)).thenReturn(objectPostProcessor);
        when(applicationContext.getBeanNamesForType(AuthenticationEventPublisher.class)).thenReturn(new String[1]);
        when(applicationContext.getBean(AuthenticationEventPublisher.class)).thenReturn(authenticationEventPublisher);
        when(applicationContext.getBean(AuthenticationManagerBuilder.class)).thenReturn(authenticationManagerBuilder);
        havelockWebSecurity.setAuthenticationConfiguration(authenticationConfiguration);
        havelockWebSecurity.setApplicationContext(applicationContext);
        havelockWebSecurity.setObjectPostProcessor(objectPostProcessor);

        FilterSecurityInterceptor filterSecurityInterceptor = mock(FilterSecurityInterceptor.class);
        ArgumentCaptor<Runnable> postActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(webSecurity.addSecurityFilterChainBuilder(any())).thenReturn(webSecurity);
        try (MockedConstruction<HttpSecurity> httpSecurityMock = Mockito.mockConstruction(
                HttpSecurity.class,
                withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS),
                (m, c) -> when(m.getSharedObject(FilterSecurityInterceptor.class)).thenReturn(filterSecurityInterceptor))) {
            havelockWebSecurity.init(webSecurity);
            verify(webSecurity).addSecurityFilterChainBuilder(httpSecurityMock.constructed().get(0));
            verify(webSecurity).postBuildAction(postActionCaptor.capture());
            postActionCaptor.getValue().run();
            verify(webSecurity).securityInterceptor(filterSecurityInterceptor);
        }
        verify(webSecurityCustomizer).customize(webSecurity);
    }

}