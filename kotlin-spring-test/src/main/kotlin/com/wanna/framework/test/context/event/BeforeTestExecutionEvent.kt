package com.wanna.framework.test.context.event

import com.wanna.framework.test.context.TestContext

/**
 * 在`@Test`方法执行之前触发的事件
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2022/11/6
 *
 * @param testContext TestContext
 */
open class BeforeTestExecutionEvent(testContext: TestContext) : TestContextEvent(testContext)