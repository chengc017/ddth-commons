package com.github.ddth.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.serial.io.JBossObjectOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serialization helper class.
 * 
 * <ul>
 * <li>JSON serialization: use {@code com.fasterxml.jackson} library.</li>
 * <li>Binary serialization: 2 choices of API
 * <ul>
 * <li>{@code jboss-serialization} library, or</li>
 * <li>{@code Kryo} library</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.2.0
 */
public class SerializationUtils {

    /*----------------------------------------------------------------------*/
    private static KryoPool kryoPool;
    static {
        KryoFactory factory = new KryoFactory() {
            public Kryo create() {
                Kryo kryo = new Kryo();
                return kryo;
            }
        };
        Queue<Kryo> queue = new LinkedBlockingQueue<Kryo>(100);
        kryoPool = new KryoPool.Builder(factory).queue(queue).softReferences().build();
    }

    /**
     * Serializes an object to byte array.
     * 
     * <p>
     * This method uses Kryo lib.
     * </p>
     * 
     * @param obj
     * @return
     */
    public static byte[] toByteArrayKryo(Object obj) {
        return toByteArrayKryo(obj, null);
    }

    /**
     * Serializes an object to byte array, with a custom class loader.
     * 
     * <p>
     * This method uses Kryo lib.
     * </p>
     * 
     * @param obj
     * @param classLoader
     * @return
     */
    public static byte[] toByteArrayKryo(final Object obj, ClassLoader classLoader) {
        ClassLoader oldClassLoader = classLoader != null ? Thread.currentThread()
                .getContextClassLoader() : null;
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            return kryoPool.run(new KryoCallback<byte[]>() {
                @Override
                public byte[] execute(Kryo kryo) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Output output = new Output(baos);
                    kryo.writeObject(output, obj);
                    output.flush();
                    output.close();
                    return baos.toByteArray();
                }
            });
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            if (oldClassLoader != null) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }

    /**
     * Deserializes a byte array back to an object.
     * 
     * <p>
     * This method uses Kryo lib.
     * </p>
     * 
     * @param data
     * @return
     */
    public static Object fromByteArrayKryo(byte[] data) {
        return fromByteArrayKryo(data, Object.class, null);
    }

    /**
     * Deserializes a byte array back to an object, with custom class loader.
     * 
     * @param data
     * @param classLoader
     * @return
     */
    public static Object fromByteArrayKryo(byte[] data, ClassLoader classLoader) {
        return fromByteArrayKryo(data, Object.class, classLoader);
    }

    /**
     * Deserializes a byte array back to an object.
     * 
     * @param data
     * @param clazz
     * @return
     */
    public static <T> T fromByteArrayKryo(byte[] data, Class<T> clazz) {
        return fromByteArrayKryo(data, clazz, null);
    }

    /**
     * Deserializes a byte array back to an object, with custom class loader.
     * 
     * @param data
     * @param clazz
     * @param classLoader
     * @return
     */
    public static <T> T fromByteArrayKryo(final byte[] data, final Class<T> clazz,
            ClassLoader classLoader) {
        ClassLoader oldClassLoader = classLoader != null ? Thread.currentThread()
                .getContextClassLoader() : null;
        if (classLoader != null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        try {
            return kryoPool.run(new KryoCallback<T>() {
                @Override
                public T execute(Kryo kryo) {
                    Input input = new Input(new ByteArrayInputStream(data));
                    return kryo.readObject(input, clazz);
                }
            });
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            if (oldClassLoader != null) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }

    /*----------------------------------------------------------------------*/
    private final static ObjectPool<ObjectMapper> poolMapper = new GenericObjectPool<ObjectMapper>(
            new BasePooledObjectFactory<ObjectMapper>() {
                @Override
                public ObjectMapper create() throws Exception {
                    return new ObjectMapper();
                }

                @Override
                public PooledObject<ObjectMapper> wrap(ObjectMapper objMapper) {
                    return new DefaultPooledObject<ObjectMapper>(objMapper);
                }
            });
    static {
        GenericObjectPool<ObjectMapper> pool = (GenericObjectPool<ObjectMapper>) poolMapper;
        pool.setMaxIdle(1);
        pool.setMaxTotal(100);
        pool.setMaxWaitMillis(5000);
    }

    /**
     * Serializes an object to byte array.
     * 
     * <p>
     * This method uses jboss-serialization lib.
     * </p>
     * 
     * @param obj
     * @return
     */
    public static byte[] toByteArray(Object obj) {
        return toByteArray(obj, null);
    }

    /**
     * Serializes an object to byte array, with a custom class loader.
     * 
     * <p>
     * This method uses jboss-serialization lib.
     * </p>
     * 
     * @param obj
     * @param classLoader
     * @return
     */
    public static byte[] toByteArray(Object obj, ClassLoader classLoader) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JBossObjectOutputStream oos = new JBossObjectOutputStream(baos);
            ClassLoader oldClassLoader = classLoader != null ? Thread.currentThread()
                    .getContextClassLoader() : null;
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            try {
                oos.writeObject(obj);
                oos.flush();
                oos.close();
                return baos.toByteArray();
            } finally {
                if (oldClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            }
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    /**
     * Deserializes a byte array back to an object.
     * 
     * <p>
     * This method uses jboss-serialization lib.
     * </p>
     * 
     * @param data
     * @return
     */
    public static Object fromByteArray(byte[] data) {
        return fromByteArray(data, Object.class, null);
    }

    /**
     * Deserializes a byte array back to an object, with custom class loader.
     * 
     * <p>
     * This method uses jboss-serialization lib.
     * </p>
     * 
     * @param data
     * @param classLoader
     * @return
     */
    public static Object fromByteArray(byte[] data, ClassLoader classLoader) {
        return fromByteArray(data, Object.class, classLoader);
    }

    /**
     * Deserializes a byte array back to an object.
     * 
     * <p>
     * This method uses jboss-serialization lib.
     * </p>
     * 
     * @param data
     * @param clazz
     * @return
     */
    public static <T> T fromByteArray(byte[] data, Class<T> clazz) {
        return fromByteArray(data, clazz, null);
    }

    /**
     * Deserializes a byte array back to an object, with custom class loader.
     * 
     * <p>
     * This method uses jboss-serialization lib.
     * </p>
     * 
     * @param data
     * @param clazz
     * @param classLoader
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromByteArray(byte[] data, Class<T> clazz, ClassLoader classLoader) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            JBossObjectInputStream ois;
            if (classLoader != null) {
                ois = new JBossObjectInputStream(bais, classLoader);
            } else {
                ois = new JBossObjectInputStream(bais);
            }
            Object obj = ois.readObject();
            ois.close();
            if (obj != null && clazz.isAssignableFrom(obj.getClass())) {
                return (T) obj;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    /*----------------------------------------------------------------------*/
    /**
     * Serializes an object to JSON string.
     * 
     * @param obj
     * @return
     */
    public static String toJsonString(Object obj) {
        return toJsonString(obj, null);
    }

    /**
     * Serializes an object to JSON string, with a custom class loader.
     * 
     * @param obj
     * @param classLoader
     * @return
     */
    public static String toJsonString(Object obj, ClassLoader classLoader) {
        try {
            ObjectMapper mapper = poolMapper.borrowObject();
            ClassLoader oldClassLoader = classLoader != null ? Thread.currentThread()
                    .getContextClassLoader() : null;
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            try {
                try {
                    return mapper.writeValueAsString(obj);
                } finally {
                    poolMapper.returnObject(mapper);
                }
            } finally {
                if (oldClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            }
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    /**
     * Deserializes a JSON string.
     * 
     * @param jsonString
     * @return
     */
    public static Object fromJsonString(String jsonString) {
        return fromJsonString(jsonString, Object.class, null);
    }

    /**
     * Deserializes a JSON string, with custom class loader.
     * 
     * @param jsonString
     * @param classLoader
     * @return
     */
    public static Object fromJsonString(String jsonString, ClassLoader classLoader) {
        return fromJsonString(jsonString, Object.class, classLoader);
    }

    /**
     * Deserializes a JSON string.
     * 
     * @param jsonString
     * @param clazz
     * @return
     */
    public static <T> T fromJsonString(String jsonString, Class<T> clazz) {
        return fromJsonString(jsonString, clazz, null);
    }

    /**
     * Deserializes a JSON string, with custom class loader.
     * 
     * @param jsonString
     * @param clazz
     * @return
     */
    public static <T> T fromJsonString(String jsonString, Class<T> clazz, ClassLoader classLoader) {
        try {
            ObjectMapper mapper = poolMapper.borrowObject();
            ClassLoader oldClassLoader = classLoader != null ? Thread.currentThread()
                    .getContextClassLoader() : null;
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            try {
                try {
                    return jsonString != null ? mapper.readValue(jsonString, clazz) : null;
                } finally {
                    poolMapper.returnObject(mapper);
                }
            } finally {
                if (oldClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            }
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}
