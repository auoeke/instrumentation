import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import sun.misc.Unsafe;

public class Agent {
    public static Instrumentation instrumentation;

    public static Unsafe unsafe() throws Throwable {
        return (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()).findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class).get();
    }

    /**
     Test whether instrumentation works by transforming {@code Object::toString} to always return {@code "transformation marker"}
     and asserting that {@link Object#toString} returns it for 5 new {@link Object}s.
     */
    public static void transform() throws Throwable {
        var string = "transformation marker";

        Transformer transformer = (module, loader, name, type, domain, classFile) -> {
            if (type == Object.class) try {
                var node = new ClassNode();
                new ClassReader(classFile).accept(node, 0);

                node.methods.stream().filter(method -> method.name.equals("toString")).findAny().ifPresent(method -> {
                    method.instructions.clear();
                    method.visitLdcInsn(string);
                    method.visitInsn(Opcodes.ARETURN);
                });

                var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);

                return writer.toByteArray();
            } catch (Throwable trouble) {
                trouble.printStackTrace();
                System.exit(1);
            }

            return null;
        };

        instrumentation.addTransformer(transformer, true);
        instrumentation.retransformClasses(Object.class);
        instrumentation.removeTransformer(transformer);

        if (!Stream.generate(Object::new).limit(5).peek(System.out::println).allMatch(o -> string.equals(o.toString()))) {
            throw new AssertionError();
        }

        System.out.println("Success!");
    }

    public static void agentmain(String options, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;
    }

    public static void premain(String options, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;
    }
}
