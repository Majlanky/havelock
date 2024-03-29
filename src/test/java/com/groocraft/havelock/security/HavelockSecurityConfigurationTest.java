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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HavelockSecurityConfigurationTest {

    @Mock
    HavelockHttpSecurityCustomizer httpSecurityCustomizer;
    @Mock
    HttpSecurity httpSecurity;
    @Mock
    DefaultSecurityFilterChain defaultSecurityFilterChain;

    HavelockSecurityConfiguration havelockSecurityConfiguration;

    @Test
    void testCustomizersAreUsedAndCustomizedObjectsAreUsed() throws Exception {
        when(httpSecurity.build()).thenReturn(defaultSecurityFilterChain);
        when(httpSecurityCustomizer.customize(httpSecurity)).thenReturn(true);

        havelockSecurityConfiguration = new HavelockSecurityConfiguration(httpSecurityCustomizer);

        assertSame(defaultSecurityFilterChain, havelockSecurityConfiguration.havelockSecurityFilterChain(httpSecurity));

    }

    @Test
    void testNoSecurityChainIsCreatedWhenNoPublicPaths() throws Exception {
        when(httpSecurityCustomizer.customize(httpSecurity)).thenReturn(false);

        havelockSecurityConfiguration = new HavelockSecurityConfiguration(httpSecurityCustomizer);


        assertNull(havelockSecurityConfiguration.havelockSecurityFilterChain(httpSecurity));
        verify(httpSecurity, never()).build();
    }

}