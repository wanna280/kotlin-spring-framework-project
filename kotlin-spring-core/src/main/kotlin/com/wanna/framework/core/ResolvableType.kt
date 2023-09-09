package com.wanna.framework.core

import com.wanna.framework.lang.Nullable
import com.wanna.framework.util.ClassUtils
import java.io.Serializable
import java.lang.reflect.*

/**
 * 对于Java的Type提供解析的工具类
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/8/27
 * @see Type
 */
open class ResolvableType {

    /**
     * 当前Class, 也就是不带泛型的Class, 可能为null
     */
    @Nullable
    private var resolved: Class<*>? = null

    /**
     * 需要进行解析的Type
     */
    private var type: Type? = null

    /**
     * 数组的元素类型, 如果无法推断出来, 那么值为null
     */
    @Nullable
    private var componentType: ResolvableType? = null

    /**
     * 泛型变量的解析器
     */
    @Nullable
    private var variableResolver: VariableResolver? = null

    /**
     * TypeProvider
     */
    @Nullable
    private var typeProvider: TypeProvider? = null

    /**
     * 泛型列表
     */
    private var generics: Array<ResolvableType>? = null

    /**
     * 接口列表
     */
    private var interfaces: Array<ResolvableType>? = null

    /**
     * 父类
     */
    private var superType: ResolvableType? = null

    private constructor(clazz: Class<*>?) {
        this.resolved = clazz ?: Any::class.java
        this.type = resolved
        this.typeProvider = null
        this.variableResolver = null
        this.componentType = null
    }

    private constructor(
        type: Type?,
        typeProvider: TypeProvider?,
        variableResolver: VariableResolver?,
        componentType: ResolvableType?
    ) {
        this.type = type
        this.typeProvider = typeProvider
        this.variableResolver = variableResolver
        this.resolved = resolveClass()
        this.componentType = componentType
    }

    private constructor(
        type: Type?,
        typeProvider: TypeProvider?,
        variableResolver: VariableResolver?
    ) {
        this.type = type
        this.typeProvider = typeProvider
        this.variableResolver = variableResolver
        this.resolved = resolveClass()
        this.componentType = null
    }

    /**
     * 根据type, 去执行对于[Class]的解析
     *
     * @return 解析得到的Class
     */
    private fun resolveClass(): Class<*>? {
        // 如果为EmptyType, 那么return null
        if (this.type == EmptyType.INSTANCE) {
            return null
        }
        // 如果type不存在, 那么直接return null, 无法解析出来合适的Class
        this.type ?: return null

        if (this.type is Class<*>) {
            return this.type as Class<*>
        }
        // 泛型数组, 比如E[], T[]
        if (this.type is GenericArrayType) {
            // 获取到元素类型E, 并完成ComponentType的解析...
            val resolvedComponent = getComponentType().resolve() ?: return null

            // 根据元素类型去创建数组, 再去getClass...
            return java.lang.reflect.Array.newInstance(resolvedComponent, 0)::class.java
        }
        // 非泛型数组
        return resolveType().resolve()
    }

    /**
     * 获取原始的Type
     *
     * @return type
     */
    open fun getType(): Type {
        return this.type ?: throw IllegalStateException("type is null")
    }

    /**
     * 获取当前类型对应的原始的JavaClass(比如List<String>, 将会返回List), 如果无法获取的话, return null
     *
     * @return Raw JavaClass
     */
    open fun getRawClass(): Class<*>? {
        if (this.resolved == this.type) {
            return this.resolved
        }
        var rawType = this.type
        if (rawType is ParameterizedType) {
            rawType = rawType.rawType
        }
        return if (rawType is Class<*>) rawType else null
    }

    /**
     * 返回解析完成得到的[Class], 如果没有已经完成解析的[Class], 那么返回Object.class
     *
     * @return 已经完成解析的Class(或者Object.class)
     */
    open fun toClass(): Class<*> {
        return resolve(Any::class.java)
    }

    /**
     * 返回解析得到的Class, 如果没有已经解析完成的Class, 那么返回给定的fallback的Class
     *
     * @return 已经完成解析的Class(或者给定的fallback的Class)
     */
    open fun resolve(fallback: Class<*>): Class<*> {
        return this.resolved ?: fallback
    }

    /**
     * 获取嵌套层级的Type信息
     *
     * * 1.比如针对List<Set<String>>这种情况, nestingLevel=1将会获取到List,
     * nestingLevel=2将会获取到Set, nestingLevel=3将会获取到String.
     * * 2.针对String[]这种情况, nestingLevel=1可以获取到String[], nestingLevel=2可以获取到String.
     * * 3.某些层级可能存在有多个泛型, 比如Map<String,Integer>, 这种情况获取到的是最后一个泛型, 也就是Integer
     * * 4.如果当前类没有泛型, 那么也支持从父类当中去进行寻找泛型...
     *
     * @param nestingLevel 要获取泛型的具体嵌套层级
     * @return 获取到的嵌套层级的泛型(可能为NONE)
     */
    open fun getNested(nestingLevel: Int): ResolvableType {
        return getNested(nestingLevel, null)
    }

