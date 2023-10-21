package com.wanna.framework.validation

import com.wanna.framework.beans.*

/**
 * 基于BeanProperty的BindingResult
 *
 * // TODO
 *
 * @see BindingResult
 * @see Errors
 * @param target 要去进行绑定的目标对象
 * @param objectName objectName
 * @param autoGrowNestedPaths 是否要自动增长内部嵌套的属性路径的对象
 * @param autoGrowCollectionLimit 集合size自从增长的长度限制
 */
open class BeanPropertyBindingResult(
    private val target: Any?,
    objectName: String,
    private val autoGrowNestedPaths: Boolean,
    private val autoGrowCollectionLimit: Int
) : AbstractPropertyBindingResult(objectName) {

    /**
     * Errors
     */
    private val errors = ArrayList<ObjectError>()

    /**
     * BeanWrapper
     */
    private var beanWrapper: BeanWrapper? = null

    override fun getTarget() = this.target

    /**
     * 获取对于目标对象的访问的[PropertyAccessor]
     *
     * @return PropertyAccessor
     */
    override fun getPropertyAccessor(): ConfigurablePropertyAccessor {
        if (this.beanWrapper == null) {
            this.beanWrapper = createBeanWrapper()
            this.beanWrapper!!.autoGrowNestedPaths = autoGrowNestedPaths
            this.beanWrapper!!.setAutoGrowCollectionLimit(autoGrowCollectionLimit)
        }
        return this.beanWrapper ?: throw IllegalStateException("BeanWrapper cannot be null")
    }

    override fun getModel(): MutableMap<String, Any> {
        return mutableMapOf()
    }

    override fun getPropertyEditorRegistry(): PropertyEditorRegistry {
        TODO("Not yet implemented")
    }

    override fun reject(errorCode: String) {

    }

    override fun reject(errorCode: String, defaultMessage: String) {

    }

    override fun hasErrors(): Boolean {
        return this.errors.isNotEmpty()
    }

    override fun getAllErrors(): List<ObjectError> = this.errors

    override fun getFieldError(name: String): FieldError? {
        return null // TODO
    }

    override fun addError(error: ObjectError) {
        this.errors.add(error)
    }

    /**
     * 创建用于目标对象的绑定的[BeanWrapper]
     *
     * @return BeanWrapper
     */
    protected open fun createBeanWrapper(): BeanWrapper {
        val target = (this.target
            ?: throw IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'"))
        return PropertyAccessorFactory.forBeanPropertyAccess(target)
    }
}