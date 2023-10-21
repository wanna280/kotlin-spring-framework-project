package com.wanna.framework.beans

import com.wanna.framework.core.MethodParameter
import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.lang.Nullable
import java.lang.reflect.Field
import kotlin.jvm.Throws

/**
 * 提供TypeConverter的默认实现
 */
open class TypeConverterSupport : PropertyEditorRegistrySupport(), TypeConverter {

    /**
     * TypeConverter的委托工具类, 同时组合ConversionService和PropertyEditor去进行类型的转换
     */
    @Nullable
    protected var delegate: TypeConverterDelegate? = null

    /**
     * 如果必要的话, 将给定的属性值, 去转换成为目标类型
     *
     * @param value 待转换的值
     * @param requiredType 转换的目标类型
     * @return 经过转换之后的值
     */
    @Nullable
    @Throws(TypeMismatchException::class)
    override fun <T : Any> convertIfNecessary(@Nullable value: Any?, @Nullable requiredType: Class<T>?): T? {
        return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType))
    }

    @Nullable
    @Throws(TypeMismatchException::class)
    override fun <T : Any> convertIfNecessary(
        value: Any?,
        requiredType: Class<T>?,
        methodParameter: MethodParameter?
    ): T? {
        val td = if (methodParameter != null) TypeDescriptor(methodParameter) else TypeDescriptor.valueOf(requiredType)
        return convertIfNecessary(value, requiredType, td)
    }

    @Nullable
    @Throws(TypeMismatchException::class)
    override fun <T : Any> convertIfNecessary(value: Any?, requiredType: Class<T>?, field: Field?): T? {
        val td = if (field != null) TypeDescriptor(field) else TypeDescriptor.valueOf(requiredType)
        return convertIfNecessary(value, requiredType, td)
    }

    @Throws(TypeMismatchException::class)
    @Nullable
    override fun <T : Any> convertIfNecessary(
        @Nullable value: Any?,
        @Nullable requiredType: Class<T>?,
        @Nullable typeDescriptor: TypeDescriptor?
    ): T? {
        try {
            return delegate?.convertIfNecessary(null, null, value, requiredType, typeDescriptor)
        } catch (ex: IllegalArgumentException) {
            throw TypeMismatchException(value, requiredType, ex)
        }
    }
}