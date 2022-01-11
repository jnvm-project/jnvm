package eu.telecomsudparis.jnvm.transformer;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
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
                                 isDefaultConstructorOf(typeDescription) ));

        //implement OffHeapObject interface
        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods() {
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
                /* TODO use srcClass according to layout SIZE and ancestor classes */
                Class<?> srcClass = OffHeapObjectHandle.class;
                byte[] srcClassBytes = ClassFileLocator.ForClassLoader.read(srcClass);
                ClassReader srcClassReader = OpenedClassReader.of(srcClassBytes);
                return new CopyingClassVisitor(OpenedClassReader.ASM_API,
                        methodVisitor,
                        srcClassReader,
                        readerFlags);
            }
        });

        builder = builder.visit(new AsmVisitorWrapper.ForDeclaredMethods() {
            @Override
            public ClassVisitor wrap(TypeDescription instrumentedType,
                    ClassVisitor methodVisitor,
                    Implementation.Context context,
                    TypePool typePool,
                    FieldList<FieldDescription.InDefinedShape> fields,
                    MethodList<?> methods,
                    int writerFlags,
                    int readerFlags) {
                ConstructorEnhancerVisitor cv = null;
                if (isFirstPersistentInHierarchy(instrumentedType)) {
                    cv = new TopLevelConstructorVisitor(
                            OpenedClassReader.ASM_API,
                            methodVisitor);
                } else {
                    cv = new ChildConstructorVisitor(
                            OpenedClassReader.ASM_API,
                            methodVisitor);
                }
                return cv;
            }
        });

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
                        return super.visitField(access, name, descriptor, signature, value);
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
                            if (descriptor.equals("()V"))
                                newName = OHOH_INIT;
                            if (descriptor.equals("(J)V"))
                                newName = OHOH_REINIT;
                            if (newName == null) {
                                return null;
                            } else {
                                int newAccess = access - Opcodes.ACC_PUBLIC + Opcodes.ACC_PRIVATE;
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
                        return super.visitMethod(access, name, descriptor, signature, exceptions);
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
            //TODO Transform constructors to allow the allocsize to be specified
            return super.visitMethod(access, name, descriptor, signature, exceptions);
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
                        loadThis();
                        invokeVirtual(Type.getObjectType(className),
                            new Method(OHOH_INIT, "()V"));
                    }
                };
            }
            //Call OHOH <reinit> right after super <init> call
            else if (name.equals("<init>") && descriptor.equals("(Ljava/lang/Void;J)V")) {
                return new AdviceAdapter(OpenedClassReader.ASM_API, mv, access, name, descriptor) {
                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack+2, maxLocals);
                    }
                    @Override
                    protected void onMethodEnter() {
                        loadThis();
                        loadArg(1);
                        invokeVirtual(Type.getObjectType(className),
                            new Method(OHOH_REINIT, "(J)V"));
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

            return super.visitMethod(access, name, descriptor, signature, exceptions);
            //TODO Transform calls to super <init> to use generated constructor with additional alloc size parameter
        }
    }
}
