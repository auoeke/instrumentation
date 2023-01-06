import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ImplLoadAgent {
	public static void main(String[] args) throws Throwable {
		bootstrap();
		Agent.transform();
	}

	private static void bootstrap() throws Throwable {
		var unsafe = Agent.unsafe();
		var lookup = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, unsafe.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")));
		lookup.findStatic(Class.forName("sun.instrument.InstrumentationImpl"), "loadAgent", MethodType.methodType(void.class, String.class)).invoke("agent.jar");
	}
}
