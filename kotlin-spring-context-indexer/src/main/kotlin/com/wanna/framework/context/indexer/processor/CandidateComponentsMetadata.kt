package com.wanna.framework.context.indexer.processor

/**
 * 候选的组件的[ItemMetadata]元数据信息的数据类
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 */
data class CandidateComponentsMetadata(val items: MutableList<ItemMetadata> = ArrayList()) {
    fun add(item: ItemMetadata) {
        items.add(item)
    }
}