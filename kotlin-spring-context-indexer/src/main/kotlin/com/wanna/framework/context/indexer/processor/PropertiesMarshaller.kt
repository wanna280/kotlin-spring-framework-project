package com.wanna.framework.context.indexer.processor

import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

/**
 * 属性的转换器(序列化/反序列化器)
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 */
object PropertiesMarshaller {

    /**
     * 将给定的Metadata元数据去写出到输出流当中
     *
     * @param metadata metadata
     * @param output 需要去进行写出的输出流
     */
    @JvmStatic
    fun write(metadata: CandidateComponentsMetadata, output: OutputStream) {
        val properties = Properties()
        metadata.items.forEach { properties[it.type] = it.stereotypes.joinToString(",") }
        properties.store(output, null)
    }

    /**
     * 从给定的输入流当中去读取Metadata元数据
     *
     * @param input 读取数据的输入流
     * @return 从输入流当中读取到的数据并转换成为CandidateComponentsMetadata
     */
    @JvmStatic
    fun read(input: InputStream): CandidateComponentsMetadata {
        // 读文件
        val properties = Properties()
        properties.load(input)

        // 转换成为CandidateComponentsMetadata
        val result = CandidateComponentsMetadata()
        properties.entries
            .map { ItemMetadata(it.key.toString(), it.value.toString().split(",").toSet()) }
            .forEach(result::add)
        return result
    }
}