    /**
     * 获取嵌套层级的Type信息
     *
     * * 1.比如针对List<Set<String>>这种情况, nestingLevel=1将会获取到List,
     * nestingLevel=2将会获取到Set, nestingLevel=3将会获取到String.
     * * 2.针对String[]这种情况, nestingLevel=1可以获取到String[], nestingLevel=2可以获取到String.
     * * 3.针对typeIndexesPerLevel, 用于获取获取某个层级(key)需要获取第几个(value)泛型, 比如Map<String,Integer>,
     * 例如指定value为1, 那么代表要获取到Integer的泛型, value为0, 那么代表要获取到String的泛型.
     * 如果typeIndexesPerLevel当中没有指定当前level要使用的泛型, 默认取最后一个泛型去进行计算, 也就是Integer
     * * 4.如果当前类没有泛型, 那么也支持从父类当中去进行寻找泛型...
     *
     * @param nestingLevel 要获取泛型的具体嵌套层级
     * @param typeIndexesPerLevel 某个层级要取第几个泛型? key-层级, value-泛型index
     * @return 获取到的嵌套层级的泛型(可能为NONE)
     */
    open fun getNested(nestingLevel: Int, typeIndexesPerLevel: Map<Int, Int>?): ResolvableType {
        var result = this
        for (i in 2..nestingLevel) {
            // 如果是数组的话, 那么获取它的元素类型, 并消耗一个层级
            if (result.isArray()) {
                result = result.getComponentType()
            } else {

                // 如果当前result不为NONE, 但是不存在有泛型的话, 那么尝试从superType去进行获取
                while (result != NONE && !result.hasGenerics()) {
                    result = result.getSuperType()
                }

                // 如果typeIndexesPerLevel当中指定了要读取当前层级要使用第几个索引的话
                // 那么取自定义的, 如果没有自定义的话, 那么取默认的层级...
                val index = typeIndexesPerLevel?.get(i) ?: (result.getGenerics().size - 1)
                result = result.getGeneric(index)
            }
        }
        return result
    }

    /**
     * 根据给定的indexes去返回对应的泛型参数的ResolvableType,
     * 例如Map<Integer, Map<String, Double>>, getGeneric(0)将会得到Integer,
     * getGeneric(1,0)将会得到String, getGeneric(1,1)将会得到Double.
     *
     * 如果没有给定indexes, 那么将会返回第一个泛型参数的ResolvableType
     *
     * @param indexes indexes
     * @return 泛型参数的ResolvableType(找不到的话, 返回NONE)
     */
    open fun getGeneric(vararg indexes: Int): ResolvableType {
        var generics = getGenerics()
        if (indexes.isEmpty()) {
            return if (generics.isEmpty()) NONE else generics[0]
        }

        var generic: ResolvableType = this
        for (index in indexes) {
            generics = generic.getGenerics()
            if (index < 0 || index >= generics.size) {
                return NONE
            }
            generic = generics[index]
        }
        return generic
    }

    /**
     * 获取该类型[ResolvableType]对应的泛型列表
     *
     * @return 当前类型的泛型列表
     */
    open fun getGenerics(): Array<ResolvableType> {
        if (this == NONE) {
            return EMPTY_TYPES_ARRAY
        }
        var generics = this.generics
        if (generics == null) {
            if (this.type is Class<*>) {

                // 如果当前类是一个抽象类, 那么通过typeParameters, 可以获取到抽象类定义的泛型
                // 例如List<E>, 在这里typeParameters可以拿到E, 元素类型为TypeVariable
                val typeParameters = (type as Class<*>).typeParameters
                generics = Array(typeParameters.size) { forType(typeParameters[it], this) }

                // ParameterizedType->带泛型的类型, 例如List<String>, List<E>
                // 如果是List<String>的情况, 这里actualTypeArguments可以拿到[String.class]
                // 如果是List<E>的情况, 这里actualTypeArguments可以拿到[E], 这里的E的类型是Class
            } else if (this.type is ParameterizedType) {
                val actualTypeArguments = (type as ParameterizedType).actualTypeArguments
                generics = Array(actualTypeArguments.size) { forType(actualTypeArguments[it], this) }
            } else {
                generics = resolveType().getGenerics()
            }
            this.generics = generics
        }
        return generics
    }

    /**
     * 解析泛型参数, 对每一个泛型参数去resolve去解析成为Class
     *
     * @return 泛型参数列表
     * @see getGenerics
     * @see resolve
     */
    open fun resolveGenerics(): Array<Class<*>?> {
        val generics = getGenerics()
        return Array(generics.size) { generics[it].resolve() }
    }

