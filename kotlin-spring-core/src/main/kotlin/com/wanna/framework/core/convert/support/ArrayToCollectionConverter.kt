package com.wanna.framework.core.convert.support

import com.wanna.framework.core.CollectionFactory
import com.wanna.framework.core.convert.ConversionService
import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.ConditionalGenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter
import com.wanna.framework.util.ClassUtils

/**
 * 将Array转换为Collection的Converter
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2022/12/4
 *
 * @param conversionService 因为Array->Collection, 存在有具体的元素类型的转换, 因此需要用到ConversionService
 */
class ArrayToCollectionConverter(private val conversionService: ConversionService) : ConditionalGenericConverter {

    /**
     * Array->Collection
     */
    override fun getConvertibleTypes(): Set<GenericConverter.ConvertiblePair> =
        setOf(GenericConverter.ConvertiblePair(Array::class.java, Collection::class.java))

    /**
     * getConvertibleTypes只能匹配外层的类型, 在matches内去匹配泛型的元素类型
     *
     * @param sourceType 数组类型, 用于获取内部的元素类型(ComponentType)
     * @param targetType 目标集合类型, 用于取泛型, 去计算元素类型
     * @return 能否进行内部的元素的类型转换?
     */
    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return ConversionUtils.canConvertElements(
            sourceType.getElementTypeDescriptor(),
            targetType.getElementTypeDescriptor(),
            conversionService
        )
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        // 只支持去处理source is Array, target is Collection的情况
        if (source is Array<*> && ClassUtils.isAssignFrom(Collection::class.java, targetType.type)) {
            val collection = CollectionFactory.createCollection<Any?>(targetType.type, source.size)

            // sourceType为Array的ComponentType
            val sourceElementType = sourceType.type.componentType
            // targetType为Collection的elementType
            val targetElementType = targetType.resolvableType.asCollection().getGenerics()[0].resolve(Any::class.java)

            // 对元素去进行类型转换, 添加到collection当中去
            source.forEach { collection.add(conversionService.convert(it, targetElementType)) }
            return collection
        }
        return null
    }
}