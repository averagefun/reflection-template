package org.example.generator;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    private final Generator generator = new Generator(new Random(42));

    static Stream<Class<?>> simpleTypes() {
        return Stream.of(boolean.class, Boolean.class, byte.class, Byte.class,
                short.class, Short.class, int.class, Integer.class, long.class, Long.class,
                float.class, Float.class, double.class, Double.class, char.class, Character.class);
    }

    @ParameterizedTest
    @MethodSource("simpleTypes")
    void canGeneratePrimitivesAndWrappers(Class<?> type) {
        Object value = generator.generateValueOfType(type);
        Class<?> expected = type.isPrimitive() ? Array.get(Array.newInstance(type, 1), 0).getClass() : type;
        assertInstanceOf(expected, value);
        if (value instanceof Float number) assertTrue(Float.isFinite(number));
        if (value instanceof Double number) assertTrue(Double.isFinite(number));
        if (value instanceof Character character) assertTrue(character >= 'a' && character <= 'z');
    }
}
