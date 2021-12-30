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
package org.aoju.bus.crypto.symmetric;

import org.aoju.bus.core.lang.Algorithm;
import org.aoju.bus.core.lang.Assert;
import org.aoju.bus.core.lang.Optional;
import org.aoju.bus.core.lang.exception.CryptoException;
import org.aoju.bus.core.toolkit.*;
import org.aoju.bus.crypto.Builder;
import org.aoju.bus.crypto.Ciphers;
import org.aoju.bus.crypto.Mode;
import org.aoju.bus.crypto.Padding;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 对称加密算法
 * 在对称加密算法中，数据发信方将明文（原始数据）和加密密钥一起经过特殊加密算法处理后，使其变成复杂的加密密文发送出去。
 * 收信方收到密文后，若想解读原文，则需要使用加密用过的密钥及相同算法的逆算法对密文进行解密，才能使其恢复成可读明文。
 * 在对称加密算法中，使用的密钥只有一个，发收信双方都使用这个密钥对数据进行加密和解密，这就要求解密方事先必须知道加密密钥。
 *
 * @author Kimi Liu
 * @version 6.3.3
 * @since JDK 1.8+
 */
public class Crypto implements Encryptor, Decryptor, Serializable {

    private static final long serialVersionUID = 1L;

    private final Lock lock = new ReentrantLock();
    /**
     * SecretKey 负责保存对称密钥
     */
    private SecretKey secretKey;
    /**
     * Cipher负责完成加密或解密工作
     */
    private Ciphers ciphers;
    /**
     * 是否0填充
     */
    private boolean isZeroPadding;

    /**
     * 构造，使用随机密钥
     *
     * @param algorithm {@link Algorithm}
     */
    public Crypto(Algorithm algorithm) {
        this(algorithm, (byte[]) null);
    }

    /**
     * 构造，使用随机密钥
     *
     * @param algorithm 算法，可以是"algorithm/mode/padding"或者"algorithm"
     */
    public Crypto(String algorithm) {
        this(algorithm, (byte[]) null);
    }

    /**
     * 构造
     *
     * @param algorithm 算法 {@link Algorithm}
     * @param key       自定义KEY
     */
    public Crypto(Algorithm algorithm, byte[] key) {
        this(algorithm.getValue(), key);
    }

    /**
     * 构造
     *
     * @param algorithm 算法 {@link Algorithm}
     * @param key       自定义KEY
     */
    public Crypto(Algorithm algorithm, SecretKey key) {
        this(algorithm.getValue(), key);
    }

    /**
     * 构造
     *
     * @param algorithm 算法
     * @param key       密钥
     */
    public Crypto(String algorithm, byte[] key) {
        this(algorithm, Builder.generateKey(algorithm, key));
    }

    /**
     * 构造
     *
     * @param algorithm 算法
     * @param key       密钥
     */
    public Crypto(String algorithm, SecretKey key) {
        this(algorithm, key, null);
    }

    /**
     * 构造
     *
     * @param algorithm  算法
     * @param key        密钥
     * @param paramsSpec 算法参数，例如加盐等
     */
    public Crypto(String algorithm, SecretKey key, AlgorithmParameterSpec paramsSpec) {
        init(algorithm, key);
        initParams(algorithm, paramsSpec);
    }

    /**
     * 拷贝解密后的流
     *
     * @param in        {@link CipherInputStream}
     * @param out       输出流
     * @param blockSize 块大小
     * @throws IOException IO异常
     */
    private static void copyForZeroPadding(CipherInputStream in, OutputStream out, int blockSize) throws IOException {
        int n = 1;
        if (IoKit.DEFAULT_BUFFER_SIZE > blockSize) {
            n = Math.max(n, IoKit.DEFAULT_BUFFER_SIZE / blockSize);
        }
        // 此处缓存buffer使用blockSize的整数倍，方便读取时可以正好将补位的0读在一个buffer中
        final int bufSize = blockSize * n;
        final byte[] preBuffer = new byte[bufSize];
        final byte[] buffer = new byte[bufSize];

        boolean isFirst = true;
        int preReadSize = 0;
        for (int readSize; (readSize = in.read(buffer)) != IoKit.EOF; ) {
            if (isFirst) {
                isFirst = false;
            } else {
                // 将前一批数据写出
                out.write(preBuffer, 0, preReadSize);
            }
            ArrayKit.copy(buffer, preBuffer, readSize);
            preReadSize = readSize;
        }
        // 去掉末尾所有的补位0
        int i = preReadSize - 1;
        while (i >= 0 && 0 == preBuffer[i]) {
            i--;
        }
        out.write(preBuffer, 0, i + 1);
        out.flush();
    }

