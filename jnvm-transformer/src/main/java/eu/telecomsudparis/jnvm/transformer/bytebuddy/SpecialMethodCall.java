package eu.telecomsudparis.jnvm.transformer.bytebuddy;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Same as net.bytebuddy.implementation.SuperMethodCall,
 * except it won't look for the appropriate super method to call but simply
 * produce an unchecked special invocation of the instrumented method on the parent class.
 */
public enum SpecialMethodCall implements Implementation.Composable {

    INSTANCE;

    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    public ByteCodeAppender appender(Target implementationTarget) {
        return new Appender(implementationTarget, Appender.TerminationHandler.RETURNING);
    }

    public Implementation andThen(Implementation implementation) {
        return new Compound(WithoutReturn.INSTANCE, implementation);
    }

    public Composable andThen(Composable implementation) {
        return new Compound.Composable(WithoutReturn.INSTANCE, implementation);
    }

    protected enum WithoutReturn implements Implementation {

        INSTANCE;

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public ByteCodeAppender appender(Target implementationTarget) {
            return new Appender(implementationTarget, Appender.TerminationHandler.DROPPING);
        }
    }

    protected static class Appender implements ByteCodeAppender {
        private final Target implementationTarget;
        private final TerminationHandler terminationHandler;

        protected Appender(Target implementationTarget, TerminationHandler terminationHandler) {
            this.implementationTarget = implementationTarget;
            this.terminationHandler = terminationHandler;
        }

        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, final MethodDescription instrumentedMethod) {
            StackManipulation superMethodCall = new StackManipulation.AbstractBase() {
                @Override
                public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                    TypeDescription parentType = implementationTarget.getInstrumentedType().getSuperClass().asErasure();
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        parentType.getInternalName(),
                        instrumentedMethod.getInternalName(),
                        instrumentedMethod.getDescriptor(),
                        parentType.isInterface());
                    int parameterSize = instrumentedMethod.getStackSize(), returnValueSize = instrumentedMethod.getReturnType().getStackSize().getSize();
                    return new Size(returnValueSize - parameterSize, Math.max(0, returnValueSize - parameterSize));
                }
            };
            if (!superMethodCall.isValid()) {
                throw new IllegalStateException("Cannot call super (or default) method for " + instrumentedMethod);
            }
            StackManipulation.Size size = new StackManipulation.Compound(
                    MethodVariableAccess.allArgumentsOf(instrumentedMethod).prependThisReference(),
                    superMethodCall,
                    terminationHandler.of(instrumentedMethod)
            ).apply(methodVisitor, implementationContext);
            return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
        }

        protected enum TerminationHandler {
            RETURNING {
                @Override
                protected StackManipulation of(MethodDescription methodDescription) {
                    return MethodReturn.of(methodDescription.getReturnType());
                }
            },
            DROPPING {
                @Override
                protected StackManipulation of(MethodDescription methodDescription) {
                    return Removal.of(methodDescription.getReturnType());
                }
            };
            protected abstract StackManipulation of(MethodDescription methodDescription);
        }
    }
}
