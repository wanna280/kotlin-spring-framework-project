package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.GenericConverter

/**
 * String转Enum的Converter
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/1/2
 */
open class StringToEnumConverter : GenericConverter {

    /**
     * String->Enum
     *
     * @return Pair of String->Enum
     */
    override fun getConvertibleTypes() = setOf(GenericConverter.ConvertiblePair(String::class.java, Enum::class.java))

    @Suppress("UNCHECKED_CAST")
    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        source ?: return null
        if (source is String && targetType.type.isEnum) {
            return java.lang.Enum.valueOf(targetType.type as Class<out Enum<*>>, source)
        }
        return null
    }
}