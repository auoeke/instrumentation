### Preamble
In order to acquire an `java.lang.instrument.Instrumentation`, an agent must be loaded.
The methods of loading an agent are several:
1. Passing a `-javaagent` option at launch. Requires the ability to add VM arguments.
2. Using the manifest attribute `Launcher-Agent-Class` (Java 9+). Requires the ability to add VM arguments.
3. Through an external VM. 
   This method is expensive and requires the module `jdk.attach` which is available only in JDKs but otherwise reliable.
4. By attaching the VM to itself. Self-attachment is unsupported and requires `jdk.attach`.
   1. By passing `-Djdk.attach.allowAttachSelf=true` as a launch argument. Requires the ability to add VM arguments.
   2. **By reflection.** This method relies on implementation details (see [OpenJDK](https://github.com/openjdk/jdk/blob/83d67452da248db17bc72de80247a670d6813cf5/src/jdk.attach/share/classes/sun/tools/attach/HotSpotVirtualMachine.java#L58-L60)
      and [OpenJ9](https://github.com/eclipse-openj9/openj9/blob/master/jcl/src/java.base/share/classes/openj9/internal/tools/attach/target/AttachHandler.java#L109-L116))
      and [might not last](https://github.com/openjdk/jdk/blob/83d67452da248db17bc72de80247a670d6813cf5/src/jdk.attach/share/classes/sun/tools/attach/HotSpotVirtualMachine.java#L76-L77).
   3. ~~By `System.setProperty("jdk.attach.allowAttachSelf", "true")`.~~
5. **By invoking the implementation method `sun.instrument.InstrumentationImpl::loadAgent` reflectively.**
   Requires `Launcher-Agent-Class`.
6. By native code? Too much effort.

The goal of this project is balancing performance, implementation effort and universality.

Initially I had implemented only option 4.2. Later I discovered option 5 which I have since implemented too.

This repository contains the attachment code and an example.
The attachment code is duplicated below; the remainder can be found in [`Agent.java`](source/Agent.java),
[`ImplLoadAgent.java`](source/ImplLoadAgent.java) and [`SelfAttachment.java`](source/SelfAttachment.java).

### `InstrumentationImpl::loadAgent`
```java
var unsafe = Agent.unsafe();
var lookup = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, unsafe.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")));
lookup.findStatic(Class.forName("sun.instrument.InstrumentationImpl"), "loadAgent", MethodType.methodType(void.class, String.class)).invoke("agent.jar");
```

### Self-attachment
```java
var VM = Class.forName("jdk.internal.misc.VM");
var unsafe = Agent.unsafe();
((Map<String, String>) unsafe.getObject(VM, unsafe.staticFieldOffset(VM.getDeclaredField("savedProps")))).put("jdk.attach.allowAttachSelf", "true");

var vm = VirtualMachine.attach(String.valueOf(ManagementFactory.getRuntimeMXBean().getPid()));
vm.loadAgent("agent.jar");
vm.detach();
```

### Testing
The Gradle task `agent` produces a minimal agent JAR `agent.jar` with all capabilities for testing.

The test is in `Agent::transform`.
