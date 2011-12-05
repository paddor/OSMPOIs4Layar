package ch.hsr.osmpois4layar;

import org.json.simple.JSONObject;
import org.postgis.Point;

public class Hotspot {

	
	private String layerName;
	private Point geolocation;
	private String title;
	
	public Hotspot(String layerName, Point geolocation, String title) {
		this.layerName = layerName;
		this.geolocation = geolocation;
		this.title = title;
	}
	
	public JSONObject to_json() {
		JSONObject j = new JSONObject();
		j.put("id", this.uniqueId());
		JSONObject anchor = new JSONObject();
		JSONObject geolocation = new JSONObject();
		geolocation.put("lon", this.geolocation.getX());
		geolocation.put("lat", this.geolocation.getY());
		anchor.put("geolocation", geolocation);
		j.put("anchor", anchor);
		return j;
	}
	
	public String uniqueId() {		
		String a = Integer.toString(this.layerName.hashCode());
		String b = Integer.toString(this.geolocation.hashCode());
		String c;
		if (this.title != null) {
			c = Integer.toString(this.title.hashCode());
		} else {
			c = "";
		}
		return Integer.toString((a + "_" + b + "_" + c).hashCode());
	}

}
