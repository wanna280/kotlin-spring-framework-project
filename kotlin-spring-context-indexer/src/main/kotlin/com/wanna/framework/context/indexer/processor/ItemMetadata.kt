package com.wanna.framework.context.indexer.processor

/**
 * 为单个类去收集出来的Metadata元数据信息
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 *
 * @param type 类全限定名
 * @param stereotypes 为该类去收集得到的一些标注的Metadata元信息
 */
data class ItemMetadata(val type: String?, val stereotypes: Set<String>)
