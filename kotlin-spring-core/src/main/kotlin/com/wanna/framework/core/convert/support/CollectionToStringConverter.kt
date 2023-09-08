package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.GenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter.*
import com.wanna.framework.util.StringUtils

/**
 * Collection转String的Converter
 */
open class CollectionToStringConverter : GenericConverter {
    override fun getConvertibleTypes() = setOf(ConvertiblePair(Collection::class.java, String::class.java))

    @Suppress("UNCHECKED_CAST")
    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        source ?: return null
        return StringUtils.collectionToCommaDelimitedString((source as Collection<Any>).map { it.toString() }
            .toList())
    }

    override fun toString() = getConvertibleTypes().toString()
}