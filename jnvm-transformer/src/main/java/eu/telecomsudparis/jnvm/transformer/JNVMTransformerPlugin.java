package eu.telecomsudparis.jnvm.transformer;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapBigObjectHandle;
import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;
import eu.telecomsudparis.jnvm.transformer.bytebuddy.SpecialMethodCall;

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
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.ClassRemapper;
//import org.objectweb.asm.commons.GeneratorAdapter;
//import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.SimpleRemapper;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class JNVMTransformerPlugin implements Plugin {

    private static final String OHOH_INIT = "_ohoh_init";
    private static final String OHOH_REINIT = "_ohoh_reinit";
    private static final String OHOH_CLINIT = "_ohoh_clinit";

    private static final MethodDescription REGISTER_USER_KLASS =
        TypeDescription.ForLoadedType.of(OffHeap.Klass.class)
            .getDeclaredMethods()
            .filter(named("registerUserKlass").and(takesArguments(Class.class)))
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

    static <T extends MethodDescription> ElementMatcher.Junction<T> isPrivateGetterFor(FieldDescription field) {
        //Do not use isGenericGetter(field.getType()) that assumes nameStartsWith("get")
        return named(privateGetterNameFor(field)).and(returnsGeneric(field.getType())).and(isPrivate());
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isPrivateSetterFor(FieldDescription field) {
        //Do not use isGenericSetter(field.getType()) that assumes nameStartsWith("set")
        return named(privateSetterNameFor(field)).and(takesGenericArguments(field.getType())).and(isPrivate());
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isGetterFor(FieldDescription field) {
        return named(getterNameFor(field)).and(isGenericGetter(field.getType()));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isSetterFor(FieldDescription field) {
        return named(setterNameFor(field)).and(isGenericSetter(field.getType()));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> conflictsWithGetterFor(FieldDescription field) {
        return isGetterFor(field).or(named(getterNameFor(field)).and(takesNoArguments()));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> conflictsWithSetterFor(FieldDescription field) {
        return isSetterFor(field).or(named(setterNameFor(field)).and(takesGenericArguments(field.getType())));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isAccessor() {
        return nameStartsWith("get").or(nameStartsWith("set"));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isAccessorFor(FieldDescription field) {
        return isGetterFor(field).or(isSetterFor(field));
    }

    static <T extends MethodDescription> ElementMatcher.Junction<T> isPrivateAccessorFor(FieldDescription field) {
        return isPrivateGetterFor(field).or(isPrivateSetterFor(field));
    }

    static boolean definesGetterFor(FieldDescription field, TypeDescription type) {
        return type.getDeclaredMethods().filter(conflictsWithGetterFor(field)).size() > 0;
    }

    static boolean definesSetterFor(FieldDescription field, TypeDescription type) {
        return type.getDeclaredMethods().filter(conflictsWithSetterFor(field)).size() > 0;
    }

    static boolean isFirstBigPersistentInHierarchy(TypeDescription type) {
            return SIZE.isMultiBlock(type)
                && (isFirstPersistentInHierarchy(type) || !SIZE.isParentMultiBlock(type));
    }

    static boolean isFirstPersistentInHierarchy(TypeDescription type) {
            TypeDescription superType = type.getSuperClass().asErasure();
            return isNotPersistent(superType);
    }

    static boolean isNotPersistent(TypeDescription type) {
            return type == null
                    || type.represents(Object.class)
                    || (!type.isAssignableTo(OffHeapObject.class)
                       && !isPersistentAnnotatedClass(type));
    }

    static boolean isPersistentAnnotatedClass(TypeDescription type) {
        return !type.isAnnotation()
            && !type.isEnum()
            && !type.isInterface()
            && type.getDeclaredAnnotations().isAnnotationPresent(Persistent.class);
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

    private static MethodDescription reconstructorHelperOf(TypeDescription type) {
        return new MethodDescription.Latent(type, new MethodDescription.Token(
            OHOH_REINIT,
            Opcodes.ACC_PRIVATE,
            TypeDescription.Generic.VOID,
            new TypeList.Generic.ForLoadedTypes(Void.class, long.class)));
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
    public static String privateGetterNameFor(FieldDescription field) {
        return "_" + getterNameFor(field);
    }
    public static String privateSetterNameFor(FieldDescription field) {
        return "_" + setterNameFor(field);
    }

    static class SIZE {
        private static final Map<TypeDescription, Long> cache = new HashMap<>();

        protected static boolean isMultiBlock(TypeDescription type) {
            return of(type) > OffHeapBigObjectHandle.BYTES_PER_BASE;
        }

        protected static boolean isParentMultiBlock(TypeDescription type) {
            return isMultiBlock(type.getSuperClass().asErasure());
        }

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
        return isPersistentAnnotatedClass(target);
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
        AsmVisitorWrapper fieldAccess = AsmVisitorWrapper.NoOp.INSTANCE;
        for (FieldDescription field : typeDescription.getDeclaredFields().filter(isPersistable())) {
            TypeDescription fieldType = field.getType().asErasure();

            if (!fieldType.isAssignableTo(OffHeapObject.class)
                && !(fieldType.isPrimitive() || fieldType.isPrimitiveWrapper())) {
                throw new IllegalStateException("illegal non-transient reference field");
            }
            Visibility setterVisibility = (!field.isFinal()) ? Visibility.PUBLIC : Visibility.PRIVATE;

            Implementation setterImplementation =
                                 MethodCall.invoke(isPersistentFieldWriterFor(field))
                                     .onDefault()
                                     .with(fieldOffset)
                                     .withArgument(0)
                                     .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

            Implementation getterImplementation =
                                 MethodCall.invoke(isPersistentFieldReaderFor(field))
                                     .onDefault()
                                     .with(fieldOffset)
                                     .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

            builder = builder.defineMethod(privateSetterNameFor(field), void.class, Visibility.PRIVATE)
                             .withParameters(field.getType())
                             .intercept(setterImplementation);

            builder = builder.defineMethod(privateGetterNameFor(field), field.getType(), Visibility.PRIVATE)
                             .intercept(getterImplementation);

            if (!(definesSetterFor(field, builder.toTypeDescription()))) {
                builder = builder.defineMethod(setterNameFor(field),
                                        void.class,
                                        setterVisibility)
                                 .withParameters(field.getType())
                                 .intercept(setterImplementation);
            }

            if (!(definesGetterFor(field, builder.toTypeDescription()))) {
                builder = builder.defineMethod(getterNameFor(field),
                                        field.getType(),
                                        Visibility.PUBLIC)
                                 .intercept(getterImplementation);
            }

            fieldAccess = new AsmVisitorWrapper.Compound(fieldAccess,
                               MemberSubstitution.relaxed()
                                 .field(is(field))
                                     .onRead()
                                     .replaceWithMethod(isPrivateGetterFor(field))
                                 .field(is(field))
                                     .onWrite()
                                     .replaceWithMethod(isPrivateSetterFor(field))
                                 .on(not(isPrivateAccessorFor(field))));

            fieldOffset += SIZE.bytesFor(field);
        }

        //add descend method
        Implementation descendImplementation = (isFirstPersistentInHierarchy(typeDescription))
                ? StubMethod.INSTANCE : SpecialMethodCall.INSTANCE;
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
        if (isFirstPersistentInHierarchy(typeDescription)) {
            builder = builder.defineConstructor(Visibility.PACKAGE_PRIVATE)
                             //.intercept(SpecialMethodCall.INSTANCE);
                             // Alternative implementation
                             .intercept(
                                     MethodCall.invoke(
                                             typeDescription.getSuperClass()
                                                     .getDeclaredMethods()
                                                     .filter(isDefaultConstructor())
                                                     .getOnly()));
        }

        //add re-constructor
        builder = builder.defineConstructor(Visibility.PUBLIC)
                         .withParameters(Void.class, long.class)
                         .intercept(
                            (isFirstPersistentInHierarchy(typeDescription))
                            ? MethodCall.invoke(
                                  isDefaultConstructorOf(typeDescription))
                              .andThen(MethodCall.invoke(
                                  reconstructorHelperOf(typeDescription))
                                      .withAllArguments())
                            : SpecialMethodCall.INSTANCE);

        //fa-wrap non-private methods
        AsmVisitorWrapper faWrapVisitor = AsmVisitorWrapper.NoOp.INSTANCE;
        if (typeDescription.getDeclaredAnnotations()
                           .ofType(TypeDescription.ForLoadedType.of(Persistent.class))
                           .getValue("fa").load(getClass().getClassLoader())
                           .represents("non-private")) {
            ElementMatcher.Junction<MethodDescription> wrappable =
                    isMethod().and(
                     not(isPrivate()
                       .or(isGetter())
                       .or(isSetter())
                       .or(isStatic())
                       .or(isOverriddenFrom(OffHeapObject.class))
                       .or(isOverriddenFrom(Object.class))))
                    .or(isConstructor().and(
                       not(isDefaultConstructor()
                           .or(isReconstructor()))));
            faWrapVisitor = Advice.to(OffHeapObjectAdvice.FailureAtomic.class).on(wrappable);
            /*
            faWrapVisitor = new AsmVisitorWrapper.ForDeclaredMethods()
                    .invokable(wrappable,
                    new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
                @Override
                public MethodVisitor wrap(TypeDescription instrumentedType,
                        MethodDescription instrumentedMethod,
                        MethodVisitor methodVisitor,
                        Implementation.Context context,
                        TypePool typePool,
                        int writerFlags,
                        int readerFlags) {
                    return new FaWrapMethodVisitor(OpenedClassReader.ASM_API, methodVisitor);
                }
            });
            */
        }

        //implement OffHeapObject interface
        AsmVisitorWrapper copyVisitor =
                (isFirstPersistentInHierarchy(typeDescription)
                || isFirstBigPersistentInHierarchy(typeDescription))
            ? new AsmVisitorWrapper.ForDeclaredMethods() {
                @Override
                public int mergeReader(int flags) {
                    return super.mergeReader(flags) | ClassReader.EXPAND_FRAMES;
                }
                @Override
                public ClassVisitor wrap(TypeDescription instrumentedType,
                        ClassVisitor methodVisitor,
                        Implementation.Context context,
                        TypePool typePool,
                        FieldList<FieldDescription.InDefinedShape> fields,
                        MethodList<?> methods,
                        int writerFlags,
                        int readerFlags) {
                    Class<?> srcClass = srcClassChooser();
                    byte[] srcClassBytes = ClassFileLocator.ForClassLoader.read(srcClass);
                    ClassReader srcClassReader = OpenedClassReader.of(srcClassBytes);
                    return new CopyingClassVisitor(OpenedClassReader.ASM_API,
                            methodVisitor,
                            srcClassReader,
                            readerFlags);
                }
                //Use srcClass according to layout SIZE and ancestor classes
                private Class<?> srcClassChooser() {
                    if (SIZE.isMultiBlock(typeDescription)) {
                        return OffHeapBigObjectHandle.class;
                    } else {
                        return OffHeapObjectHandle.class;
                    }
                }
            } : AsmVisitorWrapper.NoOp.INSTANCE;

        AsmVisitorWrapper constructorVisitor =
                new AsmVisitorWrapper.ForDeclaredMethods() {
            @Override
            public ClassVisitor wrap(TypeDescription instrumentedType,
                    ClassVisitor methodVisitor,
                    Implementation.Context context,
                    TypePool typePool,
                    FieldList<FieldDescription.InDefinedShape> fields,
                    MethodList<?> methods,
                    int writerFlags,
                    int readerFlags) {
                return (isFirstPersistentInHierarchy(instrumentedType))
                    ? new TopLevelConstructorVisitor(OpenedClassReader.ASM_API,
                            methodVisitor)
                    : new ChildConstructorVisitor(OpenedClassReader.ASM_API,
                            methodVisitor);
            }
        };

        //Remove Persistent annotation
        AsmVisitorWrapper annotationVisitor =
                new AsmVisitorWrapper.ForDeclaredMethods() {
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
        };

        builder = builder.visit(new AsmVisitorWrapper.Compound(
            faWrapVisitor,
            copyVisitor,
            constructorVisitor,
            fieldAccess,
            annotationVisitor));

        return builder;
    }

    public void close() {
        /* do nothing */
    }

    protected static class CopyingClassVisitor extends ClassVisitor {
        private final int readerFlags;
        private final ClassReader srcClass;
        private final ClassVisitor destClassVisitor;
        private String className;
        private MethodVisitor clinitVisitor;

        CopyingClassVisitor(int api, ClassVisitor destClassVisitor, ClassReader srcClass, int readerFlags) {
            super(api, destClassVisitor);
            this.readerFlags = readerFlags;
            this.srcClass = srcClass;
            this.destClassVisitor = destClassVisitor;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.className = name;
                /* visitor to substitute class name occurences in srcClass with destClass name */
                ClassVisitor remapper = new ClassRemapper(destClassVisitor,
                        new SimpleRemapper(srcClass.getClassName(), name));
                /* visitor to filter out fields or methods */
                ClassVisitor filter = new ClassVisitor(OpenedClassReader.ASM_API, remapper) {
                    @Override
                    public FieldVisitor visitField(
                            int access,
                            String name,
                            String descriptor,
                            String signature,
                            Object value) {
                        /* No field filter ? */
                        int newAccess = (((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC)
                                            ? Opcodes.ACC_STATIC : 0 )
                                        + Opcodes.ACC_PROTECTED;
                        return super.visitField(newAccess, name, descriptor, signature, value);
                    }
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String descriptor,
                            String signature,
                            String[] exceptions) {
                        /* remove abstract methods */
                        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
                            return null;
                        }
                        /* remove specifically implemented methods */
                        if (name.equals("descend")) return null;
                        /* TODO replace with direct access to static field */
                        if (name.equals("size")) return null;
                        /* TODO replace when overridden */
                        if (name.equals("equals")) return null;
                        /* TODO prepend code before super constructor call */
                        if (name.equals("<init>")) {
                            String newName = null;
                            if (descriptor.equals("()V")
                               || descriptor.equals("(J)V"))
                                newName = OHOH_INIT;
                            if (descriptor.equals("(Ljava/lang/Void;J)V"))
                                newName = OHOH_REINIT;
                            if (newName == null) {
                                return null;
                            } else {
                                int newAccess = Opcodes.ACC_PRIVATE;
                                return new MethodVisitor(OpenedClassReader.ASM_API,
                                        super.visitMethod(newAccess, newName, descriptor, signature, exceptions)) {
                                    private boolean superCallRemoved = false;
                                    @Override
                                    public void visitInsn(int opcode) {
                                        //Remove instructions preceding the super call
                                        if (superCallRemoved) {
                                            super.visitInsn(opcode);
                                        }
                                    }
                                    @Override
                                    public void visitVarInsn(int opcode, int var) {
                                        //Remove instructions preceding the super call
                                        if (superCallRemoved) {
                                            super.visitVarInsn(opcode, var);
                                        }
                                    }
                                    @Override
                                    public void visitIntInsn(int opcode, int operand) {
                                        //Remove instructions preceding the super call
                                        if (superCallRemoved) {
                                            super.visitIntInsn(opcode, operand);
                                        }
                                    }
                                    @Override
                                    public void visitMethodInsn(int opcode, String name, String owner, String descriptor, boolean isInterface) {
                                        if (superCallRemoved) {
                                            super.visitMethodInsn(opcode, name, owner, descriptor, isInterface);
                                        } else if (opcode == Opcodes.INVOKESPECIAL && owner.equals("<init>")) {
                                            //Do nothing to delete call to super constructor
                                            superCallRemoved = true;
                                        }
                                    }
                                };
                            }
                        }
                        /* TODO prepend code before super constructor call */
                        if (name.equals("<clinit>")) {
                            int newAccess = Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC;
                            return super.visitMethod(newAccess, OHOH_CLINIT, descriptor, signature, exceptions);
                        }
                        int newAccess = (((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC)
                                            ? Opcodes.ACC_STATIC : 0 )
                                        + Opcodes.ACC_PUBLIC;
                        return super.visitMethod(newAccess, name, descriptor, signature, exceptions);
                    }
                };
            /* visit srcClass to add content to destClass */
            srcClass.accept(filter, readerFlags);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            //Call renamed OHOH <clinit> from actual class <clinit>
            if (name.equals("<clinit>")) {
                if (clinitVisitor == null) {
                    clinitVisitor = new AdviceAdapter(OpenedClassReader.ASM_API, mv, access, name, descriptor) {
                        @Override
                        protected void onMethodEnter() {
                            invokeStatic(Type.getObjectType(className),
                                new Method(OHOH_CLINIT, descriptor));
                        }
                    };
                    return clinitVisitor;
                }
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            //Create class <clinit> to call renamed OHOH <clinit> when class has no static initializer
            if (clinitVisitor == null) {
                clinitVisitor = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinitVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, OHOH_CLINIT, "()V", false);
                clinitVisitor.visitInsn(Opcodes.RETURN);
            }
            super.visitEnd();
        }
    }

    protected static abstract class ConstructorEnhancerVisitor extends ClassVisitor {
        protected String className;
        protected String superName;

        ConstructorEnhancerVisitor(int api, ClassVisitor parent) {
            super(api, parent);
        }

        private static String newCtxDescriptor(String origDesc) {
            return origDesc.replace(")V", "J)V");
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
            this.superName = superName;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions) {
            MethodVisitor mv;
            if (name.equals("<init>")
                    && !descriptor.equals("(Ljava/lang/Void;J)V")
                    && !descriptor.equals("()V")) {
                //Add allocSize method parameter to constructors
                String newDesc = newCtxDescriptor(descriptor);
                int newAccess = Opcodes.ACC_PROTECTED;
                MethodVisitor newCtx =
                    super.visitMethod(newAccess, name, newDesc, signature, exceptions);
                mv = manualProtectedConstructor(newCtx, newAccess, name, newDesc);
                //Generate constructor with original signature
                //  and defer call to allocSize enhanced ctx using SIZE static field
                MethodVisitor origCtx =
                    super.visitMethod(access, name, descriptor, signature, exceptions);
                manualPublicConstructor(origCtx, access, name, descriptor, newDesc);
                return mv;
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        /*
        private MethodVisitor generateProtectedConstructor(
                MethodVisitor mv,
                int access,
                String name,
                String descriptor) {
            //LocalVariableSorter to remap local variables with newDesc
            return new LocalVariablesSorter(access, descriptor, mv);
        }
        */

        private MethodVisitor manualProtectedConstructor(
                MethodVisitor mv,
                int access,
                String name,
                String descriptor) {
            //Manual local variable sorter
            //BUG getSize is innacurate with boxed types (LONG, DOUBLE)
            //No need to update local variable table
            return new MethodVisitor(OpenedClassReader.ASM_API, mv) {
                private int nextLocal = 1;
                @Override
                public void visitCode() {
                    for(Type argType : Type.getArgumentTypes(descriptor)) {
                        nextLocal += argType.getSize();
                    }
                    super.visitCode();
                }
                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    super.visitMaxs(maxStack, nextLocal);
                }
            };
        }

        /*
        private MethodVisitor generatePublicConstructor(
                MethodVisitor mv,
                int access,
                String name,
                String oldDesc,
                String newDesc) {
            GeneratorAdapter mg = new GeneratorAdapter(mv, access, name, oldDesc);
            //Generate code
            mg.visitCode();
            mg.loadThis();
            mg.loadArgs();
            mg.getStatic(Type.getObjectType(className), "SIZE", Type.LONG_TYPE);
            mg.invokeConstructor(Type.getObjectType(className), new Method("<init>", newDesc));
            mg.returnValue();
            mg.endMethod();
            //Compute locals and stack size
            int nextLocal = 1; //This
            for (Type argType : Type.getArgumentTypes(oldDesc)) {
                nextLocal += argType.getSize(); //Args
            }
            nextLocal += Type.getType("J").getSize(); //GETSTATIC_LONG
            int stackSize = nextLocal; //All locals are loaded on the stack
            mv.visitMaxs(stackSize, nextLocal);
            return mv;
        }
        */

        private MethodVisitor manualPublicConstructor(
                MethodVisitor mv,
                int access,
                String name,
                String oldDesc,
                String newDesc) {
            //Add new constructor with old signature
            //  and call new signature with default SIZE parameter
            //  TODO properly compute local var indices
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            int nextLocal = 1;
            for (Type argType : Type.getArgumentTypes(oldDesc)) {
                mv.visitVarInsn(argType.getOpcode(Opcodes.ILOAD), nextLocal);
                nextLocal += argType.getSize();
            }
            nextLocal += Type.getType("J").getSize();
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, "SIZE", "J");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", newDesc, false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(nextLocal, nextLocal);
            mv.visitEnd();
            return mv;
        }

        private static int lastArgIndex(String descriptor) {
            int index = 1; //This
            for (Type argType : Type.getArgumentTypes(descriptor)) {
                index += argType.getSize();
            }
            return index;
        }
    }

    protected static class TopLevelConstructorVisitor extends ConstructorEnhancerVisitor {

        TopLevelConstructorVisitor(int api, ClassVisitor parent) {
            super(api, parent);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            //Call OHOH <init> right after super <init> call
            if (name.equals("<init>") && !descriptor.equals("(Ljava/lang/Void;J)V") && !descriptor.equals("()V")) {
                return new AdviceAdapter(OpenedClassReader.ASM_API, mv, access, name, descriptor) {
                    @Override
                    protected void onMethodEnter() {
                        int lastArgIdx = ConstructorEnhancerVisitor.lastArgIndex(descriptor);
                        loadThis();
                        super.visitVarInsn(Opcodes.LLOAD, lastArgIdx);
                        invokeVirtual(Type.getObjectType(className),
                            new Method(OHOH_INIT, "(J)V"));
                    }
                };
            }
            return mv;
        }
    }

    protected static class ChildConstructorVisitor extends ConstructorEnhancerVisitor {

        ChildConstructorVisitor(int api, ClassVisitor parent) {
            super(api, parent);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            //Transform calls to super <init> in constructors
            //  to use the generated one with additional alloc size parameter
            if (name.equals("<init>")
                    && !descriptor.equals("(Ljava/lang/Void;J)V")
                    && !descriptor.equals("()V")) {
                return new MethodVisitor(OpenedClassReader.ASM_API, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String name, String owner, String desc, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESPECIAL
                                && owner.equals("<init>")
                                && name.equals(superName)) {
                            //Call constructor from parent with extra alloc size parameter
                            String newDesc = ConstructorEnhancerVisitor.newCtxDescriptor(desc);
                            int lastArgIdx = ConstructorEnhancerVisitor.lastArgIndex(descriptor);
                            super.visitVarInsn(Opcodes.LLOAD, lastArgIdx);
                            super.visitMethodInsn(opcode, name, owner, newDesc, isInterface);
                        } else {
                            super.visitMethodInsn(opcode, name, owner, desc, isInterface);
                        }
                    }
                };
            }
            return mv;
        }
    }

    //Not used, ByteBuddy can generate reconstructors just fine directly now,
    //  no need to further instrument them manually
    protected static class TopLevelReconstructorVisitor extends TopLevelConstructorVisitor {

        TopLevelReconstructorVisitor(int api, ClassVisitor parent) {
            super(api, parent);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            //Call OHOH <reinit> right after super <init> call
            if (name.equals("<init>") && descriptor.equals("(Ljava/lang/Void;J)V")) {
                return new AdviceAdapter(OpenedClassReader.ASM_API, mv, access, name, descriptor) {
                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack+2, maxLocals);
                    }
                    @Override
                    protected void onMethodEnter() {
                        loadThis();
                        super.visitInsn(Opcodes.ACONST_NULL);
                        loadArg(1);
                        invokeVirtual(Type.getObjectType(className),
                            new Method(OHOH_REINIT, descriptor));
                    }
                };
            }
            return mv;
        }
    }

    //Not used, ByteBuddy can generate reconstructors just fine directly now,
    //  no need to further instrument them manually
    protected static class ChildReconstructorVisitor extends ChildConstructorVisitor {

        ChildReconstructorVisitor(int api, ClassVisitor parent) {
            super(api, parent);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            //Transform reconstructor to call to super class reconstructor
            //  instead of default empty constructor
            if (name.equals("<init>")
                    && descriptor.equals("(Ljava/lang/Void;J)V")) {
                return new MethodVisitor(OpenedClassReader.ASM_API, mv) {
                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        int stackSize = maxStack;
                        stackSize += Type.getObjectType("java/lang/Void").getSize();
                        stackSize += Type.LONG_TYPE.getSize();
                        super.visitMaxs(stackSize, maxLocals);
                    }
                    @Override
                    public void visitMethodInsn(int opcode, String name, String owner, String desc, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESPECIAL
                                && owner.equals("<init>")
                                && desc.equals("()V")
                                && name.equals(className)) {
                            //Delegate to super class reconstructor instead of this()
                            super.visitInsn(Opcodes.ACONST_NULL);
                            super.visitVarInsn(Opcodes.LLOAD, 2);
                            super.visitMethodInsn(opcode, superName, owner, descriptor, isInterface);
                        } else {
                            super.visitMethodInsn(opcode, name, owner, desc, isInterface);
                        }
                    }
                };
            }
            return mv;
        }
    }

    //Not used, ByteBuddy Advice seems to work appropriately.
    //  Kept here temporarily, in case a simpler manual version
    //  might be needed later on.
    protected static class FaWrapMethodVisitor extends MethodVisitor {

        private boolean startAdded = false;
        private boolean endAdded = false;

        private static final String CLASSNAME = "eu/telecomsudparis/jnvm/offheap/OffHeap";

        FaWrapMethodVisitor(int api, MethodVisitor parent) {
            super(api, parent);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (!startAdded) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CLASSNAME, "start_recording", "()V", false);
                startAdded = true;
            }
        }
        @Override
        public void visitInsn(int opcode) {
            if (!endAdded && opcode == Opcodes.RETURN) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, CLASSNAME, "stop_recording", "()V", false);
                endAdded = true;
            }
            super.visitInsn(opcode);
        }
    }
}
