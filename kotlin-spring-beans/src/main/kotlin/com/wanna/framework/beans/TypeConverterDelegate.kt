package com.wanna.framework.beans

import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.lang.Nullable
import com.wanna.framework.util.ClassUtils
import com.wanna.framework.util.StringUtils
import java.beans.PropertyEditor
import java.util.Optional
import kotlin.jvm.Throws

/**
 * TypeConverter的委托工具类
 *
 * @param registry PropertyEditor的注册中心, 维护了大量的PropertyEditor
 */
class TypeConverterDelegate(private val registry: PropertyEditorRegistrySupport) {

    /**
     * 如果必要的话, 需要去完成类型的转换
     *
     * @param propertyName propertyName(可以为null)
     * @param oldValue 该属性的旧的值(可以为null)
     * @param newValue 该属性的新值(不能为null)
     * @param requiredType 需要转换成为的类型(不能为null)
     * @return 转换之后的属性值
     * @throws IllegalArgumentException 参数类型不匹配的话
     */
    @Nullable
    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> convertIfNecessary(
        @Nullable propertyName: String?,
        @Nullable oldValue: Any?,
        @Nullable newValue: Any?,
        requiredType: Class<T>?
    ): T? {
        return convertIfNecessary(propertyName, oldValue, newValue, requiredType, TypeDescriptor.valueOf(requiredType))
    }


    /**
     * 如果必要的话, 需要去完成类型的转换
     *
     * @param propertyName propertyName(可以为null)
     * @param oldValue 该属性的旧的值(可以为null)
     * @param newValue 该属性的新值(不能为null)
     * @param requiredType 需要转换成为的类型
     * @param typeDescriptor 转换成为的目标类型的对象的描述符信息
     * @return 转换之后的属性值
     * @throws IllegalArgumentException 参数类型不匹配的话
     */
    @Nullable
    @Throws(IllegalArgumentException::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> convertIfNecessary(
        @Nullable propertyName: String?,
        @Nullable oldValue: Any?,
        @Nullable newValue: Any?,
        requiredType: Class<T>?,
        @Nullable typeDescriptor: TypeDescriptor?
    ): T? {

        var editor = registry.findCustomEditor(requiredType)

        val conversionService = registry.getConversionService()
        // 寻找自定义的PropertyEditor去进行转换...

        // 如果没有找到合适的PropertyEditor, 但是ConversionService存在, 那么尝试使用ConversionService去进行类型转换...
        if (editor == null && (conversionService != null && newValue != null && typeDescriptor != null)) {
            val sourceTypeDescriptor = TypeDescriptor.forObject(newValue)

            // check ConversionService
            if (conversionService.canConvert(sourceTypeDescriptor, typeDescriptor)) {
                return conversionService.convert(newValue, sourceTypeDescriptor, typeDescriptor) as T?
            }
        }

        var convertedValue = newValue

        // 如果newValue并不是目标类型(requiredType)的对象
        // 那么尝试使用PropertyEditor去进行类型的转换...
        if (editor != null || (requiredType != null && !ClassUtils.isAssignableValue(requiredType, convertedValue))) {

            // 如果converterValue是字符串, 但是需要的是Collection<Class>或者是Collection<Enum>的话
            // 那么我们需要先去把converterValue去转换成为String[]
            if (typeDescriptor != null
                && requiredType != null
                && ClassUtils.isAssignable(Collection::class.java, requiredType)
                && convertedValue is String
            ) {
                val elementTypeDescriptor = typeDescriptor.getElementTypeDescriptor()
                if (elementTypeDescriptor != null) {
                    val type = elementTypeDescriptor.type
                    if (Class::class.java == type || ClassUtils.isAssignable(Enum::class.java, type)) {
                        convertedValue = StringUtils.commaDelimitedListToStringArray(convertedValue)
                    }
                }

                // 如果没有找到合适的Editor的话, 那么去寻找一个默认的PropertyEditor
                editor = editor ?: findDefaultEditor(requiredType)

                // 使用PropertyEditor去进行类型的转换...
                convertedValue = doConvertValue(oldValue, convertedValue, requiredType, editor)
            }
        }

        if (requiredType != null) {
            if (convertedValue != null) {
                if (requiredType == Any::class.java) {
                    return convertedValue as T?
                } else if (requiredType.isArray) {

                } else if (convertedValue is Collection<*>) {

                } else if (convertedValue is Map<*, *>) {

                }
            } else {
                // convertedValue==null
                if (requiredType == Optional::class.java) {
                    convertedValue = Optional.empty<Any>()
                }
            }

            if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
                if (conversionService != null && typeDescriptor != null) {
                    val sourceTypeDesc = TypeDescriptor.forObject(newValue)
                    if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
                        return conversionService.convert(newValue, sourceTypeDesc, typeDescriptor) as T?
                    }
                }
            }

        }

