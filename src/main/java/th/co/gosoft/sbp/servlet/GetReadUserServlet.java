package th.co.gosoft.sbp.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import th.co.gosoft.sbputil.PropertiesUtils;

@WebServlet("/GetReadUserServlet")
public class GetReadUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String GET_READ_USER_LIST = PropertiesUtils.getProperties("domain_get_read_user_list");
       
    public GetReadUserServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    String topicId = (String) request.getParameter("topicId");
        if(topicId == null) {
            throw new RuntimeException("topicId should not Empty!!!");
        }
        String getNopinURLString = GET_READ_USER_LIST+"?topicId="+topicId;
        
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.getWriter().print(callWebService(getNopinURLString).toString());
	}

	private StringBuilder callWebService(String urlString) {
        HttpURLConnection con = null;
        try{
            URL getNopinURL = new URL(urlString);
            con = (HttpURLConnection) getNopinURL.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            if(status == 200) {
                return parseResponseToJsonString(con);
            } 
            throw new RuntimeException("Status Code : "+status+", Internal Server Error");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
            
        }
    }

    private StringBuilder parseResponseToJsonString(HttpURLConnection con) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        return sb;
    }
}
