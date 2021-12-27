This repository contains the attachment code and an example.
The attachment code is provided here in order to make the readme not look empty.

See [Agent.java](source/net/auoeke/selfattachment/Agent.java).
```java
var VM = Class.forName("jdk.internal.misc.VM");
var theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
theUnsafe.trySetAccessible();
var unsafe = (Unsafe) theUnsafe.get(null);
((Map<String, String>) unsafe.getObject(VM, unsafe.staticFieldOffset(VM.getDeclaredField("savedProps")))).put("jdk.attach.allowAttachSelf", "true");

var vm = VirtualMachine.attach(String.valueOf(ManagementFactory.getRuntimeMXBean().getPid()));
vm.loadAgent("agent.jar");
vm.detach();
```
