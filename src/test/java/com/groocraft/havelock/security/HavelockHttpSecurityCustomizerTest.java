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

import com.groocraft.havelock.CorsConfigurationResolver;
import com.groocraft.havelock.PublicPathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HavelockHttpSecurityCustomizerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    HttpSecurity httpSecurity;
    @Mock
    PublicPathResolver publicPathResolver;

    HavelockHttpSecurityCustomizer httpSecurityCustomizer;

    @Test
    void testNoHttpSecurityAddedWhenNoPublicEndpoints() throws Exception {
        httpSecurityCustomizer = new HavelockHttpSecurityCustomizer(publicPathResolver, mock(CorsConfigurationResolver.class), false, false);

        when(publicPathResolver.getPublicPaths()).thenReturn(new HashSet<>());
        httpSecurityCustomizer.customize(httpSecurity);

        verifyNoInteractions(httpSecurity);
    }

    @Test
    void testCorsIsEnabledWhenConfigured() throws Exception {
        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/get/test");
        httpSecurityCustomizer = new HavelockHttpSecurityCustomizer(publicPathResolver, mock(CorsConfigurationResolver.class), true, false);

        when(publicPathResolver.getPublicPaths()).thenReturn(publicPaths);
        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        ArgumentCaptor<Customizer<CsrfConfigurer<HttpSecurity>>> csrfCustomizer = ArgumentCaptor.forClass(Customizer.class);

        when(matcherConfigurer.and()).thenReturn(httpSecurity);
        when(httpSecurity.authorizeRequests()).thenReturn(urlRegistry);
        when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
        when(urlRegistry.and()).thenReturn(httpSecurity);
        when(httpSecurity.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
        when(httpSecurity.cors(corsCustomizer.capture())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(csrfCustomizer.capture())).thenReturn(httpSecurity);

        httpSecurityCustomizer.customize(httpSecurity);

        assertEquals(1, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Collections.singletonList("/get/test")));
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
        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/get/test");
        httpSecurityCustomizer = new HavelockHttpSecurityCustomizer(publicPathResolver, mock(CorsConfigurationResolver.class), false, true);

        when(publicPathResolver.getPublicPaths()).thenReturn(publicPaths);
        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        ArgumentCaptor<Customizer<CsrfConfigurer<HttpSecurity>>> csrfCustomizer = ArgumentCaptor.forClass(Customizer.class);

        when(matcherConfigurer.and()).thenReturn(httpSecurity);
        when(httpSecurity.authorizeRequests()).thenReturn(urlRegistry);
        when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
        when(urlRegistry.and()).thenReturn(httpSecurity);
        when(httpSecurity.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
        when(httpSecurity.cors(corsCustomizer.capture())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(csrfCustomizer.capture())).thenReturn(httpSecurity);

        httpSecurityCustomizer.customize(httpSecurity);

        assertEquals(1, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Arrays.asList("/get/test")));
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
        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/get/test");
        CorsConfigurationSource configurationSource = mock(CorsConfigurationSource.class);
        CorsConfigurationResolver corsConfigurationResolver = mock(CorsConfigurationResolver.class);
        when(corsConfigurationResolver.getConfigurationSource()).thenReturn(Optional.of(configurationSource));
        httpSecurityCustomizer = new HavelockHttpSecurityCustomizer(publicPathResolver, corsConfigurationResolver, true, false);

        when(publicPathResolver.getPublicPaths()).thenReturn(publicPaths);
        ArgumentCaptor<String> permittedPaths = ArgumentCaptor.forClass(String.class);
        HttpSecurity.RequestMatcherConfigurer matcherConfigurer = mock(HttpSecurity.RequestMatcherConfigurer.class, Answers.RETURNS_DEEP_STUBS);
        ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry urlRegistry =
                mock(ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry.class);
        ExpressionUrlAuthorizationConfigurer.AuthorizedUrl authorizedUrl = mock(ExpressionUrlAuthorizationConfigurer.AuthorizedUrl.class,
                Answers.RETURNS_DEEP_STUBS);
        ArgumentCaptor<Customizer<CorsConfigurer<HttpSecurity>>> corsCustomizer = ArgumentCaptor.forClass(Customizer.class);
        ArgumentCaptor<Customizer<CsrfConfigurer<HttpSecurity>>> csrfCustomizer = ArgumentCaptor.forClass(Customizer.class);

        when(matcherConfigurer.and()).thenReturn(httpSecurity);
        when(httpSecurity.authorizeRequests()).thenReturn(urlRegistry);
        when(urlRegistry.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(urlRegistry);
        when(urlRegistry.and()).thenReturn(httpSecurity);
        when(httpSecurity.requestMatchers().antMatchers(permittedPaths.capture())).thenReturn(matcherConfigurer);
        when(httpSecurity.cors(corsCustomizer.capture())).thenReturn(httpSecurity);
        when(httpSecurity.csrf(csrfCustomizer.capture())).thenReturn(httpSecurity);

        httpSecurityCustomizer.customize(httpSecurity);

        assertEquals(1, permittedPaths.getAllValues().size());
        assertTrue(permittedPaths.getAllValues().containsAll(Collections.singletonList("/get/test")));
        verify(authorizedUrl).permitAll();

        CorsConfigurer<HttpSecurity> corsConfigurer = mock(CorsConfigurer.class);
        corsCustomizer.getValue().customize(corsConfigurer);
        verify(corsConfigurer, never()).disable();
        verify(corsConfigurer).configurationSource(configurationSource);
    }

}