    open fun resolveGeneric(vararg indexes: Int): Class<*>? {
        return getGeneric(*indexes).resolve()
    }

    /**
     * 检查当前[ResolvableType], 是否存在有泛型参数
     *
     * @return 当前ResolvableType有泛型参数return true; 否则return false
     */
    open fun hasGenerics(): Boolean {
        return getGenerics().isNotEmpty()
    }

    /**
     * 提供对于[Collection]的泛型解析的快捷方法, 如果当前类型没有实现[Collection]接口的话,
     * 那么返回[NONE]
     *
     * @return Collection的ResolvableType(or NONE)
     * @see as
     * @see asMap
     */
    open fun asCollection(): ResolvableType {
        return `as`(Collection::class.java)
    }

    /**
     * 提供对于[Map]的泛型解析的快捷方法, 如果当前类型没有实现[Map]接口的话, 那么返回[NONE]
     *
     * @return Map的ResolvableType(or NONE)
     * @see as
     * @see asMap
     */
    open fun asMap(): ResolvableType {
        return `as`(Map::class.java)
    }

    /**
     * 获取到数组的元素类型[ResolvableType], 如果不存在的话, return NONE
     *
     * @return 数组的元素类型的ResolvableType(or NONE)
     */
    open fun getComponentType(): ResolvableType {
        if (this == NONE) {
            return NONE
        }
        if (this.componentType != null) {
            return this.componentType!!
        }
        if (this.type is Class<*>) {
            val componentType = (this.type as Class<*>).componentType
            return forType(componentType, this.variableResolver)
        }
        if (this.type is GenericArrayType) {
            return forType((this.type as GenericArrayType).genericComponentType, this.variableResolver)
        }
        return resolveType().getComponentType()
    }

    /**
     * 检查当前[ResolvableType]类型, 是否是数组
     *
     * @return 当前是数组的话, return true; 不是数组的话, return false
     */
    open fun isArray(): Boolean {
        if (this == NONE) {
            return false
        }
        if (this.type is Class<*>) {
            return (this.type as Class<*>).isArray
        }
        if (this.type is GenericArrayType) {
            return true
        }
        return resolveType().isArray()
    }

    /**
     * 从当前类的所有的接口/父类的继承关系当中, 去找到和给定的type匹配的[ResolvableType],
     * 如果给定的type不是当前类的接口/父类的话, 那么返回[NONE]
     *
     * @param type 待匹配的类型
     * @return 匹配上type的ResolvableType(没匹配上, 那么return NONE)
     * @see getSuperType
     * @see getInterfaces
     */
    open fun `as`(type: Class<*>): ResolvableType {
        if (this == NONE) {
            return NONE
        }
        val resolved = resolve()
        if (resolved == null || resolved == type) {
            return this
        }
        // 遍历当前类的所有直接接口
        for (interfaceType in getInterfaces()) {

            // 使用该接口, 递归去执行as方法, 去匹配type
            val interfaceAsType = interfaceType.`as`(type)
            if (interfaceAsType != NONE) {
                return interfaceAsType
            }
        }
        // 接口上没有找到合适的, 那么尝试从父类上去进行递归匹配...
        return getSuperType().`as`(type)
    }

    /**
     * 获取当前类的直接接口列表[ResolvableType]
     *
     * @return 当前类的直接接口列表ResolvableType
     */
    open fun getInterfaces(): Array<ResolvableType> {
        val resolved = resolve() ?: return EMPTY_TYPES_ARRAY
        var interfaces = this.interfaces
        if (this.interfaces == null) {
            val genericInterfaces = resolved.genericInterfaces

            // 为所有的泛型接口, 去构建对应的ResolvableType
            // 每个泛型接口的owner都是子类(子接口), type指定父接口
            interfaces = Array(genericInterfaces.size) { forType(genericInterfaces[it], this) }
            this.interfaces = interfaces
        }
        return interfaces!!
    }

    /**
     * 获取父类类型[ResolvableType]
     *
     * @return 父类superType(不存在的话, 返回NONE)
     */
    open fun getSuperType(): ResolvableType {
        val resolved = resolve() ?: return NONE
        val superclass = resolved.genericSuperclass ?: return NONE

        var superType = superType
        if (superType == null) {
            // 为superType, 去构建ResolvableType
            // 每个泛型父类的owner都是子类, type=superClass
            superType = forType(superclass, this)
            this.superType = superType
        }
        return superType
    }

    /**
     * 返回根据[Type]去解析得到的[Class], 如果没有已经完成解析的[Class], 那么return null
     *
     * @return 已经完成解析的Class(如果不存在的话, return null)
     */
    open fun resolve(): Class<*>? {
        return this.resolved
    }

