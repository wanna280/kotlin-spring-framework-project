package com.wanna.framework.core.convert.support

import com.wanna.framework.core.ResolvableType
import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.core.convert.converter.ConditionalConverter
import com.wanna.framework.core.convert.converter.ConditionalGenericConverter
import com.wanna.framework.core.convert.converter.Converter
import com.wanna.framework.core.convert.converter.GenericConverter
import com.wanna.framework.core.convert.converter.GenericConverter.ConvertiblePair
import com.wanna.framework.lang.Nullable
import com.wanna.framework.util.ClassUtils
import java.lang.reflect.Array
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 这是一个通用(带泛型)的ConversionService, 它为Converter的注册中心以及可以被配置的ConversionService提供了模板的实现;
 * 它已经可以支持去进行类型的转换, 但是它内部并没有添加默认的Converter, 也就是说, 它本身并不能工作, 需要往内部加入Converter, 才能完成
 * 类型的转换工作, 在DefaultConversionService当中, 就添加了一些默认的Converter去完成类型转换的处理
 *
 * @see DefaultConversionService
 * @see ConfigurableConversionService
 */
open class GenericConversionService : ConfigurableConversionService {

    /**
     * Converter的注册中心, 内部维护了全部的Converter的列表
     */
    private val converters = Converters()

    companion object {

        /**
         * 不进行任何操作的Converter
         */
        @JvmStatic
        private val NO_OP_CONVERTER = NoOpConverter("NO_OP")
    }

    /**
     * 判断Converter注册中心当中, 是否存在有这样的Converter, 能去完成从sourceType->targetType的类型转换?
     *
     * @param sourceType sourceType
     * @param targetType targetType
     * @return 是否能支持从sourceType->targetType?
     */
    override fun canConvert(sourceType: Class<*>, targetType: Class<*>): Boolean {
        return canConvert(TypeDescriptor.forClass(sourceType), TypeDescriptor.forClass(targetType))
    }

    /**
     * 判断Converter注册中心当中, 是否存在有这样的Converter, 能去完成从sourceType->targetType的类型转换?
     *
     * @param sourceType sourceType
     * @param targetType targetType
     * @return 是否能支持从sourceType->targetType?
     */
    override fun canConvert(@Nullable sourceType: TypeDescriptor?, targetType: TypeDescriptor): Boolean {
        sourceType ?: return true
        val converter = getConverter(sourceType, targetType)
        return converter != null
    }

    /**
     * 将source转换为targetType的类型转换
     *
     * @param targetType 要将source对象转换成什么类型
     * @param source 要去进行转换的对象
     * @return 转换完成的对象(如果无法完成转换, 那么return null)
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> convert(source: Any?, targetType: Class<T>): T? {
        return convert(source, TypeDescriptor.forClass(targetType)) as T?
    }

    /**
     * 将source转换为targetType的类型转换
     *
     * @param targetType 要将source对象转换成什么类型
     * @param source 要去进行转换的对象
     * @return 转换完成的对象(如果无法完成转换, 那么return null)
     */
    override fun convert(source: Any?, targetType: TypeDescriptor): Any? {
        source ?: return null  // if null, return null
        return convert(source, TypeDescriptor.forObject(source), targetType)
    }

    /**
     * 将source, 转换为目标类型(targetType), 支持去解析targetType的泛型信息
     *
     * @param source 源对象(可以为空)
     * @param sourceType sourceType
     * @param targetType 要转换的目标类型(TypeDescriptor, 支持泛型的解析)
     * @return 转换之后的结果
     */
    @Nullable
    override fun convert(
        @Nullable source: Any?,
        @Nullable sourceType: TypeDescriptor?,
        targetType: TypeDescriptor
    ): Any? {
        sourceType ?: return null // TODO
        // 获取到支持将sourceType-->targetType的转换器列表
        val converter = getConverter(sourceType, targetType)
        if (converter != null) {
            return converter.convert(source, sourceType, targetType)
        }
        return null
    }

    /**
     * 添加一个自定义的Converter, 自动去解析Converter的泛型类型去进行注册
     *
     * @param converter 你想要添加的Converter
     * @throws IllegalStateException 如果无法解析出来Converter的泛型类型
     */
    override fun addConverter(converter: Converter<*, *>) {
        // 解析要添加的Converter的泛型类型
        val generics = ResolvableType.forClass(converter::class.java).`as`(Converter::class.java).getGenerics()
        if (generics.isEmpty()) {
            throw IllegalStateException("无法解析添加的Converter的泛型类型[$converter]")
        }
        // 将Converter包装成为GenericConverter, 并添加该Converter能处理的映射类型...
        addConverter(ConverterAdapter(converter).addConvertibleType(generics[0].resolve()!!, generics[1].resolve()!!))
    }

