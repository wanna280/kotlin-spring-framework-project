package com.wanna.framework.core.generic

import com.wanna.framework.core.ResolvableType
import org.junit.jupiter.api.Test

/**
 * ResolvableType的测试类
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/1
 */

open class ResolvableTypeBase<T> {
    private var genericArray: Array<T>? = null
}

class ResolvableTypeImpl : ResolvableTypeBase<String>() {

    private var list: ArrayList<String>? = null

    private var map: HashMap<String, Int>? = null

    private var array = arrayOf<Int>()

    private var complexMap: Map<Int, Map<String, Double>>? = null
}


class ResolvableTypeTest {

    /**
     * 测试List的泛型
     */
    @Test
    fun testListGeneric() {
        val listFieldGenericResolvableType =
            ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("list"))
        val collectionResolvableType = listFieldGenericResolvableType.asCollection()
        val listGeneric = collectionResolvableType.getGenerics()[0].resolve()
        println("listGeneric is -> ${listGeneric?.name}")
        assert(listGeneric == String::class.java)
    }

    /**
     * 测试Map的泛型
     */
    @Test
    fun testMapGeneric() {
        val mapFieldGenericResolvableType =
            ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("map"))
        val mapResolvableType = mapFieldGenericResolvableType.asMap()
        val keyType = mapResolvableType.getGenerics()[0].resolve()
        val valueType = mapResolvableType.getGenerics()[1].resolve()

        println("keyType is -> ${keyType?.name}, valueType is -> ${valueType?.name}")
        assert(keyType == String::class.java)
        assert(valueType == Int::class.javaObjectType)
    }

    /**
     * 测试在子类当中取父类的泛型
     */
    @Test
    fun testSuperGeneric() {
        val resolvableType = ResolvableType.forType(ResolvableTypeImpl::class.java)
        val baseResolvableType = resolvableType.`as`(ResolvableTypeBase::class.java)

        assert(resolvableType.getGenerics().isEmpty())

        println("superType generic is -> ${baseResolvableType.getGenerics()[0].resolve()?.name}")
        assert(baseResolvableType.getGenerics()[0].resolve() == String::class.java)
    }

    /**
     * 测试数组的元素类型
     */
    @Test
    fun testArrayComponentType() {
        val arrayResolvableType = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("array"))
        val componentType = arrayResolvableType.getComponentType()

        println("componentType -> {${componentType.resolve()?.name}}")
        assert(componentType.resolve() == Int::class.javaObjectType)
    }

    /**
     * 测试泛型数组, 获取元素类型的情况
     */
    @Test
    fun testGenericArrayComponentType() {
        val genericArrayResolvableType =
            ResolvableType.forField(
                ResolvableTypeBase::class.java.getDeclaredField("genericArray"),
                ResolvableTypeImpl::class.java
            )

        val componentType = genericArrayResolvableType.getComponentType().resolve()
        println("genericArray base generic is -> ${componentType?.name}")
        assert(componentType == String::class.java)
    }

    /**
     * 测试复杂的Map的情况
     */
    @Test
    fun testComplexMapGeneric() {
        val complexMapResolvableType =
            ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("complexMap"))
        val keyType = complexMapResolvableType.asMap().getGenerics()[0].resolve()
        println("keyType is -> ${keyType?.name}")
        assert(keyType == Int::class.javaObjectType)

        val valueType = complexMapResolvableType.asMap().getGenerics()[1]
        println("valueType is -> ${valueType.resolve()?.name}")
        assert(valueType.resolve() == Map::class.java)

        val valueKeyType = valueType.getGenerics()[0].resolve()
        val valueValueType = valueType.getGenerics()[1].resolve()
        println("valueKeyType is -> ${valueKeyType?.name}, valueValueType is -> ${valueValueType?.name}")
        assert(valueKeyType == String::class.java)
        assert(valueValueType == Double::class.javaObjectType)
    }

    /**
     * 测试getGeneric方法, 根据指定某一个层级的index去读取泛型信息
     */
    @Test
    fun testGetGenericIndexMethod() {
        val complexMapResolvableType =
            ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("complexMap"))

        val keyType = complexMapResolvableType.getGeneric(0).resolve()
        println("keyType is ${keyType?.name}")
        assert(keyType == Int::class.javaObjectType)

        val valueKeyGeneric = complexMapResolvableType.getGeneric(1, 0)
        println("valueKeyGeneric is ${valueKeyGeneric.resolve()?.name}")
        assert(valueKeyGeneric.resolve() == String::class.java)

        val valueValueGeneric = complexMapResolvableType.getGeneric(1, 1)
        println("valueValueGeneric is ${valueValueGeneric.resolve()?.name}")
        assert(valueValueGeneric.resolve() == Double::class.javaObjectType)
    }

    @Test
    fun testNestedMethod() {
        val complexMapResolvableType =
            ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("complexMap"))

        val nested = complexMapResolvableType.getNested(2)
        println("nested default -> $nested")
        assert(
            nested.resolve() == Map::class.java
                    && nested.getGenerics()[0].resolve() == String::class.java
                    && nested.getGenerics()[1].resolve() == Double::class.javaObjectType
        )

        val nestedFirst = complexMapResolvableType.getNested(2, mapOf(2 to 0))
        println("nested first -> $nestedFirst")
        assert(nestedFirst.resolve() == Int::class.javaObjectType)

        val nestedLast = complexMapResolvableType.getNested(2, mapOf(2 to 1))
        println("nested last -> $nestedLast")

        assert(
            nestedLast.resolve() == Map::class.java
                    && nestedLast.getGenerics()[0].resolve() == String::class.java
                    && nestedLast.getGenerics()[1].resolve() == Double::class.javaObjectType
        )
    }

}