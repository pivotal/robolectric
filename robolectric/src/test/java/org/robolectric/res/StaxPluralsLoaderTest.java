package org.robolectric.res;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.R;
import org.robolectric.util.TestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.util.TestUtil.testResources;

public class StaxPluralsLoaderTest {
  private PackageResourceTable resourceTable;

  @Before
  public void setUp() throws Exception {
    resourceTable = new ResourceTableFactory().newResourceTable("org.robolectric");
    StaxPluralsLoader pluralsLoader = new StaxPluralsLoader(resourceTable, "plurals", ResType.CHAR_SEQUENCE);

    new StaxDocumentLoader(R.class.getPackage().getName(), testResources().getResourceBase(),
        new NodeHandler().addHandler("resources", new NodeHandler()
            .addHandler("plurals", pluralsLoader)
        )
    ).load("values");
  }

  @Test
  public void testPluralsAreResolved() throws Exception {
    ResName resName = new ResName(TestUtil.TEST_PACKAGE, "plurals", "beer");
    PluralRules pluralRules = (PluralRules) resourceTable.getValue(resName, "");
    assertThat(pluralRules.find(0).string).isEqualTo("@string/howdy");
    assertThat(pluralRules.find(1).string).isEqualTo("One beer");
    assertThat(pluralRules.find(2).string).isEqualTo("Two beers");
    assertThat(pluralRules.find(3).string).isEqualTo("%d beers, yay!");
  }
}
