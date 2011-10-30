package ch.hsr.OSMPOIs4Layar;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.*;
import java.util.Properties;

/**
 * Servlet implementation class GetPOIs
 */
public class GetPOIs extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection conn; // database connection
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
		writer.println("<html>");
		writer.println("<head><title>OSMPOIs4Layar</title></head>");
		writer.println("<body>");
		writer.println("	<h1>OSMPOIs4Layar</h1>");
		writer.println("<p>" + Database.host + "</p>");
		if (connected) {
			writer.println("<p>Successfully connected to the database. :-)</p>");
			writer.println("<p>Database connection as String:" + conn.toString() + "</p>");
		} else {
			writer.println("No connection to the database. :-(");
		}

		try {
			String[] pois = getPOIsFromDatabase(100);
			writer.println("<p>Found "+ pois.length + " POIs in database.</p>");
			writer.println("<ul>");
			for (String poi : pois) {
				writer.println("<li>" + poi + "</li>");
			}
			writer.println("</ul>");
		} catch (SQLException e) {
			writer.println("Failed to get the POIs from the database. :-(");
			writer.println("<pre>");
			e.printStackTrace(writer);
			writer.println("</pre>");
		}

		writer.println("<body>");
		writer.println("</html>");
		writer.close();
	}

	private String[] getPOIsFromDatabase(int limit) throws SQLException {
		Statement st = conn.createStatement();
		ResultSet rs;

		rs = st.executeQuery(
				"SELECT ST_AsText(osm_poi.way) AS geom, name AS label " +
		"FROM osm_poi, (SELECT ST_Transform( ST_GeomFromText('POINT(8.856484 47.232707)', 4326), 900913) way) AS mylocation " +
		"WHERE ST_DWithin(osm_poi.way, mylocation.way,1000) " +
		"LIMIT " + limit);

		String[] points = new String[ limit ];
		int i = 0;
		while (rs.next()) {
			points[i++] = rs.getString(1);
		}
		rs.close();
		st.close();

		String[] clean_points = new String[i];
		for (int j = 0; j < i; j++) {
			clean_points[j] = points[j];
		}

		return clean_points;
}

	private void establishDatabaseConnection() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch(ClassNotFoundException e) {
			System.err.println("Unable to load PostgreSQL database driver.");
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
		} catch (SQLException e) {
			System.err.println("Unable to connect to database.");
			e.printStackTrace();
		}
	}
}