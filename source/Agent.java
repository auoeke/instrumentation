import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.stream.Stream;
import com.sun.tools.attach.VirtualMachine;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import sun.misc.Unsafe;

public class Agent {
    public static Instrumentation instrumentation;

    public static void main(String[] args) throws Throwable {
        // Attach the agent.
        bootstrap();
        transform();
    }

    /**
     Enable self-attachment for this VM and attach the agent.
     <p>
     Must not be invoked during main class initialization.
     */
    private static void bootstrap() throws Throwable {
        var VM = Class.forName("jdk.internal.misc.VM");
        var unsafe = (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()).findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class).get();
        ((Map<String, String>) unsafe.getObject(VM, unsafe.staticFieldOffset(VM.getDeclaredField("savedProps")))).put("jdk.attach.allowAttachSelf", "true");

        var vm = VirtualMachine.attach(String.valueOf(ManagementFactory.getRuntimeMXBean().getPid()));
        vm.loadAgent("agent.jar");
        vm.detach();
    }

    /**
     Test whether instrumentation works by transforming {@code Object::toString} to always return {@code "transformation marker"}
     and asserting that {@link Object#toString} returns it for 10 new {@link Object}s.
     */
    private static void transform() throws Throwable {
        var string = "transformation marker";

        Transformer transformer = (module, loader, name, type, domain, bytecode) -> {
            if (type != Object.class) {
                return bytecode;
            }

            var node = new ClassNode();
            new ClassReader(bytecode).accept(node, 0);

            node.methods.stream().filter(method -> method.name.equals("toString")).findAny().ifPresent(method -> {
                method.instructions.clear();
                method.visitLdcInsn(string);
                method.visitInsn(Opcodes.ARETURN);
            });

            var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);

            return writer.toByteArray();
        };

        instrumentation.addTransformer(transformer, true);
        instrumentation.retransformClasses(Object.class);
        instrumentation.removeTransformer(transformer);

        if (!Stream.generate(Object::new).limit(10).peek(System.out::println).map(o -> string.equals(o.toString())).reduce(true, Boolean::logicalAnd)) {
            throw new AssertionError();
        }
    }

    public static void agentmain(String options, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;
    }
}