    /**
     * 初始化
     *
     * @param algorithm 算法
     * @param key       密钥，如果为{@code null}自动生成一个key
     * @return Crypto的子对象，即子对象自身
     */
    public Crypto init(String algorithm, SecretKey key) {
        Assert.notBlank(algorithm, "'Algorithm' must be not blank !");
        this.secretKey = key;

        // 检查是否为ZeroPadding，是则替换为NoPadding，并标记以便单独处理
        if (algorithm.contains(Padding.ZeroPadding.name())) {
            algorithm = StringKit.replace(algorithm, Padding.ZeroPadding.name(), Padding.NoPadding.name());
            this.isZeroPadding = true;
        }

        this.ciphers = new Ciphers(algorithm);
        return this;
    }

    /**
     * 获得对称密钥
     *
     * @return 获得对称密钥
     */
    public SecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * 获得加密或解密器
     *
     * @return 加密或解密
     */
    public javax.crypto.Cipher getCipher() {
        return this.ciphers.getCipher();
    }

    /**
     * 设置 {@link AlgorithmParameterSpec}，通常用于加盐或偏移向量
     *
     * @param params {@link AlgorithmParameterSpec}
     * @return 自身
     */
    public Crypto setParams(AlgorithmParameterSpec params) {
        this.ciphers.setParams(params);
        return this;
    }

    /**
     * 设置偏移向量
     *
     * @param iv {@link IvParameterSpec}偏移向量
     * @return 自身
     */
    public Crypto setIv(IvParameterSpec iv) {
        return setParams(iv);
    }

    /**
     * 设置偏移向量
     *
     * @param iv 偏移向量，加盐
     * @return 自身
     */
    public Crypto setIv(byte[] iv) {
        setIv(new IvParameterSpec(iv));
        return this;
    }

    /**
     * 设置随机数生成器，可自定义随机数种子
     *
     * @param random 随机数生成器，可自定义随机数种子
     * @return this
     */
    public Crypto setRandom(SecureRandom random) {
        this.ciphers.setRandom(random);
        return this;
    }

    /**
     * 初始化模式并清空数据
     *
     * @param mode 模式枚举
     * @return this
     */
    public Crypto setMode(Mode.Cipher mode) {
        lock.lock();
        try {
            initMode(mode.getValue());
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            lock.unlock();
        }
        return this;
    }

