package eu.telecomsudparis.jnvm.transformer;

import java.util.Collection;
import java.util.Arrays;
import java.util.stream.Stream;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.transformer.sample.*;

class JNVMTransformerPluginTest {

    static Stream<Class<?>> types() {
        return Stream.of(
            FourLongs.class,
            Simple.class,
            Complexe.class,
            InvalidSimple.class
        );
    }

    private Plugin plugin;

    @BeforeAll
    static void setupAll() {
        //Clear
    }

    @BeforeEach
    void setup() {
        plugin = new JNVMTransformerPlugin();
    }

    @ParameterizedTest
    @MethodSource("types")
    public void testMatches(Class<?> type) throws Exception {
        Assertions.assertTrue(plugin.matches(TypeDescription.ForLoadedType.of(type)));
    }

    @ParameterizedTest
    @MethodSource("types")
    public void testApply(Class<?> type) throws Exception {
        Class<?> transformed =
            plugin.apply(
                new ByteBuddy().redefine(type),
                TypeDescription.ForLoadedType.of(type),
                ClassFileLocator.ForClassLoader.of(type.getClassLoader()))
            .make()
            .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
            .getLoaded();

        Assertions.assertNotNull(transformed);
        Assertions.assertNotSame(transformed, type);
        Assertions.assertNotEquals(transformed, type);

        Assertions.assertTrue(isPersistent(transformed), "implements OffHeapObject");
        Assertions.assertTrue(hasField(transformed, "CLASS_ID"), "has CLASS_ID");
        Assertions.assertTrue(hasField(transformed, "SIZE"), "has SIZE");
        Assertions.assertTrue(hasOnlyTransientInstanceFields(transformed), "has only transient fields");
        Assertions.assertTrue(hasReconstructor(transformed), "has Reconstructor");
    }

    /* TODO:
     * -has get/set for persistent fields
     * -has FA-wrap public methods
     */
    private static boolean isPersistent(Class<?> transformed) {
        return OffHeapObject.class.isAssignableFrom(transformed);
    }
    private static boolean hasField(Class<?> transformed, String field) {
        try {
            return transformed.getDeclaredField(field) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
    private static boolean hasOnlyTransientInstanceFields(Class<?> transformed) {
        return Arrays.stream(transformed.getFields())
                    .filter(f -> ! Modifier.isStatic(f.getModifiers()))
                    .allMatch(f -> Modifier.isTransient(f.getModifiers()));
    }
    private static boolean hasReconstructor(Class<?> transformed) {
        try {
            return transformed.getConstructor(Void.class, long.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @AfterEach
    void tearDown() {
        //Clear
    }

    @AfterAll
    static void tearDownAll() {
        //Clear
    }

}
