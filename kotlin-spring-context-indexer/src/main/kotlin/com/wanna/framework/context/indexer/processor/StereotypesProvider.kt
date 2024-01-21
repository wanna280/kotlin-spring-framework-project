package com.wanna.framework.context.indexer.processor

import javax.lang.model.element.Element

/**
 * 在目标元素身上进行标注信息的获取的Provider
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 */
fun interface StereotypesProvider {
    fun getStereotypes(element: Element): Set<String>
}