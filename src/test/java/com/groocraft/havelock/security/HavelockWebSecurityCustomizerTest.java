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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HavelockWebSecurityCustomizerTest {


    @Mock
    Environment environment;
    @Mock
    EnableHavelock enableHavelock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    WebSecurity webSecurity;

    HavelockWebSecurityCustomizer webSecurityCustomizer;

    @BeforeEach
    void setUp() {
        webSecurityCustomizer = new HavelockWebSecurityCustomizer(environment, enableHavelock);
    }

    @Test
    void testSpringDocsAreExposedWhenConfiguredWithoutSwaggerConfig() {
        when(enableHavelock.exposeSpringDoc()).thenReturn(true);

        when(environment.getProperty(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));

        webSecurityCustomizer.customize(webSecurity);

        ArgumentCaptor<String> ignoredPathsCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSecurity.ignoring()).antMatchers(ignoredPathsCaptor.capture());

        assertTrue(ignoredPathsCaptor.getAllValues()
                .containsAll(Arrays.asList("/swagger-ui.html", "/swagger-ui**", "/swagger-ui/**", "/v3/api-docs**", "/v3/api-docs/**")));
    }

    @Test
    void testSpringDocsAreExposedWhenConfiguredWithSwaggerConfig() {
        when(enableHavelock.exposeSpringDoc()).thenReturn(true);

        String configuredSwaggerUiPath = "/hello.html";
        String configuredDocsPath = "/v3/docs";

        when(environment.getProperty(eq("springdoc.swagger-ui.path"), anyString())).thenReturn(configuredSwaggerUiPath);
        when(environment.getProperty(eq("springdoc.api-docs.path"), anyString())).thenReturn(configuredDocsPath);

        webSecurityCustomizer.customize(webSecurity);

        ArgumentCaptor<String> ignoredPathsCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSecurity.ignoring()).antMatchers(ignoredPathsCaptor.capture());

        assertTrue(ignoredPathsCaptor.getAllValues()
                .containsAll(Arrays.asList(configuredSwaggerUiPath, "/swagger-ui**", "/swagger-ui/**", configuredDocsPath + "**", configuredDocsPath + "/**")));
    }

    @Test
    void testSpringDocsAreNotExposedWhenConfiguredNotTo() {
        when(enableHavelock.exposeSpringDoc()).thenReturn(false);

        webSecurityCustomizer.customize(webSecurity);

        verify(webSecurity.ignoring(), never()).antMatchers(any(String.class));
    }


}