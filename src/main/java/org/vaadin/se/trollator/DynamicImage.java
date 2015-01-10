package org.vaadin.se.trollator;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Image;

public class DynamicImage extends Image {

	private BufferedImage imageData;
	private String uri = "img" + System.currentTimeMillis() + ".png";
	Resource resource = new ExternalResource(uri);

	public DynamicImage(BufferedImage image) {
		this.imageData = image;
		setSource(resource);
	}

	private final RequestHandler requestHandler = new RequestHandler() {

		@Override
		public boolean handleRequest(VaadinSession session,
				VaadinRequest request, VaadinResponse response)
				throws IOException {
			System.out.println("Requesting "+ request.getPathInfo());
			if (imageData != null && ("/" + uri).equals(request.getPathInfo())) {
				response.setContentType("image/png");
				ImageIO.write(imageData, "png", response.getOutputStream());
				return true;
			}
			return false;
		}
	};

	@Override
	public void attach() {
		super.attach();
		getSession().addRequestHandler(requestHandler);
	}

	@Override
	public void detach() {
		super.detach();
		getSession().removeRequestHandler(requestHandler);
	}
}
