package org.robolectric.manifest;

import android.content.res.Resources;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.res.ResourceIndex;
import org.robolectric.res.ResourceLoader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link MetaData}
 */
@RunWith(JUnit4.class)
public class MetaDataTest {

  @Mock ResourceLoader resourceLoader;
  @Mock ResourceIndex resourceIndex;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = Resources.NotFoundException.class)
  public void testNonExistantResource_throwsResourceNotFoundException() {
    when(resourceLoader.getResourceIndex()).thenReturn(resourceIndex);
    Element metaDataElement = createMetaDataNode("aName", "@xml/non_existant_resource");

    MetaData metaData = new MetaData(ImmutableList.<Node>of(metaDataElement));
    metaData.init(resourceLoader, "a.package");
  }

  private static Element createMetaDataNode(String name, String value) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    Element metaDataElement;
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      metaDataElement = db.newDocument().createElement("meta-data");
      metaDataElement.setAttribute("android:name", name);
      metaDataElement.setAttribute("android:value", value);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    return metaDataElement;
  }
}
