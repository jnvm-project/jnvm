package eu.telecomsudparis.jnvm.transformer;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.pool.TypePool;

import eu.telecomsudparis.jnvm.offheap.OffHeapObject;

class JNVMTransformerPluginTest {

    private static final String SAMPLE_PACKAGE = "eu.telecomsudparis.jnvm.transformer.sample.";
    private static Stream<String> types() {
        return Stream.of(
                "FourLongs",
                "Simple",
                "Complexe",
                "InvalidSimple")
            .map(SAMPLE_PACKAGE::concat);
    }

    private static Map<String, byte[]> instrumentedTypes;

    private Plugin plugin;
    private ClassFileLocator classFileLocator;
    private TypePool typePool;

    @BeforeAll
    static void setupAll() {
        instrumentedTypes = new HashMap<>();
    }

    @BeforeEach
    void setup() {
        plugin = new JNVMTransformerPlugin();
        classFileLocator = new ClassFileLocator.Compound(
                new ClassFileLocator.Simple(instrumentedTypes),
                ClassFileLocator.ForClassLoader.of(getClass().getClassLoader()));
        typePool = new TypePool.Default(
                new TypePool.CacheProvider.Simple(), classFileLocator,
                TypePool.Default.ReaderMode.FAST);
    }

    @ParameterizedTest
    @MethodSource("types")
    public void testMatches(String typeName) throws Exception {
        TypeDescription type = TypePool.Default.ofSystemLoader().describe(typeName).resolve();
        Assertions.assertTrue(plugin.matches(type));
    }

    @ParameterizedTest
    @MethodSource("types")
    public void testApply(String typeName) throws Exception {
        TypeDescription type = typePool.describe(typeName).resolve();

        DynamicType.Unloaded<?> dynamicType =
            plugin.apply(
                new ByteBuddy().redefine(type, classFileLocator), type, classFileLocator)
            .make();

        byte[] typeBytes = classFileLocator.locate(typeName).resolve();
        byte[] dynamicTypeBytes = dynamicType.getBytes();
        Assertions.assertNotNull(typeBytes);
        Assertions.assertNotNull(dynamicTypeBytes);
        Assertions.assertNotSame(dynamicTypeBytes, typeBytes);
        Assertions.assertNotEquals(dynamicTypeBytes, typeBytes);

        instrumentedTypes.put(type.getName(), dynamicTypeBytes);
        Class<?> transformed = dynamicType
            .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
            .getLoaded();

        Assertions.assertNotNull(transformed);
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
        plugin = null;
        classFileLocator = null;
        typePool = null;
    }

    @AfterAll
    static void tearDownAll() {
        instrumentedTypes = null;
    }

}
