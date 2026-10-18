package org.example;

import org.example.classes.Cart;
import org.example.classes.Shape;
import org.example.generator.Generator;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        System.out.println(gen.generateValueOfType(Integer.class));
        System.out.println(gen.generateValueOfType(String.class));
        Shape shape = gen.generateValueOfType(Shape.class);
        System.out.println("Shape: " + shape.getClass().getSimpleName());
        Cart cart = gen.generateValueOfType(Cart.class);
        cart.getItems().forEach(product ->
                System.out.println("Product: " + product.getName() + ", price: " + product.getPrice()));
    }
}
