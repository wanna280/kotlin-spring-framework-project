package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.ConversionService
import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.ConditionalGenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter.*
import com.wanna.framework.util.StringUtils

/**
 * 将String转为Array的Converter, 支持对将String类型的转换类型转换为对应的元素类型
 *
 * @param conversionService ConversionService for convert elementType
 */
open class StringToArrayConverter(val conversionService: ConversionService) : ConditionalGenericConverter {
    override fun getConvertibleTypes() = setOf(ConvertiblePair(String::class.java, Array::class.java))

    /**
     * 匹配, 能否将String去转换成为目标数组的元素类型
     *
     * @param sourceType sourceType(String)
     * @param targetType targetType(List<E>)
     * @return 如果能将String转换为目标元素类型, return true; 否则return false
     */
    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(), conversionService)
    }
    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        source ?: return null
        if (source is String && targetType.type.isArray) {

            // 获取目标元素类型
            val elementType =
                targetType.getElementTypeDescriptor() ?: throw IllegalStateException("No target element type")

            // 将String去转换为StringArray(使用","去进行分割)
            val sourceList = StringUtils.commaDelimitedListToStringArray(source)

            // 基于Java反射包下的Array类, 去创建一个数组
            val array = java.lang.reflect.Array.newInstance(elementType.type, sourceList.size)

            // 遍历sourceList当中的全部元素(String), 挨个交给ConversionService去进行类型转换
            for (index in sourceList.indices) {
                val convertedElement = conversionService.convert(sourceList[index], elementType)
                java.lang.reflect.Array.set(array, index, convertedElement)
            }
            return array
        }
        return null
    }

    override fun toString() = getConvertibleTypes().toString()
}