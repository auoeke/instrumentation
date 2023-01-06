import java.util.Map;
import com.sun.tools.attach.VirtualMachine;

public class SelfAttachment {
	public static void main(String[] args) throws Throwable {
		bootstrap();
		Agent.transform();
	}

	/**
	 Enable self-attachment for this VM and load the agent.
	 <p>
	 Must not be invoked during main class initialization.
	 */
	private static void bootstrap() throws Throwable {
		var VM = Class.forName("jdk.internal.misc.VM");
		var unsafe = Agent.unsafe();
		((Map<String, String>) unsafe.getObject(VM, unsafe.staticFieldOffset(VM.getDeclaredField("savedProps")))).put("jdk.attach.allowAttachSelf", "true");

		var vm = VirtualMachine.attach(String.valueOf(ProcessHandle.current().pid()));
		vm.loadAgent("agent.jar");
		vm.detach();
	}
}
