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

package com.groocraft.havelock.mock;

import com.groocraft.havelock.annotation.Public;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Public
@RequestMapping({"/base", "/base2"})
public class MultipleController {

    @GetMapping({"/get/test", "/get2/test"})
    public String get() {
        return "test";
    }

    @PutMapping({"/put/test", "/put2/test"})
    public void put(String data) {

    }

    @PostMapping({"/post/test", "/post2/test"})
    public void post(String data) {

    }

    @DeleteMapping({"/delete/test", "/delete2/test"})
    public void delete(String id) {

    }

}
