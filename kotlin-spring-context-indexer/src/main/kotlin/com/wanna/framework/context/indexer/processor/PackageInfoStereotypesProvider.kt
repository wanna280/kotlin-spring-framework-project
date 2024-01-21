package com.wanna.framework.context.indexer.processor

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

/**
 * 为每个`package-info`的标注信息提供获取的[StereotypesProvider]实现
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/21
 */
class PackageInfoStereotypesProvider : StereotypesProvider {
    companion object {
        const val STEREOTYPE = "package-info"
    }

    /**
     * 如果目标元素是一个PACKAGE, 需要收集出来它的标注信息为"package-info"
     *
     * @param element 待进行check的元素
     * @return 如果该Element是PACKAGE, 那么返回"package-info"; 否则返回emptySet
     */
    override fun getStereotypes(element: Element): Set<String> {
        if (element.kind == ElementKind.PACKAGE) {
            return setOf(STEREOTYPE)
        }
        return emptySet()
    }
}