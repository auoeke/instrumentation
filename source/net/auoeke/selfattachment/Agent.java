package net.auoeke.selfattachment;

import com.sun.tools.attach.VirtualMachine;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.stream.Stream;
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
     <b>Must not be invoked during main class initialization.</b>
     */
    private static void bootstrap() throws Throwable {
        var VM = Class.forName("jdk.internal.misc.VM");
        var theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.trySetAccessible();
        var unsafe = (Unsafe) theUnsafe.get(null);
        ((Map<String, String>) unsafe.getObject(VM, unsafe.staticFieldOffset(VM.getDeclaredField("savedProps")))).put("jdk.attach.allowAttachSelf", "true");

        var vm = VirtualMachine.attach(String.valueOf(ManagementFactory.getRuntimeMXBean().getPid()));
        vm.loadAgent("agent.jar");
        vm.detach();
    }

    private static void transform() throws Throwable {
        Transformer transformer = (module, loader, name, type, domain, bytecode) -> {
            if (!name.equals("java/lang/Object")) {
                return bytecode;
            }

            var node = new ClassNode();
            new ClassReader(bytecode).accept(node, 0);

            node.methods.stream().filter(method -> method.name.equals("toString")).findAny().ifPresent(method -> {
                method.instructions.clear();
                method.visitLdcInsn("transformation marker");
                method.visitInsn(Opcodes.ARETURN);
            });

            var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);

            return writer.toByteArray();
        };

        instrumentation.addTransformer(transformer, true);
        instrumentation.retransformClasses(Object.class);
        instrumentation.removeTransformer(transformer);

        Stream.generate(Object::new).limit(10).forEach(System.out::println);
    }

    public static void agentmain(String options, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;
    }
}
