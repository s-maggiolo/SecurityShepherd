package servlets.challenges;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import utils.ShepherdExposedLogManager;
import dbProcs.Database;
/**
 * Level : SQL Injection 5
 * <br><br>
 * 
 * @author mark
 *
 */
public class SqlInjection5 extends HttpServlet
{
	private static final String levelName = "SQLi C5 Shop";
	private static String levelSolution = "343f2e424d5d7a2eff7f9ee5a5a72fd97d5a19ef7bff3ef2953e033ea32dd7ee";
	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(SqlInjection5.class);
	/**
	 * //TODO - JavaDoc
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdExposedLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		//Attempting to recover username of session that made request
		try
		{
			if (request.getSession() != null)
			{
				HttpSession ses = request.getSession();
				String userName = (String) ses.getAttribute("decyrptedUserName");
				log.debug(userName + " accessed " + levelName + " Servlet");
			}
			else
			{
				log.debug(levelName + " Servlet Accessed by an empty session");
			}
		}
		catch (Exception e)
		{
			log.debug(levelName + " Servlet Accessed");
			log.error("Could not retrieve username from session");
		}
		PrintWriter out = response.getWriter();  
		out.print(getServletInfo());
		String htmlOutput = new String();
		String applicationRoot = getServletContext().getRealPath("");
		Encoder encoder = ESAPI.encoder();
		try
		{
			int megustaAmount = validateAmount(Integer.parseInt(request.getParameter("megustaAmount")));
			log.debug("megustaAmount - " + megustaAmount);
			int trollAmount = validateAmount(Integer.parseInt(request.getParameter("trollAmount")));
			log.debug("trollAmount - " + trollAmount);
			int rageAmount = validateAmount(Integer.parseInt(request.getParameter("rageAmount")));
			log.debug("rageAmount - " + rageAmount);
			int notBadAmount = validateAmount(Integer.parseInt(request.getParameter("notBadAmount")));
			log.debug("notBadAmount - " + notBadAmount);
			String couponCode = request.getParameter("couponCode");
			log.debug("couponCode - " + couponCode);
			
			//Working out costs
			int megustaCost = megustaAmount * 30;
			int trollCost = trollAmount * 3000;
			int rageCost = rageAmount * 45;
			int notBadCost = notBadAmount * 15;
			int perCentOffMegusta = 0; // Will search for coupons in DB and update this int
			int perCentOffTroll = 0; // Will search for coupons in DB and update this int
			int perCentOffRage = 0; // Will search for coupons in DB and update this int
			int perCentOffNotBad = 0; // Will search for coupons in DB and update this int
			
			htmlOutput = new String();
			Connection conn = Database.getChallengeConnection(applicationRoot, "SqlInjectionChallenge5Shop");
			log.debug("Looking for Coupons");
			PreparedStatement prepstmt = conn.prepareStatement("SELECT itemId, perCentOff FROM coupons WHERE couponCode = ?"
					+ "UNION SELECT itemId, perCentOff FROM vipCoupons WHERE couponCode = ?");
			prepstmt.setString(1, couponCode);
			prepstmt.setString(2, couponCode);
			ResultSet coupons = prepstmt.executeQuery();
			try
			{
				if(coupons.next())
				{
					if(coupons.getInt(1) == 1) // MeGusta
					{
						log.debug("Found coupon for %" + coupons.getInt(2) + " off MeGusta");
						perCentOffMegusta = coupons.getInt(2);
					}
					else if (coupons.getInt(1) == 2) // Troll
					{
						log.debug("Found coupon for %" + coupons.getInt(2) + " off Troll");
						perCentOffTroll = coupons.getInt(2);
					}
					else if (coupons.getInt(1) == 3) // Rage
					{
						log.debug("Found coupon for %" + coupons.getInt(2) + " off Rage");
						perCentOffRage = coupons.getInt(2);
					}
					else if (coupons.getInt(1) == 4) // NotBad
					{
						log.debug("Found coupon for %" + coupons.getInt(2) + " off NotBad");
						perCentOffNotBad = coupons.getInt(2);
					}
					
				}
			}
			catch(Exception e)
			{
				log.debug("Could Not Find Coupon: " + e.toString());
			}
			conn.close();
			
			//Work Out Final Cost
			megustaCost = megustaCost - (megustaCost * (perCentOffMegusta/100));
			rageCost = rageCost - (rageCost * (perCentOffRage/100));
			notBadCost = notBadCost - (notBadCost * (perCentOffNotBad/100));
			trollCost = trollCost - (trollCost * (perCentOffTroll/100));
			int finalCost = megustaCost + rageCost + notBadAmount + trollCost;
			
			//Output Order
			htmlOutput = "<h3>Order Complete</h3>"
					+ "Your order has been made and has been sent to our magic shipping department that knows where you want this to be delieved via brain wave sniffing techniques.<br/><br/>"
					+ "Your order comes to a total of <a><strong>$" + finalCost + "</strong></a>";
			if (trollAmount > 0 && trollCost == 0)
			{
				htmlOutput += "<br><br>Trolls were free, Well Done - <a><b>" + encoder.encodeForHTML(levelSolution) + "</b></a>";
			}
		}
		catch(Exception e)
		{
			log.debug("Didn't complete order: " + e.toString());
			htmlOutput += "<p> Order Failed - Please try again later</p>";
		}
		try
		{
			Thread.sleep(1000);
		}
		catch(Exception e)
		{
			log.error("Failed to Pause: " + e.toString());
		}
		out.write(htmlOutput);
	}
	
	private static int validateAmount (int amount)
	{
		if(amount < 0)
			amount = 0;
		return amount;
	}
}