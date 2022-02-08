### Preamble
In order to acquire a `java.lang.instrument.Instrumentation` instance for the current JVM, either
- the current JVM or
- an external JVM

must attach to the current JVM; the obvious choice is the former.
The problem therein is that HotSpot and its derivatives lock it behind a system property
that must be set in the command line (setting the property dynamically is ineffective);
so the options are
1. using an external JVM,
2. passing `-Djdk.attach.allowAttachSelf=true` as one of the current JVM's launch arguments and
3. messing with reflection.

Option #1 is expensive.<br>
Option #2 is unfeasible in many environments.<br>
Option #3 is neither but [its effectiveness is subject to change](https://github.com/openjdk/jdk/blob/83d67452da248db17bc72de80247a670d6813cf5/src/jdk.attach/share/classes/sun/tools/attach/HotSpotVirtualMachine.java#L76-L77).

This project addresses option #3.

### Attachment
This repository contains the attachment code and an example.
The attachment code is duplicated below; the remainder can be found in [Agent.java](source/Agent.java).
```java
var VM = Class.forName("jdk.internal.misc.VM");
var unsafe = (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()).findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class).get();
((Map<String, String>) unsafe.getObject(VM, unsafe.staticFieldOffset(VM.getDeclaredField("savedProps")))).put("jdk.attach.allowAttachSelf", "true");

var vm = VirtualMachine.attach(String.valueOf(ManagementFactory.getRuntimeMXBean().getPid()));
vm.loadAgent("agent.jar");
vm.detach();
```

### Testing
The task `agent` produces a minimal agent JAR `agent.jar` with all capabilities for testing in development environments.

The test is in `Agent::transform`.
