package com.wanna.framework.core.generic;

import com.wanna.framework.core.ResolvableType;
import com.wanna.framework.util.ReflectionUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * ResolvableType在Java当中的测试
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/1
 */
public class ResolvableTypeJavaTest {

    /**
     * 不带泛型, 相当于{@code Map<?,?>}
     */
    private Map map;


    @Test
    public void test() {
        final ResolvableType resolvableType = ResolvableType.forField(ReflectionUtils.findField(ResolvableTypeJavaTest.class, "map"));
        System.out.println(resolvableType); // Map<?,?>
    }
}
