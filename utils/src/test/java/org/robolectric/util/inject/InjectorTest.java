package org.robolectric.util.inject;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

public class InjectorTest {

  private Injector injector;

  @Before
  public void setUp() throws Exception {
    injector = new Injector();
  }

  @Test
  public void whenImplSpecified_shouldProvideInstance() throws Exception {
    injector.register(Thing.class, MyThing.class);

    assertThat(injector.get(Thing.class))
        .isInstanceOf(MyThing.class);
  }

  @Test
  public void whenImplSpecified_shouldUseSameInstance() throws Exception {
    injector.register(Thing.class, MyThing.class);

    Thing thing = injector.get(Thing.class);
    assertThat(injector.get(Thing.class))
        .isSameAs(thing);
  }

  // specified in resources/META-INF/services/org.robolectric.util.inject.Thing
  @Test
  public void whenServiceSpecified_shouldProvideInstance() throws Exception {
    assertThat(injector.get(Thing.class))
        .isInstanceOf(ThingFromServiceConfig.class);
  }

  // specified in resources/META-INF/services/org.robolectric.util.inject.Thing
  @Test
  public void whenServiceSpecified_shouldUseSameInstance() throws Exception {
    Thing thing = injector.get(Thing.class);
    assertThat(injector.get(Thing.class))
        .isSameAs(thing);
  }

  @Test
  public void whenDefaultSpecified_shouldProvideInstance() throws Exception {
    injector.registerDefault(Umm.class, MyUmm.class);

    assertThat(injector.get(Umm.class))
        .isInstanceOf(MyUmm.class);
  }

  @Test
  public void whenDefaultSpecified_shouldUseSameInstance() throws Exception {
    Thing thing = injector.get(Thing.class);
    assertThat(injector.get(Thing.class))
        .isSameAs(thing);
  }

  @Test
  public void whenNoImplOrServiceOrDefaultSpecified_shouldThrow() throws Exception {
    try {
      injector.get(Umm.class);
      fail();
    } catch (InjectionException e) {
      // ok
    }
  }

  @Test
  public void registerDefaultService_providesFallbackImplOnlyIfNoServiceSpecified()
      throws Exception {
    injector.registerDefault(Thing.class, MyThing.class);

    assertThat(injector.get(Thing.class))
        .isInstanceOf(ThingFromServiceConfig.class);

    injector.registerDefault(Umm.class, MyUmm.class);
    assertThat(injector.get(Thing.class))
        .isInstanceOf(ThingFromServiceConfig.class);
  }


  @Test
  public void shouldInjectConstructor() throws Exception {
    injector.register(Thing.class, MyThing.class);
    injector.register(Umm.class, MyUmm.class);

    Umm umm = injector.get(Umm.class);
    assertThat(umm).isNotNull();
    assertThat(umm).isInstanceOf(MyUmm.class);

    MyUmm myUmm = (MyUmm) umm;
    assertThat(myUmm.thing).isNotNull();
    assertThat(myUmm.thing).isInstanceOf(MyThing.class);

    assertThat(myUmm.thing).isSameAs(injector.get(Thing.class));
  }

  @Test public void subInjector_whenNoRegisteredProvider_shouldDelegateToSuper() throws Exception {
    injector.register(Thing.class, MyThing.class);
    Injector subInjector = new Injector(this.injector);

    assertThat(subInjector.get(Thing.class))
        .isInstanceOf(MyThing.class);
  }

  /////////////////////////////

  public static class MyThing implements Thing {

  }

  public static class ThingFromServiceConfig implements Thing {

  }

  private interface Umm {

  }

  public static class MyUmm implements Umm {

    private final Thing thing;

    @Inject
    MyUmm(Thing thing) {
      this.thing = thing;
    }
  }
}