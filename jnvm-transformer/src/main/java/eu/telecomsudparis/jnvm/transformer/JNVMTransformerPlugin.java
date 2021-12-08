package eu.telecomsudparis.jnvm.transformer;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;

import net.bytebuddy.asm.Advice;
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
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
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

    static <T extends MethodDescription> ElementMatcher.Junction<T> isReconstructorOf(TypeDescription type) {
        return isReconstructor().and(isDeclaredBy(type));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isReconstructor() {
        return isConstructor().and(isPublic()).and(takesArguments(Void.class, long.class));
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

    static boolean isFirstPersistentInHierarchy(TypeDescription type) {
            TypeDescription superType = type.getSuperClass().asErasure();
            return isNotPersistent(superType);
    }

    static boolean isNotPersistent(TypeDescription type) {
            return type == null
                    || type.represents(Object.class)
                    || !type.isAssignableTo(OffHeapObject.class);
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isPersistentFieldWriterFor(FieldDescription field) {
        return isDeclaredBy(OffHeapObject.class)
               .and(named("set" + nameGlue(field) + "Field"))
               .and(returns(void.class))
               .and(takesArgument(0, long.class))
               .and(takesArgument(1, typeGlue(field)));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isPersistentFieldReaderFor(FieldDescription field) {
        return isDeclaredBy(OffHeapObject.class)
               .and(named("get" + nameGlue(field) + "Field"))
               .and(returns(typeGlue(field)))
               .and(takesArgument(0, long.class));
    }

    private static TypeDescription typeGlue(FieldDescription field) {
        return (field.getType().asErasure().isAssignableTo(OffHeapObject.class))
            ? TypeDescription.ForLoadedType.of(OffHeapObject.class)
            : field.getType().asErasure();
    }

    private static String nameGlue(FieldDescription field) {
        int typeSort = Type.getType(field.getDescriptor()).getSort();
        String ret = null;
        switch(typeSort) {
            case Type.BOOLEAN:
                ret = "Boolean";
                break;
            case Type.BYTE:
                ret = "Byte";
                break;
            case Type.CHAR:
                ret = "Char";
                break;
            case Type.DOUBLE:
                ret = "Double";
                break;
            case Type.FLOAT:
                ret = "Float";
                break;
            case Type.INT:
                ret = "Integer";
                break;
            case Type.LONG:
                ret = "Long";
                break;
            case Type.OBJECT:
                ret = "Handle";
                break;
            case Type.SHORT:
                ret = "Short";
                break;
            default:
                break;
        }
        return ret;
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
            return (isNotPersistent(superType))
                ? 0L : of(superType);
        }

        private static long computeFor(TypeDescription type) {
            return type.getDeclaredFields()
                .filter(isPersistable())
                .stream()
                    .mapToLong(field -> bytesFor(field))
                    .reduce(0L, Long::sum);
        }

        private static long bytesFor(FieldDescription field) {
            int typeSort = Type.getType(field.getDescriptor()).getSort();
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
            }
            return ret;
        }
    }

    static class OffHeapObjectAdvice {

        protected static class Descend {
            @Advice.OnMethodExit
            public static void descend(@Advice.FieldValue OffHeapObject member) {
                OffHeapObject oho = member;
                if (!oho.mark()) {
                    oho.descend();
                }
            }
        }

        protected static class FailureAtomic {
            @Advice.OnMethodEnter
            public static void FAStart() {
                OffHeap.startRecording();
            }

            @Advice.OnMethodExit
            public static void FAStop() {
                OffHeap.stopRecording();
            }
        }

    }

    //TODO allow un-annotated types from a given list
    public boolean matches(TypeDescription target) {
        return !target.isAnnotation()
               && !target.isEnum()
               && !target.isInterface()
               && target.getDeclaredAnnotations().isAnnotationPresent(Persistent.class);
    }

    //TODO implement OffHeapObject interface methods
    //TODO annotation & advice to cache proxies using getters return value
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

        //add classId method
        builder = builder.defineMethod("classId", long.class, Ownership.STATIC,
                                                           Visibility.PUBLIC)
                         .intercept(FieldAccessor.ofField("CLASS_ID"));

        //add size method
        builder = builder.defineMethod("size", long.class, Ownership.STATIC,
                                                           Visibility.PUBLIC)
                         .intercept(FieldAccessor.ofField("SIZE"));

        //Add getters/setters and replace field access
        long fieldOffset = SIZE.ofParent(typeDescription);
        for (FieldDescription field : typeDescription.getDeclaredFields().filter(isPersistable())) {
            TypeDescription fieldType = field.getType().asErasure();

            if (!fieldType.isAssignableTo(OffHeapObject.class)
                && !(fieldType.isPrimitive() || fieldType.isPrimitiveWrapper())) {
                throw new IllegalStateException("illegal non-transient reference field");
            }
            if (!field.isFinal()) {
                builder = builder.defineMethod(setterNameFor(field),
                                               void.class,
                                               Visibility.PUBLIC)
                                 .withParameters(field.getType())
                                 .intercept(
                                     MethodCall.invoke(isPersistentFieldWriterFor(field))
                                         .onDefault()
                                         .with(fieldOffset)
                                         .withArgument(0)
                                         .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
            }

            builder = builder.defineMethod(getterNameFor(field),
                                           field.getType(),
                                           Visibility.PUBLIC)
                             .intercept(
                                 MethodCall.invoke(isPersistentFieldReaderFor(field))
                                     .onDefault()
                                     .with(fieldOffset)
                                     .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));

            builder = builder.visit(MemberSubstitution.relaxed()
                                 .field(is(field))
                                     .onRead()
                                     .replaceWithMethod(isGetterFor(field))
                                 .field(is(field))
                                     .onWrite()
                                     .replaceWithMethod(isSetterFor(field))
                                 .on(not(isAccessor())));

            fieldOffset += SIZE.bytesFor(field);
        }

        //add descend method
        Implementation descendImplementation = (isFirstPersistentInHierarchy(typeDescription))
                ? StubMethod.INSTANCE : SuperMethodCall.INSTANCE;
        for (FieldDescription field : typeDescription.getDeclaredFields()
                .filter(isPersistable().and(fieldType(isSubTypeOf(OffHeapObject.class))))) {
            descendImplementation = Advice.withCustomMapping()
              .bind(Advice.FieldValue.class, field)
              .to(OffHeapObjectAdvice.Descend.class)
              .wrap(descendImplementation);
        }
        builder = builder.defineMethod("descend", void.class, Visibility.PUBLIC)
                         .intercept(descendImplementation);

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

        //fa-wrap non-private methods
        if (typeDescription.getDeclaredAnnotations().ofType(TypeDescription.ForLoadedType.of(Persistent.class)).getValue("fa").load(getClass().getClassLoader()).represents("non-private")) {
            builder = builder.visit(Advice.to(OffHeapObjectAdvice.FailureAtomic.class)
                                          .on(isMethod().and(
                                                not(isPrivate()
                                                  .or(isGetter())
                                                  .or(isSetter())
                                                  .or(isStatic())
                                                  .or(isOverriddenFrom(OffHeapObject.class))
                                                  .or(isOverriddenFrom(Object.class))))
                                              .or(isConstructor().and(
                                                not(isDefaultConstructor()
                                                    .or(isReconstructor()))))));
        }

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
