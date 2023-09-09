package com.wanna.framework.beans.factory.exception

import com.wanna.framework.beans.BeansException
import com.wanna.framework.core.ResolvableType
import com.wanna.framework.lang.Nullable

/**
 * 当前Bean的定义信息未在BeanFactory找到的异常
 *
 * @param message 描述Bean的缺失的更多详细信息
 * @param beanName 缺失的Bean的beanName
 * @param resolvableType 缺失的Bean的详细类型信息
 */
open class NoSuchBeanDefinitionException
private constructor(
    message: String,
    @Nullable val beanName: String?,
    @Nullable val resolvableType: ResolvableType?
) : BeansException(message, null) {

    /**
     * 缺失的Bean的类型
     */
    @Nullable
    val beanType = resolvableType?.resolve()


    /**
     * 创建一个新的[NoSuchBeanDefinitionException]实例对象
     *
     * @param name 缺失的Bean的beanName
     */
    constructor(name: String) : this(
        "No bean named '$name' available",
        name,
        null
    )

    /**
     * 创建一个新的[NoSuchBeanDefinitionException]实例对象
     *
     * @param name 缺失的Bean的beanName
     * @param message 描述问题的更多详细的信息
     */
    constructor(name: String, message: String?) : this(
        "No bean named '$name' available: $message",
        name,
        null
    )

    /**
     * 创建一个新的[NoSuchBeanDefinitionException]实例对象
     *
     * @param type 描述缺失的Bean的详细类型信息
     */
    constructor(type: Class<*>) : this(ResolvableType.forClass(type))

    /**
     * 创建一个新的[NoSuchBeanDefinitionException]实例对象
     *
     * @param type 描述缺失的Bean的详细的类型信息
     * @param message 描述问题的更多详细的信息
     */
    constructor(type: Class<*>, message: String) : this(ResolvableType.forClass(type), message)

    /**
     * 创建一个新的[NoSuchBeanDefinitionException]实例对象
     *
     * @param resolvableType 描述缺失的Bean的详细类型信息
     */
    constructor(resolvableType: ResolvableType) : this(
        "No qualifying bean of type '$resolvableType' available",
        null,
        resolvableType
    )

    /**
     * 创建一个新的[NoSuchBeanDefinitionException]实例对象
     *
     * @param resolvableType 描述缺失的Bean的详细的类型信息
     * @param message 描述问题的更多详细的信息
     */
    constructor(resolvableType: ResolvableType, message: String) : this(
        "No qualifying bean of type '$resolvableType' available: $message",
        null,
        resolvableType
    )
}