package filters;

import jakarta.servlet.Filter;
import java.io.IOException;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.transaction.UserTransaction;

/**
 * Created by Γεώργιος on 28/12/2015.
 */
public class ConnectionFilter implements Filter {


    @Override
    public void destroy() {
    }


@Resource
private UserTransaction utx;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            utx.begin();
            chain.doFilter(request, response);
            utx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {

    }
}