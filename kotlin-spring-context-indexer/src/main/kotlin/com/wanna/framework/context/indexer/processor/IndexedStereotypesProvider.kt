package com.wanna.framework.context.indexer.processor

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

/**
 * `@Indexed`注解标注的类的收集的Provider
 *
 * 在目标类的注解/父类/父接口当中去进行`@Indexed`的收集, 收集到的是标注了`@Indexed`注解的所有的相关类, 最终收集结果是一个Set
 *
 * * 1.如果目标类A的注解当中有标注`@Indexed`元注解, 那么收集起来标注`@Indexed`元注解对应的注解,
 * 比如目标类标注了`@Component`注解, 但是因为`@Component`注解标注了`@Indexed`元注解, 因此在Set当中就会收集起来`Component`,
 * 就算是标注了`@Service`/`@Controller`等注解, 最终也是通过`@Component`注解去标注的`@Indexed`注解,
 * 最终也是收集出来`@Component`注解对应的类, 也是在Set当中收集出来一个`Component`
 * * 2.如果目标类A/目标类的父类/目标类的父接口(所有的间接父类/父接口也算), 直接标注了`@Indexed`注解, 那么将标注了`@Indexed`注解的类收集起来.
 * 如果是类A标注了`@Indexed`注解, 在Set当中收集起来类A的类名; 如果是父类标注了`@Indexed`注解, 那么在Set当中收集出来父类的类名;
 * 如果类A的父接口标注了`@Indexed`注解, 那么在Set当中收集出来类A的父接口的类名.
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 */
class IndexedStereotypesProvider(private val typeHelper: TypeHelper) : StereotypesProvider {
    companion object {
        /**
         * `@Indexed`注解的类名
         */
        private const val INDEXED_ANNOTATION = "com.wanna.framework.context.stereotype.Indexed"
    }


    /**
     * 获取给定的元素上, 标注了`@Indexed`注解的所有的类
     *
     * @param element 待进行check的类(或者接口)
     * @return 收集到的标注了`@Indexed`注解的类的集合列表
     */
    override fun getStereotypes(element: Element): Set<String> {
        val stereotypes = LinkedHashSet<String>()
        if (element.kind.isClass || element.kind == ElementKind.INTERFACE) {

            // 递归检查所有的注解(以及递归的元注解), 去进行收集
            collectStereotypesOnAnnotations(LinkedHashSet(), stereotypes, element)

            // 在类和所有的父类(父接口)上去进行搜索
            collectStereotypesOnTypes(LinkedHashSet(), stereotypes, element)
        }
        return stereotypes
    }

    /**
     * 处理给定的类上的全部的注解, 会处理该类上的注解的所有元注解
     *
     * @param seen 已经处理过的类型Element
     * @param stereotypes 收集结果的列表
     * @param type 正在去进行处理的类
     */
    private fun collectStereotypesOnAnnotations(
        seen: MutableSet<Element>,
        stereotypes: MutableSet<String>,
        type: Element
    ) {
        val annotationMirrors = typeHelper.getAllAnnotationMirrors(type)
        for (annotationMirror in annotationMirrors) {
            // 先处理当前类上的当前注解
            val next = collectStereotypes(seen, stereotypes, type, annotationMirror) ?: continue

            // 如果next不为null, 说明它是一个用户自定义注解, 可能在元注解上去定义了"@Indexed"注解
            // 我们需要处理元注解的情况, 因此我们需要把注解类当做一个普通的类, 去进行递归处理
            collectStereotypesOnAnnotations(seen, stereotypes, next)
        }
    }

    /**
     * 处理给定的类上的一个注解
     *
     * @param seen 已经处理过的类型Element
     * @param stereotypes 收集结果的列表
     * @param type 正在去进行处理的类
     * @param annotation 正在处理目标类上的注解
     *
     * @return 返回该注解对应的Java类的Element
     */
    private fun collectStereotypes(
        seen: MutableSet<Element>,
        stereotypes: MutableSet<String>,
        type: Element,
        annotation: AnnotationMirror
    ): Element? {
        // 如果当前就是@Indexed注解, 那么收集起来
        if (isIndexedAnnotation(annotation)) {
            stereotypes.add(this.typeHelper.getType(type) ?: throw IllegalStateException("Cannot get type for $type"))
        }
        // 获取到该注解的类型
        val annotationElement = annotation.annotationType.asElement()
        // 这个注解已经处理过了, 那么跳过
        if (seen.contains(annotationElement)) {
            return null
        }
        if (!isIndexedAnnotation(annotation)) {
            seen.add(annotationElement)
        }
        // 返回当前正在处理的注解的类型, 把它当做一个类Element, 后续去进行递归处理该注解对应的类上标注的注解列表...
        // Note: 如果是java.lang开头的注解, 不需要去进行继续处理...
        return if (annotationElement.toString().startsWith("java.lang")) null else annotationElement
    }

    /**
     * 递归给定的类以及它的所有的父类(父接口), 去检查是否有标注`@Indexed`注解
     *
     * @param seen 已经处理过的类集合
     * @param stereotypes 收集结果的列表
     * @param type 当前正在处理的类元素
     */
    private fun collectStereotypesOnTypes(seen: MutableSet<Element>, stereotypes: MutableSet<String>, type: Element) {
        if (!seen.contains(type)) {
            seen.add(type)
            if (isAnnotatedWithIndexed(type)) {
                stereotypes.add(
                    this.typeHelper.getType(type) ?: throw IllegalStateException("Cannot get type for $type")
                )
            }
            // 递归处理父类
            val superClass = typeHelper.getSuperClass(type)
            if (superClass != null) {
                collectStereotypesOnTypes(seen, stereotypes, superClass)
            }

            // 递归处理直接父接口
            val directInterfaces = typeHelper.getDirectInterfaces(type)
            directInterfaces.forEach { collectStereotypesOnTypes(seen, stereotypes, it) }
        }
    }

    /**
     * 检查给定的元素(类), 是否直接标注`@Indexed`注解
     *
     * @param element 待check的元素(类)
     * @return 如果该类有标注`@Indexed`注解, return true; 否则return false
     */
    private fun isAnnotatedWithIndexed(element: Element): Boolean {
        for (annotationMirror in element.annotationMirrors) {
            if (isIndexedAnnotation(annotationMirror)) {
                return true
            }
        }
        return false
    }

    /**
     * 检查给定的注解, 是否是`@Indexed`注解
     *
     * @param annotation 待check的注解
     * @return 如果是`@Indexed`注解, return true; 否则return false
     */
    private fun isIndexedAnnotation(annotation: AnnotationMirror): Boolean =
        INDEXED_ANNOTATION == annotation.annotationType.toString()
}