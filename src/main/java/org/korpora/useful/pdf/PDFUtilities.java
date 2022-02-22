package org.korpora.useful.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * utility class for PDF production
 *
 * @author Bernhard Fisseni
 */
public final class PDFUtilities {

    /**
     * the namespace of TIFF properties in EXIV2 XMPs (as yet unused)
     */
    public static final String TIFF = "http://ns.adobe.com/tiff/1.0/";

    /**
     * the namespaces for crs in XMP
     */
    public static final String CRS = "http://ns.adobe.com/camera-raw-settings/1.0/";

    /**
     * default JPEG quality
     */
    public static final float JPEG_QUALITY = 0.92f;
    private static final int DEFAULT_RESOLUTION = 240;

    /**
     * grid units per inch
     */
    static float GRID_PER_INCH = 72F;

    static final String COLOR_REGISTRY = "http://www.color.org";
    static final String SRGB_PROFILE = "sRGB IEC61966-2.1";
    static final byte[] colorProfileBytes;
    static final String[] CREATORS = new String[]{
            "Universität Duisburg-Essen, Arbeitsstelle für Edition und "
                    + "Editionstechnik",
            "Leibniz-Institut für Deutsche Sprache"};

    static {
        try (InputStream colorProfileStream = PDFUtilities.class
                .getResourceAsStream("/sRGB.icc")) {
            colorProfileBytes = colorProfileStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * just static methods, no real constructor needed
     */
    private PDFUtilities() {
    }

    /**
     * add XMP metadata to output document
     *
     * @param document the PDF document
     * @param title    the title
     * @param level    the PDF/A level
     * @param subLevel the PDF/A conformance level
     * @throws ProcessingException in case of problems
     */
    public static void addMetaData(final PDDocument document,
                                   final String title, final int level,
                                   final PDFSubLevel subLevel) throws ProcessingException {
        // add XMP metadata
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();

        try (ByteArrayOutputStream metaBytes = new ByteArrayOutputStream()) {
            PDDocumentInformation info = document.getDocumentInformation();
            // create XMP
            info.setTitle(title);
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.setTitle(title);
            for (String creator : CREATORS)
                dc.addCreator(creator);

            PDFAIdentificationSchema id = xmp
                    .createAndAddPFAIdentificationSchema();
            id.setPart(level);
            id.setConformance(subLevel.toString());

            XmpSerializer serializer = new XmpSerializer();
            serializer.serialize(xmp, metaBytes, true);

            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata(metaBytes.toByteArray());
            document.getDocumentCatalog().setMetadata(metadata);
        } catch (BadFieldValueException e) {
            // won't happen here, as the provided value is valid
            throw new IllegalArgumentException(e);
        } catch (IOException | TransformerException e) {
            throw new ProcessingException(e);
        }

    }

    /**
     * add SRGB color space to make PDF/A checkers happy
     *
     * @param document the PDF document
     * @throws IOException if anything goes wrong
     */
    public static void addColorProfileSRGB(PDDocument document)
            throws IOException {
        try (ByteArrayInputStream colorProfile = new ByteArrayInputStream(
                colorProfileBytes)) {
            PDOutputIntent intent = new PDOutputIntent(document, colorProfile);
            intent.setInfo(SRGB_PROFILE);
            intent.setOutputCondition(SRGB_PROFILE);
            intent.setOutputConditionIdentifier(SRGB_PROFILE);
            intent.setRegistryName(COLOR_REGISTRY);
            document.getDocumentCatalog().addOutputIntent(intent);
        }
    }

    /**
     * add a new page to the current output document
     *
     * @param document the document
     * @param width    the width
     * @param height   the height
     * @return the content stream of the new page
     * @throws IOException if anything goes wrong
     */
    public static PDPageContentStream newPage(final PDDocument document,
                                              final float width,
                                              final float height) throws IOException {
        PDPage currentPage = new PDPage(new PDRectangle(width, height));
        document.addPage(currentPage);
        return new PDPageContentStream(document, currentPage,
                PDPageContentStream.AppendMode.APPEND, true, true);
    }

    /**
     * add a new page to the current output document
     *
     * @param document   the document
     * @param width      the width
     * @param height     the height
     * @param cropUpperX the upper right x coordinate of the cropBox
     * @param cropUpperY the upper right y coordinate of the cropBox
     * @param cropLowerX the lower left x coordinate of the cropBox
     * @param cropLowerY the lower left y coordinate of the cropBox
     * @return the content stream of the new page
     * @throws IOException if anything goes wrong
     */
    public static PDPageContentStream newPage(final PDDocument document,
                                              final float width,
                                              final float height,
                                              final float cropUpperX,
                                              final float cropUpperY,
                                              final float cropLowerX,
                                              final float cropLowerY) throws IOException {
        PDPage currentPage = new PDPage(new PDRectangle(width, height));
        PDRectangle cropBox = new PDRectangle();
        cropBox.setUpperRightX(cropUpperX);
        cropBox.setUpperRightY(cropUpperY);
        cropBox.setLowerLeftX(cropLowerX);
        cropBox.setLowerLeftY(cropLowerY);
        currentPage.setCropBox(cropBox);
        document.addPage(currentPage);
        return new PDPageContentStream(document, currentPage,
                PDPageContentStream.AppendMode.APPEND, true, true);
    }

    public static float toGridValue(float value, float resolution) {
        return value * GRID_PER_INCH / resolution;
    }

    public static float toGridValue(int value, int resolution) {
        return ((float) value) * GRID_PER_INCH / ((float) resolution);
    }

    public static float toGridValue(float value) {
        return value * GRID_PER_INCH / DEFAULT_RESOLUTION;
    }

    public static PDRectangle makeGridRectangle(int width, int height, int xResolution, int yResolution) {
        return new PDRectangle(toGridValue(width), toGridValue(height));
    }

    public static PDRectangle makeGridRectangle(int width, int height) {
        return makeGridRectangle(width, height, DEFAULT_RESOLUTION, DEFAULT_RESOLUTION);
    }

}
