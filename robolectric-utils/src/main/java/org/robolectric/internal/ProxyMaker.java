package org.robolectric.internal;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.V1_7;

public class ProxyMaker {
  private static final String TARGET_FIELD = "__proxy__";
  private static final String PROXY_NAME =
      Type.getInternalName(ProxyMaker.class) + "$GeneratedProxy";
  private static final Type PROXY_TYPE = Type.getType(PROXY_NAME);

  private static final Unsafe UNSAFE;
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private final MethodMapper methodMapper;
  private final ClassValue<Factory> factories;

  public ProxyMaker(MethodMapper methodMapper) {
    this.methodMapper = methodMapper;
    factories = new ClassValue<Factory>() {
      @Override protected Factory computeValue(Class<?> type) {
        return createProxyFactory(type);
      }
    };
  }

  public <T> T createProxy(Class<T> targetClass, T target) {
    return factories.get(targetClass).createProxy(targetClass, target);
  }

  <T> Factory createProxyFactory(Class<T> targetClass) {
    Type targetType = Type.getType(targetClass);
    String targetName = targetType.getInternalName();
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES| ClassWriter.COMPUTE_MAXS);
    writer.visit(V1_7, ACC_PUBLIC | ACC_SUPER | ACC_FINAL, PROXY_NAME, null, targetName, null);

    writer.visitField(ACC_PUBLIC, TARGET_FIELD, targetType.getDescriptor(), null, null);

    for (java.lang.reflect.Method method : targetClass.getMethods()) {
      if (!shouldProxy(method)) continue;

      Method proxyMethod = Method.getMethod(method);
      GeneratorAdapter m = new GeneratorAdapter(ACC_PUBLIC, Method.getMethod(method), null, null, writer);
      m.loadThis();
      m.getField(PROXY_TYPE, TARGET_FIELD, targetType);
      m.loadArgs();
      String targetMethod = methodMapper.getName(targetClass.getName(), method.getName());
      // In Java 8 we could use invokespecial here but not in 7, from jvm spec:
      // If an invokespecial instruction names a method which is not an instance
      // initialization method, then the type of the target reference on the operand
      // stack must be assignment compatible with the current class (JLS §5.2).
      m.visitMethodInsn(INVOKEVIRTUAL, targetName, targetMethod, proxyMethod.getDescriptor(), false);
      m.returnValue();
      m.endMethod();
    }

    writer.visitEnd();

    final Class<?> proxyClass = UNSAFE.defineAnonymousClass(targetClass, writer.toByteArray(), null);

    try {
      final MethodHandle setter = LOOKUP.findSetter(proxyClass, TARGET_FIELD, targetClass);
      return new Factory() {
        @Override public <E> E createProxy(Class<E> targetClass, E target) {
          try {
            Object proxy = UNSAFE.allocateInstance(proxyClass);

            setter.invoke(proxy, target);

            return targetClass.cast(proxy);
          } catch (Throwable t) {
            throw new AssertionError(t);
          }
        }
      };
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new AssertionError(e);
    }
  }

  private static boolean shouldProxy(java.lang.reflect.Method method) {
    int modifiers = method.getModifiers();
    return !Modifier.isAbstract(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isPrivate(
        modifiers) && !Modifier.isNative(modifiers);
  }

  interface MethodMapper {
    String getName(String className, String methodName);
  }

  interface Factory {
    <T> T createProxy(Class<T> targetClass, T target);
  }
}
