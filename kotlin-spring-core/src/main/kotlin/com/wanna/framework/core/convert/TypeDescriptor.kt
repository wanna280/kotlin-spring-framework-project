package com.wanna.framework.core.convert

import com.wanna.framework.core.MethodParameter
import com.wanna.framework.core.ResolvableType
import com.wanna.framework.lang.Nullable
import com.wanna.framework.util.ClassUtils
import java.io.Serializable
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field

/**
 * Spring针对于转换工作, 去封装的一个类型描述符, 主要新增泛型的支持;
 * 如果使用Class的方式去进行解析, 那么无法获取到泛型的类型, 但是如果使用ResolvableType, 则支持去进行泛型解析;
 *
 * 比如解析"Collection<String> --> List<String>", 就需要用到泛型的相关的支持
 *
 * @param resolvableType ResolvableType(with Generics if Necessary)
 *
 * @see ConversionService.canConvert
 * @see ConversionService.convert
 */
open class TypeDescriptor(
    val resolvableType: ResolvableType,
    val type: Class<*>,
    private val annotatedElement: AnnotatedElementAdapter
) : Serializable {

    /**
     * 基于Property去构建TypeDescriptor
     *
     * @param property Property
     */
    constructor(property: Property) : this(
        ResolvableType.forMethodParameter(property.methodParameter),
        ResolvableType.forMethodParameter(property.methodParameter).resolve(property.type),
        AnnotatedElementAdapter(property.annotations)
    )

    /**
     * 基于字段Field去构建TypeDescriptor
     *
     * @param field Field
     */
    constructor(field: Field) : this(
        ResolvableType.forField(field),
        ResolvableType.forField(field).resolve(field.type),
        AnnotatedElementAdapter(field.annotations)
    )

    /**
     * 基于方法参数去构建TypeDescriptor
     *
     * @param methodParameter 方法参数MethodParameter
     */
    constructor(methodParameter: MethodParameter) : this(
        ResolvableType.forMethodParameter(methodParameter),
        ResolvableType.forMethodParameter(methodParameter).resolve(methodParameter.getNestedParameterType()),
        AnnotatedElementAdapter(
            // -1代表方法返回值, 直接取方法上的注解信息
            if (methodParameter.getParameterIndex() == -1) methodParameter.getMethodAnnotations()
            else methodParameter.getAnnotations()
        )
    )

    /**
     * 基于ResolvableType和type和annotations去进行构建TypeDescriptor
     *
     * @param resolvableType ResolvableType
     * @param type type, 为null的话, 可以根据ResolvableType去进行计算
     * @param annotations 注解信息
     */
    constructor(resolvableType: ResolvableType, type: Class<*>?, annotations: Array<Annotation>) : this(
        resolvableType,
        type ?: resolvableType.toClass(),
        AnnotatedElementAdapter(annotations)
    )


    /**
     * 获取集合/数组元素类型的描述符
     *
     * @return 元素类型的[TypeDescriptor]
     */
    @Nullable
    open fun getElementTypeDescriptor(): TypeDescriptor? {
        if (type.isArray) {
            return TypeDescriptor(this.resolvableType.getComponentType(), null, getAnnotations())
        } else if (ClassUtils.isAssignFrom(Collection::class.java, type)) {
            return TypeDescriptor(this.resolvableType.asCollection().getGenerics()[0], null, getAnnotations())
        }
        return null
    }

    open fun getAnnotations(): Array<Annotation> {
        return annotatedElement.annotations
    }

    /**
     * AnnotatedElement的适配器, 通过将给定的注解数组, 去包装适配成为AnnotatedElement
     *
     * @param annotations 待适配的注解列表
     */
    class AnnotatedElementAdapter(private val annotations: Array<Annotation>?) : AnnotatedElement,
        Serializable {

        override fun isAnnotationPresent(annotationClass: Class<out Annotation>): Boolean {
            for (annotation in getAnnotations()) {
                if (annotation.annotationClass.java == annotationClass) {
                    return true
                }
            }
            return false
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Annotation?> getAnnotation(annotationClass: Class<T>): T? {
            for (annotation in getAnnotations()) {
                if (annotation.annotationClass.java == annotationClass) {
                    return annotation as T
                }
            }
            return null
        }

        override fun getAnnotations(): Array<Annotation> {
            return this.annotations ?: EMPTY_ANNOTATION_ARRAY
        }

        override fun getDeclaredAnnotations(): Array<Annotation> {
            return getAnnotations()
        }
    }

    companion object {

        /**
         * 空的注解数组常量
         */
        @JvmStatic
        private val EMPTY_ANNOTATION_ARRAY = emptyArray<Annotation>()

        @JvmStatic
        private val CACHED_COMMON_TYPES = arrayOf(
            Byte::class.java, Byte::class.javaObjectType,
            Boolean::class.java, Boolean::class.javaObjectType,
            Short::class.java, Short::class.javaObjectType,
            Int::class.java, Int::class.javaObjectType,
            Long::class.java, Long::class.javaObjectType,
            Double::class.java, Long::class.javaObjectType,
            Char::class.java, Char::class.javaObjectType,
            Float::class.java, Float::class.javaObjectType,
            String::class.java, Any::class.java
        )

        /**
         * 常用类型的TypeDescriptor缓存
         */
        @JvmStatic
        private val commonTypesCache = LinkedHashMap<Class<*>, TypeDescriptor>()

        init {
            // 为所有的要去进行缓存的类型, 去构建出来缓存
            for (type in CACHED_COMMON_TYPES) {
                commonTypesCache[type] = valueOf(type)
            }
        }

        /**
         * 构建[TypeDescriptor]的工厂方法
         *
         * @param clazz 要去进行获取[TypeDescriptor]的类
         * @return 为该类去获取到的[TypeDescriptor]描述信息
         */
        @JvmStatic
        fun valueOf(@Nullable clazz: Class<*>?): TypeDescriptor {
            val type = clazz ?: Any::class.java

            // 如果是基础数据类型, 直接走缓存去进行获取
            val desc = commonTypesCache[type]
            return desc ?: TypeDescriptor(
                ResolvableType.forClass(type),
                type,
                AnnotatedElementAdapter(type.annotations)
            )
        }

        /**
         * 为给定的方法参数去构建TypeDescriptor
         *
         * @param parameter 方法参数MethodParameter
         * @return 针对该方法参数去进行构建的类型描述信息TypeDescriptor
         */
        @JvmStatic
        fun forMethodParameter(parameter: MethodParameter): TypeDescriptor {
            return TypeDescriptor(parameter)
        }

        /**
         * 为给定的字段去构建TypeDescriptor
         *
         * @param field Field字段
         * @return 针对该字段去进行构建的类型描述信息TypeDescriptor
         */
        @JvmStatic
        fun forField(field: Field): TypeDescriptor {
            return TypeDescriptor(field)
        }

        /**
         * 为给定的类去获取到类型的描述信息的[TypeDescriptor]
         *
         * @param type type
         * @return TypeDescriptor
         */
        @JvmStatic
        fun forClass(type: Class<*>): TypeDescriptor = valueOf(type)

        /**
         * 为给定的对象的类去获取到[TypeDescriptor]
         *
         * @param source 要去进行描述的目标对象
         * @return TypeDescriptor
         */
        @JvmStatic
        fun forObject(source: Any): TypeDescriptor = valueOf(source.javaClass)
    }

}