package org.insightcentre.saffron.web.swagger;

import io.swagger.jaxrs.config.BeanConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class SwaggerInitializer implements ServletContextListener {

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setVersion( "1.0.2" );
        beanConfig.setResourcePackage( "org.insightcentre.saffron.web.api" ); // replace with your packages
        beanConfig.setBasePath( "http://localhost:8080/api/" );
        beanConfig.setDescription( "My RESTful resources" );
        beanConfig.setTitle( "My RESTful API" );
        beanConfig.setScan( true );
        System.out.println("Swagger initialised");
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }

}