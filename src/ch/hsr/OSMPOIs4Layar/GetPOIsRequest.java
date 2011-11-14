package ch.hsr.OSMPOIs4Layar;

import javax.servlet.http.HttpServletRequest;

public class GetPOIsRequest {

	private HttpServletRequest request;

	public String userId, layerName, version;
	public double lat, lon;
	private int accuracy, radius;


	public GetPOIsRequest(HttpServletRequest request) {
		this.request = request;
		this.accuracy = 5;
		parse();
	}

	private void parse() {
		try {
			// mandatory
			this.userId = request.getParameter("userId");
			this.layerName = request.getParameter("layerName");
			this.lat = Float.parseFloat(request.getParameter("lat"));
			this.lon = Float.parseFloat(request.getParameter("lon"));
			this.version = request.getParameter("version");

			// optional
			String raw_accuracy = request.getParameter("accuracy");
			if (raw_accuracy != null) {
				this.accuracy = Integer.parseInt(raw_accuracy);
				if (this.accuracy < 0)
					throw new IllegalArgumentException("Negative accuracy.");
			} else {
				this.accuracy = -1;
			}


			String raw_radius = request.getParameter("radius");
			if (raw_radius != null) {
				this.radius = Integer.parseInt(raw_radius);
				if (this.radius < 0)
					throw new IllegalArgumentException("Negative radius.");
			} else {
				this.radius = -1;
			}
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Malformed request.");
		}
	}

	public boolean hasAccuracy() {
		if (accuracy == -1) return false;
		return true;
	}

	public boolean hasRadius() {
		if (radius == -1) return false;
		return true;
	}

	public int getAccuracy() {
		return this.accuracy;	
	}

	public int getRadius() {
		return this.radius;	
	}
}
