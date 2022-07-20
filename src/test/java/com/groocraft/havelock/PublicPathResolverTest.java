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

import com.groocraft.havelock.mock.AllMappingsController;
import com.groocraft.havelock.mock.BasePathController;
import com.groocraft.havelock.mock.ControllerWithPublicEndpoints;
import com.groocraft.havelock.mock.ExtendedMappingController;
import com.groocraft.havelock.mock.MappingController;
import com.groocraft.havelock.mock.MappingPublicController;
import com.groocraft.havelock.mock.MatcherController;
import com.groocraft.havelock.mock.MultipleController;
import com.groocraft.havelock.mock.NonPublicMappingController;
import com.groocraft.havelock.mock.PublicController;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicPathResolverTest {

    @Mock
    ListableBeanFactory listableBeanFactory;

    @ParameterizedTest
    @MethodSource("controllerVariants")
    void testPermittedPathResolving(Object controller, List<String> expectedPaths) {

        Map<String, Object> controllers = new HashMap<>();
        controllers.put("controller", controller);
        when(listableBeanFactory.getBeansWithAnnotation(Controller.class)).thenReturn(controllers);

        PublicPathResolver publicPathResolver = new PublicPathResolver(listableBeanFactory);

        assertEquals(expectedPaths.size(), publicPathResolver.getPublicPaths().size());
        assertAll(() -> {
            for (String permittedPath : publicPathResolver.getPublicPaths()) {
                assertTrue(expectedPaths.contains(permittedPath), "Permitted path " + permittedPath + " should not be there");
            }
            for (String expectedPath : expectedPaths) {
                assertTrue(publicPathResolver.getPublicPaths().contains(expectedPath), "Permitted paths are missing " + expectedPath);
            }
        });
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
                                "/base2/get2/test", "/base2/put2/test", "/base2/post2/test", "/base2/delete2/test")),
                Arguments.of(new ExtendedMappingController(), Arrays.asList("/base", "/base/different")),
                Arguments.of(new NonPublicMappingController(), Arrays.asList("/base", "/base/different")));
    }

}