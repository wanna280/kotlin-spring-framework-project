package com.wanna.framework.beans.factory.support

import com.wanna.framework.beans.BeanFactoryAware
import com.wanna.framework.beans.factory.BeanFactory
import com.wanna.framework.beans.factory.FactoryBean
import com.wanna.framework.beans.factory.support.definition.RootBeanDefinition
import com.wanna.framework.core.ResolvableType
import com.wanna.framework.lang.Nullable
import com.wanna.framework.util.ClassUtils
import java.util.Properties

/**
 * 它是一个支持泛型的检查的[AutowireCandidateResolver]
 *
 * @see AutowireCandidateResolver
 */
open class GenericTypeAwareAutowireCandidateResolver : BeanFactoryAware, SimpleAutowireCandidateResolver() {

    /**
     * BeanFactory
     */
    @Nullable
    private var beanFactory: BeanFactory? = null

    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    fun getBeanFactory(): BeanFactory {
        return this.beanFactory ?: throw IllegalStateException("BeanFactory has not been inited")
    }

    override fun isAutowireCandidate(bdHolder: BeanDefinitionHolder, descriptor: DependencyDescriptor): Boolean {
        // 检查BeanDefinition的isAutowireCandidate属性
        if (!super.isAutowireCandidate(bdHolder, descriptor)) {
            return false
        }
        // 检查泛型类型是否匹配
        return checkGenericTypeMatch(bdHolder, descriptor)
    }

    /**
     * 检查给定的BeanDefinition的的类型和要去进行注入的元素的类型之间的泛型是否匹配?
     *
     * @param bdHolder 待进行依赖的匹配的BeanDefinitionHolder
     * @param descriptor 待进行注入的元素的描述符
     */
    protected open fun checkGenericTypeMatch(
        bdHolder: BeanDefinitionHolder,
        descriptor: DependencyDescriptor
    ): Boolean {
        val dependencyResolvableType = descriptor.getResolvableType()
        // 如果依赖的类型, 是Class, 那么无需去进行泛型的检查, 直接return true
        if (dependencyResolvableType.getType() is Class<*>) {
            return true
        }

        var targetType: ResolvableType? = null

        var cacheType = false
        val rbd: RootBeanDefinition? =
            if (bdHolder.beanDefinition is RootBeanDefinition) bdHolder.beanDefinition else null
        if (rbd != null) {
            targetType = rbd.targetType
            if (targetType == null) {
                cacheType = true
                targetType = getReturnTypeForFactoryMethod(rbd, descriptor)
            }
        }

        if (targetType == null) {
            // 正常的情况: BeanFactory已经被设置的情况下, 直接根据beanFactory去进行getType
            if (this.beanFactory != null) {
                val beanType = this.beanFactory!!.getType(bdHolder.beanName)
                if (beanType != null) {
                    targetType = ResolvableType.forClass(beanType)
                }
            }
            // fallback: 如果BeanFactory不存在, 或者是通过BeanFactory也没解析出来BeanType的话
            // 那么我们尽最大可能去进行寻找, 直接根据BeanDefinition的beanClass去进行解析
            if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
                val beanClass = rbd.getBeanClass()
                if (!ClassUtils.isAssignable(FactoryBean::class.java, beanClass)) {
                    targetType = ResolvableType.forClass(beanClass)
                }
            }
        }
        if (targetType == null) {
            return true
        }
        if (cacheType) {
            rbd?.targetType = targetType
        }
        // 如果允许fallback匹配, 那么如果有不写泛型的元素, 正常情况下Map<String, String>, 但是对于不写泛型的Map这种情况, 直接认为算是匹配
        if (descriptor.fallbackMatchAllowed() &&
            (targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties::class.java)
        ) {
            return true
        }

        // 泛型类型的匹配...
        return descriptor.getResolvableType().isAssignableFrom(targetType)
    }

    /**
     * 为给定的BeanDefinition对应的FactoryMethod(@Bean方法)去获取到方法的返回值类型
     *
     * @param rbd 待提取FactoryMethod的BeanDefinition
     * @param descriptor 待进行依赖注入的依赖描述符
     * @return 为FactoryMethod去获取到的方法的返回值类型(无法获取到的话, return null)
     */
    @Nullable
    protected open fun getReturnTypeForFactoryMethod(
        rbd: RootBeanDefinition,
        descriptor: DependencyDescriptor
    ): ResolvableType? {
        // 优先检查factoryMethodReturnType, 因为在到达AutowireCandidateResolver之前,
        // BeanFactory当中很可能已经完成了factoryMethodReturnType的解析工作...
        var returnType = rbd.factoryMethodReturnType
        if (returnType == null) {
            val factoryMethod = rbd.getResolvedFactoryMethod()
            if (factoryMethod != null) {
                returnType = ResolvableType.forMethodReturnType(factoryMethod)
            }
        }
        if (returnType != null) {
            val resolvedClass = returnType.resolve()
            // 检查BeanDefinition的FactoryMethod返回值类型是否是DependencyType的子类
            // 只有在方法的返回值类型和依赖类型匹配的情况下, 才去进行返回
            // 否则, 在容器当中注册单例Bean的情况下, 实例类型可能已经被注册了...
            if (resolvedClass != null && ClassUtils.isAssignable(descriptor.getDependencyType(), resolvedClass)) {
                return returnType
            }
        }

        return null
    }
}