package filters;

import beans.*;
import java.io.IOException;
import javax.faces.application.ResourceHandler;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class RestrictPageFilter implements Filter {

    private static final Logger log = Logger.getLogger(RestrictPageFilter.class.getName()); 
    private FilterConfig config;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) resp;
        
        if (httpReq.getRequestURL().toString().contains("index.jsp")
                || (httpReq.getRequestURL().toString().contains("/login.jsf"))) {
            chain.doFilter(req, resp);
            return;
        }
       
        if (httpReq.getSession().getAttribute(LoginBean.AUTH_KEY) == null 
                &&("partial/ajax").equals(httpReq.getHeader("Faces-Request"))) {
            log.debug("No authorized access - AJAX");
            httpRes.setContentType("text/xml");
            httpRes.getWriter().append("<?xml version='1.0' encoding='UTF-8'?>")
                    .printf("<partial-response><redirect url='%s'></redirect></partial-response>", httpReq.getContextPath() + "/login.jsf");
            return;
        }
        else if (httpReq.getSession().getAttribute(LoginBean.AUTH_KEY) == null) {
            log.debug("No authorized access - FULL HTTP");
            httpRes.sendRedirect(httpReq.getContextPath() + "/login.jsf");
            return;
        } 
        else if (!httpReq.getRequestURI().startsWith(httpReq.getContextPath() + ResourceHandler.RESOURCE_IDENTIFIER)) {
            log.debug("No cache");
            httpRes.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            httpRes.setHeader("Pragma", "no-cache");
            httpRes.setDateHeader("Expires", 0);
        }
        
        chain.doFilter(httpReq, httpRes);

    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.setConfig(config);
    }

    @Override
    public void destroy() {
        setConfig(null);
    }

    /**
     * @return the config
     */
    public FilterConfig getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(FilterConfig config) {
        this.config = config;
    }
}