    /**
     * 指定对于type的解析, 将[Type]解析成为[ResolvableType]
     *
     * @return 对type执行解析得到的ResolvableType
     * @see type
     * @see Type
     */
    private fun resolveType(): ResolvableType {
        // 如果是ParameterizedType, 例如List<String>, Map<String, String>
        if (this.type is ParameterizedType) {
            return forType((type as ParameterizedType).rawType, this.variableResolver)
        }
        // 如果是WildcardType, 通配符的情况, 也就是"? extends"/"? super"这种情况
        if (this.type is WildcardType) {
            var resolved = resolveBounds((type as WildcardType).upperBounds)
            if (resolved == null) {
                resolved = resolveBounds((type as WildcardType).lowerBounds)
            }
            return forType(resolved, this.variableResolver)
        }

        // 如果是TypeVariable, 说明是一个泛型的元素, 例如List<E>中的E就是TypeVariable
        // 这种情况, 我们就得使用variableResolver去进行解析...
        // 例如对于ArrayList<String>的接口列表可以获取到List<E>, List<E>的接口列表可以获取到Collection<E>
        // 当使用asCollection方法获取到Collection<E>时, 就得尝试从子类找到List<E>
        // List<E>再往子类去找到ArrayList<String>, 通过泛型名字E, 从而最终找到元素类型String
        // 特殊情况, 对于Map/List/Set这种不指定泛型的情况, K/V/E的类型就是TypeVariable
        if (this.type is TypeVariable<*>) {
            val typeVariable = this.type as TypeVariable<*>
            val resolved = this.variableResolver?.resolveVariable(typeVariable)
            if (resolved != null) {
                return resolved
            }
            return forType(resolveBounds(typeVariable.bounds), this.variableResolver)
        }
        return NONE
    }

    private fun resolveBounds(bounds: Array<Type>): Type? {
        if (bounds.isEmpty() || bounds[0] == Any::class.java) {
            return null
        }
        return bounds[0]
    }


    /**
     * 执行对于泛型变量的解析, 把[TypeVariable]解析成为[ResolvableType]
     *
     * @param typeVariable 带解析的泛型变量, 比如E/T/K/V
     * @return 根据TypeVariable去解析得到的类型ResolvableType
     */
    private fun resolveVariable(typeVariable: TypeVariable<*>): ResolvableType? {

        if (this.type is ParameterizedType) {
            val parameterizedType = this.type as ParameterizedType
            val resolved = resolve() ?: return null
            val variables = resolved.typeParameters

            // 遍历当前类定义的所有的泛型参数, 去寻找, 如果该泛型参数的name和给定的TypeVariable的name一致
            // 那么就去返回该位置的泛型参数...比如ArrayList<String>, 对于Collection<E>, 就能从子类当中
            // 去解析得到ArrayList当中对应位置为E的泛型, 从而解析得到String...
            for ((index, variable) in variables.withIndex()) {
                if (variable.name == typeVariable.name) {
                    val actualType = parameterizedType.actualTypeArguments[index]
                    return forType(actualType, this.variableResolver)
                }
            }

            val ownerType = parameterizedType.ownerType
            if (ownerType != null) {
                return forType(ownerType, this.variableResolver).resolveVariable(typeVariable)
            }
        }

        return null
    }

    /**
     * 将当前[ResolvableType]转换为泛型变量(K/V/E)的解析器
     *
     * @return 泛型变量解析器
     */
    open fun asVariableResolver(): VariableResolver? {
        if (this == NONE) {
            return null
        }
        return DefaultVariableResolver(this)
    }

    /**
     * 检查给定的实例, 是否是当前类型
     *
     * @param obj 待检查的实例对象
     * @return 该实例是否是当前的类型?
     */
    open fun isInstance(@Nullable obj: Any?): Boolean {
        return obj != null && isAssignableFrom(obj::class.java)
    }

    /**
     * 检查给定的目标类型, 是否是当前type的子类
     *
     * @param other 待匹配的类
     * @return 如果other是当前Type的子类, 那么return true; 否则return false
     */
    open fun isAssignableFrom(@Nullable other: Class<*>?): Boolean {
        return isAssignableFrom(forClass(other), null)
    }

    /**
     * 检查给定的目标类型, 是否是当前type的子类
     *
     * @param other 待匹配的类
     * @return 如果other是当前Type的子类, 那么return true; 否则return false
     */
    open fun isAssignableFrom(other: ResolvableType): Boolean {
        return isAssignableFrom(other, null)
    }

