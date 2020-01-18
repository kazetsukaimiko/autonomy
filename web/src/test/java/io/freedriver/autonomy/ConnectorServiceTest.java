package io.freedriver.autonomy;


import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.autonomy.service.ConnectorService;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import javax.inject.Inject;
import java.io.IOException;

//@RunWith(Arquillian.class)
//@DefaultDeployment
public class ConnectorServiceTest {
    @Inject
    private ConnectorService victim = new ConnectorService();

    public ConnectorService getVictim() {
        return victim;
    }

    @Test
    public void testGenerateFromMappings() throws IOException {
        WorkspaceEntity entity = getVictim().
                generateFromMappings();
        System.out.println(entity);
    }
}
