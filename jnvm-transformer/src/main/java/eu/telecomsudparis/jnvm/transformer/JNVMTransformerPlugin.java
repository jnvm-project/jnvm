package eu.telecomsudparis.jnvm.transformer;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberRemoval;
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

    static <T extends MethodDescription> ElementMatcher.Junction<T> isDefaultConstructorOf(TypeDescription type) {
        return isDefaultConstructor().and(isDeclaredBy(type));
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
    //TODO visit fields and replace accesses/assignation with get/set
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
                         .value(0L);

        //Strip non-transient fields
        builder = builder.visit(new MemberRemoval().stripFields(
                    not( isStatic().or(isTransient()) )));

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
