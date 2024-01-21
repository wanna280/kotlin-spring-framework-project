package com.wanna.framework.context.indexer.processor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.QualifiedNameable
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 * TypeHelper, 通过组合[ProcessingEnvironment]去为Java当中的元信息的获取提供支持的工具类
 *
 * * 1.获取给定的类Element的父类
 * * 2.获取给定的类Element的父接口
 * * 3.获取给定的类Element对应的类全限定名
 * * 4.获取给定的类Element的所有直接标注的注解
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 *
 * @param env Javac编译器回调的ProcessingEnvironment, 存放了编译过程当中可能需要用到的上下文参数信息
 */
class TypeHelper(private val env: ProcessingEnvironment) {

    /**
     * 获取给定的类的直接父类(如果父类是Object, 那么return null)
     *
     * @param element element
     * @return 给定的类的父类, 如果父类是Object, 那么return null
     */
    fun getSuperClass(element: Element): Element? {
        val directSupertypes = env.typeUtils.directSupertypes(element.asType())
        if (directSupertypes.isEmpty()) {
            return null  // java.lang.Object
        }
        return env.typeUtils.asElement(directSupertypes[0])
    }

    /**
     * 获取给定的类的直接接口列表
     *
     * @param element element
     * @return 该类的所有直接接口
     */
    fun getDirectInterfaces(element: Element): List<Element> {
        val directSupertypes = env.typeUtils.directSupertypes(element.asType())

        val directInterfaces = ArrayList<Element>()
        for ((index, typeMirror) in directSupertypes.withIndex()) {
            if (index < 1) {  // index=0 -> superClass
                continue
            }
            directInterfaces.add(env.typeUtils.asElement(typeMirror) ?: continue)
        }
        return directInterfaces
    }

    /**
     * 获取给定的[Element]对应的全限定类名(如果是静态内部类, 那么需要转换成为"外部类名$内部类名"的格式)
     *
     * @param element element
     * @return 该Element的全限定类名
     */
    fun getType(element: Element?): String? = getType(element?.asType())

    /**
     * 获取给定的[AnnotationMirror]注解的类型(annotationType)对应的全限定类名
     *
     * @param element 注解AnnotationMirror
     * @return 该注解的类型(annotationType)的全限定类名
     */
    fun getType(element: AnnotationMirror?): String? = getType(element?.annotationType)

    private fun getType(type: TypeMirror?): String? {
        type ?: return null

        // DeclaredType, 包含类/接口
        if (type is DeclaredType) {
            // 外层元素(如果type是顶级类, 那么返回所在包名; 如果type是内部类, 则返回它的外部类)
            val enclosingElement = type.asElement().enclosingElement

            // 如果外层元素是一个类/接口, 那么需要拼成"外部类$内部类"的格式,
            // 比如"com.wanna.App"类当中有一个内部类User, 那么enclosingElement的原始限定名是"com.wanna.App.User"
            // 我们需要转换成为"com.wanna.App$User"这样的格式
            if (enclosingElement is TypeElement) {
                return getQualifiedName(enclosingElement) + "$" + type.asElement().simpleName.toString();

                // 如果外层元素不是一个类的话, 那么直接返回元素type的全限定名就行...
            } else {
                return getQualifiedName(type.asElement())
            }
        }
        return type.toString()
    }

    private fun getQualifiedName(element: Element): String {
        if (element is QualifiedNameable) {
            return element.qualifiedName.toString()
        }
        return element.toString()
    }

    /**
     * 获取给定的元素标注的所有注解列表
     *
     * @param element 目标元素(类/接口/...)
     * @return 该元素的注解(AnnotationMirror)列表
     */
    fun getAllAnnotationMirrors(element: Element): List<AnnotationMirror> {
        try {
            return env.elementUtils.getAllAnnotationMirrors(element)
        } catch (ex: Throwable) {
            // 如果有些注解并不存在, 可能会失败...
            return emptyList()
        }
    }
}