    /**
     * 更新数据，分组加密中间结果可以当作随机数
     * 第一次更新数据前需要调用{@link #setMode(Mode.Cipher)}初始化加密或解密模式，然后每次更新数据都是累加模式
     *
     * @param data 被加密的bytes
     * @return update之后的bytes
     */
    public byte[] update(byte[] data) {
        final Cipher cipher = this.ciphers.getCipher();
        lock.lock();
        try {
            return cipher.update(paddingDataWithZero(data, cipher.getBlockSize()));
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 更新数据，分组加密中间结果可以当作随机数
     * 第一次更新数据前需要调用{@link #setMode(Mode.Cipher)}初始化加密或解密模式，然后每次更新数据都是累加模式
     *
     * @param data 被加密的bytes
     * @return update之后的hex数据
     */
    public String updateHex(byte[] data) {
        return HexKit.encodeHexStr(update(data));
    }

    @Override
    public byte[] encrypt(byte[] data) {
        lock.lock();
        try {
            final javax.crypto.Cipher cipher = initMode(javax.crypto.Cipher.ENCRYPT_MODE);
            return cipher.doFinal(paddingDataWithZero(data, cipher.getBlockSize()));
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void encrypt(InputStream data, OutputStream out, boolean isClose) throws CryptoException {
        lock.lock();
        CipherOutputStream cipherOutputStream = null;
        try {
            final javax.crypto.Cipher cipher = initMode(javax.crypto.Cipher.ENCRYPT_MODE);
            cipherOutputStream = new CipherOutputStream(out, cipher);
            long length = IoKit.copy(data, cipherOutputStream);
            if (this.isZeroPadding) {
                final int blockSize = cipher.getBlockSize();
                if (blockSize > 0) {
                    // 按照块拆分后的数据中多余的数据
                    final int remainLength = (int) (length % blockSize);
                    if (remainLength > 0) {
                        // 补充0
                        cipherOutputStream.write(new byte[blockSize - remainLength]);
                        cipherOutputStream.flush();
                    }
                }
            }
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            lock.unlock();
            // CipherOutputStream必须关闭，才能完全写出
            IoKit.close(cipherOutputStream);
            if (isClose) {
                IoKit.close(data);
            }
        }
    }

    @Override
    public byte[] decrypt(byte[] bytes) {
        final int blockSize;
        final byte[] decryptData;

        lock.lock();
        try {
            final javax.crypto.Cipher cipher = initMode(javax.crypto.Cipher.DECRYPT_MODE);
            blockSize = cipher.getBlockSize();
            decryptData = cipher.doFinal(bytes);
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            lock.unlock();
        }

        return removePadding(decryptData, blockSize);
    }

    @Override
    public void decrypt(InputStream data, OutputStream out, boolean isClose) throws CryptoException {
        lock.lock();
        CipherInputStream cipherInputStream = null;
        try {
            final javax.crypto.Cipher cipher = initMode(javax.crypto.Cipher.DECRYPT_MODE);
            cipherInputStream = new CipherInputStream(data, cipher);
            if (this.isZeroPadding) {
                final int blockSize = cipher.getBlockSize();
                if (blockSize > 0) {
                    copyForZeroPadding(cipherInputStream, out, blockSize);
                    return;
                }
            }
            IoKit.copy(cipherInputStream, out);
        } catch (IOException e) {
            throw new CryptoException(e);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException(e);
        } finally {
            lock.unlock();
            // CipherOutputStream必须关闭，才能完全写出
            IoKit.close(cipherInputStream);
            if (isClose) {
                IoKit.close(data);
            }
        }
    }

    /**
     * 初始化加密解密参数，如IV等
     *
     * @param algorithm  算法
     * @param paramsSpec 用户定义的{@link AlgorithmParameterSpec}
     * @return this
     */
    private Crypto initParams(String algorithm, AlgorithmParameterSpec paramsSpec) {
        if (null == paramsSpec) {
            byte[] iv = Optional.ofNullable(ciphers)
                    .map(Ciphers::getCipher).map(Cipher::getIV).get();

            // 随机IV
            if (StringKit.startWithIgnoreCase(algorithm, "PBE")) {
                // 对于PBE算法使用随机数加盐
                if (null == iv) {
                    iv = RandomKit.randomBytes(8);
                }
                paramsSpec = new PBEParameterSpec(iv, 100);
            } else if (StringKit.startWithIgnoreCase(algorithm, Algorithm.AES.getValue())) {
                if (null != iv) {
                    // AES使用Cipher默认的随机盐
                    paramsSpec = new IvParameterSpec(iv);
                }
            }
        }

        return setParams(paramsSpec);
    }

    /**
     * 初始化{@link javax.crypto.Cipher}为加密或者解密模式
     *
     * @param mode 模式，见{@link javax.crypto.Cipher#ENCRYPT_MODE} 或 {@link javax.crypto.Cipher#DECRYPT_MODE}
     * @return {@link javax.crypto.Cipher}
     * @throws InvalidKeyException                无效key
     * @throws InvalidAlgorithmParameterException 无效算法
     */
    private javax.crypto.Cipher initMode(int mode) throws InvalidKeyException, InvalidAlgorithmParameterException {
        return this.ciphers.initMode(mode, this.secretKey).getCipher();
    }

    /**
     * 数据按照blockSize的整数倍长度填充填充0
     * 在{@link Padding#ZeroPadding} 模式下，且数据长度不是blockSize的整数倍才有效，否则返回原数据
     * 见：https://blog.csdn.net/OrangeJack/article/details/82913804
     *
     * @param data      数据
     * @param blockSize 块大小
     * @return 填充后的数据，如果isZeroPadding为false或长度刚好，返回原数据
     */
    private byte[] paddingDataWithZero(byte[] data, int blockSize) {
        if (this.isZeroPadding) {
            final int length = data.length;
            // 按照块拆分后的数据中多余的数据
            final int remainLength = length % blockSize;
            if (remainLength > 0) {
                // 新长度为blockSize的整数倍，多余部分填充0
                return ArrayKit.resize(data, length + blockSize - remainLength);
            }
        }
        return data;
    }

    /**
     * 数据按照blockSize去除填充部分，用于解密
     * 在{@link Padding#ZeroPadding} 模式下，且数据长度不是blockSize的整数倍才有效，否则返回原数据
     *
     * @param data      数据
     * @param blockSize 块大小，必须大于0
     * @return 去除填充后的数据，如果isZeroPadding为false或长度刚好，返回原数据
     */
    private byte[] removePadding(byte[] data, int blockSize) {
        if (this.isZeroPadding && blockSize > 0) {
            final int length = data.length;
            final int remainLength = length % blockSize;
            if (remainLength == 0) {
                // 解码后的数据正好是块大小的整数倍，说明可能存在补0的情况，去掉末尾所有的0
                int i = length - 1;
                while (i >= 0 && 0 == data[i]) {
                    i--;
                }
                return ArrayKit.resize(data, i + 1);
            }
        }
        return data;
    }

}
