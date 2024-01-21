package com.wanna.framework.context.indexer.processor

import java.io.InputStream
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.FileObject
import javax.tools.StandardLocation

/**
 * Metadata的仓库, 负责Metadata的读/取
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 */
class MetadataStore(private val env: ProcessingEnvironment) {
    companion object {
        /**
         * Metadata存放的文件位置
         */
        private const val METADATA_PATH = "META-INF/spring.components"
    }

    /**
     * 读取"META-INF/spring.components"文件, 并封装成为[CandidateComponentsMetadata]
     *
     * @return Metadata
     */
    fun readMetadata(): CandidateComponentsMetadata? {
        try {
            getMetadataResource().openInputStream().use { return PropertiesMarshaller.read(it) }
        } catch (ex: Throwable) {
            // 无法读取到Metadata, 那么忽略就行
            return null
        }
    }

    /**
     * 将给定的[CandidateComponentsMetadata]去写出到"META-INF/spring.components"文件
     *
     * @param metadata 待进行写入文件的Metadata数据
     */
    fun writeMetadata(metadata: CandidateComponentsMetadata) {
        if (metadata.items.isNotEmpty()) {
            createMetadataResource().openOutputStream().use { PropertiesMarshaller.write(metadata, it) }
        }
    }

    /**
     * 获取Metadata资源文件(META-INF/spring.components)
     *
     * @return 操作Metadata资源文件的FileObject
     */
    private fun getMetadataResource(): FileObject {
        return env.filer.getResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH)
    }

    /**
     * 创建Metadata资源文件(META-INF/spring.components)
     *
     * @return 操作Metadata资源文件的FileObject
     */
    private fun createMetadataResource(): FileObject {
        return env.filer.createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH)
    }

}