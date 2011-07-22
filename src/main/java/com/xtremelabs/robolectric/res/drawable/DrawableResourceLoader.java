package com.xtremelabs.robolectric.res.drawable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.xtremelabs.robolectric.res.ResourceExtractor;
import com.xtremelabs.robolectric.res.XmlLoader;

/**
 * DrawableResourceLoader
 */
public class DrawableResourceLoader extends XmlLoader {
    /** document */
    protected Map<String, Document> documents = new HashMap<String, Document>();

    /** resource directory */
    protected File resourceDirectory;

    /**
     * DrawableResourceLoader constructor.
     * @param extractor Extractor
     * @param resourceDirectory Resource directory
     */
    public DrawableResourceLoader(ResourceExtractor extractor,
            File resourceDirectory) {
        super(extractor);
        this.resourceDirectory = resourceDirectory;
    }

    /**
     * Check if resource is xml.
     * @param resourceId Resource id
     * @return Boolean
     */
    public boolean isXml(int resourceId) {
        return documents.containsKey(resourceExtractor
                .getResourceName(resourceId));
    }

    /**
     * Store document locally keyed by resource name.
     * @param xmlFile Xml file
     * @param document  Document
     * @param isSystem System resource
     * @throws Exception
     * @see XmlLoader#processResourceXml(File,Document,boolean)
     */
    @Override
    protected void processResourceXml(File xmlFile, Document document,
            boolean isSystem) throws Exception {
        String name = toResourceName(xmlFile);
        if (!documents.containsKey(name)) {
            documents.put(name, document);
        }
    }

    /**
     * Convert file name to resource name.
     * @param xmlFile Xml File
     * @return Resource name
     */
    private String toResourceName(File xmlFile) {
        try {
            return xmlFile.getCanonicalPath().replaceAll("[/\\\\\\\\]", "/")
                    .replaceAll("^.*?/res/", "").replaceAll("\\..+$", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get drawables by resource id.
     * @param resourceId Resource id
     * @return Drawables
     */
    public int[] getDrawableIds(int resourceId) {
        Document document = documents.get(resourceExtractor
                .getResourceName(resourceId));

        NodeList items = document.getElementsByTagName("item");
        int[] drawableIds = new int[items.getLength()];

        for (int i = 0; i < items.getLength(); i++) {
            Node item = items.item(i);
            Node drawableName = item.getAttributes().getNamedItem(
                    "android:drawable");
            drawableIds[i] = resourceExtractor.getResourceId(drawableName
                    .getNodeValue());
        }

        return drawableIds;
    }
}
