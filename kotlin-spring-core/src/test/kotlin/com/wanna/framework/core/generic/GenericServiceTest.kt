package com.wanna.framework.core.generic

import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.support.DefaultConversionService
import com.wanna.framework.core.convert.support.GenericConversionService
import org.junit.jupiter.api.Test

/**
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/8
 */
class GenericServiceTest {

    val conversionService = DefaultConversionService()

    @Test
    fun testGetClassHierarchy() {
        val declaredClasses = GenericConversionService::class.java.declaredClasses
        for (declaredClass in declaredClasses) {
            if (declaredClass.name.contains("Converters")) {
                val method = declaredClass.getDeclaredMethod("getClassHierarchy", Class::class.java)
                method.isAccessible = true
                val ctor = declaredClass.getDeclaredConstructor()
                ctor.isAccessible = true
                val converters = ctor.newInstance()
                val result = method.invoke(converters, emptyArray<ArrayList<Any>>()::class.java)
                println(result)
            }
        }
    }

    @Test
    fun testStr2Int() {
        assert(conversionService.canConvert(String::class.java, Int::class.java))
    }

    @Test
    fun testInt2Str() {
        assert(conversionService.canConvert(Int::class.java, String::class.java))
    }

    @Test
    fun stringList2StringArray() {
        val converter = conversionService.getConverter(
            TypeDescriptor.valueOf(List::class.java),
            TypeDescriptor.forObject(emptyArray<String>())
        )
        val converted = converter?.convert(
            listOf("1", "2", "3"), TypeDescriptor.valueOf(List::class.java),
            TypeDescriptor.forObject(emptyArray<String>())
        )
        println(converted)

    }

    @Test
    fun stringList2IntArray() {
        val converter = conversionService.getConverter(
            TypeDescriptor.valueOf(List::class.java),
            TypeDescriptor.forObject(emptyArray<Int>())
        )
        val converted = converter?.convert(
            listOf("1", "2", "3"), TypeDescriptor.valueOf(List::class.java),
            TypeDescriptor.forObject(emptyArray<Int>())
        )
        println(converted)

    }
}