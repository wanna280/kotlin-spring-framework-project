package com.wanna.framework.context.indexer.processor

import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

/**
 * Metadata的收集的容器Collector
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 *
 * @param typeHelper TypeHelper, 类型的获取的工具类
 * @param processingEnvironment 当前Javac编译器编译过程的上下文参数信息
 * @param previousMetadata 先前的已经存在的Metadata数据, 如果之前已经存在了的话, 那么我们可能需要merge之前的Metadata元数据
 */
class MetadataCollector(
    private val typeHelper: TypeHelper,
    private val processingEnvironment: ProcessingEnvironment,
    private val previousMetadata: CandidateComponentsMetadata?
) {
    /**
     * 维护本次build构建过程当中, 收集得到的所有的Metadata数据
     */
    private val items = ArrayList<ItemMetadata>()

    /**
     * 本次build构建已经处理过的类
     */
    private val processedSourceTypes = LinkedHashSet<String>()

    /**
     * 收集单个ItemMetadata
     *
     * @param itemMetadata ItemMetadata
     */
    fun add(itemMetadata: ItemMetadata) {
        items.add(itemMetadata)
    }

    /**
     * 根据[RoundEnvironment]拿到当前正在进行处理的[Element]列表, 将它们的状态分别去设置为已经处理过
     *
     * @param roundEnvironment RoundEnvironment
     */
    fun processing(roundEnvironment: RoundEnvironment) {
        roundEnvironment.rootElements.forEach(this::markAsProcessed)
    }

    /**
     * 将给定的[Element]去标注为已经处理过
     *
     * @param element element
     */
    private fun markAsProcessed(element: Element) {
        if (element is TypeElement) {
            this.processedSourceTypes.add(this.typeHelper.getType(element) ?: return)
        }
    }

    /**
     * 获取收集到的元信息Metadata, 包含本次build过程当中的Metadata信息, 也会merge之前已经存在的Metadata元信息
     *
     * @return 收集到的元信息Metadata
     */
    fun getMetadata(): CandidateComponentsMetadata {
        // 根据本次的构建结果去构建CandidateComponentsMetadata
        val result = CandidateComponentsMetadata(ArrayList(items))

        // 如果先前有Metadata数据, 那么我们可能需要merge一遍之前的Metadata数据
        previousMetadata?.items?.filter(this::shouldBeMerged)?.forEach(result::add)
        return result
    }

    /**
     * 如果原先的Metadata数据当中有一些额外的数据, 我们需要考虑是否要去进行merge
     *
     * @param itemMetadata 原先的ItemMetadata
     * @return 给定的ItemMetadata是否需要merge到新的Metadata当中
     */
    private fun shouldBeMerged(itemMetadata: ItemMetadata): Boolean {
        // 如果没有type, 那么一定不需要去进行merge
        val sourceType = itemMetadata.type ?: return false

        // 如果本次build过程当中, 已经处理过这个类, 那么我们不再需要之前的数据, 以最新的为准即可
        if (processedInCurrentBuild(sourceType)) {
            return false
        }
        // 如果本次build的过程当中, 虽然没处理过这个类, 但是本次build的过程当中都没有这个类,
        // 说明这个类可能已经被删除了, 这种情况我们也不要之前的数据, 这个类已经被删除, 收集起来的数据会有问题...
        if (deletedInCurrentBuild(sourceType)) {
            return false
        }
        return true
    }

    /**
     * 检查目标类, 是否在本次的Build构建过程当中, 已经被删除
     *
     * @param sourceType 待进行check的目标类名
     * @return 如果本次build的过程当中, 不包含这样的类, 那么return true; 本次build的过程当中包含这样的类, return false
     */
    private fun deletedInCurrentBuild(sourceType: String): Boolean {
        return this.processingEnvironment.elementUtils.getTypeElement(sourceType) == null
    }

    /**
     * 检查目标类, 在本次的Build构建过程当中, 是否已经被处理过
     *
     * @param sourceType 待进行check的目标类名
     * @return 该类在本次build的过程当中, 是否已经处理过
     */
    private fun processedInCurrentBuild(sourceType: String): Boolean {
        return this.processedSourceTypes.contains(sourceType)
    }
}