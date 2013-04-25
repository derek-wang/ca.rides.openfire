package org.jivesoftware.openfire.plugin.messageHistory.webServlet;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.plugin.messageHistory.util.DbUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * User: John Kuo
 * Date: 5/11/12
 * Time: 1:10 PM
 * This Servlet class provides HTTP GET method request to the retail site for HistoryMessage related functionalities
 */
public class MessageHistoryServlet extends HttpServlet{
    private static final String METHOD_PARAM = "method";
	private static final String PARAM_NAME = "param";

    public void init(ServletConfig servletConfig) throws ServletException
	{
		super.init(servletConfig);

		// Exclude this servlet from requiring the user to login
		AuthCheckFilter.addExclude("messageHistory/history");
	}

   protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		// get the list of params and the type list, type is always set to
		// String
		String methodName = request.getParameter(METHOD_PARAM);
		String[] paramList = request.getParameterValues(PARAM_NAME);
		int numberOfParam = 0;
		if (paramList != null)
			numberOfParam = paramList.length;

		Class<String>[] paramTypeList = new Class[numberOfParam];
		for (int i = 0; i < numberOfParam; i++)
		{
			paramTypeList[i] = String.class;
		}

		// get the method and run it
		Class<DbUtil> cls = DbUtil.class;
		DbUtil dummyObject = new DbUtil();
		Method method;
		String result;
		try
		{
			method = cls.getMethod(methodName, paramTypeList);
			result = (String) method.invoke(dummyObject, paramList);
		}
		catch (Exception e)
		{
			result = e.getClass().getName() + " " + e.getMessage();
			e.printStackTrace();
		}

		// return the result
		response.setContentType("application/json");
		response.getWriter().println(result);
		response.getWriter().flush();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		doGet(request, response);
	}

	public void destroy()
	{
		super.destroy();

		// Release the excluded URL
		AuthCheckFilter.removeExclude("messageHistory/history");
	}
}