    /**
     * 往"sourceType-->targetType"的映射当中去添加一个Converter,
     * 因为Converter本身并不包含泛型信息, 因此, 我们应该尝试去进行converter的父类当中的泛型的类型去进行解析
     *
     * @param sourceType sourceType
     * @param targetType targetType
     * @param converter 你想要添加的Converter
     */
    override fun <S : Any, T : Any> addConverter(
        sourceType: Class<S>,
        targetType: Class<T>,
        converter: Converter<in S, out T>
    ) {
        addConverter(ConverterAdapter(converter).addConvertibleType(sourceType, targetType))
    }

    /**
     * 直接添加一个GenericConverter到Converter注册中心当中
     *
     * @param converter 你想要添加的GenericConverter
     */
    override fun addConverter(converter: GenericConverter) {
        converters.addConverter(converter)
    }

    /**
     * 根据"sourceType-->targetType"的映射关系, 去移除该映射下的Converter列表
     *
     * @param sourceType sourceType
     * @param targetType targetType
     */
    override fun removeConvertible(sourceType: Class<*>, targetType: Class<*>) {
        this.converters.removeConverter(sourceType, targetType)
    }

    /**
     * 获取到针对sourceType->targetType之间的转换的Converter
     *
     * @param sourceType sourceType
     * @param targetType targetType
     * @return 获取到的用于进行转换的Converter(找不到的话, return null)
     */
    @Nullable
    open fun getConverter(sourceType: TypeDescriptor, targetType: TypeDescriptor): GenericConverter? {
        // TODO, cache
        // 从Converter注册中心(Converters)当中去寻找到sourceType->targetType的Converter
        var converter = this.converters.find(sourceType, targetType)
        if (converter == null) {
            converter = getDefaultConverter(sourceType, targetType)
        }
        if (converter != null) {
            return converter
        }
        return null
    }

    /**
     * 获取默认的Converter
     *
     * @param sourceType sourceType
     * @param targetType targetType
     * @return 默认的Converter(如果获取不到的话, return null)
     */
    @Nullable
    fun getDefaultConverter(sourceType: TypeDescriptor, targetType: TypeDescriptor): GenericConverter? {
        // 如果sourceType->targetType可以转换成功的话, 那么return NO_OP_CONVERTER
        // 比如sourceType=String, targetType=Object, 这种很明显是可以去进行转换的...
        return if (ClassUtils.isAssignFrom(targetType.type, sourceType.type)) NO_OP_CONVERTER else null
    }

    /**
     * Converter的注册中心
     */
    private class Converters {
        companion object {
            /**
             * 空的ConvertersForPair列表
             */
            @JvmStatic
            private val EMPTY_CONVERTERS = ConvertersForPair()
        }

        /**
         * 全局的Converter列表
         */
        val globalConverters = CopyOnWriteArraySet<GenericConverter>()

        /**
         * Converter注册中心当中维护的Converter列表, key-(sourceType->targetType)的Pair映射对,
         * value-能完成(sourceType->targetType)对应的Pair映射的Converter列表
         */
        val converters = ConcurrentHashMap<ConvertiblePair, ConvertersForPair>()

        /**
         * 注册Converter, key是ConvertibleType, value是GenericConverter;
         * 将GenericConverter可以转换的类型拿出来作为Key, 去完成Mapping->Converters的映射关系注册
         *
         * @param converter GenericConverter
         */
        fun addConverter(converter: GenericConverter) {
            val convertibleTypes = converter.getConvertibleTypes()
            // 如果convertibleTypes返回为null, 那么添加到GlobalConverters当中
            if (convertibleTypes == null) {
                if (converter !is ConditionalConverter) {
                    throw IllegalStateException("Only conditional converters may return null convertible types")
                }
                globalConverters.add(converter)
            } else {
                // 遍历所有的convertibleTypes, 去进行注册
                convertibleTypes.forEach { converters[it] = getMatchableConverters(it).addConverter(converter) }
            }
        }

        /**
         * 根据ConvertiblePair去获取到ConvertersForPair(如果不存在的话, 创建一个ConvertersForPair)
         *
         * @param convertiblePair convertiblePair
         * @return ConvertersForPair
         */
        private fun getMatchableConverters(convertiblePair: ConvertiblePair): ConvertersForPair {
            return this.converters.computeIfAbsent(convertiblePair) {
                ConvertersForPair()
            }
        }

