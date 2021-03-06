/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.html2excel.core;

import com.github.liaochong.html2excel.core.parser.Table;
import com.github.liaochong.html2excel.core.parser.Tr;
import com.github.liaochong.html2excel.core.reflect.ClassFieldContainer;
import com.github.liaochong.html2excel.utils.ReflectUtil;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author liaochong
 * @version 1.0
 */
public class DefaultStreamExcelBuilder extends AbstractSimpleExcelBuilder implements SimpleStreamExcelBuilder {
    /**
     * 线程池
     */
    private ExecutorService executorService;
    /**
     * 流工厂
     */
    private HtmlToExcelStreamFactory htmlToExcelStreamFactory;

    private WorkbookType workbookType = WorkbookType.SXLSX;

    private DefaultStreamExcelBuilder() {
    }

    /**
     * 获取实例，设定需要渲染的数据的类类型
     *
     * @param dataType 数据的类类型
     * @return DefaultExcelBuilder
     */
    public static DefaultStreamExcelBuilder of(Class<?> dataType) {
        Objects.requireNonNull(dataType);
        DefaultStreamExcelBuilder defaultExcelBuilder = new DefaultStreamExcelBuilder();
        defaultExcelBuilder.dataType = dataType;
        return defaultExcelBuilder;
    }

    @Override
    public DefaultStreamExcelBuilder rowAccessWindowSize(int rowAccessWindowSize) {
        super.rowAccessWindowSize(rowAccessWindowSize);
        return this;
    }

    @Override
    public DefaultStreamExcelBuilder workbookType(WorkbookType workbookType) {
        super.workbookType(workbookType);
        return this;
    }

    @Override
    public DefaultStreamExcelBuilder threadPool(ExecutorService executorService) {
        Objects.requireNonNull(executorService);
        this.executorService = executorService;
        return this;
    }

    /**
     * 流式构建启动，包含一些初始化操作，等待队列容量采用CPU核心数目
     *
     * @param groups 分组
     * @return DefaultExcelBuilder
     */
    public DefaultStreamExcelBuilder start(Class<?>... groups) {
        this.start(HtmlToExcelStreamFactory.DEFAULT_WAIT_SIZE, groups);
        return this;
    }

    @Override
    public DefaultStreamExcelBuilder start(int waitQueueSize, Class<?>... groups) {
        Objects.requireNonNull(dataType);
        htmlToExcelStreamFactory = new HtmlToExcelStreamFactory(waitQueueSize, executorService);
        htmlToExcelStreamFactory.rowAccessWindowSize(rowAccessWindowSize).workbookType(workbookType);

        ClassFieldContainer classFieldContainer = ReflectUtil.getAllFieldsOfClass(dataType);
        filteredFields = getFilteredFields(classFieldContainer, groups);

        this.initStyleMap();
        Table table = this.createTable();
        htmlToExcelStreamFactory.start(table);

        Tr head = this.createThead();
        if (Objects.isNull(head)) {
            return this;
        }
        List<Tr> headList = new ArrayList<>();
        headList.add(head);
        htmlToExcelStreamFactory.append(headList);
        return this;
    }

    @Override
    public void append(List<?> data) {
        if (Objects.isNull(data) || data.isEmpty()) {
            return;
        }
        List<List<Object>> contents = getRenderContent(data, filteredFields);
        List<Tr> trList = this.createTbody(contents, 0);
        htmlToExcelStreamFactory.append(trList);
    }

    @Override
    public Workbook build() {
        return htmlToExcelStreamFactory.build();
    }

    @Override
    public Workbook build(List<?> data, Class<?>... groups) {
        throw new UnsupportedOperationException();
    }
}