    /**
     * 检查给定的目标类型, 是否是当前type的子类
     *
     * @param other 待匹配的类
     * @param matchedBefore 在之前已经进行匹配的过的Type(Key-this.type, Value-other.type)
     * @return 如果other是当前Type的子类, 那么return true; 否则return false
     */
    private fun isAssignableFrom(other: ResolvableType, matchedBefore: MutableMap<Type, Type>?): Boolean {
        var matchedBeforeToUse = matchedBefore

        if (this == NONE || other == NONE) {
            return false
        }

        // 如果是数组的话, 那么只需要判断元素类型是否匹配即可...
        if (isArray()) {
            return other.isArray() && this.getComponentType().isAssignableFrom(other.getComponentType())
        }
        // 如果之前已经匹配过this.type和other.type, 这种情况直接pass掉, 不再进行继续匹配...
        if (matchedBeforeToUse != null && matchedBeforeToUse[this.type!!] == other.type) {
            return true
        }

        val ourBounds = WildcardBounds.get(this)
        val typeBounds = WildcardBounds.get(other)

        // 如果other是<? extends Number>或者说<? super Number>的情况
        // 那么我们必须要求, this和other的泛型完全一致才算匹配, kind一致&bounds完全一致
        if (typeBounds != null) {
            return ourBounds != null && ourBounds.isSameKind(typeBounds)
                    && ourBounds.isAssignable(*typeBounds.bounds)
        }

        // 如果this是<? extends Number>或者说<? super Number>的情况
        // 那么我们只需要去要求, other的类型是Number就行...
        if (ourBounds != null) {
            return ourBounds.isAssignable(other)
        }

        val extraMatch = matchedBeforeToUse != null

        var ourResolved: Class<*>? = null

        // 如果是TypeVariable的话, 我们需要先解析一波
        if (this.type is TypeVariable<*>) {
            // 如果this存在有variableResolver, 那么尝试使用它去进行泛型变量的解析
            if (this.variableResolver != null) {
                ourResolved = this.variableResolver?.resolveVariable(this.type as TypeVariable<*>)?.resolve()
            }
        }

        // 如果没解析出来当前的type的话, 那么我们使用Object.class作为fallback
        ourResolved = ourResolved ?: resolve(Any::class.java)

        // other的类型(Class), 默认为Object.class
        val otherResolved = other.resolve(Any::class.java)

        // extraMatch=true这种情况是内部的泛型类型的检查, 我们不能检查isAssignable, 只能够判断equals
        // 比如List<CharSequence>和List<String>这种情况不能认为是存在有继承关系...
        if (extraMatch) {
            if (ourResolved != otherResolved) {
                return false
            }

            // 如果extraMatch=false, 说明不是内部泛型的匹配, 我们直接判断isAssignable就行...
        } else {
            if (!ClassUtils.isAssignable(ourResolved, otherResolved)) {
                return false
            }
        }

        // 如果this和other都不是<? extends Number>或者说<? super Number>这种情况
        // 比如类似简单的List<String>这种情况, 我们需要去递归检查泛型...
        val ourGenerics = getGenerics()
        val typeGenerics = other.`as`(resolve(Any::class.java)).getGenerics()
        if (ourGenerics.size != typeGenerics.size) {
            return false
        }

        // 对于泛型的匹配, 那么需要把type塞入到matchedBefore列表当中...(key是this.type, value是other.type)
        matchedBeforeToUse = matchedBeforeToUse ?: LinkedHashMap(1)
        matchedBeforeToUse[this.type!!] = other.type!!

        // 对应位置的泛型, 依次去进行匹配, 其中一个匹配不上, 就return false...
        for (i in ourGenerics.indices) {
            if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], matchedBeforeToUse)) {
                return false
            }
        }
        return true
    }

    override fun toString(): String {
        if (isArray()) {
            return getComponentType().toString() + "[]"
        }
        if (this.resolved == null) {
            return "?"
        }
        if (this.type is TypeVariable<*>) {
            if (variableResolver?.resolveVariable(this.type as TypeVariable<*>) == null) {
                return "?"
            }
        }
        if (this.hasGenerics()) {
            return resolved!!.name + "<" + getGenerics().joinToString(",") { it.toString() } + ">"
        }
        return this.resolved!!.name
    }


    companion object {
        /**
         * 空的ResolvableType常量
         */
        @JvmField
        val NONE = ResolvableType(EmptyType.INSTANCE, null, null)

        /**
         * 空的ResolvableType[]常量
         */
        @JvmField
        val EMPTY_TYPES_ARRAY = emptyArray<ResolvableType>()

        /**
         * 基于给定的Class, 去包装成为ResolvableType
         *
         * @return ResolvableType
         */
        @JvmStatic
        fun forClass(clazz: Class<*>?): ResolvableType {
            return ResolvableType(clazz)
        }

        /**
         * 基于给定的Field, 去为该类型解析成为ResolvableType
         *
         * @param field 要去进行解析的字段
         * @return 为该字段解析得到的ResolvableType
         */
        @JvmStatic
        fun forField(field: Field): ResolvableType {
            return forType(null, FieldTypeProvider(field), null)
        }

        /**
         * 指定Field以及该Field的实现类, 从而去解析成为ResolvableType,
         * 有可能父类定义了一个T[]的字段, 具体的类型是子类给定的, 如果没有子类, 这个T[]的类型就无法被解析出来,
         * 但是提供了子类之后, 我们可以基于子类, 去解析父类的泛型, 从而解析得到这个T
         *
         * @param field field, 可能为父类当中的字段
         * @param implementingClass 该字段对应的类的实现类
         * @return 针对该Field去进行描述得到的ResolvableType
         */
        @JvmStatic
        fun forField(field: Field, implementingClass: Class<*>?): ResolvableType {
            val owner = forType(implementingClass).`as`(field.declaringClass)
            return forType(null, FieldTypeProvider(field), owner.asVariableResolver())
        }

        /**
         * 获取指定的Field的指定嵌套层级的Type信息
         *
         * @param field Field, 待解析的字段
         * @param nestingLevel nestingLevel, 要获取的泛型的嵌套等级
         * @return 获取到的字段的指定层级的泛型的Type信息对应的ResolvableType
         */
        @JvmStatic
        fun forField(field: Field, nestingLevel: Int): ResolvableType {
            return forField(field).getNested(nestingLevel)
        }

        /**
         * 指定Field以及该Field的实现类, 从而对该字段的指定层级的泛型信息, 去解析成为ResolvableType,
         * 有可能父类定义了一个T[]的字段, 具体的类型是子类给定的, 如果没有子类, 这个T[]的类型就无法被解析出来,
         * 但是提供了子类之后, 我们可以基于子类, 去解析父类的泛型, 从而解析得到这个T
         *
         * @param field field, 可能为父类当中的字段
         * @param nestingLevel nestingLevel, 要获取的泛型的嵌套等级
         * @param implementingClass 该字段对应的类的实现类
         * @return 针对该Field去进行描述得到的ResolvableType
         */
        @JvmStatic
        fun forField(field: Field, nestingLevel: Int, implementingClass: Class<*>?): ResolvableType {
            return forField(field, implementingClass).getNested(nestingLevel)
        }

        /**
         * 针对给定的Type去解析成为ResolvableType
         *
         * @param type type
         * @return 解析得到的ResolvableType
         */
        @JvmStatic
        fun forType(type: Type?): ResolvableType {
            return forType(type, null, null)
        }

        /**
         * 针对给定的Type去解析成为ResolvableType
         *
         * @param type type
         * @param owner 该type对应的Owner, 比如字段对应的具体实现类
         * @return 解析得到的ResolvableType
         */
        @JvmStatic
        fun forType(type: Type?, owner: ResolvableType?): ResolvableType {
            return forType(type, owner?.asVariableResolver())
        }

        /**
         * 针对给定的Type去解析成为ResolvableType
         *
         * @param type type
         * @param variableResolver 提供对于TypeVariable的解析的解析器(很可能由owner提供, 让子类去解析父类的泛型信息)
         * @return 解析得到的ResolvableType
         */
        @JvmStatic
        fun forType(
            type: Type?,
            variableResolver: VariableResolver?
        ): ResolvableType {
            return forType(type, null, variableResolver)
        }

        /**
         * 针对给定的Type去解析成为ResolvableType
         *
         * @param type type
         * @param typeProvider typeProvider(如果type为null, 支持从typeProvider当中去进行getType)
         * @param variableResolver 提供对于TypeVariable的解析的解析器(很可能由owner提供, 让子类去解析父类的泛型信息)
         * @return 解析得到的ResolvableType
         */
        @JvmStatic
        private fun forType(
            type: Type?,
            typeProvider: TypeProvider?,
            variableResolver: VariableResolver?
        ): ResolvableType {
            var typeToUse = type
            if (typeToUse == null && typeProvider != null) {
                typeToUse = typeProvider.getType()
            }
            if (typeToUse == null) {
                return NONE
            }

            if (typeToUse is Class<*>) {
                return ResolvableType(typeToUse, typeProvider, variableResolver, null)
            }

            return ResolvableType(typeToUse, typeProvider, variableResolver)
        }

        /**
         * 将一个方法参数去转换成为[ResolvableType]
         *
         * @param method 方法
         * @param index 方法参数index?
         * @return 解析得到描述该方法参数的ResolveType
         */
        @JvmStatic
        fun forMethodParameter(method: Method, index: Int): ResolvableType {
            return forMethodParameter(MethodParameter(method, index))
        }

        /**
         * 将一个方法参数去转换成为[ResolvableType]
         *
         * @param method 方法
         * @param index 方法参数index?
         * @param implementingClass 该方法的具体实现类
         * @return 解析得到描述该方法参数的ResolveType
         */
        @JvmStatic
        fun forMethodParameter(method: Method, index: Int, implementingClass: Class<*>): ResolvableType {
            return forMethodParameter(MethodParameter(method, index, implementingClass))
        }

        /**
         * 根据给定的方法的返回值去构建出来[ResolvableType]
         *
         * @param method method
         * @return 根据方法返回值, 去构建出来的ResolvableType
         */
        @JvmStatic
        fun forMethodReturnType(method: Method): ResolvableType {
            return forMethodParameter(MethodParameter(method, -1))
        }

        /**
         * 将一个方法参数类型去转换为ResolvableType
         *
         * @param methodParameter 方法参数(包装了jdk的Parameter)
         * @return 方法参数(来自jdk的Parameter)转换得到的ResolvableType
         */
        @JvmStatic
        fun forMethodParameter(methodParameter: MethodParameter): ResolvableType {
            return forMethodParameter(methodParameter, targetType = null)
        }

        /**
         * 将一个方法参数去转换成为ResolvableType
         *
         * @param methodParameter 方法参数
         * @param targetType targetType
         * @return 针对该方法参数去解析得到的ResolvableType
         */
        @JvmStatic
        fun forMethodParameter(methodParameter: MethodParameter, @Nullable targetType: Type?): ResolvableType {
            return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel())
        }

        /**
         * 将一个方法参数去转换成为ResolvableType
         *
         * @param methodParameter 方法参数
         * @param implementationType 该方法参数对应的实现类的类型
         * @return 针对该方法参数, 去解析得到的ResolvableType
         */
        @JvmStatic
        fun forMethodParameter(
            methodParameter: MethodParameter,
            @Nullable implementationType: ResolvableType?
        ): ResolvableType {
            // 获取到具体的实现类(如果不存在的话, 那么从方法参数当中去进行获取containingClass)
            val implementationTypeToUse = implementationType ?: forType(methodParameter.getContainingClass())
            // 将实现类使用as转换为方法所在的类...
            val owner = implementationTypeToUse.`as`(methodParameter.getDeclaringClass())

            return forType(
                null,
                MethodParameterTypeProvider(methodParameter),
                owner.asVariableResolver()
            ).getNested(methodParameter.getNestingLevel(), methodParameter.getOriginTypeIndexesPerLevel())
        }

        /**
         * 将一个方法参数类型的指定泛型嵌套级别的参数, 去转换成为ResolvableType
         *
         * @param methodParameter 方法参数
         * @param nestingLevel 泛型参数嵌套级别
         * @param targetType targetType
         * @return 方法参数/方法参数的泛型信息, 去转换得到的ResolvableType
         */
        @JvmStatic
        fun forMethodParameter(methodParameter: MethodParameter, targetType: Type?, nestingLevel: Int): ResolvableType {
            // 使用containingClass去解析成为ResolvableType, 再使用as切换到declaringClass的ResolvableType
            val owner = forType(methodParameter.getContainingClass()).`as`(methodParameter.getDeclaringClass())
            return forType(
                targetType,
                MethodParameterTypeProvider(methodParameter),
                owner.asVariableResolver()
            ).getNested(nestingLevel, methodParameter.getOriginTypeIndexesPerLevel())
        }

        /**
         * 在明确知道类的泛型的情况下, 可以直接去给定一个Class, 并且直接指定它的泛型的方式去快捷构建出来[ResolvableType]
         *
         * @param clazz 目标Class
         * @param generics 泛型列表
         * @return 携带有泛型的Class解析成为的ResolvableType
         */
        @JvmStatic
        fun forClassWithGenerics(clazz: Class<*>, vararg generics: Class<*>): ResolvableType {
            return forClassWithGenerics(forClass(clazz), *generics.map { forClass(it) }.toTypedArray())
        }

        /**
         * 给定目标类和该类的泛型信息泛型, 去构建[ResolvableType]
         *
         * @param resolvableType 目标类
         * @param generics 该类的泛型列表
         * @return ResolvableType
         */
        @JvmStatic
        private fun forClassWithGenerics(
            resolvableType: ResolvableType,
            vararg generics: ResolvableType
        ): ResolvableType {
            if (generics.isNotEmpty()) {
                resolvableType.generics = arrayOf(*generics)
            }
            return resolvableType
        }

        @JvmStatic
        fun clearCache() {

        }
    }

    /**
     * TypeProvider
     */
    interface TypeProvider {
        fun getType(): Type?

        fun getSource(): Any? = null
    }

    private class FieldTypeProvider(private val field: Field) : TypeProvider {
        override fun getType(): Type? {
            return field.genericType
        }

        override fun getSource(): Any {
            return field
        }
    }

    private class MethodParameterTypeProvider(private val methodParameter: MethodParameter) : TypeProvider {
        override fun getType(): Type {
            return methodParameter.getGenericParameterType()
        }

        override fun getSource(): Any {
            return this.methodParameter
        }
    }


    /**
     * 提供对于TypeVariable的解析的Resolver策略接口
     *
     * @see TypeVariable
     */
    interface VariableResolver {

        /**
         * 返回VariableResolver的source
         *
         * @return source of VariableResolver
         */
        fun getSource(): Any

        /**
         * 执行对于泛型变量的解析
         *
         * @param typeVariable 泛型变量
         * @return 解析完成得到的变量值, 解析不到的话return null
         */
        fun resolveVariable(typeVariable: TypeVariable<*>): ResolvableType?
    }

    /**
     * 默认的[VariableResolver]实现, 基于[ResolvableType]去提供泛型的解析能力
     *
     * @param source 被利用来作为泛型的解析的ResolvableType
     */
    private class DefaultVariableResolver(private val source: ResolvableType) : VariableResolver {
        override fun getSource(): Any {
            return source
        }

        override fun resolveVariable(typeVariable: TypeVariable<*>): ResolvableType? {
            return source.resolveVariable(typeVariable)
        }
    }

    /**
     * 空的Type标识
     */
    private class EmptyType : Type, Serializable {
        companion object {
            @JvmField
            val INSTANCE = EmptyType()
        }

        fun readResolve(): Any = INSTANCE
    }

    /**
     * 内部使用的针对通配符的情况, 也就是泛型的协变(`<? extends T>`)/逆变(`<? super T>`), 去进行解析和处理的工具类
     *
     * @param kind UPPER协变/LOWER逆变
     * @param bounds lowerBounds/upperBounds, 例如针对<? extends T>将会得到T
     *
     * @see WildcardType.getLowerBounds
     * @see WildcardType.getUpperBounds
     */
    private class WildcardBounds(val kind: Kind, val bounds: Array<ResolvableType>) {

        /**
         * 检查当前的[WildcardBounds]和给定的[WildcardBounds]的通配符类型(UPPER/LOWER)是否一致
         *
         * @param bounds bounds
         * @return 当前bound和给定的bound的kind是否一致
         * @see Kind
         */
        fun isSameKind(bounds: WildcardBounds): Boolean {
            return this.kind == bounds.kind
        }

        /**
         * 判断给定的Type, 是否是继承自bounds当中的Type
         *
         * @param types 待检查的Type列表
         * @return 继承关系是否成立?
         */
        fun isAssignable(vararg types: ResolvableType): Boolean {
            for (bound in this.bounds) {
                for (type in types) {
                    if (!isAssignable(bound, type)) {
                        return false
                    }
                }
            }
            return true
        }

        private fun isAssignable(source: ResolvableType, from: ResolvableType): Boolean {
            if (this.kind == Kind.LOWER) {
                return from.isAssignableFrom(source)
            }
            return source.isAssignableFrom(from)
        }

        companion object {

            /**
             * 对于[WildcardBounds]的构建的工厂方法(只有在type类型为[WildcardType]的情况, 才会返回非空; 否则return null)
             *
             * @param type 要去进行构建WildcardBounds的ResolvableType
             * @return 针对该type去构建得到的WildcardBounds(如果不存在的话, 那么return null)
             */
            @Nullable
            fun get(type: ResolvableType): WildcardBounds? {
                var resolveToWildcard = type

                // 如果type不是WildcardType的话, 那么执行对于
                while (resolveToWildcard.type !is WildcardType) {
                    if (resolveToWildcard == NONE) {
                        return null
                    }
                    resolveToWildcard = resolveToWildcard.resolveType()
                }
                val wildcardType = resolveToWildcard.type as WildcardType

                // 如果有lowerBounds, 那么为LOWER; 否则为UPPER
                val kind = if (wildcardType.lowerBounds.isEmpty()) Kind.UPPER else Kind.LOWER
                // 如果有lowerBounds, 那么取lowBounds; 否则取upperBounds, 一定不为null, 可能为emptyArray
                val bounds =
                    if (wildcardType.lowerBounds.isEmpty()) wildcardType.upperBounds else wildcardType.lowerBounds

                // 将bounds当中的每个元素去解析成为ResolvableType
                val resolvableBounds = Array(bounds.size) { forType(bounds[it], type.variableResolver) }

                // 构建WildcardBounds对象
                return WildcardBounds(kind, resolvableBounds)
            }
        }

        /**
         * 泛型的通配符类型的枚举值(协变UPPER/逆变LOWER)
         */
        enum class Kind {

            /**
             * <? extends T>(Kotlin当中的<out T>)的情况, 协变
             */
            UPPER,

            /**
             * <? super T>(Kotlin当中的<in T>)的情况, 逆变
             */
            LOWER
        }
    }
}