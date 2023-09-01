package com.wanna.framework.simple.test.inject.objectfactory

import com.wanna.framework.beans.factory.ObjectFactory
import com.wanna.framework.beans.factory.ObjectProvider
import com.wanna.framework.context.annotation.AnnotationConfigApplicationContext
import com.wanna.framework.context.annotation.Autowired
import com.wanna.framework.context.annotation.Configuration
import com.wanna.framework.context.stereotype.Component
import java.util.Optional

/**
 * Spring的ObjectFactory的注入的测试
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/1
 */
@Configuration(proxyBeanMethods = false)
class SpringObjectFactoryInjectTest {
    @Autowired
    private var objectProvider: ObjectProvider<User>? = null

    @Autowired
    private var objectFactory: ObjectFactory<User>? = null

    @Autowired
    private var optionalUser: Optional<User>? = null


    @Component
    class User {

    }

}

fun main() {
    val applicationContext =
        AnnotationConfigApplicationContext(SpringObjectFactoryInjectTest::class.java)

    val springObjectFactoryInjectTest = applicationContext.getBean(SpringObjectFactoryInjectTest::class.java)
    println(springObjectFactoryInjectTest)
}