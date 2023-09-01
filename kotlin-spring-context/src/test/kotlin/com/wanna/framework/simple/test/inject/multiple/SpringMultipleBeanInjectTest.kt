package com.wanna.framework.simple.test.inject.multiple

import com.wanna.framework.beans.factory.config.BeanPostProcessor
import com.wanna.framework.context.annotation.AnnotationConfigApplicationContext
import com.wanna.framework.context.annotation.Autowired
import com.wanna.framework.context.annotation.Configuration

/**
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/1
 */
@Configuration(proxyBeanMethods = false)
class SpringInjectTest {

    @Autowired
    internal var beanPostProcessorMap: Map<String, BeanPostProcessor>? = null

    @Autowired
    internal var beanPostProcessorList: List<BeanPostProcessor>? = null

    @Autowired
    internal var beanPostProcessorSet: Set<BeanPostProcessor>? = null
}

fun main() {
    val applicationContext = AnnotationConfigApplicationContext(SpringInjectTest::class.java)

    val springInjectTest = applicationContext.getBean(SpringInjectTest::class.java)
    println(springInjectTest.beanPostProcessorMap)
}