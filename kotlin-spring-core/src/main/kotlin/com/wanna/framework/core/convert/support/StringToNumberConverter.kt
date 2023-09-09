package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.GenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter.ConvertiblePair

/**
 * 这是一个可以将String转换为Number类型的Converter, 支持转为基础类型, 也支持转换成为包装类型
 *
 * @see GenericConverter
 */
@Suppress("UNCHECKED_CAST")
open class StringToNumberConverter : GenericConverter {
    private val convertiblePairs = HashSet<ConvertiblePair>()

    // 初始化它支持转换的映射列表
    init {
        // 1.初始化它转为基础数据类型的映射列表
        convertiblePairs.add(ConvertiblePair(String::class.java, Int::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Byte::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Double::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Float::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Short::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Char::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Long::class.java))
        convertiblePairs.add(ConvertiblePair(String::class.java, Boolean::class.java))

        // 2.(fixed:)初始化它转换为包装类型的映射列表(Note: XXX::class.javaObjectType, 可以获取到包装类型...)
        convertiblePairs.add(ConvertiblePair(String::class.java, Int::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Byte::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Double::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Float::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Short::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Char::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Long::class.javaObjectType))
        convertiblePairs.add(ConvertiblePair(String::class.java, Boolean::class.javaObjectType))
    }

    override fun getConvertibleTypes() = this.convertiblePairs

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        val sourceStr = source?.toString() ?: return null

        val targetTypeClass = targetType.type

        if (targetTypeClass == Int::class.java || targetTypeClass == Int::class.javaObjectType) {
            return sourceStr.toInt()
        }
        if (targetTypeClass == Byte::class.java || targetTypeClass == Byte::class.javaObjectType) {
            return sourceStr.toByte()
        }
        if (targetTypeClass == Long::class.java || targetTypeClass == Long::class.javaObjectType) {
            return sourceStr.toLong()
        }
        if (targetTypeClass == Double::class.java || targetTypeClass == Double::class.javaObjectType) {
            return sourceStr.toDouble()
        }
        if (targetTypeClass == Short::class.java || targetTypeClass == Short::class.javaObjectType) {
            return sourceStr.toShort()
        }
        if (targetTypeClass == Float::class.java || targetTypeClass == Float::class.javaObjectType) {
            return sourceStr.toFloat()
        }
        if (targetTypeClass == Char::class.java || targetTypeClass == Char::class.javaObjectType) {
            return sourceStr.toInt().toChar()
        }
        if (targetTypeClass == Boolean::class.java || targetTypeClass == Boolean::class.javaObjectType) {
            return sourceStr.toBoolean()
        }
        throw UnsupportedOperationException("不支持将sourceType=[$sourceType]转换为targetType=[$targetTypeClass]")
    }

    override fun toString() = getConvertibleTypes().toString()
}