package com.wanna.framework.context.indexer.processor

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

/**
 * 为标准的"javax.*"注解的元信息的获取的[StereotypesProvider],
 * 主要处理那些标注了javax.*`注解的类, 将它们标注的对应的注解信息收集起来
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/21
 */
class StandardStereotypesProvider(private val typeHelper: TypeHelper) : StereotypesProvider {

    /**
     * 收集出来目标类(或接口)身上标注了所有的"javax."注解的信息
     *
     * @return 该类(或接口)上标注的"javax."的注解信息, 如果不是类/接口, 返回emptySet
     */
    override fun getStereotypes(element: Element): Set<String> {
        val stereotypes = LinkedHashSet<String>()
        // 类/接口需要去进行收集
        if (element.kind.isClass || element.kind == ElementKind.INTERFACE) {
            // 获取目标类/接口上的注解信息
            val annotationMirrors = typeHelper.getAllAnnotationMirrors(element)
            for (annotation in annotationMirrors) {

                // 获取注解的类名, 检查是否以"javax."作为开头...
                val type = this.typeHelper.getType(annotation)
                if (type != null && type.startsWith("javax.")) {
                    stereotypes.add(type)
                }
            }
        }
        return stereotypes
    }
}