        /**
         * 寻找到从sourceType->targetType的合适的类型转换的Converter
         *
         * @param sourceType sourceType
         * @param targetType targetType
         * @return 寻找到的合适的类型转换的Converter(找不到return null)
         */
        @Nullable
        fun find(sourceType: TypeDescriptor, targetType: TypeDescriptor): GenericConverter? {

            // 获取到sourceType对应的候选继承关系
            val sourceCandidates = getClassHierarchy(sourceType.type)
            // 获取到targetType对应的候选继承关系
            val targetCandidates = getClassHierarchy(targetType.type)

            // 遍历sourceType和targetType的所有的父类, 尝试去进行匹配...
            for (sourceCandidate in sourceCandidates) {
                for (targetCandidate in targetCandidates) {

                    // 根据Pair去进行精确查找
                    val convertersForPair = ConvertiblePair(sourceCandidate, targetCandidate)
                    val converter = getRegisteredConverter(sourceType, targetType, convertersForPair)
                    if (converter != null) {
                        return converter
                    }
                }
            }
            return null
        }

        /**
         * 从已经注册的Converter列表当中, 找到合适的可以处理sourceType->targetType之间的转换关系的Converter
         *
         * @param sourceType sourceType
         * @param targetType targetType
         * @param convertiblePair sourceType->targetType之前的映射关系Pair
         * @return 寻找到的合适的Converter(没有找到return null)
         */
        @Nullable
        private fun getRegisteredConverter(
            sourceType: TypeDescriptor,
            targetType: TypeDescriptor,
            convertiblePair: ConvertiblePair
        ): GenericConverter? {

            // 1.根据ConvertiblePair去寻找到合适的已经完成手动完成注册的Converter列表
            val convertersForPair = this.converters[convertiblePair]
            if (convertersForPair != null) {
                // 从Converter列表当中, 决策最终要使用哪个Converter
                val converter = convertersForPair.getConverter(sourceType, targetType)
                if (converter != null) {
                    return converter
                }
            }

            // 2.根据GlobalConverters再去fallback寻找一遍, 进行动态匹配
            for (globalConverter in globalConverters) {
                if (globalConverter is ConditionalConverter && globalConverter.matches(sourceType, targetType)) {
                    return globalConverter
                }
            }

            // fallback也寻找不到的话, return null
            return null
        }

        /**
         * 获取给定的类的所有的继承关系的类(对于数组的话, 会考虑父类数组的情况, 比如ArrayList[], 会添加List[]和Collection[]的情况)
         * 最终获取到的类元素的位置是, 先按照父类的顺序去进行BFS遍历, 再按照接口的顺序去进行BFS遍历
         *
         * @param type 待获取继承关系的类
         * @return 和该类有继承关系的类的列表
         */
        private fun getClassHierarchy(type: Class<*>): List<Class<*>> {
            val hierarchy = ArrayList<Class<*>>()
            val visited = LinkedHashSet<Class<*>>()

            // 先把当前类去添加到hierarchy列表当中
            addToClassHierarchy(0, ClassUtils.resolvePrimitiveIfNecessary(type), false, hierarchy, visited)

            val array = type.isArray
            var i = 0
            while (i < hierarchy.size) {
                var candidate = hierarchy[i]

                // 这里获取到元素类型去进行添加, 原因在于比如ArrayList[], 需要添加List[]和Collection[]到hierarchy集合当中
                candidate = if (array) candidate.componentType else ClassUtils.resolvePrimitiveIfNecessary(candidate)

                // 添加直接父类, 对于后续的所有的Class都将直接插入到当前type的后面(index+1)...
                val superclass = candidate.superclass
                if (superclass != null && superclass != Any::class.java && superclass != Enum::class.java) {
                    addToClassHierarchy(i + 1, candidate.superclass, array, hierarchy, visited)
                }

                // 添加所有的接口到hierarchy当中
                addInterfacesToClassHierarchy(candidate, array, hierarchy, visited)
                i++
            }

            // 如果type是枚举的话, 需要往hierarchy当中去添加Enum[]和Enum的情况
            if (type.isEnum) {
                addToClassHierarchy(hierarchy.size, Enum::class.java, array, hierarchy, visited)
                addToClassHierarchy(hierarchy.size, Enum::class.java, false, hierarchy, visited)
            }

            // 在最后, 考虑添加Object[]和Object到hierarchy当中(原来是数组会添加Object[]和Object, 原来不是数组只会添加Object)
            addToClassHierarchy(hierarchy.size, Any::class.java, array, hierarchy, visited)
            addToClassHierarchy(hierarchy.size, Any::class.java, false, hierarchy, visited)
            return hierarchy
        }

