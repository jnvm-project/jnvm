package eu.telecomsudparis.jnvm.transformer;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

import eu.telecomsudparis.jnvm.transformer.annotations.Persistent;

public class JNVMTransformerPlugin implements Plugin {

    //TODO allow un-annotated types from a given list
    public boolean matches(TypeDescription target) {
        return target.getDeclaredAnnotations().isAnnotationPresent(Persistent.class);
    }

    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder,
                                        TypeDescription typeDescription,
                                        ClassFileLocator classFileLocator) {
        return builder;
    }

    public void close() {
        /* do nothing */
    }

}
