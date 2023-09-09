package com.wanna.framework.core.generic

import com.wanna.framework.core.ResolvableType
import com.wanna.framework.core.convert.converter.Converter
import org.junit.jupiter.api.Test
import java.lang.reflect.WildcardType

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

open class ResolvableTypeImpl : ResolvableTypeBase<String>() {

    private var list: ArrayList<String>? = null

    private var map: HashMap<String, Int>? = null

    private var array = arrayOf<Int>()

    private var complexMap: Map<Int, Map<String, Double>>? = null

    // 测试isAssignable的简单匹配泛型的支持
    private var map1: Map<String, String>? = null
    private var map2: Map<String, String>? = null
    private var map3: Map<String, Int>? = null

    private var base1: ResolvableTypeBase<in String>? = null
    private var base2: ResolvableTypeBase<out String>? = null
    private var base3: ResolvableTypeBase<String>? = null

    // 测试对于泛型的情况下, 判断isAssignable
    private var numbers1: List<Number>? = null
    private var numbers2: List<Int>? = null

    // 测试Kotlin当中, 某些情况下, 针对泛型T去转换成为<? extends T>的情况
    private var baseKt: List<ResolvableTypeImpl>? = null
    private var baseKt2: Converter<ResolvableTypeImpl, ResolvableTypeImpl>? = null
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

    /**
     * 泛型的简单匹配, 让类型匹配支持泛型
     */
    @Test
    fun testSimpleGenericMatch() {
        val map1 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("map1"))
        val map2 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("map2"))
        val map3 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("map3"))
        println(map1)
        println(map2)
        println(map3)
        assert(map1.isAssignableFrom(map2))
        assert(!map1.isAssignableFrom(map3))
    }

    /**
     * 测试通配符的泛型匹配, <? extends T>和<? super T>
     */
    @Test
    fun testWildcardGenericMatch() {
        // ResolvableTypeBase<in String>
        val base1 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("base1"))
        // ResolvableTypeBase<out String>
        val base2 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("base2"))
        // ResolvableTypeBase<String>
        val base3 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("base3"))

        // 两者都有泛型, 但是一个是lowerBound, 另外一个是upper, 匹配不上...
        assert(!base1.isAssignableFrom(base2))

        // base3没有bounds, base1有, 匹配不上
        assert(!base3.isAssignableFrom(base1))
        // base1有bounds, base3有, 匹配得上
        assert(base1.isAssignableFrom(base3))

        // base3没有bounds, base2有, 匹配不上
        assert(!base3.isAssignableFrom(base2))
        // base2有bounds, base3有, 匹配得上
        assert(base2.isAssignableFrom(base3))
    }

    @Test
    fun testGenericExtraMatch() {
        // Kotlin当中在List等类型当中(自定义的类型, 貌似不会去进行替换)
        // 如果在泛型当中指定非final的类T, 那么会被替换成为Java当中的<? extends T>
        // 例如: List<Number>, 会被当做List<? extends Number>
        val numbers1 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("numbers1"))
        val numbers2 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("numbers2"))

        assert(numbers1.isAssignableFrom(numbers2))
    }

    /**
     * 测试Kotlin当中的特殊的泛型的情况(可能会把T转换成为<? extends T>)
     */
    @Test
    fun testKotlinExtraGeneric() {
        // List<ResolvableTypeImpl> -> List<? extends ResolvableTypeImpl>
        val baseKt = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("baseKt"))
        // Converter<ResolvableTypeImpl, ResolvableTypeImpl>
        val baseKt2 = ResolvableType.forField(ResolvableTypeImpl::class.java.getDeclaredField("baseKt2"))
        println(baseKt)
        println(baseKt2)

        assert(baseKt.getGenerics()[0].getType() is WildcardType)
        assert(baseKt2.getGenerics()[0].getType() !is WildcardType)
        assert(baseKt2.getGenerics()[0].getType() is Class<*>)
    }

}