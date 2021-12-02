package eu.telecomsudparis.jnvm.transformer;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberRemoval;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class JNVMTransformerPlugin implements Plugin {

    private static final MethodDescription REGISTER_USER_KLASS =
        TypeDescription.ForLoadedType.of(OffHeap.Klass.class)
            .getDeclaredMethods()
            .filter(named("registerUserKlass").and(takesArguments(Class.class)))
            .getOnly();

    private static final MethodDescription REC_INSTANCE =
        TypeDescription.ForLoadedType.of(OffHeap.class)
            .getDeclaredMethods()
            .filter(named("recInstance"))
            .getOnly();

    static <T extends FieldDescription> ElementMatcher.Junction<T> isPersistable() {
        return not( isStatic().or(isTransient()) );
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultConstructorOf(TypeDescription type) {
        return isDefaultConstructor().and(isDeclaredBy(type));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isGetterFor(FieldDescription field) {
        return named(getterNameFor(field)).and(isGenericGetter(field.getType()));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isSetterFor(FieldDescription field) {
        return named(setterNameFor(field)).and(isGenericSetter(field.getType()));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isAccessor() {
        return nameStartsWith("get").or(nameStartsWith("set"));
    }

    private static String firstToUpperCase(String str) {
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }
    public static String getterNameFor(FieldDescription field) {
        return "get" + firstToUpperCase(field.getName());
    }
    public static String setterNameFor(FieldDescription field) {
        return "set" + firstToUpperCase(field.getName());
    }

    static class SIZE {
        private static final Map<TypeDescription, Long> cache = new HashMap<>();

        protected static long of(TypeDescription type) {
            return cache.computeIfAbsent(type, t -> computeFor(t) + ofParent(t));
        }

        protected static long ofParent(TypeDescription type) {
            TypeDescription superType = type.getSuperClass().asErasure();
            return (superType == null
                    || superType.represents(Object.class)
                    || !superType.isAssignableTo(OffHeapObject.class))
                ? 0L : of(superType);
        }

        private static long computeFor(TypeDescription type) {
            return type.getDeclaredFields()
                .filter(isPersistable())
                .stream().map(t -> {
                    int typeSort = Type.getType(t.getDescriptor()).getSort();
                    long ret = -1L;
                    switch(typeSort) {
                    case Type.BOOLEAN:
                        ret = Integer.BYTES;
                        break;
                    case Type.BYTE:
                        ret = Byte.BYTES;
                        break;
                    case Type.CHAR:
                        ret = Character.BYTES;
                        break;
                    case Type.DOUBLE:
                        ret = Double.BYTES;
                        break;
                    case Type.FLOAT:
                        ret = Float.BYTES;
                        break;
                    case Type.INT:
                        ret = Integer.BYTES;
                        break;
                    case Type.LONG:
                        ret = Long.BYTES;
                        break;
                    case Type.OBJECT:
                        ret = Long.BYTES;
                        break;
                    case Type.SHORT:
                        ret = Short.BYTES;
                        break;
                    default:
                        break;
                    }
                    return ret;
            }).reduce(0L, Long::sum);
        }
    }

    //TODO allow un-annotated types from a given list
    public boolean matches(TypeDescription target) {
        return !target.isAnnotation()
               && !target.isEnum()
               && !target.isInterface()
               && target.getDeclaredAnnotations().isAnnotationPresent(Persistent.class);
    }

    //TODO compute persistent layout
    //TODO generate get/set for persistent fields
    //TODO implement OffHeapObject interface methods
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                        TypeDescription typeDescription,
                                        ClassFileLocator classFileLocator) {
        //implement OffHeapObject interface
        builder = builder.implement(OffHeapObject.class);

        //add CLASS_ID static field
        builder = builder.defineField("CLASS_ID", long.class,
                                                Ownership.STATIC,
                                                Visibility.PRIVATE,
                                                FieldManifestation.FINAL)
                         .invokable(isTypeInitializer())
                         .intercept(MethodCall.invoke(REGISTER_USER_KLASS)
                                              .withOwnType()
                                              .setsField(named("CLASS_ID")));

        //add SIZE static field
        builder = builder.defineField("SIZE", long.class,
                                              Ownership.STATIC,
                                              Visibility.PRIVATE,
                                              FieldManifestation.FINAL)
                         .value(SIZE.of(typeDescription));

        //Add getters/setters and replace field access
        for (FieldDescription field : typeDescription.getDeclaredFields().filter(isPersistable())) {
            if (!field.isFinal()) {
                builder = builder.defineMethod(setterNameFor(field),
                                               void.class,
                                               Visibility.PUBLIC)
                                 .withParameters(field.getType())
                                 .intercept(FieldAccessor.ofBeanProperty());
            }

            builder = builder.defineMethod(getterNameFor(field),
                                           field.getType(),
                                           Visibility.PUBLIC)
                             .intercept(FieldAccessor.ofBeanProperty());

            builder = builder.visit(MemberSubstitution.relaxed()
                                 .field(is(field))
                                     .onRead()
                                     .replaceWithMethod(isGetterFor(field))
                                 .field(is(field))
                                     .onWrite()
                                     .replaceWithMethod(isSetterFor(field))
                                 .on(not(isAccessor())));

        }

        //Strip non-transient fields
        builder = builder.visit(new MemberRemoval().stripFields(isPersistable()));

        //add default constructor
        builder = builder.defineConstructor(Visibility.PACKAGE_PRIVATE)
                         .intercept(SuperMethodCall.INSTANCE);

        //add re-constructor
        builder = builder.defineConstructor(Visibility.PUBLIC)
                         .withParameters(Void.class, long.class)
                         .intercept(
                             MethodCall.invoke(
                                 isDefaultConstructorOf(typeDescription) )
                             .andThen(MethodCall.invoke(REC_INSTANCE)
                                 .withThis()
                                 .withArgument(1)));

        //Remove Persistent annotation
        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods() {
                    private final AnnotationVisitor REMOVE_ANNOTATION = null;
                    @Override
                    public ClassVisitor wrap(TypeDescription instrumentedType,
                            ClassVisitor methodVisitor,
                            Implementation.Context context,
                            TypePool typePool,
                            FieldList<FieldDescription.InDefinedShape> fields,
                            MethodList<?> methods,
                            int writerFlags,
                            int readerFlags) {

                        return new ClassVisitor(OpenedClassReader.ASM_API,
                                                methodVisitor) {
                            @Override
                            public AnnotationVisitor visitAnnotation(
                                    String descriptor,
                                    boolean visible) {
                                if(Type.getDescriptor(Persistent.class)
                                       .equals(descriptor)) {
                                    return REMOVE_ANNOTATION;
                                }
                                return super.visitAnnotation(descriptor, visible);
                            }
                        };
                    }
                  });

        return builder;
    }

    public void close() {
        /* do nothing */
    }

}
