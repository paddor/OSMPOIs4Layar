package ch.hsr.OSMPOIs4Layar;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;

public class RequestParser {
	
	private HttpServletRequest request;
	
	public RequestParser(HttpServletRequest request) {
		this.request = request;
	}


	public HashMap<String, Object> parse() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		try {
			// mandatory
			map.put("userId", request.getParameter("userId"));
			map.put("layerName", request.getParameter("layerName"));
			map.put("lat", Float.parseFloat(request.getParameter("lat")));
			map.put("lon", Float.parseFloat(request.getParameter("lon")));
			map.put("version", request.getParameter("version"));

			// optional
			String raw_accuracy = request.getParameter("accuracy");
			if (raw_accuracy != null)
				map.put("accuracy", Integer.parseInt(raw_accuracy));

			String raw_radius = request.getParameter("radius");
			if (raw_radius != null) 
				map.put("radius", Integer.parseInt(raw_radius));
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Malformed request.");
		}

		return map;
	}

}
