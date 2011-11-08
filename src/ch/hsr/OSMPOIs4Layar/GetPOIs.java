package ch.hsr.OSMPOIs4Layar;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import org.postgis.*;

import org.json.simple.*;

import ch.hsr.OSMPOIs4Layar.*;

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

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("processing request ...");
		PrintWriter writer = response.getWriter();
		print_header(writer);

		try {
			RequestParser requestParser = new RequestParser(request);
			HashMap<String, Object> requestProperties = requestParser.parse();
		} catch (IllegalArgumentException e) {
			JSONObject j = new JSONObject();
			j.put("errorCode", 20); // 20..29 allowed
			j.put("errorString", e.getMessage());
			j.writeJSONString(writer);
			return;
		}

		writer.println("<code><ul>");
		for(Enumeration<String> names = request.getParameterNames(); names.hasMoreElements();) {
			String name = names.nextElement();
			String value = request.getParameter(name);
			writer.println("<li>" + name +": " + value + "</li>");
		}
		writer.println("</ul></code>");

		try {
			Point[] pois = getPOIsFromDatabase();
			writer.println("<p>Found "+ pois.length + " POIs in database. (" + pois.getClass().getName() + ").</p>");
			writer.println("<ul>");
			for (Point poi : pois) {
				writer.println("<li>" + poi.toString() +"</li>");
			}
			writer.println("</ul>");
		} catch (SQLException e) {
			writer.println("Failed to get the POIs from the database. :-(");
			writer.println("<pre>");
			e.printStackTrace(writer);
			writer.println("</pre>");
		}

		print_footer(writer);
	}

	private void print_footer(PrintWriter writer) {
		writer.println("<body>");
		writer.println("</html>");
		writer.close();
	}

	private void print_header(PrintWriter writer) throws IOException {
		writer.println("<html>");
		writer.println("<head><title>OSMPOIs4Layar</title></head>");
		writer.println("<body>");
		writer.println("	<h1>OSMPOIs4Layar</h1>");

		if (connected) {
			writer.println("<p>Successfully connected to the database " + Database.dbname + ".</p>");
		} else {
			writer.println("<p>Failed to connect to the database.</p>");
		}
	}

	private Point[] getPOIsFromDatabase() throws SQLException {
		Statement st = conn.createStatement();
		ResultSet rs;
		int limit = 1000;

		rs = st.executeQuery(
		"SELECT Transform(osm_poi.way, 4326) AS geom, name AS label " +
		"FROM osm_poi, (SELECT ST_Transform( ST_GeomFromText('POINT(8.856484 47.232707)', 4326), 900913) way) AS mylocation " +
		"WHERE ST_DWithin(osm_poi.way, mylocation.way, 1000) ");

		Point[] pois = new Point[ limit ];
		int i = 0;
		while (rs.next()) {
			PGgeometry geom = (PGgeometry)rs.getObject(1);
			pois[i++] = (Point)geom.getGeometry();
		}
		rs.close();
		st.close();

		Point[] clean_pois = new Point[i];
		for (int j = 0; j < i; j++) {
			clean_pois[j] = pois[j];
		}

		return clean_pois;
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