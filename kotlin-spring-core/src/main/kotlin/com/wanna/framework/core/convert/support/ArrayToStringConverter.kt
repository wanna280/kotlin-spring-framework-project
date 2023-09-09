package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.GenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter.*
import com.wanna.framework.util.StringUtils

/**
 * Array转为String的的Converter, 将数组内的全部元素全部toString, 并使用","去进行join
 */
open class ArrayToStringConverter : GenericConverter {

    override fun getConvertibleTypes() = setOf(ConvertiblePair(Array::class.java, String::class.java))

    @Suppress("UNCHECKED_CAST")
    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        source ?: return null
        return StringUtils.collectionToCommaDelimitedString(setOf(*(source as Array<Any>)).map { it.toString() }
            .toList())
    }

    override fun toString() = getConvertibleTypes().toString()
}