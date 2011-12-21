package ch.hsr.osmpois4layar;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import org.postgis.*;

import org.json.simple.*;

import ch.hsr.osmpois4layar.*;

/**
 * Servlet implementation class GetPOIs
 */
public class GetPOIs extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private java.sql.Connection conn; // database connection
	private Boolean connected = false;

    public GetPOIs() {
        // TODO Auto-generated constructor stub
    }

	public void init(ServletConfig config) throws ServletException {
		establishDatabaseConnection();
	}

//	public void destroy() {
//	}

	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		GetPOIsRequest getPOIsRequest;

		try {
			getPOIsRequest = new GetPOIsRequest(request);
		} catch (IllegalArgumentException e) {
			JSONObject j = new JSONObject();
			j.put("errorCode", 20); // 20..29 allowed
			j.put("errorString", e.getMessage());
			j.writeJSONString(writer);
			return;
		}

		try {
			ArrayList<Hotspot> pois = getPOIsFromDatabase(getPOIsRequest);
			JSONObject j = new JSONObject();
			JSONArray hotspots = new JSONArray();
			for(Hotspot poi : pois) {
				hotspots.add(poi.to_json());
			}
			j.put("hotspots", hotspots);
			j.put("errorCode", 0);
			j.put("errorString", "ok");
			j.writeJSONString(writer);
			return;
		} catch (SQLException e) {
			JSONObject j = new JSONObject();
			j.put("errorCode", 21); // 20..29 allowed
			j.put("errorString", "Failed to get hotspots from the database.");
			j.writeJSONString(writer);
			return;
		}
	}

	private ArrayList<Hotspot> getPOIsFromDatabase(GetPOIsRequest properties) throws SQLException {
		Statement st = conn.createStatement();
		ResultSet rs;

		double lon = properties.lon;
		double lat = properties.lat;
		int radius;

		if (properties.hasRadius()) {
			radius = properties.getRadius();
		} else {
			radius = 1000;
		}
		
		if (properties.hasAccuracy()) {
			radius += properties.getAccuracy();
		}

		String query =
		"SELECT Transform(osm_poi.way, 4326) AS geom, name AS label " +
		"FROM osm_poi, (SELECT ST_Transform( ST_GeomFromText('POINT(" + lon + " " + lat +
		")', 4326), 900913) way) AS mylocation " +
		"WHERE ST_DWithin(osm_poi.way, mylocation.way, " + radius + ") ";
		if (properties.hasName()) {
			query = query + "AND name = '%" + properties.getName() + "%'";
		}
		System.err.println(query);
		rs = st.executeQuery(query);

		ArrayList<Hotspot> hotspots = new ArrayList<Hotspot>();
		while (rs.next()) {
			PGgeometry geom = (PGgeometry)rs.getObject(1);
			Hotspot hotspot = new Hotspot(properties.layerName, (Point)geom.getGeometry(), (String)rs.getObject(2)); 
			hotspots.add(hotspot);
		}
		rs.close();
		st.close();

		return hotspots;
	}

	private void establishDatabaseConnection() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch(ClassNotFoundException e) {
			System.err.println("Failed to load PostgreSQL database driver.");
			e.printStackTrace();
			return;
		}

		String url = "jdbc:postgresql://" + Database.host + ":" + Database.port + "/" + Database.dbname;
		Properties props = new Properties();
		props.setProperty("user", Database.user);
		props.setProperty("password", Database.password);
		try {
			conn = DriverManager.getConnection(url, props);
			connected = true;
			System.err.println("Successfully connected to database.");

		    /*
		    * Add the geometry types to the connection. Note that you
		    * must cast the connection to the pgsql-specific connection
		    * implementation before calling the addDataType() method.
		    */
		    ((org.postgresql.PGConnection)conn).addDataType("geometry",Class.forName("org.postgis.PGgeometry"));
		    ((org.postgresql.PGConnection)conn).addDataType("box3d",Class.forName("org.postgis.PGbox3d"));
		    System.err.println("Successfully registered PostGIS data types.");
		} catch (SQLException e) {
			System.err.println("Failed to connect to database.");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to register PostGIS data types.");
			e.printStackTrace();
			return;
		}
	}
}