        /**
         * 将给定的类type的所有直接实现的接口, 全部添加到hierarchy列表当中
         *
         * @param type 要添加接口的类type
         * @param asArray 是否需要将type转换成为数组去进行添加
         * @param hierarchy 继承关系的类的集合, 待添加元素
         * @param visited 已经访问过的类的集合(避免重复处理)
         */
        private fun addInterfacesToClassHierarchy(
            type: Class<*>,
            asArray: Boolean,
            hierarchy: MutableList<Class<*>>,
            visited: MutableSet<Class<*>>
        ) {
            for (implementedInterface in type.interfaces) {
                addToClassHierarchy(hierarchy.size, implementedInterface, asArray, hierarchy, visited)
            }
        }

        /**
         * 将给定的类type去添加到hierarchy列表当中
         *
         * @param index 要添加到的位置index
         * @param type 要去进行添加的类
         * @param asArray 是否要将该类去转换成为数组类去添加(比如type=String, asArray=true, 那么需要添加String[]到hierarchy列表当中)
         * @param hierarchy 待添加继承关系的类集合hierarchy
         * @param visited 已经访问过的类的集合(如果访问过, 不再添加到hierarchy当中)
         */
        private fun addToClassHierarchy(
            index: Int,
            type: Class<*>,
            asArray: Boolean,
            hierarchy: MutableList<Class<*>>,
            visited: MutableSet<Class<*>>
        ) {
            val typeToAdd = if (asArray) Array.newInstance(type, 0).javaClass else type
            if (visited.add(typeToAdd)) {
                hierarchy.add(index, typeToAdd)
            }
        }

        /**
         * 根据(sourceType->targetType)的映射Mapping, 去移除掉该Mapping相应的Converter列表
         *
         * @param sourceType sourceType
         * @param targetType targetType
         */
        fun removeConverter(sourceType: Class<*>, targetType: Class<*>) {
            this.converters.remove(ConvertiblePair(sourceType, targetType))
        }
    }

    /**
     * 这是一个映射(Pair,sourceType->targetType的映射)对应的Converter列表的注册中心;
     * 比如一个Integer->String的映射可能会存在有多个Converter都能去进行转换...这里就注册负责维护多个Converter的列表
     *
     * @see ConvertiblePair
     */
    class ConvertersForPair {

        /**
         * Converters
         */
        private val converters = ConcurrentLinkedDeque<GenericConverter>()

        /**
         * 添加一个Converter
         *
         * @param converter 需要添加的Converter
         */
        fun addConverter(converter: GenericConverter): ConvertersForPair {
            converters += converter
            return this
        }

        /**
         * 从Converter列表当中, 去寻找打一个合适的支持从sourceType->targetType进行转换的Converter
         *
         * @param sourceType sourceType
         * @param targetType targetType
         * @return 找到的处理这样的类型转换的Converter(没有找到合适的, return null)
         */
        @Nullable
        fun getConverter(sourceType: TypeDescriptor, targetType: TypeDescriptor): GenericConverter? {
            for (converter in this.converters) {
                if (converter !is ConditionalGenericConverter) {
                    return converter
                }
                if (converter.matches(sourceType, targetType)) {
                    return converter
                }
            }
            return null
        }
    }

    /**
     * 这是一个Converter的Adapter, 它可以将普通的Converter转换为GenericConverter去进行包装
     *
     * @param converter 想要去进行包装的Converter
     */
    @Suppress("UNCHECKED_CAST")
    private class ConverterAdapter(private val converter: Converter<*, *>) : GenericConverter {
        /**
         * 包装的普通的Converter, 可以支持的转换的类型映射
         */
        private val convertibleTypes = HashSet<ConvertiblePair>()

        /**
         * 添加可以转换的类型到列表当中("sourceType->targetType"的映射关系)
         *
         * @param sourceType sourceType
         * @param targetType targetType
         * @return this
         */
        fun addConvertibleType(sourceType: Class<*>, targetType: Class<*>): ConverterAdapter {
            this.convertibleTypes.add(ConvertiblePair(sourceType, targetType))
            return this
        }

        /**
         * 获取当前Converter支持转换的类型映射(Mapping)列表
         *
         * @return 当前这个Converter支持的转换的映射列表
         */
        override fun getConvertibleTypes(): Set<ConvertiblePair> = convertibleTypes

        /**
         * 将source去进行类型的转换("sourceType-->targetType")
         *
         * @param source 要去进行转换的对象
         * @param sourceType sourceType
         * @param targetType targetType
         * @return 经过Converter转换之后的对象
         */
        override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? =
            (converter as Converter<Any, Any>).convert(source)

        override fun toString() = getConvertibleTypes().toString()
    }

    /**
     * 不进行任何操作的Converter
     */
    private class NoOpConverter(val name: String) : GenericConverter {
        override fun getConvertibleTypes(): Set<ConvertiblePair>? = null

        override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
            return source
        }

        override fun toString(): String = name
    }
}