package com.wanna.framework.context.indexer.processor

import java.io.IOException
import javax.annotation.processing.Completion
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*

/**
 * 基于Javac的注解处理器[javax.annotation.processing.Processor]为Spring项目当中的候选组件去建立索引的Indexer
 *
 * * 1.收集起来所有的标注了`@Indexed`注解的类(所有标注了`@Component`注解的Bean将会被收集起来)
 * * 2.收集起来所有的标注了`javax.`相关的注解的类
 * * 3.收集起来package-info相关信息
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2024/1/20
 */
open class CandidateComponentsIndexer : Processor {
    /**
     * TypeHelper, 为Java当中的元信息的获取提供支持的工具类
     */
    private lateinit var typeHelper: TypeHelper

    /**
     * Metadata的收集的容器Collector
     */
    private lateinit var metadataCollector: MetadataCollector

    /**
     * Metadata的存储的仓库, 存放我们收集得到的所有的元数据, 提供读取和写入功能
     */
    private lateinit var metadataStore: MetadataStore

    /**
     * 注解的获取的Provider
     */
    private val stereotypesProviders = ArrayList<StereotypesProvider>()

    override fun getSupportedOptions(): Set<String> = emptySet()

    override fun getSupportedAnnotationTypes(): Set<String> = setOf("*")

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest();

    override fun getCompletions(
        element: Element?,
        annotation: AnnotationMirror?,
        member: ExecutableElement?,
        userText: String?
    ): Iterable<Completion> = emptyList()

    /**
     * 根据编译器的上下文参数信息去完成相关的初始化工作
     *
     * @param processingEnv Javac编译环境上下文信息
     */
    override fun init(processingEnv: ProcessingEnvironment) {
        // 初始化TypeHelper
        this.typeHelper = TypeHelper(processingEnv)

        // 收集标注`@Indexed`注解的信息的Provider
        this.stereotypesProviders.add(IndexedStereotypesProvider(typeHelper))
        // 收集标注`javax.x`注解的信息的Provider
        this.stereotypesProviders.add(StandardStereotypesProvider(typeHelper))
        // 收集package-info的信息的Provider
        this.stereotypesProviders.add(PackageInfoStereotypesProvider())

        // 初始化MetadataStore
        this.metadataStore = MetadataStore(processingEnv)

        // 初始化MetadataCollector
        this.metadataCollector = MetadataCollector(typeHelper, processingEnv, this.metadataStore.readMetadata())
    }

    /**
     * 真正去进行处理, 可能会依赖于之前初始化的[ProcessingEnvironment]去进行处理
     *
     * @param annotations 从应用程序当中去扫描到的所有的注解列表
     * @param roundEnv 维护需要去进行处理的类的参数信息的Environment上下文参数信息
     */
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // 先交给MetadataCollector去进行处理
        metadataCollector.processing(roundEnv)

        // 遍历每个rootElement(外部类/接口)去进行元信息的收集
        roundEnv.rootElements.forEach(this::processElement)

        // 在处理完成之后, 将元数据写入到"META-INF/spring.components"当中
        if (roundEnv.processingOver()) {
            try {
                metadataStore.writeMetadata(metadataCollector.getMetadata())
            } catch (ex: IOException) {
                throw IllegalStateException("Failed to write metadata", ex)
            }
        }
        return false
    }


    /**
     * 处理单个类的Metadata的收集(Element可能存在内部类, 需要去进行递归处理)
     *
     * @param element 待去进行处理的类Element
     */
    private fun processElement(element: Element) {
        // 为当前类去添加Metadata元数据
        addMetadataFor(element)

        // 对该类的内部类去进行check, 我们需要扫描出来那些静态内部类(或者接口), 去进行递归收集元数据
        staticTypesIn(element.enclosedElements).forEach(this::processElement)
    }

    /**
     * 为单个类去进行Metadata的收集
     *
     * @param element 待去进行处理的类Element(可能是静态内部类)
     */
    private fun addMetadataFor(element: Element) {
        // 依次使用所有的StereotypesProvider, 去对当前类的Metadata去进行收集
        // 1. IndexedStereotypesProvider负责收集出来目标类Element当中, 所有的标注了`@Indexed`注解的相关类(元素可能有注解, 父类, 父接口)
        // 2. StandardStereotypesProvider负责收集出来目标类Element当中, 所有的标注的`javax.`相关的注解信息
        // 3. PackageInfoStereotypesProvider负责收集目标Element是一个PACKAGE的时候, 收集到stereotype为"package-info"
        val stereotypes = stereotypesProviders.map { it.getStereotypes(element) }.flatten().toSet()

        // 将当前类收集到的元信息存放到metadataCollector当中
        if (stereotypes.isNotEmpty()) {
            metadataCollector.add(ItemMetadata(this.typeHelper.getType(element), stereotypes))
        }
    }

    /**
     * 判断给定的这些Element当中, 是否是静态内部类(静态内部类/接口)
     *
     * @param elements 待进行check的元素列表Elements
     * @return 所有的是静态内部类的Element的列表
     */
    private fun staticTypesIn(elements: Iterable<Element>): List<TypeElement> {
        val list = ArrayList<TypeElement>()
        for (element in elements) {
            // 是类或者接口(这里判断接口, 得用kind==INTERFACE, 不能用isInterface, isInterface的判断会导致注解也被扫描进来, 实际上注解我们不太关注)
            if (element.kind.isClass || element.kind == ElementKind.INTERFACE) {
                if (element.modifiers.contains(Modifier.STATIC) && element is TypeElement) {
                    list.add(element)
                }
            }
        }
        return list;
    }


}