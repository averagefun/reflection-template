package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Generator {
    private static final int MAX_DEPTH = 16;
    private final Random random;

    public Generator() {
        this(new Random());
    }

    public Generator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> type) {
        if (type == null) {
            throw new GenerationException("type is null");
        }
        return (T) generate(type, new ArrayList<>(), 0);
    }

    private Object generate(Type type, List<Class<?>> path, int depth) {
        if (depth > MAX_DEPTH) {
            throw failure(type, "too deep");
        }
        if (type instanceof ParameterizedType parameterized) {
            Type raw = parameterized.getRawType();
            Type[] arguments = parameterized.getActualTypeArguments();
            if (raw instanceof Class<?> rawClass) {
                if (isList(rawClass)) return generateList(arguments[0], path, depth);
                if (isMap(rawClass)) return generateMap(arguments[0], arguments[1], path, depth);
            }
            throw failure(type, "unsupported");
        }
        if (!(type instanceof Class<?> clazz)) {
            throw failure(type, "unsupported");
        }
        if (clazz == void.class || clazz == Void.class) {
            throw failure(type, "unsupported");
        }
        if (clazz == boolean.class || clazz == Boolean.class) return random.nextBoolean();
        if (clazz == byte.class || clazz == Byte.class) return (byte) random.nextInt(256);
        if (clazz == short.class || clazz == Short.class) return (short) random.nextInt(65536);
        if (clazz == int.class || clazz == Integer.class) return random.nextInt(2001) - 1000;
        if (clazz == long.class || clazz == Long.class) return random.nextLong();
        if (clazz == float.class || clazz == Float.class) return random.nextFloat() * 2000 - 1000;
        if (clazz == double.class || clazz == Double.class) return random.nextDouble() * 2000 - 1000;
        if (clazz == char.class || clazz == Character.class) return (char) ('a' + random.nextInt(26));
        if (clazz == String.class || clazz == Object.class) {
            int length = random.nextInt(12) + 1;
            StringBuilder value = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                value.append((char) ('a' + random.nextInt(26)));
            }
            return value.toString();
        }
        if (clazz.isEnum()) {
            Object[] constants = clazz.getEnumConstants();
            if (constants.length == 0) throw failure(type, "empty enum");
            return constants[random.nextInt(constants.length)];
        }
        if (clazz.isArray()) {
            int length = random.nextInt(3) + 1;
            Object array = Array.newInstance(clazz.getComponentType(), length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, generate(clazz.getComponentType(), path, depth + 1));
            }
            return array;
        }
        if (isList(clazz)) return generateList(String.class, path, depth);
        if (isMap(clazz)) return generateMap(String.class, String.class, path, depth);

        Generatable annotation = clazz.getDeclaredAnnotation(Generatable.class);
        if (annotation == null) throw failure(type, "missing @Generatable");
        if (path.contains(clazz)) return null;
        List<Class<?>> nextPath = new ArrayList<>(path);
        nextPath.add(clazz);
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return generateImplementation(clazz, annotation, nextPath, depth);
        }
        if (annotation.implementations().length != 0) {
            throw failure(type, "unexpected implementations");
        }
        return generateObject(clazz, nextPath, depth);
    }

    private List<Object> generateList(Type elementType, List<Class<?>> path, int depth) {
        int size = random.nextInt(3) + 1;
        List<Object> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(generate(elementType, path, depth + 1));
        }
        return result;
    }

    private Map<Object, Object> generateMap(Type keyType, Type valueType, List<Class<?>> path, int depth) {
        int size = random.nextInt(3) + 1;
        Map<Object, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            result.put(generate(keyType, path, depth + 1), generate(valueType, path, depth + 1));
        }
        return result;
    }

    private Object generateImplementation(Class<?> type, Generatable annotation, List<Class<?>> path, int depth) {
        List<Class<?>> implementations = new ArrayList<>(Arrays.asList(annotation.implementations()));
        if (implementations.isEmpty()) {
            throw failure(type, "no implementations");
        }
        for (Class<?> implementation : implementations) {
            if (!type.isAssignableFrom(implementation) || implementation.isInterface()
                    || Modifier.isAbstract(implementation.getModifiers())
                    || implementation.getDeclaredAnnotation(Generatable.class) == null) {
                throw failure(type, "bad implementation: " + implementation.getTypeName());
            }
        }
        Collections.shuffle(implementations, random);
        GenerationException error = failure(type, "implementations failed");
        for (Class<?> implementation : implementations) {
            if (path.contains(implementation)) continue;
            try {
                return generate(implementation, path, depth + 1);
            } catch (GenerationException cause) {
                error.addSuppressed(cause);
            }
        }
        throw error;
    }

    private Object generateObject(Class<?> type, List<Class<?>> path, int depth) {
        if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) {
            throw failure(type, "needs an outer object");
        }
        List<Constructor<?>> constructors = new ArrayList<>(Arrays.asList(type.getDeclaredConstructors()));
        Collections.shuffle(constructors, random);
        GenerationException error = failure(type, "constructors failed");
        for (Constructor<?> constructor : constructors) {
            try {
                if (!constructor.trySetAccessible()) {
                    throw failure(type, "constructor blocked: " + constructor);
                }
                Type[] parameterTypes = constructor.getGenericParameterTypes();
                Object[] arguments = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    arguments[i] = generate(parameterTypes[i], path, depth + 1);
                }
                return constructor.newInstance(arguments);
            } catch (InvocationTargetException cause) {
                error.addSuppressed(new GenerationException("constructor threw: " + constructor, cause.getCause()));
            } catch (ReflectiveOperationException | RuntimeException cause) {
                error.addSuppressed(new GenerationException("constructor failed: " + constructor, cause));
            }
        }
        throw error;
    }

    private boolean isList(Class<?> type) {
        return type == List.class || type == ArrayList.class;
    }

    private boolean isMap(Class<?> type) {
        return type == Map.class || type == HashMap.class || type == LinkedHashMap.class;
    }

    private GenerationException failure(Type type, String reason) {
        return new GenerationException(type.getTypeName() + ": " + reason);
    }
}
