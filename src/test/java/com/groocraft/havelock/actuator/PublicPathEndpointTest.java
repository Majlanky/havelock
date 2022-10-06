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

package com.groocraft.havelock.actuator;

import com.groocraft.havelock.PublicPathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicPathEndpointTest {

    @Mock
    PublicPathResolver publicPathResolver;

    @Test
    void testEndpointReturnsProvidedSet() {
        Set<String> publicPaths = new HashSet<>();
        publicPaths.add("/test");
        publicPaths.add("/test2");
        publicPaths.add("/test3");
        when(publicPathResolver.getPublicPaths()).thenReturn(publicPaths);

        PublicPathEndpoint endpoint = new PublicPathEndpoint(publicPathResolver);
        assertEquals(publicPaths, endpoint.publicPaths());
    }

}