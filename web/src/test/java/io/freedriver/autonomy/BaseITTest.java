package io.freedriver.autonomy;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

@RunWith(Arquillian.class)
@DefaultDeployment
public abstract class BaseITTest extends BaseTest {

}