        // 使用Editor去进行转换...
        val defaultEditors = registry.defaultEditors
        if (defaultEditors != null && newValue is String) {
            editor = defaultEditors[requiredType!!]
            if (editor != null) {
                editor.asText = newValue
                convertedValue = editor.value
            }
        }
        // bugfix...这里如果直接检查isAssignFrom的话, 有可能会存在需要的最终结果当中,
        // 其中一个是基础类型、一个是包装类型, 从而导致问题, 因此我们还是使用cast去进行检查吧
        try {
            return convertedValue as T
        } catch (ex: Exception) {
            // 如果参数类型转换存在问题的话, 那么丢出去不合法参数异常...
            throw IllegalArgumentException(
                "参数类型不匹配, 需要的类型是[${ClassUtils.getQualifiedName(requiredType!!)}], 但是实际得到的是[${
                    ClassUtils.getQualifiedName(convertedValue?.javaClass ?: Unit::class.java)
                }]"
            )
        }
    }

    @Nullable
    private fun findDefaultEditor(requiredType: Class<*>?): PropertyEditor? {
        var editor: PropertyEditor? = null
        if (requiredType != null) {
            editor = registry.getDefaultEditor(requiredType)
        }
        return editor
    }

    /**
     * 利用[PropertyEditor]去将value去转换成为预期的类型(如果必要的话, 优先从String去进行转换)
     *
     * @param oldValue oldValue(如果不存在旧值, 那么为null)
     * @param newValue newValue
     * @param requiredType 需要的目标类型
     * @param editor PropertyEditor
     * @return 经过类型转换之后得到的值
     */
    @Suppress("UNCHECKED_CAST")
    private fun doConvertValue(
        @Nullable oldValue: Any?,
        @Nullable newValue: Any?,
        @Nullable requiredType: Class<*>?,
        @Nullable editor: PropertyEditor?
    ): Any? {
        var editorToUse = editor
        var convertedValue = newValue

        // 如果convertedValue不是String, 那么尝试使用editor去进行转换一下...
        if (editorToUse != null && convertedValue !is String) {
            editorToUse.value = convertedValue
            val newConvertedValue = editorToUse.value

            if (newConvertedValue != convertedValue) {
                convertedValue = newConvertedValue
                editorToUse = null
            }
        }

        var returnValue = convertedValue

        // 如果requiredType不是数组, 但是当前是String[], 那么需要把String[]去转换成为String
        if (requiredType != null && !requiredType.isArray && convertedValue is Array<*> && convertedValue.isArrayOf<String>()) {
            convertedValue = StringUtils.arrayToCommaDelimitedString(convertedValue as Array<String>);
        }

        if (convertedValue is String) {
            if (editor != null) {
                return doConvertTextValue(oldValue, convertedValue, editor)
            } else if (requiredType == String::class.java) {
                returnValue = convertedValue
            }
        }

        return newValue
    }


    /**
     * 执行对于textValue的类型转换
     *
     * @param oldValue oldValue
     * @param newTextValue newValue(文本格式的value)
     * @param editor 类型转换的PropertyEditor
     * @return 利用PropertyEditor经过类型转换之后的值
     */
    private fun doConvertTextValue(@Nullable oldValue: Any?, newTextValue: String, editor: PropertyEditor): Any {

        editor.asText = newTextValue
        return editor.value
    }
}