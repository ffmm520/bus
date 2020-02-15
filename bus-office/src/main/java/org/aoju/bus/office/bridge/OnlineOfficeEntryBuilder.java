/*
 * The MIT License
 *
 * Copyright (c) 2015-2020 aoju.org All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.aoju.bus.office.bridge;

import org.aoju.bus.office.Builder;
import org.aoju.bus.office.metric.OfficeManagerEntryBuilder;

/**
 * 当不需要office实例来执行转换时，该类保存{@link OnlineOfficeEntryManager}的配置.
 *
 * @author Kimi Liu
 * @version 5.6.1
 * @since JDK 1.8+
 */
public class OnlineOfficeEntryBuilder implements OfficeManagerEntryBuilder {

    private long taskExecutionTimeout = Builder.DEFAULT_TASK_EXECUTION_TIMEOUT;

    @Override
    public long getTaskExecutionTimeout() {
        return taskExecutionTimeout;
    }

    @Override
    public void setTaskExecutionTimeout(final long taskExecutionTimeout) {
        this.taskExecutionTimeout = taskExecutionTimeout;
    }

}