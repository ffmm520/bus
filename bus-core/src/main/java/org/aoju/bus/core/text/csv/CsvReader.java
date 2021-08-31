/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.core.text.csv;

import org.aoju.bus.core.lang.Assert;
import org.aoju.bus.core.lang.Charset;
import org.aoju.bus.core.lang.exception.InstrumentException;
import org.aoju.bus.core.toolkit.FileKit;
import org.aoju.bus.core.toolkit.IoKit;
import org.aoju.bus.core.toolkit.ObjectKit;
import org.aoju.bus.core.toolkit.StringKit;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV文件读取器,参考：FastCSV
 *
 * @author Kimi Liu
 * @version 6.2.8
 * @since JDK 1.8+
 */
public final class CsvReader {

    CsvReadConfig config;

    /**
     * 构造,使用默认配置项
     */
    public CsvReader() {
        this(null);
    }

    /**
     * 构造
     *
     * @param config 配置项
     */
    public CsvReader(CsvReadConfig config) {
        this.config = ObjectKit.defaultIfNull(config, CsvReadConfig.defaultConfig());
    }

    /**
     * 设置字段分隔符,默认逗号
     *
     * @param fieldSeparator 字段分隔符,默认逗号
     */
    public void setFieldSeparator(char fieldSeparator) {
        this.config.setFieldSeparator(fieldSeparator);
    }

    /**
     * 设置 文本分隔符,文本包装符,默认双引号
     *
     * @param textDelimiter 文本分隔符,文本包装符,默认双引号
     */
    public void setTextDelimiter(char textDelimiter) {
        this.config.setTextDelimiter(textDelimiter);
    }

    /**
     * 设置是否首行做为标题行,默认false
     *
     * @param containsHeader 是否首行做为标题行,默认false
     */
    public void setContainsHeader(boolean containsHeader) {
        this.config.setContainsHeader(containsHeader);
    }

    /**
     * 设置是否跳过空白行,默认true
     *
     * @param skipEmptyRows 是否跳过空白行,默认true
     */
    public void setSkipEmptyRows(boolean skipEmptyRows) {
        this.config.setSkipEmptyRows(skipEmptyRows);
    }

    /**
     * 设置每行字段个数不同时是否抛出异常,默认false
     *
     * @param errorOnDifferentFieldCount 每行字段个数不同时是否抛出异常,默认false
     */
    public void setErrorOnDifferentFieldCount(boolean errorOnDifferentFieldCount) {
        this.setErrorOnDifferentFieldCount(errorOnDifferentFieldCount);
    }

    /**
     * 读取CSV文件,默认UTF-8编码
     *
     * @param file CSV文件
     * @return {@link CsvData},包含数据列表和行信息
     * @throws InstrumentException IO异常
     */
    public CsvData read(File file) throws InstrumentException {
        return read(file, Charset.UTF_8);
    }

    /**
     * 从字符串中读取CSV数据
     *
     * @param csvStr CSV字符串
     * @return {@link CsvData}，包含数据列表和行信息
     */
    public CsvData read(String csvStr) {
        return read(new StringReader(csvStr));
    }

    /**
     * 从字符串中读取CSV数据
     *
     * @param csvStr     CSV字符串
     * @param rowHandler 行处理器，用于一行一行的处理数据
     */
    public void read(String csvStr, CsvHandler rowHandler) {
        read(parse(new StringReader(csvStr)), rowHandler);
    }

    /**
     * 读取CSV文件
     *
     * @param file    CSV文件
     * @param charset 文件编码,默认系统编码
     * @return {@link CsvData},包含数据列表和行信息
     * @throws InstrumentException IO异常
     */
    public CsvData read(File file, java.nio.charset.Charset charset) throws InstrumentException {
        return read(Objects.requireNonNull(file.toPath(), "file must not be null"), charset);
    }

    /**
     * 读取CSV文件,默认UTF-8编码
     *
     * @param path CSV文件
     * @return {@link CsvData},包含数据列表和行信息
     * @throws InstrumentException IO异常
     */
    public CsvData read(Path path) throws InstrumentException {
        return read(path, Charset.UTF_8);
    }

