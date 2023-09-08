package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.ConversionService
import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.ConditionalGenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter.*
import com.wanna.framework.lang.Nullable

/**
 * 将Collection转为Array的Converter
 */
open class CollectionToArrayConverter(private val conversionService: ConversionService) : ConditionalGenericConverter {

    /**
     * Collection->Array
     */
    override fun getConvertibleTypes() = setOf(ConvertiblePair(Collection::class.java, Array::class.java))

    /**
     * 对元素类型进行匹配, 只有在元素类型匹配上的情况下, 才执行Convert
     *
     * @param sourceType sourceType(集合)
     * @param targetType targetType(数组)
     * @return 能否将集合的元素类型去转换成为数组的元素类型?
     */
    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return ConversionUtils.canConvertElements(
            sourceType.getElementTypeDescriptor(),
            targetType.getElementTypeDescriptor(),
            conversionService
        )
    }

    /**
     * 将source对象去转换成为目标类型的对象
     *
     * @param source 待转换的对象
     * @param sourceType sourceType
     * @param targetType targetType
     * @return 转换得到的目标对象
     */
    @Nullable
    override fun convert(@Nullable source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        source ?: return null

        // targetType is Array, 我们使用componentType去作为targetElementType
        val targetElementType =
            targetType.getElementTypeDescriptor() ?: throw IllegalStateException("No target element type")

        if (source is Collection<*>) {
            // 利用反射去实例化出来一个Array
            val array = java.lang.reflect.Array.newInstance(targetElementType.type, source.size)
            val iterator = source.iterator()
            // 完成类型的转换, 并使用反射去设置到Array当中去
            for (index in 0 until source.size) {
                val originValue = iterator.next()
                val convertedValue = conversionService.convert(originValue, targetElementType)
                java.lang.reflect.Array.set(array, index, convertedValue)
            }

            return array
        }

        return null
    }

    override fun toString() = getConvertibleTypes().toString()
}