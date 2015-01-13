package org.vaadin.se.trollator;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.vaadin.teemu.webcam.Webcam;

@Theme("valo")
@SuppressWarnings("serial")
public class TrollatorUI extends UI {

    private static final String TROLL_IMAGE = "/home/se/images/trollface.png";
    private static final String OPENCV_HAARCASCADES_HOME = "/usr/local/share/OpenCV/haarcascades/";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @WebServlet(value = {"/*"}, asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = TrollatorUI.class)
    public static class Servlet extends VaadinServlet {
    }

    protected File receivedImageFile;

    protected DynamicImage image;

    @Override
    protected void init(VaadinRequest request) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        ;
        setContent(layout);

        Webcam upload = new Webcam();
        upload.setReceiver(new Upload.Receiver() {

            @Override
            public OutputStream receiveUpload(String filename, String mimeType) {
                try {
                    receivedImageFile = File.createTempFile(
                            "img" + System.currentTimeMillis(), ".png");
                    return new FileOutputStream(receivedImageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        upload.addCaptureSucceededListener(new Webcam.CaptureSucceededListener() {

            @Override
            public void captureSucceeded(Webcam.CaptureSucceededEvent event) {

                // Face detect
                Mat original = Imgcodecs.imread(receivedImageFile
                        .getAbsolutePath());
                Rect[] faces = faceDetect(original);
                BufferedImage detectedImage = toBufferedImage(original);

                BufferedImage overlay = loadImage(new File(TROLL_IMAGE));

                Graphics2D g = detectedImage.createGraphics();
                for (int i = 0; i < faces.length; i++) {
                    Rect rect = faces[i];
                    g.drawImage(overlay, rect.x, rect.y, rect.x + rect.width,
                            rect.y + rect.height, 0, 0, overlay.getWidth(),
                            overlay.getHeight(), null);
                }
                g.dispose();

                // remove previous results
                if (image != null) {
                    layout.removeComponent(image);
                }

                // add image
                image = new DynamicImage(detectedImage);
                layout.addComponent(image);

            }
        });
        layout.addComponent(upload);

    }

    protected BufferedImage loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Rect[] faceDetect(Mat image) {

		// Create a face detector from the cascade file in the resources
        // directory.
        CascadeClassifier faceDetector = new CascadeClassifier(OPENCV_HAARCASCADES_HOME
                + "haarcascade_frontalface_default.xml");

		// Detect faces in the image.
        // MatOfRect is a special container class for Rect.
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections, 1.2, 4, 0,
                new org.opencv.core.Size(10, 10), new org.opencv.core.Size(
                        1000, 1000));

        System.out.println(String.format("Detected %s faces",
                faceDetections.toArray().length));

        return faceDetections.toArray();

    }

    public static BufferedImage toBufferedImage(Mat mat) {
        MatOfByte mb = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, mb);
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO
                    .read(new ByteArrayInputStream(mb.toArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bufferedImage;
    }

}
