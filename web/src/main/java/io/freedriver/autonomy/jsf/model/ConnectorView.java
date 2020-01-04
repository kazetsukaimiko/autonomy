package io.freedriver.autonomy.jsf.model;

import io.freedriver.autonomy.entity.jsonlink.BoardEntity;
import io.freedriver.autonomy.iface.Positional;
import io.freedriver.autonomy.service.ConnectorService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.stream.Collectors;

@Named
@RequestScoped
public class ConnectorView {

    @Inject
    private ConnectorService connectors;

    public List<BoardEntity> getConnectors() {
        return connectors.getWorkspace()
                .getBoards();
    }
}