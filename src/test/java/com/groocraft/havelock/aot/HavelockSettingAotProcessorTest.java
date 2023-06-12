/*
 * Copyright 2023 the original author or authors.
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

package com.groocraft.havelock.aot;

import com.groocraft.havelock.HavelockSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HavelockSettingAotProcessorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RegisteredBean registeredBean;
    @Mock
    GenerationContext generationContext;
    @Mock
    BeanRegistrationCode beanRegistrationCode;
    @Mock
    GeneratedMethods generatedMethods;
    @Mock
    GeneratedMethod generatedMethod;
    @Mock
    BeanRegistrationAotContribution beanRegistrationAotContribution;
    @Mock
    BeanRegistrationCodeFragments beanRegistrationCodeFragments;

    @Test
    void testProcessorIsUsedOnlyForSettingBean() {
        HavelockSettingAotProcessor processor = new HavelockSettingAotProcessor();
        doReturn(Object.class).when(registeredBean).getBeanClass();

        assertNull(processor.processAheadOfTime(registeredBean));
    }

    @Test
    void testStaticInstanceSupplierReflectsPassedParameters() {
        HavelockSettingAotProcessor processor = new HavelockSettingAotProcessor();
        doReturn(HavelockSetting.class).when(registeredBean).getBeanClass();
        when(beanRegistrationCode.getClassName()).thenReturn(ClassName.get(HavelockSetting.class));
        when(beanRegistrationCode.getMethods()).thenReturn(generatedMethods);
        when(registeredBean.getBeanFactory().getBeanDefinition(any()).getConstructorArgumentValues()
                .getIndexedArgumentValue(0, Boolean.class).getValue()).thenReturn(false);
        when(registeredBean.getBeanFactory().getBeanDefinition(any()).getConstructorArgumentValues()
                .getIndexedArgumentValue(1, Boolean.class).getValue()).thenReturn(false);
        when(registeredBean.getBeanFactory().getBeanDefinition(any()).getConstructorArgumentValues()
                .getIndexedArgumentValue(2, String.class).getValue()).thenReturn("test");
        when(registeredBean.getBeanName()).thenReturn("havelockSetting");

        ArgumentCaptor<UnaryOperator<BeanRegistrationCodeFragments>> fragmentCaptor = ArgumentCaptor.forClass(UnaryOperator.class);
        try (MockedStatic<BeanRegistrationAotContribution> staticMock = Mockito.mockStatic(BeanRegistrationAotContribution.class)) {
            staticMock.when(() -> BeanRegistrationAotContribution.withCustomCodeFragments(fragmentCaptor.capture())).thenReturn(beanRegistrationAotContribution);
            processor.processAheadOfTime(registeredBean);
        }

        BeanRegistrationCodeFragments codeFragments = fragmentCaptor.getValue().apply(beanRegistrationCodeFragments);
        assertEquals(HavelockSettingAotProcessor.HavelockSettingBeanRegistrationCodeFragments.class, codeFragments.getClass());

        ArgumentCaptor<Consumer<MethodSpec.Builder>> generatedMethodArgumentCaptor = ArgumentCaptor.forClass(Consumer.class);
        when(generatedMethods.add(any(String.class), generatedMethodArgumentCaptor.capture())).thenReturn(generatedMethod);
        when(generatedMethod.getName()).thenReturn("getInstance");

        CodeBlock codeBlock = codeFragments.generateInstanceSupplierCode(generationContext, beanRegistrationCode, null, false);

        assertEquals("org.springframework.beans.factory.support.InstanceSupplier.of(com.groocraft.havelock.HavelockSetting::getInstance)", codeBlock.toString());

        MethodSpec.Builder builder = MethodSpec.methodBuilder("getInstance");
        generatedMethodArgumentCaptor.getValue().accept(builder);
        MethodSpec methodSpec = builder.build();
        assertEquals("""
                /**
                 * Get the bean instance for 'havelockSetting'.
                 */
                private static com.groocraft.havelock.HavelockSetting getInstance(
                    org.springframework.beans.factory.support.RegisteredBean registeredBean) {
                  return new com.groocraft.havelock.HavelockSetting(false, false, "test");
                }
                """, methodSpec.toString());
    }
}