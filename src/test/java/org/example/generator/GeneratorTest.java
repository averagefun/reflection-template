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
import java.util.Objects;
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

    @Test
    void doesNotReturnTheSameValuesEveryTime() {
        Set<Integer> numbers = new HashSet<>();
        Set<String> strings = new HashSet<>();
        Set<Color> colors = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int number = generator.generateValueOfType(int.class);
            assertTrue(number >= -1000 && number <= 1000);
            numbers.add(number);
            String text = generator.generateValueOfType(String.class);
            assertTrue(text.matches("[a-z]{1,12}"));
            strings.add(text);
            colors.add(generator.generateValueOfType(Color.class));
        }
        assertTrue(numbers.size() > 1);
        assertTrue(strings.size() > 1);
        assertTrue(colors.size() > 1);
        assertInstanceOf(String.class, generator.generateValueOfType(Object.class));
    }

    static Stream<Class<?>> arrayTypes() {
        return Stream.of(int[].class, boolean[].class, double[].class, Integer[].class,
                String[].class, Color[].class, Rectangle[].class, Shape[].class, int[][].class);
    }

    @ParameterizedTest
    @MethodSource("arrayTypes")
    void fillsArrays(Class<?> type) {
        Object array = generator.generateValueOfType(type);
        assertInstanceOf(type, array);
        assertTrue(Array.getLength(array) >= 1 && Array.getLength(array) <= 3);
        for (int i = 0; i < Array.getLength(array); i++) {
            assertNotNull(Array.get(array, i));
        }
    }

    @Test
    void fillsListsAndMaps() {
        for (Class<?> type : List.of(List.class, ArrayList.class)) {
            List<?> list = (List<?>) generator.generateValueOfType(type);
            assertInstanceOf(type, list);
            assertTrue(list.size() >= 1 && list.size() <= 3);
            list.forEach(value -> assertInstanceOf(String.class, value));
        }
        for (Class<?> type : List.of(Map.class, HashMap.class, LinkedHashMap.class)) {
            Map<?, ?> map = (Map<?, ?>) generator.generateValueOfType(type);
            assertInstanceOf(type, map);
            assertTrue(map.size() >= 1 && map.size() <= 3);
            map.forEach((key, value) -> {
                assertInstanceOf(String.class, key);
                assertInstanceOf(String.class, value);
            });
        }
    }

    @Test
    void worksWithExampleClasses() {
        assertTrue(generator.generateValueOfType(Example.class).toString().matches("Example\\(-?\\d+\\)"));
        assertTrue(Double.isFinite(generator.generateValueOfType(Rectangle.class).getArea()));
        assertTrue(Double.isFinite(generator.generateValueOfType(Triangle.class).getPerimeter()));
        Product product = generator.generateValueOfType(Product.class);
        assertNotNull(product.getName());
        assertTrue(Double.isFinite(product.getPrice()));
        Cart cart = generator.generateValueOfType(Cart.class);
        assertFalse(cart.getItems().isEmpty());
        cart.getItems().forEach(item -> {
            assertInstanceOf(Product.class, item);
            assertNotNull(item.getName());
        });
    }

    @Test
    void shapeCanBeRectangleOrTriangle() {
        Set<Class<?>> types = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Shape shape = generator.generateValueOfType(Shape.class);
            assertNotNull(shape);
            assertTrue(shape instanceof Rectangle || shape instanceof Triangle);
            types.add(shape.getClass());
        }
        assertEquals(Set.of(Rectangle.class, Triangle.class), types);
    }

    @Test
    void worksWithNewClassesToo() {
        Order order = generator.generateValueOfType(Order.class);
        assertNotNull(order.customer.name);
        assertFalse(order.products.isEmpty());
        order.products.forEach((key, values) -> {
            assertNotNull(key);
            assertFalse(values.isEmpty());
            values.forEach(value -> assertNotNull(value.getName()));
        });
        assertInstanceOf(Empty.class, generator.generateValueOfType(Empty.class));
        assertNotNull(generator.generateValueOfType(Label.class).text());
    }

    @Test
    void createsACatForAnimal() {
        Animal animal = generator.generateValueOfType(Animal.class);
        assertInstanceOf(Cat.class, animal);
        assertNotNull(((Cat) animal).name);
    }

    @Test
    void doesNotGetStuckInCycles() {
        BinaryTreeNode tree = generator.generateValueOfType(BinaryTreeNode.class);
        assertNotNull(tree.getData());
        assertNull(tree.getLeft());
        assertNull(tree.getRight());
        First first = generator.generateValueOfType(First.class);
        assertNotNull(first.second);
        assertNull(first.second.first);
        Pair pair = generator.generateValueOfType(Pair.class);
        assertNotNull(pair.first);
        assertNotNull(pair.second);
        assertNotSame(pair.first, pair.second);
    }

    @Test
    void triesAnotherConstructorOrImplementation() {
        for (int i = 0; i < 20; i++) {
            assertNotNull(generator.generateValueOfType(Fallback.class).name);
            assertInstanceOf(Working.class, generator.generateValueOfType(Service.class));
        }
    }

    @Test
    void skipsRecursiveImplementationAndTriesAnother() {
        Branch branch = new Generator(new Random(0)).generateValueOfType(Branch.class);
        assertInstanceOf(Leaf.class, branch.child());
    }

    static Stream<Class<?>> unsupportedTypes() {
        return Stream.of(void.class, Void.class, Unannotated.class, MissingImplementations.class,
                WrongImplementation.class, AbstractImplementation.class, UnmarkedImplementation.class,
                ConcreteWithImplementations.class, EmptyEnum.class, LinkedList.class,
                WildcardList.class, Generic.class, GenericArray.class, Inner.class);
    }

    @ParameterizedTest
    @MethodSource("unsupportedTypes")
    void failsForUnsupportedTypes(Class<?> type) {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(type));
        assertTrue(error.getMessage().contains(type.getTypeName()));
    }

    @Test
    void keepsTheOriginalConstructorError() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(Broken.class));
        assertTrue(error.getMessage().contains("no constructor"));
        assertEquals(1, error.getSuppressed().length);
        assertInstanceOf(IllegalStateException.class, error.getSuppressed()[0].getCause());
        assertEquals("unavailable", error.getSuppressed()[0].getCause().getMessage());
        assertThrows(GenerationException.class, () -> generator.generateValueOfType(null));
    }

    @Test
    void stopsWhenArraysAreTooDeep() {
        Class<?> type = String.class;
        for (int i = 0; i < 18; i++) {
            type = Array.newInstance(type, 0).getClass();
        }
        Class<?> nestedArray = type;
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(nestedArray));
        assertTrue(error.getMessage().contains("maximum depth exceeded"));
    }

    enum Color { RED, GREEN, BLUE }
    enum EmptyEnum { }

    @Generatable
    record Label(String text) { }

    @Generatable
    static class Customer {
        final String name;
        private Customer(String name) { this.name = name; }
    }

    @Generatable
    record Order(Customer customer, Map<String, List<Product>> products) { }

    @Generatable
    static class Empty { }

    @Generatable(implementations = Cat.class)
    abstract static class Animal { }

    @Generatable
    static class Cat extends Animal {
        final String name;
        Cat(String name) { this.name = name; }
    }

    @Generatable
    record First(Second second) { }

    @Generatable
    record Second(First first) { }

    @Generatable
    record Pair(Customer first, Customer second) { }

    @Generatable(implementations = {Branch.class, Leaf.class})
    interface Node { }

    @Generatable
    record Branch(Node child) implements Node {
        Branch { Objects.requireNonNull(child); }
    }

    @Generatable
    static class Leaf implements Node { }

    @Generatable
    static class Fallback {
        final String name;
        Fallback() { throw new IllegalStateException("unavailable"); }
        Fallback(String name) { this.name = name; }
    }

    @Generatable(implementations = {Broken.class, Working.class})
    interface Service { }

    @Generatable
    static class Broken implements Service {
        Broken() { throw new IllegalStateException("unavailable"); }
    }

    @Generatable
    static class Working implements Service { }

    static class Unannotated { }

    @Generatable
    interface MissingImplementations { }

    @Generatable(implementations = Customer.class)
    interface WrongImplementation { }

    @Generatable(implementations = AbstractImplementation.class)
    abstract static class AbstractImplementation { }

    @Generatable(implementations = Unmarked.class)
    interface UnmarkedImplementation { }

    static class Unmarked implements UnmarkedImplementation { }

    @Generatable(implementations = Customer.class)
    static class ConcreteWithImplementations { }

    @Generatable
    record WildcardList(List<?> values) { }

    @Generatable
    record Generic<T>(T value) { }

    @Generatable
    record GenericArray<T>(T[] values) { }

    @Generatable
    class Inner { }
}