    /**
     * 读取CSV文件
     *
     * @param path    CSV文件
     * @param charset 文件编码,默认系统编码
     * @return {@link CsvData},包含数据列表和行信息
     * @throws InstrumentException IO异常
     */
    public CsvData read(Path path, java.nio.charset.Charset charset) throws InstrumentException {
        Assert.notNull(path, "path must not be null");
        try (Reader reader = FileKit.getReader(path, charset)) {
            return read(reader);
        } catch (IOException e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 从Reader中读取CSV数据,读取后关闭Reader
     *
     * @param reader Reader
     * @return {@link CsvData},包含数据列表和行信息
     * @throws InstrumentException IO异常
     */
    public CsvData read(Reader reader) throws InstrumentException {
        final CsvParser csvParser = parse(reader);
        final List<CsvRow> rows = new ArrayList<>();
        read(csvParser, rows::add);
        final List<String> header = config.containsHeader ? csvParser.getHeader() : null;

        return new CsvData(header, rows);
    }

    /**
     * 从Reader中读取CSV数据，读取后关闭Reader
     *
     * @param reader     Reader
     * @param rowHandler 行处理器，用于一行一行的处理数据
     */
    public void read(Reader reader, CsvHandler rowHandler) {
        read(parse(reader), rowHandler);
    }

    /**
     * 读取CSV数据，读取后关闭Parser
     *
     * @param csvParser  CSV解析器
     * @param rowHandler 行处理器，用于一行一行的处理数据
     */
    private void read(CsvParser csvParser, CsvHandler rowHandler) {
        try {
            CsvRow csvRow;
            while (null != (csvRow = csvParser.nextRow())) {
                rowHandler.handle(csvRow);
            }
        } finally {
            IoKit.close(csvParser);
        }
    }

    /**
     * 从字符串中读取CSV数据并转换为Bean列表，读取后关闭Reader
     * 此方法默认识别首行为标题行。
     *
     * @param <T>    Bean类型
     * @param csvStr csv字符串
     * @param clazz  Bean类型
     * @return Bean列表
     */
    public <T> List<T> read(String csvStr, Class<T> clazz) {
        // 此方法必须包含标题
        this.config.setContainsHeader(true);

        final List<T> result = new ArrayList<>();
        read(new StringReader(csvStr), (row) -> result.add(row.toBean(clazz)));
        return result;
    }

    /**
     * 从Reader中读取CSV数据，结果为Map，读取后关闭Reader
     * 此方法默认识别首行为标题行
     *
     * @param reader Reader
     * @return {@link CsvData}，包含数据列表和行信息
     * @throws InstrumentException IO异常
     */
    public List<Map<String, String>> readMapList(Reader reader) throws InstrumentException {
        // 此方法必须包含标题
        this.config.setContainsHeader(true);

        final List<Map<String, String>> result = new ArrayList<>();
        read(reader, (row) -> result.add(row.getFieldMap()));
        return result;
    }

    /**
     * 从Reader中读取CSV数据并转换为Bean列表，读取后关闭Reader
     * 此方法默认识别首行为标题行
     *
     * @param <T>    Bean类型
     * @param reader Reader
     * @param clazz  Bean类型
     * @return Bean列表
     */
    public <T> List<T> read(Reader reader, Class<T> clazz) {
        // 此方法必须包含标题
        this.config.setContainsHeader(true);

        final List<T> result = new ArrayList<>();
        read(reader, (row) -> result.add(row.toBean(clazz)));
        return result;
    }

    /**
     * 构建 {@link CsvParser}
     *
     * @param reader Reader
     * @return CsvParser
     * @throws InstrumentException IO异常
     */
    private CsvParser parse(Reader reader) throws InstrumentException {
        return new CsvParser(reader, config);
    }

    /**
     * 从Reader中读取CSV数据并转换为Bean列表，读取后关闭Reader
     * 此方法默认识别首行为标题行
     *
     * @param <T>            Bean类型
     * @param reader         Reader
     * @param startLineIndex 起始行号,不需要大于 0，因为首行是标题行
     * @param clazz          Bean类型
     * @return Bean列表
     */
    public <T> List<T> read(Reader reader, int startLineIndex, Class<T> clazz) {
        if (startLineIndex < 1) {
            throw new IndexOutOfBoundsException(StringKit.format("start line index {} is lower than first row index 1.", startLineIndex));
        }
        // 此方法必须包含标题
        this.config.setContainsHeader(true);

        final List<T> result = new ArrayList<>();
        read(reader, (row) -> {
            if (row.getOriginalLineNumber() >= startLineIndex) {
                result.add(row.toBean(clazz));
            }
        });
        return result;
    }

}
