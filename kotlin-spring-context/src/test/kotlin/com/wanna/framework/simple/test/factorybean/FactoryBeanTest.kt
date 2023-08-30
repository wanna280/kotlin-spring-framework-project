package com.wanna.framework.simple.test.factorybean

import com.wanna.framework.beans.factory.FactoryBean
import com.wanna.framework.context.annotation.*
import com.wanna.framework.context.stereotype.Component

@ComponentScan(["com.wanna.framework.simple.test.factorybean"])
@Configuration(proxyBeanMethods = false)
class FactoryBeanTest {

    @Autowired
    var user: User? = null

}

class User

@Component
class UserFactoryBean : FactoryBean<User> {
    override fun getObjectType() = User::class.java
    override fun getObject() = User()
    override fun isSingleton() = true
    override fun isPrototype() = false
}

fun main() {
    val applicationContext = AnnotationConfigApplicationContext(FactoryBeanTest::class.java)
    val bean = applicationContext.getBean("&userFactoryBean")
    println(bean)
    val beanObject = applicationContext.getBean("userFactoryBean")
    println(beanObject)

    val bean1 = applicationContext.getBean(FactoryBeanTest::class.java)
    println(bean1.user)
}