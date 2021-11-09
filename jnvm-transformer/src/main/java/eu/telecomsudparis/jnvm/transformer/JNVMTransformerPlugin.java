package eu.telecomsudparis.jnvm.transformer;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class JNVMTransformerPlugin implements Plugin {

    public boolean matches(TypeDescription target) {
        return true;
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
