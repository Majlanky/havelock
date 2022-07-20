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

package com.groocraft.havelock;

import com.groocraft.havelock.annotation.EnableHavelock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorsConfigurationResolverTest {

    @Mock
    ListableBeanFactory listableBeanFactory;
    @Mock
    EnableHavelock enableHavelock;
    @Mock
    CorsConfigurationSource corsConfigurationSource;

    @Test
    void testExistingDefaultCorsConfigurationSourceIsUsedByDefault() {
        when(enableHavelock.corsConfigurationSource()).thenReturn(null);
        Map<String, CorsConfigurationSource> corsConfigurationSources = new HashMap<>();
        corsConfigurationSources.put("corsConfigurationSource", corsConfigurationSource);
        when(listableBeanFactory.getBeansOfType(CorsConfigurationSource.class)).thenReturn(corsConfigurationSources);

        CorsConfigurationResolver corsConfigurationResolver = new CorsConfigurationResolver(listableBeanFactory, enableHavelock);

        assertTrue(corsConfigurationResolver.getConfigurationSource().isPresent());
        assertSame(corsConfigurationSource, corsConfigurationResolver.getConfigurationSource().get());
    }

    @Test
    void testEmptyOptionalIsReturnedWhenNoCorsConfigurationSourceFound() {
        when(enableHavelock.corsConfigurationSource()).thenReturn(null);
        when(listableBeanFactory.getBeansOfType(CorsConfigurationSource.class)).thenReturn(new HashMap<>());

        CorsConfigurationResolver corsConfigurationResolver = new CorsConfigurationResolver(listableBeanFactory, enableHavelock);

        assertFalse(corsConfigurationResolver.getConfigurationSource().isPresent());
    }

    @Test
    void testExceptionThrownWhenMoreCorsConfigurationSourceFound() {
        when(enableHavelock.corsConfigurationSource()).thenReturn(null);
        Map<String, CorsConfigurationSource> corsConfigurationSources = new HashMap<>();
        corsConfigurationSources.put("corsConfigurationSource", corsConfigurationSource);
        corsConfigurationSources.put("corsConfigurationSource2", corsConfigurationSource);
        corsConfigurationSources.put("corsConfigurationSource3", corsConfigurationSource);
        when(listableBeanFactory.getBeansOfType(CorsConfigurationSource.class)).thenReturn(corsConfigurationSources);

        CorsConfigurationResolver corsConfigurationResolver = new CorsConfigurationResolver(listableBeanFactory, enableHavelock);
        assertThrows(IllegalArgumentException.class, corsConfigurationResolver::getConfigurationSource);
    }

    @Test
    void testConfiguredCorsConfigurationSourceIsUsedWhenExists() {
        when(enableHavelock.corsConfigurationSource()).thenReturn("corsConfigurationSource");
        when(listableBeanFactory.getBean("corsConfigurationSource", CorsConfigurationSource.class)).thenReturn(corsConfigurationSource);

        CorsConfigurationResolver corsConfigurationResolver = new CorsConfigurationResolver(listableBeanFactory, enableHavelock);

        assertTrue(corsConfigurationResolver.getConfigurationSource().isPresent());
        assertSame(corsConfigurationSource, corsConfigurationResolver.getConfigurationSource().get());
    }

}