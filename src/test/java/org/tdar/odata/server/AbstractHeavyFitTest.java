package org.tdar.odata.server;


import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations={"classpath:applicationContext.xml", "classpath:/org/tdar/odata/server/AbstractHeavyFitTest-context.xml"})
public abstract class AbstractHeavyFitTest extends AbstractFitTest{
	
}